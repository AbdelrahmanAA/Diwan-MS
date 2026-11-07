package com.diwan.medical.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;

/** Whether a scheduled dose was taken or skipped. One row per medication and dose time. */
@Entity
@Table(name = "medical_dose_log")
public class MedicalDose {

    public enum Status { TAKEN, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long profileId;

    @Column(nullable = false)
    private Long medicationId;

    /** The dose time it answers, at the phone's local time */
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(nullable = false)
    private Instant recordedAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long v) { profileId = v; }
    public Long getMedicationId() { return medicationId; }
    public void setMedicationId(Long v) { medicationId = v; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime v) { scheduledAt = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { status = v; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant v) { recordedAt = v; }
}
