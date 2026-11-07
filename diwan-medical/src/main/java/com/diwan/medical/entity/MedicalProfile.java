package com.diwan.medical.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import com.diwan.medical.domain.ProfileRelation;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A person whose records an account keeps: the account holder (SELF) and family members. */
@Entity
@Table(name = "medical_profile")
public class MedicalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ProfileRelation relation;

    private LocalDate birthDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public ProfileRelation getRelation() { return relation; }
    public void setRelation(ProfileRelation v) { relation = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { birthDate = v; }
}
