package com.voicetransactions.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "medical_vaccines")
public class MedicalVaccine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String vaccineName;
    private Integer doseNumber;
    private LocalDate vaccinationDate;
    private LocalDate nextDoseDate;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) private LocalDateTime recordedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist public void pre() { LocalDateTime n=LocalDateTime.now(); if(recordedAt==null)recordedAt=n; if(createdAt==null)createdAt=n; updatedAt=n; }
    @PreUpdate public void upd() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getVaccineName(){return vaccineName;} public void setVaccineName(String v){vaccineName=v;}
    public Integer getDoseNumber(){return doseNumber;} public void setDoseNumber(Integer v){doseNumber=v;}
    public LocalDate getVaccinationDate(){return vaccinationDate;} public void setVaccinationDate(LocalDate v){vaccinationDate=v;}
    public LocalDate getNextDoseDate(){return nextDoseDate;} public void setNextDoseDate(LocalDate v){nextDoseDate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public LocalDateTime getRecordedAt(){return recordedAt;} public void setRecordedAt(LocalDateTime v){recordedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}