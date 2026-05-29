package com.voicetransactions.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "medical_chronic_diseases")
public class MedicalChronicDisease {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String diseaseName;
    private LocalDate diagnosedAt;
    @Column(columnDefinition = "TEXT") private String currentTreatment;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) private LocalDateTime recordedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist public void pre() { LocalDateTime n=LocalDateTime.now(); if(recordedAt==null)recordedAt=n; if(createdAt==null)createdAt=n; updatedAt=n; }
    @PreUpdate public void upd() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getDiseaseName(){return diseaseName;} public void setDiseaseName(String v){diseaseName=v;}
    public LocalDate getDiagnosedAt(){return diagnosedAt;} public void setDiagnosedAt(LocalDate v){diagnosedAt=v;}
    public String getCurrentTreatment(){return currentTreatment;} public void setCurrentTreatment(String v){currentTreatment=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public LocalDateTime getRecordedAt(){return recordedAt;} public void setRecordedAt(LocalDateTime v){recordedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}