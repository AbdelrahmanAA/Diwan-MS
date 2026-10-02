package com.diwan.users.outbox;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36) private String eventId;
    @Column(nullable = false) private String topic;
    private String messageKey;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime publishedAt;
    @Column(nullable = false) private int attempts;
    @Column(length = 500) private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(String eventId, String topic, String messageKey, String payload) {
        this.eventId = eventId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }

    public void markPublished() {
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }
}
