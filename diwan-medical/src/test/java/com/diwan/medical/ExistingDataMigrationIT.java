package com.diwan.medical;

import com.diwan.common.crypto.FieldEncryptor;
import com.diwan.medical.service.LegacyDataEncryptor;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/** What happens to the data of a live database: from the v1 schema with plain text to profiles and ciphertext. */
@Testcontainers
class ExistingDataMigrationIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    private static final String NOW = "'2026-01-01 10:00:00'";
    private static final FieldEncryptor ENCRYPTOR =
            FieldEncryptor.fromSpec("t1:" + Base64.getEncoder().encodeToString(new byte[32]));
    private static JdbcTemplate jdbc;

    private static Flyway flyway(String target) {
        return Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration").target(target).load();
    }

    @BeforeAll
    static void migrateAnExistingDatabase() throws Exception {
        flyway("1").migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        jdbc.update("insert into medical_medications (user_id, name, dosage, notes, recorded_at, created_at, updated_at) values "
                + "(1, 'Metformin', '500mg', 'مع الطعام', " + NOW + ", " + NOW + ", " + NOW + ")");
        for (String severity : new String[]{"High", "mild", "very bad"}) {
            jdbc.update("insert into medical_allergies (user_id, allergen, severity, notes, recorded_at, created_at, updated_at) values "
                    + "(1, ?, ?, 'old note', " + NOW + ", " + NOW + ", " + NOW + ")", "Allergen " + severity, severity);
        }
        jdbc.update("insert into medical_blood_type (user_id, blood_type, recorded_at, created_at, updated_at) values "
                + "(2, 'O+', " + NOW + ", " + NOW + ", " + NOW + ")");

        flyway("latest").migrate();
        new LegacyDataEncryptor(jdbc, ENCRYPTOR).run(new DefaultApplicationArguments());
    }

    @Test
    void everyAccountGetsOneOwnProfileAndKeepsItsRecords() {
        assertThat(jdbc.queryForObject("select count(*) from medical_profile where relation = 'SELF'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                select count(*) from medical_medications m join medical_profile p on p.id = m.profile_id
                where p.user_id = 1 and m.user_id = 1""", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from medical_blood_type b join medical_profile p on p.id = b.profile_id
                where p.user_id = 2""", Integer.class)).isEqualTo(1);
    }

    @Test
    void existingTextIsEncryptedAndReadsBackUnchanged() {
        var row = jdbc.queryForMap("select name, dosage, notes from medical_medications");

        assertThat(row.values()).allSatisfy(v -> assertThat(v.toString()).startsWith("enc:"));
        assertThat(ENCRYPTOR.decrypt(row.get("name").toString())).isEqualTo("Metformin");
        assertThat(ENCRYPTOR.decrypt(row.get("notes").toString())).isEqualTo("مع الطعام");
        assertThat(ENCRYPTOR.decrypt(jdbc.queryForObject("select blood_type from medical_blood_type", String.class))).isEqualTo("O+");
    }

    @Test
    void severitiesBecomeCodesAndNothingIsLost() {
        assertThat(jdbc.queryForList("select severity from medical_allergies order by id", String.class))
                .containsExactly("SEVERE", "MILD", null);
        String notes = jdbc.queryForObject("select notes from medical_allergies where severity is null", String.class);
        assertThat(ENCRYPTOR.decrypt(notes)).isEqualTo("old note\nvery bad");
    }

    @Test
    void runningAgainChangesNothing() throws Exception {
        String before = jdbc.queryForObject("select name from medical_medications", String.class);

        new LegacyDataEncryptor(jdbc, ENCRYPTOR).run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("select name from medical_medications", String.class)).isEqualTo(before);
    }
}
