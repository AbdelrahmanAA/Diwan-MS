package com.voicetransactions.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;   // BCrypt hashed

    /** Reset token for forgot-password flow */
    @Column(length = 64)
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                              { return id; }
    public String getFullName()                      { return fullName; }
    public void   setFullName(String v)              { this.fullName = v; }
    public String getEmail()                         { return email; }
    public void   setEmail(String v)                 { this.email = v; }
    public String getPassword()                      { return password; }
    public void   setPassword(String v)              { this.password = v; }
    public String getResetToken()                    { return resetToken; }
    public void   setResetToken(String v)            { this.resetToken = v; }
    public LocalDateTime getResetTokenExpiresAt()    { return resetTokenExpiresAt; }
    public void   setResetTokenExpiresAt(LocalDateTime v) { this.resetTokenExpiresAt = v; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public LocalDateTime getUpdatedAt()              { return updatedAt; }
}
