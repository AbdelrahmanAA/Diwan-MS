package com.voicetransactions.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "medical_lab_results")
public class MedicalLabResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String testName;
    private String result;
    private String unit;
    private String referenceRange;
    private LocalDate testDate;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) private LocalDateTime recordedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist public void pre() { LocalDateTime n=LocalDateTime.now(); if(recordedAt==null)recordedAt=n; if(createdAt==null)createdAt=n; updatedAt=n; }
    @PreUpdate public void upd() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getTestName(){return testName;} public void setTestName(String v){testName=v;}
    public String getResult(){return result;} public void setResult(String v){result=v;}
    public String getUnit(){return unit;} public void setUnit(String v){unit=v;}
    public String getReferenceRange(){return referenceRange;} public void setReferenceRange(String v){referenceRange=v;}
    public LocalDate getTestDate(){return testDate;} public void setTestDate(LocalDate v){testDate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public LocalDateTime getRecordedAt(){return recordedAt;} public void setRecordedAt(LocalDateTime v){recordedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}