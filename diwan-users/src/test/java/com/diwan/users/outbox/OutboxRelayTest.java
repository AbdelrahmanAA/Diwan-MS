package com.diwan.users.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final OutboxRelay relay = new OutboxRelay(repository, kafka, 50, 1000, 7);

    private static OutboxEvent event(String id, long userId) {
        return new OutboxEvent(id, "user.invalidated", String.valueOf(userId), "{\"userId\":" + userId + "}");
    }

    @Test
    void publishedEventsAreMarkedAndSentWithKeyAndPayload() {
        OutboxEvent e = event("e1", 7);
        when(repository.lockPending(any(Pageable.class))).thenReturn(List.of(e));
        when(kafka.send(eq("user.invalidated"), eq("7"), eq("{\"userId\":7}")))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        relay.publishPending();

        assertNotNull(e.getPublishedAt());
        assertEquals(0, e.getAttempts());
    }

    @Test
    void failureKeepsEventPendingAndStopsToPreserveOrdering() {
        OutboxEvent first = event("e1", 7);
        OutboxEvent second = event("e2", 8);
        when(repository.lockPending(any(Pageable.class))).thenReturn(List.of(first, second));
        when(kafka.send(eq("user.invalidated"), eq("7"), eq(first.getPayload())))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        relay.publishPending();

        assertNull(first.getPublishedAt());
        assertEquals(1, first.getAttempts());
        assertTrue(first.getLastError().contains("broker down"));
        assertNull(second.getPublishedAt());
        verify(kafka, never()).send(eq("user.invalidated"), eq("8"), eq(second.getPayload()));
    }

    @Test
    void nothingPendingSendsNothing() {
        when(repository.lockPending(any(Pageable.class))).thenReturn(List.of());
        relay.publishPending();
        verify(kafka, never()).send(any(String.class), any(String.class), any(String.class));
    }
}
