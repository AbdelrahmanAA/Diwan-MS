package com.diwan.medical.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A record of any category; the category decides which fields apply (see RecordCategories). */
public class MedicalRecordRequest {

    private Long profileId;
    @NotBlank(message = "{validation.required}") private String category;
    @Size(max = 4000, message = "{validation.size}") private String notes;
    private LocalDateTime recordedAt;
    @Size(max = 255, message = "{validation.size}") private String name;
    @Size(max = 255, message = "{validation.size}") private String dosage;
    @Size(max = 255, message = "{validation.size}") private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 255, message = "{validation.size}") private String allergen;
    @Size(max = 255, message = "{validation.size}") private String severity;
    @Size(max = 2000, message = "{validation.size}") private String reaction;
    @Size(max = 255, message = "{validation.size}") private String bloodType;
    @Size(max = 255, message = "{validation.size}") private String surgeryName;
    private LocalDate surgeryDate;
    @Size(max = 255, message = "{validation.size}") private String hospital;
    @Size(max = 255, message = "{validation.size}") private String surgeon;
    @Size(max = 255, message = "{validation.size}") private String diseaseName;
    private LocalDate diagnosedAt;
    @Size(max = 2000, message = "{validation.size}") private String currentTreatment;
    @Size(max = 255, message = "{validation.size}") private String testName;
    @Size(max = 255, message = "{validation.size}") private String result;
    @Size(max = 255, message = "{validation.size}") private String unit;
    @Size(max = 255, message = "{validation.size}") private String referenceRange;
    private LocalDate testDate;
    @Size(max = 255, message = "{validation.size}") private String vaccineName;
    @Min(value = 1, message = "{validation.positive}") private Integer doseNumber;
    private LocalDate vaccinationDate;
    private LocalDate nextDoseDate;
    @Size(max = 255, message = "{validation.size}") private String condition;
    @Size(max = 255, message = "{validation.size}") private String relation;
    @Size(max = 255, message = "{validation.size}") private String contactName;
    @Size(max = 255, message = "{validation.size}") private String phone;
    @Size(max = 255, message = "{validation.size}") private String contactRelation;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long v) { profileId = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime v) { recordedAt = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDosage() { return dosage; }
    public void setDosage(String v) { dosage = v; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String v) { frequency = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { endDate = v; }
    public String getAllergen() { return allergen; }
    public void setAllergen(String v) { allergen = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { severity = v; }
    public String getReaction() { return reaction; }
    public void setReaction(String v) { reaction = v; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String v) { bloodType = v; }
    public String getSurgeryName() { return surgeryName; }
    public void setSurgeryName(String v) { surgeryName = v; }
    public LocalDate getSurgeryDate() { return surgeryDate; }
    public void setSurgeryDate(LocalDate v) { surgeryDate = v; }
    public String getHospital() { return hospital; }
    public void setHospital(String v) { hospital = v; }
    public String getSurgeon() { return surgeon; }
    public void setSurgeon(String v) { surgeon = v; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String v) { diseaseName = v; }
    public LocalDate getDiagnosedAt() { return diagnosedAt; }
    public void setDiagnosedAt(LocalDate v) { diagnosedAt = v; }
    public String getCurrentTreatment() { return currentTreatment; }
    public void setCurrentTreatment(String v) { currentTreatment = v; }
    public String getTestName() { return testName; }
    public void setTestName(String v) { testName = v; }
    public String getResult() { return result; }
    public void setResult(String v) { result = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { unit = v; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String v) { referenceRange = v; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate v) { testDate = v; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String v) { vaccineName = v; }
    public Integer getDoseNumber() { return doseNumber; }
    public void setDoseNumber(Integer v) { doseNumber = v; }
    public LocalDate getVaccinationDate() { return vaccinationDate; }
    public void setVaccinationDate(LocalDate v) { vaccinationDate = v; }
    public LocalDate getNextDoseDate() { return nextDoseDate; }
    public void setNextDoseDate(LocalDate v) { nextDoseDate = v; }
    public String getCondition() { return condition; }
    public void setCondition(String v) { condition = v; }
    public String getRelation() { return relation; }
    public void setRelation(String v) { relation = v; }
    public String getContactName() { return contactName; }
    public void setContactName(String v) { contactName = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getContactRelation() { return contactRelation; }
    public void setContactRelation(String v) { contactRelation = v; }
}
