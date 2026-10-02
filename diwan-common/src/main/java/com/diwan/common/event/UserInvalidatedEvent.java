package com.diwan.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by diwan-users when an account is deleted or deactivated.
 *
 * <p>Schema versioning: only ever ADD optional fields and bump {@link #CURRENT_VERSION}; never rename or
 * remove one. Consumers ignore unknown fields, and fields missing from older messages stay {@code null}/0.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInvalidatedEvent {

    /** Default topic name; the actual name is configured with {@code diwan.kafka.topics.user-invalidated}. */
    public static final String TOPIC = "user.invalidated";
    public static final int CURRENT_VERSION = 1;

    private int version;
    private String eventId;      // unique per event, lets consumers de-duplicate (delivery is at-least-once)
    private Instant occurredAt;
    private Long userId;
    private String reason;       // "DELETED" or "DEACTIVATED"

    /** For deserialization: fields are filled from the message only. */
    public UserInvalidatedEvent() {}

    public UserInvalidatedEvent(Long userId, String reason) {
        this.version = CURRENT_VERSION;
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.userId = userId;
        this.reason = reason;
    }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
