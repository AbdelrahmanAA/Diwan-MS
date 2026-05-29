package com.diwan.smarthome.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "smart_devices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
public class SmartDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Raw device ID scanned from QR code */
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    /** Human-readable name given by the user */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Base MQTT topic, e.g. "myhome/SmartLamp_ABC123".
     * Append "/cmd" to publish commands, "/state" to subscribe to state.
     */
    @Column(name = "mqtt_topic", nullable = false, length = 200)
    private String mqttTopic;

    /** Last known state: ON | OFF | UNKNOWN */
    @Column(nullable = false, length = 20)
    private String state = "UNKNOWN";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    // ── Getters & setters ────────────────────────────────────────────────────
    public Long getId()                  { return id; }
    public Long getUserId()              { return userId; }
    public void setUserId(Long v)        { userId = v; }
    public String getDeviceId()          { return deviceId; }
    public void setDeviceId(String v)    { deviceId = v; }
    public String getName()              { return name; }
    public void setName(String v)        { name = v; }
    public String getMqttTopic()         { return mqttTopic; }
    public void setMqttTopic(String v)   { mqttTopic = v; }
    public String getState()             { return state; }
    public void setState(String v)       { state = v; }
    public boolean isActive()            { return active; }
    public void setActive(boolean v)     { active = v; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
