package com.diwan.medical.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Who changed which record and when. Holds no medical content. */
@Entity
@Table(name = "medical_audit")
public class MedicalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long profileId;

    @Column(nullable = false, length = 10)
    private String action;

    @Column(nullable = false, length = 32)
    private String category;

    private Long recordId;

    @Column(length = 64)
    private String requestId;

    @Column(nullable = false)
    private Instant createdAt;

    protected MedicalAudit() {}

    public MedicalAudit(Long userId, Long profileId, String action, String category, Long recordId, String requestId,
                        Instant createdAt) {
        this.userId = userId;
        this.profileId = profileId;
        this.action = action;
        this.category = category;
        this.recordId = recordId;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public String getAction() { return action; }
    public String getCategory() { return category; }
    public Long getRecordId() { return recordId; }
}
