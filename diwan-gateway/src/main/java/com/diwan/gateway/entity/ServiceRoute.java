package com.diwan.gateway.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_routes")
public class ServiceRoute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String baseUrl;

    @Column(nullable = false)
    private String pathPrefix;

    @Column(nullable = false)
    private boolean active = true;

    private int timeoutMs = 5000;

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist public void pre() { LocalDateTime n = LocalDateTime.now(); createdAt = n; updatedAt = n; }
    @PreUpdate  public void upd() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getServiceName() { return serviceName; } public void setServiceName(String v) { serviceName = v; }
    public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String v) { baseUrl = v; }
    public String getPathPrefix() { return pathPrefix; } public void setPathPrefix(String v) { pathPrefix = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public int getTimeoutMs() { return timeoutMs; } public void setTimeoutMs(int v) { timeoutMs = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
