package com.diwan.portfolio.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Per-user defaults: language for alerts/notifications and the base currency of new portfolios. */
@Entity
@Table(name = "user_preference")
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreference() {}

    public UserPreference(Long userId, String language, String baseCurrency) {
        this.userId = userId;
        this.language = language;
        this.baseCurrency = baseCurrency;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
}
