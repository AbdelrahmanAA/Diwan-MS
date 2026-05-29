package com.voicetransactions.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "medical_family_history")
public class MedicalFamilyHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private String condition;
    private String relation;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) private LocalDateTime recordedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist public void pre() { LocalDateTime n=LocalDateTime.now(); if(recordedAt==null)recordedAt=n; if(createdAt==null)createdAt=n; updatedAt=n; }
    @PreUpdate public void upd() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getCondition(){return condition;} public void setCondition(String v){condition=v;}
    public String getRelation(){return relation;} public void setRelation(String v){relation=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public LocalDateTime getRecordedAt(){return recordedAt;} public void setRecordedAt(LocalDateTime v){recordedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}