package com.diwan.medical;

import com.diwan.common.security.GatewaySigner;
import com.diwan.medical.repository.MedicalAuditRepository;
import com.diwan.medical.service.UserDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole service against a real MySQL, with gateway-signed identity headers. */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@ActiveProfiles("DEV")
@Testcontainers
class MedicalApiIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    private static final GatewaySigner SIGNER =
            GatewaySigner.fromSecret("dev-only-jwt-secret-do-not-use-outside-dev-0123456789");
    private static final AtomicLong USERS = new AtomicLong(1000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserDataService userData;
    @Autowired MedicalAuditRepository audit;

    private long user;

    @BeforeEach
    void newUser() {
        user = USERS.incrementAndGet();
    }

    private ResultActions call(MockHttpServletRequestBuilder req, Object body, long userId, String lang) throws Exception {
        long ts = System.currentTimeMillis();
        String id = String.valueOf(userId);
        MockHttpServletRequestBuilder r = req.header("X-User-Id", id).header("X-User-Email", "u" + id + "@test")
                .header("X-User-Role", "USER").header("X-Gateway-Timestamp", ts)
                .header("X-Gateway-Signature", SIGNER.sign(id, "u" + id + "@test", "USER", ts))
                .header("Accept-Language", lang);
        if (body != null) {
            r = r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(r);
    }

    private ResultActions call(MockHttpServletRequestBuilder req, Object body) throws Exception {
        return call(req, body, user, "en");
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static Map<String, Object> record(String category, Object... fields) {
        Map<String, Object> m = new HashMap<>();
        m.put("category", category);
        for (int i = 0; i < fields.length; i += 2) {
            m.put((String) fields[i], fields[i + 1]);
        }
        return m;
    }

    private JsonNode add(Map<String, Object> record) throws Exception {
        return body(call(post("/api/medical"), record).andExpect(status().isOk()));
    }

    private long addPerson(String name, String relation) throws Exception {
        return body(call(post("/api/medical/profiles"), Map.of("name", name, "relation", relation))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    @Test
    void anAccountHasItsOwnProfileFromTheFirstRequest() throws Exception {
        JsonNode profiles = body(call(get("/api/medical/profiles"), null).andExpect(status().isOk()));

        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).get("self").asBoolean()).isTrue();
        assertThat(profiles.get(0).get("relation").asText()).isEqualTo("SELF");
    }

    @Test
    void recordsAreKeptPerPerson() throws Exception {
        long child = addPerson("Sara", "CHILD");
        add(record("allergies", "allergen", "Penicillin"));
        add(record("allergies", "allergen", "Peanuts", "profileId", child));

        assertThat(body(call(get("/api/medical?category=allergies"), null)).findValuesAsText("allergen"))
                .containsExactly("Penicillin");
        assertThat(body(call(get("/api/medical?category=allergies&profileId=" + child), null)).findValuesAsText("allergen"))
                .containsExactly("Peanuts");
    }

    @Test
    void anotherAccountCannotReachAPersonOrRecord() throws Exception {
        long child = addPerson("Sara", "CHILD");
        long id = add(record("medications", "name", "Metformin", "profileId", child)).get("id").asLong();
        long stranger = USERS.incrementAndGet();

        call(get("/api/medical?profileId=" + child), null, stranger, "en").andExpect(status().isNotFound());
        call(get("/api/medical/overview?profileId=" + child), null, stranger, "en").andExpect(status().isNotFound());
        call(post("/api/medical"), record("medications", "name", "X", "profileId", child), stranger, "en")
                .andExpect(status().isNotFound());
        call(put("/api/medical/" + id), record("medications", "name", "Hacked"), stranger, "en")
                .andExpect(status().isNotFound());
        call(delete("/api/medical/" + id + "?category=medications"), null, stranger, "en").andExpect(status().isNotFound());
        call(delete("/api/medical/profiles/" + child), null, stranger, "en").andExpect(status().isNotFound());

        assertThat(body(call(get("/api/medical?category=medications&profileId=" + child), null)).findValuesAsText("name"))
                .containsExactly("Metformin");
    }

    @Test
    void medicalTextIsStoredEncrypted() throws Exception {
        long id = add(record("medications", "name", "Metformin", "dosage", "500mg", "notes", "With food")).get("id").asLong();

        Map<String, Object> row = jdbc.queryForMap("select name, dosage, notes from medical_medications where id = ?", id);

        assertThat(row.values()).allSatisfy(v -> assertThat(v.toString()).startsWith("enc:"));
    }

    @Test
    void personNamesAndContactsAreStoredEncrypted() throws Exception {
        long child = addPerson("Sara", "CHILD");
        long contact = add(record("emergencyContacts", "contactName", "Omar", "phone", "+966500000000")).get("id").asLong();

        assertThat(jdbc.queryForObject("select name from medical_profile where id = ?", String.class, child)).startsWith("enc:");
        assertThat(jdbc.queryForObject("select phone from medical_emergency_contacts where id = ?", String.class, contact))
                .startsWith("enc:");
    }

    @Test
    void anEditCanClearAField() throws Exception {
        long id = add(record("medications", "name", "Metformin", "endDate", "2030-01-01")).get("id").asLong();

        JsonNode updated = body(call(put("/api/medical/" + id), record("medications", "name", "Metformin"))
                .andExpect(status().isOk()));

        assertThat(updated.get("endDate").isNull()).isTrue();
    }

    @Test
    void aPersonHasOneBloodType() throws Exception {
        add(record("bloodType", "bloodType", "O+"));
        add(record("bloodType", "bloodType", "a-"));

        JsonNode list = body(call(get("/api/medical?category=bloodType"), null));

        assertThat(list.findValuesAsText("bloodType")).containsExactly("A-");
    }

    @Test
    void invalidInputIsRefusedInTheUsersLanguage() throws Exception {
        call(post("/api/medical"), record("allergies", "allergen", "Dust", "severity", "Terrible"), user, "ar")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("medical.severity.invalid"))
                .andExpect(jsonPath("$.message").value("اختر درجة الشدة من القائمة"));
        call(post("/api/medical"), record("bloodType", "bloodType", "Z"), user, "en").andExpect(status().isBadRequest());
        call(post("/api/medical"), record("medications"), user, "en").andExpect(status().isBadRequest());
        call(post("/api/medical"), record("nothing"), user, "en")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("medical.category.unknown"));
        call(post("/api/medical"), record("medications", "name", "X", "startDate", "2026-02-01", "endDate", "2026-01-01"),
                user, "en").andExpect(jsonPath("$.code").value("medical.dates.order"));
        call(post("/api/medical"), record("emergencyContacts", "contactName", "A", "phone", "call me"), user, "en")
                .andExpect(jsonPath("$.code").value("medical.phone.invalid"));
    }

    @Test
    void overviewShowsWhatMattersNow() throws Exception {
        LocalDate today = LocalDate.now();
        add(record("bloodType", "bloodType", "B+"));
        add(record("allergies", "allergen", "Dust", "severity", "MILD"));
        add(record("allergies", "allergen", "Penicillin", "severity", "LIFE_THREATENING"));
        add(record("allergies", "allergen", "Latex", "severity", "SEVERE"));
        add(record("medications", "name", "Current"));
        add(record("medications", "name", "Finished", "endDate", today.minusDays(1).toString()));
        add(record("vaccines", "vaccineName", "Later", "nextDoseDate", today.plusDays(30).toString()));
        add(record("vaccines", "vaccineName", "Soon", "nextDoseDate", today.plusDays(3).toString()));
        add(record("vaccines", "vaccineName", "Missed", "nextDoseDate", today.minusDays(3).toString()));

        JsonNode o = body(call(get("/api/medical/overview"), null).andExpect(status().isOk()));

        assertThat(o.get("bloodType").asText()).isEqualTo("B+");
        assertThat(o.get("allergies").findValuesAsText("allergen")).containsExactly("Penicillin", "Latex", "Dust");
        assertThat(o.get("currentMedications").findValuesAsText("name")).containsExactly("Current");
        assertThat(o.get("upcoming").findValuesAsText("vaccineName")).containsExactly("Soon", "Later");
        assertThat(o.get("counts").get("medications").asInt()).isEqualTo(2);
    }

    @Test
    void timelineIsNewestFirstAndSearchable() throws Exception {
        add(record("surgeries", "surgeryName", "Appendectomy", "surgeryDate", "2020-05-01"));
        add(record("labResults", "testName", "HbA1c", "testDate", "2024-01-15", "notes", "Fasting"));
        add(record("vaccines", "vaccineName", "Influenza", "vaccinationDate", "2022-10-01"));
        add(record("emergencyContacts", "contactName", "Omar", "phone", "+966500000000"));

        JsonNode all = body(call(get("/api/medical/timeline"), null).andExpect(status().isOk()));
        assertThat(all.get("items").findValuesAsText("title")).containsExactly("HbA1c", "Influenza", "Appendectomy");
        assertThat(all.get("totalItems").asInt()).isEqualTo(3);

        JsonNode searched = body(call(get("/api/medical/timeline?q=fast"), null));
        assertThat(searched.get("items").findValuesAsText("title")).containsExactly("HbA1c");

        JsonNode second = body(call(get("/api/medical/timeline?size=2&page=1"), null));
        assertThat(second.get("items").findValuesAsText("title")).containsExactly("Appendectomy");
        assertThat(second.get("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void removingAPersonRemovesTheirRecordsButNotYours() throws Exception {
        long child = addPerson("Sara", "CHILD");
        add(record("allergies", "allergen", "Peanuts", "profileId", child));
        add(record("allergies", "allergen", "Dust"));

        call(delete("/api/medical/profiles/" + child), null).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from medical_allergies where user_id = ?", Integer.class, user)).isEqualTo(1);
    }

    @Test
    void theAccountHoldersProfileCannotBeRemovedOrReassigned() throws Exception {
        long self = body(call(get("/api/medical/profiles"), null)).get(0).get("id").asLong();

        call(delete("/api/medical/profiles/" + self), null).andExpect(status().isBadRequest());
        call(put("/api/medical/profiles/" + self), Map.of("name", "X", "relation", "CHILD"))
                .andExpect(status().isBadRequest());
        call(post("/api/medical/profiles"), Map.of("name", "X", "relation", "SELF")).andExpect(status().isBadRequest());
        call(put("/api/medical/profiles/" + self), Map.of("name", "Abdo", "relation", "SELF")).andExpect(status().isOk());
    }

    @Test
    void deletingAnAccountErasesEverythingOfItAndNothingElse() throws Exception {
        long other = USERS.incrementAndGet();
        call(post("/api/medical"), record("allergies", "allergen", "Keep me"), other, "en").andExpect(status().isOk());
        long child = addPerson("Sara", "CHILD");
        add(record("allergies", "allergen", "Peanuts", "profileId", child));
        add(record("emergencyContacts", "contactName", "Omar", "phone", "+966500000000"));

        userData.deleteAll(user);
        userData.deleteAll(user); // delivered twice

        for (String table : List.of("medical_profile", "medical_allergies", "medical_emergency_contacts", "medical_audit")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table + " where user_id = ?", Integer.class, user))
                    .as(table).isZero();
        }
        assertThat(jdbc.queryForObject("select count(*) from medical_allergies where user_id = ?", Integer.class, other)).isEqualTo(1);
    }

    @Test
    void changesAreAuditedWithoutMedicalContent() throws Exception {
        long id = add(record("medications", "name", "Metformin")).get("id").asLong();
        call(put("/api/medical/" + id), record("medications", "name", "Metformin XR")).andExpect(status().isOk());
        call(delete("/api/medical/" + id + "?category=medications"), null).andExpect(status().isNoContent());

        assertThat(audit.findByUserIdOrderByIdAsc(user)).extracting(a -> a.getAction() + ":" + a.getCategory() + ":" + a.getRecordId())
                .containsExactly("CREATE:medications:" + id, "UPDATE:medications:" + id, "DELETE:medications:" + id);
    }

    @Test
    void doseTimesAreKeptSortedAndValidated() throws Exception {
        JsonNode m = add(record("medications", "name", "Metformin", "times", List.of("20:00", "08:00", "08:00"), "reminders", true));
        assertThat(m.get("times")).extracting(JsonNode::asText).containsExactly("08:00", "20:00");
        assertThat(m.get("reminders").asBoolean()).isTrue();

        call(post("/api/medical"), record("medications", "name", "X", "times", List.of("25:00")))
                .andExpect(jsonPath("$.code").value("medical.times.invalid"));
        call(post("/api/medical"), record("medications", "name", "X", "reminders", true))
                .andExpect(jsonPath("$.code").value("medical.reminders.need_times"));
    }

    @Test
    void theReminderPlanCoversEveryPersonAndOnlyWhatIsDue() throws Exception {
        long child = addPerson("Sara", "CHILD");
        LocalDate today = LocalDate.now();
        add(record("medications", "name", "Mine", "times", List.of("08:00"), "reminders", true));
        add(record("medications", "name", "Hers", "times", List.of("09:00"), "reminders", true, "profileId", child));
        add(record("medications", "name", "Silent", "times", List.of("10:00")));
        add(record("medications", "name", "Ended", "times", List.of("11:00"), "reminders", true,
                "endDate", today.minusDays(1).toString()));
        add(record("appointments", "appointmentWith", "Dr. Ahmed", "appointmentAt", today.plusDays(2) + "T10:30"));
        add(record("appointments", "appointmentWith", "Past", "appointmentAt", today.minusDays(2) + "T10:30"));

        JsonNode plan = body(call(get("/api/medical/reminders"), null).andExpect(status().isOk()));

        assertThat(plan.get("medications").findValuesAsText("name")).containsExactly("Mine", "Hers");
        assertThat(plan.get("medications").get(1).get("person").asText()).isEqualTo("Sara");
        assertThat(plan.get("appointments").findValuesAsText("with")).containsExactly("Dr. Ahmed");
        assertThat(jdbc.queryForObject("select appointment_with from medical_appointments where user_id = ? limit 1",
                String.class, user)).startsWith("enc:");
    }

    @Test
    void anAppointmentIsUpcomingInTheOverview() throws Exception {
        LocalDate today = LocalDate.now();
        add(record("vaccines", "vaccineName", "Flu", "nextDoseDate", today.plusDays(10).toString()));
        add(record("appointments", "appointmentWith", "Dentist", "appointmentAt", today.plusDays(1) + "T09:00"));

        JsonNode o = body(call(get("/api/medical/overview"), null));

        assertThat(o.get("upcoming").findValuesAsText("title")).containsExactly("Dentist", "Flu");
    }

    @Test
    void aDoseCanBeMarkedAndChangedButOnlyByTheOwner() throws Exception {
        long id = add(record("medications", "name", "Metformin", "times", List.of("08:00"))).get("id").asLong();
        String at = LocalDate.now() + "T08:00";

        call(post("/api/medical/doses"), Map.of("medicationId", id, "scheduledAt", at, "status", "SKIPPED")).andExpect(status().isOk());
        call(post("/api/medical/doses"), Map.of("medicationId", id, "scheduledAt", at, "status", "taken")).andExpect(status().isOk());
        call(post("/api/medical/doses"), Map.of("medicationId", id, "scheduledAt", at, "status", "MAYBE"))
                .andExpect(jsonPath("$.code").value("medical.dose.status_invalid"));
        call(post("/api/medical/doses"), Map.of("medicationId", id, "scheduledAt", at, "status", "TAKEN"),
                USERS.incrementAndGet(), "en").andExpect(status().isNotFound());

        JsonNode today = body(call(get("/api/medical/doses?date=" + LocalDate.now()), null).andExpect(status().isOk()));
        assertThat(today.findValuesAsText("status")).containsExactly("TAKEN");

        call(delete("/api/medical/" + id + "?category=medications"), null).andExpect(status().isNoContent());
        assertThat(body(call(get("/api/medical/doses?date=" + LocalDate.now()), null))).isEmpty();
    }

    @Test
    void requestsWithoutAGatewaySignatureAreRejected() throws Exception {
        mvc.perform(get("/api/medical").header("X-User-Id", String.valueOf(user))).andExpect(status().isUnauthorized());
    }
}
