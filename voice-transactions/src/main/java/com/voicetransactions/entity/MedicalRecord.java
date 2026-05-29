package com.voicetransactions.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_records", indexes = {
    @Index(name = "idx_med_user", columnList = "userId"),
    @Index(name = "idx_med_cat", columnList = "category")
})
public class MedicalRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 50) private String category;
    @Column(nullable = false, length = 300) private String title;
    @Column(columnDefinition = "TEXT") private String details;
    @Column(nullable = false) private LocalDateTime recordedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (recordedAt == null) recordedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { this.userId = v; }
    public String getCategory() { return category; } public void setCategory(String v) { this.category = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getDetails() { return details; } public void setDetails(String v) { this.details = v; }
    public LocalDateTime getRecordedAt() { return recordedAt; } public void setRecordedAt(LocalDateTime v) { this.recordedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}