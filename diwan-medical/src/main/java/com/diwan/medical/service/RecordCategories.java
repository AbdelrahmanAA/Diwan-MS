package com.diwan.medical.service;

import com.diwan.common.web.ApiException;
import com.diwan.medical.domain.Severity;
import com.diwan.medical.dto.MedicalRecordRequest;
import com.diwan.medical.entity.*;
import com.diwan.medical.repository.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** The record categories: their fields, rules and storage, in one place. */
@Component
public class RecordCategories {

    public static final String MEDICATIONS = "medications";
    public static final String ALLERGIES = "allergies";
    public static final String BLOOD_TYPE = "bloodType";
    public static final String CHRONIC_DISEASES = "chronicDiseases";
    public static final String VACCINES = "vaccines";
    public static final String EMERGENCY_CONTACTS = "emergencyContacts";
    public static final String APPOINTMENTS = "appointments";
    static final int MAX_DOSE_TIMES = 8;

    private static final Set<String> BLOOD_TYPES = Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final Pattern PHONE = Pattern.compile("[+0-9 ()-]{5,20}");
    private static final Pattern TIME = Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]");

    private final Map<String, RecordCategory<?>> byCode = new LinkedHashMap<>();

    public RecordCategories(MedicalMedicationRepository medications, MedicalAllergyRepository allergies,
                            MedicalBloodTypeRepository bloodTypes, MedicalSurgeryRepository surgeries,
                            MedicalChronicDiseaseRepository chronicDiseases, MedicalLabResultRepository labResults,
                            MedicalVaccineRepository vaccines, MedicalFamilyHistoryRepository familyHistory,
                            MedicalEmergencyContactRepository emergencyContacts,
                            MedicalAppointmentRepository appointments) {
        add(new RecordCategory<>(MEDICATIONS, medications, MedicalMedication::new, false, true,
                MedicalMedication::getName, MedicalMedication::getStartDate,
                r -> {
                    required(r.getName());
                    inOrder(r.getStartDate(), r.getEndDate());
                    doseTimes(r.getTimes());
                    if (Boolean.TRUE.equals(r.getReminders()) && doseTimes(r.getTimes()) == null) {
                        throw ApiException.badRequest("medical.reminders.need_times");
                    }
                },
                (r, e) -> {
                    e.setName(clean(r.getName()));
                    e.setDosage(clean(r.getDosage()));
                    e.setFrequency(clean(r.getFrequency()));
                    e.setStartDate(r.getStartDate());
                    e.setEndDate(r.getEndDate());
                    e.setScheduleTimes(doseTimes(r.getTimes()));
                    e.setReminders(Boolean.TRUE.equals(r.getReminders()));
                },
                (e, v) -> {
                    v.setName(e.getName());
                    v.setDosage(e.getDosage());
                    v.setFrequency(e.getFrequency());
                    v.setStartDate(e.getStartDate());
                    v.setEndDate(e.getEndDate());
                    v.setTimes(timesOf(e.getScheduleTimes()));
                    v.setReminders(e.isReminders());
                }));
        add(new RecordCategory<>(ALLERGIES, allergies, MedicalAllergy::new, false, true,
                MedicalAllergy::getAllergen, e -> null,
                r -> {
                    required(r.getAllergen());
                    Severity.normalize(r.getSeverity());
                },
                (r, e) -> {
                    e.setAllergen(clean(r.getAllergen()));
                    e.setSeverity(Severity.normalize(r.getSeverity()));
                    e.setReaction(clean(r.getReaction()));
                },
                (e, v) -> {
                    v.setAllergen(e.getAllergen());
                    v.setSeverity(e.getSeverity());
                    v.setReaction(e.getReaction());
                }));
        add(new RecordCategory<>(BLOOD_TYPE, bloodTypes, MedicalBloodType::new, true, false,
                MedicalBloodType::getBloodType, e -> null,
                r -> {
                    if (r.getBloodType() == null || !BLOOD_TYPES.contains(r.getBloodType().trim().toUpperCase())) {
                        throw ApiException.badRequest("medical.blood_type.invalid");
                    }
                },
                (r, e) -> e.setBloodType(r.getBloodType().trim().toUpperCase()),
                (e, v) -> v.setBloodType(e.getBloodType())));
        add(new RecordCategory<>("surgeries", surgeries, MedicalSurgery::new, false, true,
                MedicalSurgery::getSurgeryName, MedicalSurgery::getSurgeryDate,
                r -> required(r.getSurgeryName()),
                (r, e) -> {
                    e.setSurgeryName(clean(r.getSurgeryName()));
                    e.setSurgeryDate(r.getSurgeryDate());
                    e.setHospital(clean(r.getHospital()));
                    e.setSurgeon(clean(r.getSurgeon()));
                },
                (e, v) -> {
                    v.setSurgeryName(e.getSurgeryName());
                    v.setSurgeryDate(e.getSurgeryDate());
                    v.setHospital(e.getHospital());
                    v.setSurgeon(e.getSurgeon());
                }));
        add(new RecordCategory<>(CHRONIC_DISEASES, chronicDiseases, MedicalChronicDisease::new, false, true,
                MedicalChronicDisease::getDiseaseName, MedicalChronicDisease::getDiagnosedAt,
                r -> required(r.getDiseaseName()),
                (r, e) -> {
                    e.setDiseaseName(clean(r.getDiseaseName()));
                    e.setDiagnosedAt(r.getDiagnosedAt());
                    e.setCurrentTreatment(clean(r.getCurrentTreatment()));
                },
                (e, v) -> {
                    v.setDiseaseName(e.getDiseaseName());
                    v.setDiagnosedAt(e.getDiagnosedAt());
                    v.setCurrentTreatment(e.getCurrentTreatment());
                }));
        add(new RecordCategory<>("labResults", labResults, MedicalLabResult::new, false, true,
                MedicalLabResult::getTestName, MedicalLabResult::getTestDate,
                r -> required(r.getTestName()),
                (r, e) -> {
                    e.setTestName(clean(r.getTestName()));
                    e.setResult(clean(r.getResult()));
                    e.setUnit(clean(r.getUnit()));
                    e.setReferenceRange(clean(r.getReferenceRange()));
                    e.setTestDate(r.getTestDate());
                },
                (e, v) -> {
                    v.setTestName(e.getTestName());
                    v.setResult(e.getResult());
                    v.setUnit(e.getUnit());
                    v.setReferenceRange(e.getReferenceRange());
                    v.setTestDate(e.getTestDate());
                }));
        add(new RecordCategory<>(VACCINES, vaccines, MedicalVaccine::new, false, true,
                MedicalVaccine::getVaccineName, MedicalVaccine::getVaccinationDate,
                r -> {
                    required(r.getVaccineName());
                    inOrder(r.getVaccinationDate(), r.getNextDoseDate());
                },
                (r, e) -> {
                    e.setVaccineName(clean(r.getVaccineName()));
                    e.setDoseNumber(r.getDoseNumber());
                    e.setVaccinationDate(r.getVaccinationDate());
                    e.setNextDoseDate(r.getNextDoseDate());
                },
                (e, v) -> {
                    v.setVaccineName(e.getVaccineName());
                    v.setDoseNumber(e.getDoseNumber());
                    v.setVaccinationDate(e.getVaccinationDate());
                    v.setNextDoseDate(e.getNextDoseDate());
                }));
        add(new RecordCategory<>("familyHistory", familyHistory, MedicalFamilyHistory::new, false, false,
                MedicalFamilyHistory::getCondition, e -> null,
                r -> required(r.getCondition()),
                (r, e) -> {
                    e.setCondition(clean(r.getCondition()));
                    e.setRelation(clean(r.getRelation()));
                },
                (e, v) -> {
                    v.setCondition(e.getCondition());
                    v.setRelation(e.getRelation());
                }));
        add(new RecordCategory<>(EMERGENCY_CONTACTS, emergencyContacts, MedicalEmergencyContact::new, false, false,
                MedicalEmergencyContact::getContactName, e -> null,
                r -> {
                    required(r.getContactName());
                    required(r.getPhone());
                    if (!PHONE.matcher(r.getPhone().trim()).matches()) {
                        throw ApiException.badRequest("medical.phone.invalid");
                    }
                },
                (r, e) -> {
                    e.setContactName(clean(r.getContactName()));
                    e.setPhone(clean(r.getPhone()));
                    e.setContactRelation(clean(r.getContactRelation()));
                },
                (e, v) -> {
                    v.setContactName(e.getContactName());
                    v.setPhone(e.getPhone());
                    v.setContactRelation(e.getContactRelation());
                }));
        add(new RecordCategory<>(APPOINTMENTS, appointments, MedicalAppointment::new, false, true,
                MedicalAppointment::getAppointmentWith, e -> e.getAppointmentAt().toLocalDate(),
                r -> {
                    required(r.getAppointmentWith());
                    if (r.getAppointmentAt() == null) {
                        throw ApiException.badRequest("validation.required");
                    }
                },
                (r, e) -> {
                    e.setAppointmentWith(clean(r.getAppointmentWith()));
                    e.setAppointmentAt(r.getAppointmentAt().withSecond(0).withNano(0));
                    e.setLocation(clean(r.getLocation()));
                },
                (e, v) -> {
                    v.setAppointmentWith(e.getAppointmentWith());
                    v.setAppointmentAt(e.getAppointmentAt());
                    v.setLocation(e.getLocation());
                }));
    }

    /** The stored form of dose times: distinct "HH:mm", earliest first, comma separated; null when none. */
    static String doseTimes(List<String> times) {
        if (times == null || times.isEmpty()) {
            return null;
        }
        if (times.size() > MAX_DOSE_TIMES || times.stream().anyMatch(t -> t == null || !TIME.matcher(t.trim()).matches())) {
            throw ApiException.badRequest("medical.times.invalid", MAX_DOSE_TIMES);
        }
        return String.join(",", times.stream().map(String::trim).distinct().sorted().toList());
    }

    public static List<String> timesOf(String stored) {
        return stored == null || stored.isBlank() ? List.of() : List.of(stored.split(","));
    }

    private void add(RecordCategory<?> category) {
        byCode.put(category.code(), category);
    }

    public Collection<RecordCategory<?>> all() {
        return byCode.values();
    }

    public RecordCategory<?> require(String code) {
        RecordCategory<?> category = byCode.get(code);
        if (category == null) {
            throw ApiException.badRequest("medical.category.unknown", code);
        }
        return category;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void required(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("validation.required");
        }
    }

    private static void inOrder(LocalDate first, LocalDate second) {
        if (first != null && second != null && second.isBefore(first)) {
            throw ApiException.badRequest("medical.dates.order");
        }
    }
}
