package com.voicetransactions.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicalRecordRequest {
    // Common
    private Long userId;
    private String category; // medications|allergies|bloodType|surgeries|chronicDiseases|labResults|vaccines|familyHistory
    private String notes;
    private LocalDateTime recordedAt;

    // medications
    private String name;
    private String dosage;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;

    // allergies
    private String allergen;
    private String severity;
    private String reaction;

    // bloodType
    private String bloodType;

    // surgeries
    private String surgeryName;
    private LocalDate surgeryDate;
    private String hospital;
    private String surgeon;

    // chronicDiseases
    private String diseaseName;
    private LocalDate diagnosedAt;
    private String currentTreatment;

    // labResults
    private String testName;
    private String result;
    private String unit;
    private String referenceRange;
    private LocalDate testDate;

    // vaccines
    private String vaccineName;
    private Integer doseNumber;
    private LocalDate vaccinationDate;
    private LocalDate nextDoseDate;

    // familyHistory
    private String condition;
    private String relation;

    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public LocalDateTime getRecordedAt(){return recordedAt;} public void setRecordedAt(LocalDateTime v){recordedAt=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDosage(){return dosage;} public void setDosage(String v){dosage=v;}
    public String getFrequency(){return frequency;} public void setFrequency(String v){frequency=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public String getAllergen(){return allergen;} public void setAllergen(String v){allergen=v;}
    public String getSeverity(){return severity;} public void setSeverity(String v){severity=v;}
    public String getReaction(){return reaction;} public void setReaction(String v){reaction=v;}
    public String getBloodType(){return bloodType;} public void setBloodType(String v){bloodType=v;}
    public String getSurgeryName(){return surgeryName;} public void setSurgeryName(String v){surgeryName=v;}
    public LocalDate getSurgeryDate(){return surgeryDate;} public void setSurgeryDate(LocalDate v){surgeryDate=v;}
    public String getHospital(){return hospital;} public void setHospital(String v){hospital=v;}
    public String getSurgeon(){return surgeon;} public void setSurgeon(String v){surgeon=v;}
    public String getDiseaseName(){return diseaseName;} public void setDiseaseName(String v){diseaseName=v;}
    public LocalDate getDiagnosedAt(){return diagnosedAt;} public void setDiagnosedAt(LocalDate v){diagnosedAt=v;}
    public String getCurrentTreatment(){return currentTreatment;} public void setCurrentTreatment(String v){currentTreatment=v;}
    public String getTestName(){return testName;} public void setTestName(String v){testName=v;}
    public String getResult(){return result;} public void setResult(String v){result=v;}
    public String getUnit(){return unit;} public void setUnit(String v){unit=v;}
    public String getReferenceRange(){return referenceRange;} public void setReferenceRange(String v){referenceRange=v;}
    public LocalDate getTestDate(){return testDate;} public void setTestDate(LocalDate v){testDate=v;}
    public String getVaccineName(){return vaccineName;} public void setVaccineName(String v){vaccineName=v;}
    public Integer getDoseNumber(){return doseNumber;} public void setDoseNumber(Integer v){doseNumber=v;}
    public LocalDate getVaccinationDate(){return vaccinationDate;} public void setVaccinationDate(LocalDate v){vaccinationDate=v;}
    public LocalDate getNextDoseDate(){return nextDoseDate;} public void setNextDoseDate(LocalDate v){nextDoseDate=v;}
    public String getCondition(){return condition;} public void setCondition(String v){condition=v;}
    public String getRelation(){return relation;} public void setRelation(String v){relation=v;}
}