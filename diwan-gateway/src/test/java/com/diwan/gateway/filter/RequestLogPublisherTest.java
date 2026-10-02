package com.diwan.gateway.filter;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLogPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @Test
    void publishesToTheConfiguredTopic() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        RequestLogPublisher publisher = new RequestLogPublisher(kafka, meters, "request-logs", 10, 1000);
        publisher.publish("/api/x", "{\"a\":1}");
        verify(kafka, timeout(2000)).send("request-logs", "/api/x", "{\"a\":1}");
        publisher.stop();
    }

    @Test
    void aStuckKafkaNeverBlocksTheCallerAndOverflowIsDropped() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        when(kafka.send(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            release.await(30, TimeUnit.SECONDS); // simulates the producer blocking on unavailable metadata
            return CompletableFuture.completedFuture(null);
        });
        RequestLogPublisher publisher = new RequestLogPublisher(kafka, meters, "request-logs", 5, 1000);

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) publisher.publish("/k" + i, "{}");
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(tookMs < 500, "publish() must not block, took " + tookMs + " ms");
        assertTrue(meters.counter("gateway.request.log.dropped").count() > 900, "most events should have been dropped");
        release.countDown();
        publisher.stop();
    }

    @Test
    void sendFailuresAreCountedNotThrown() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("down")));
        RequestLogPublisher publisher = new RequestLogPublisher(kafka, meters, "request-logs", 10, 1000);
        publisher.publish("/x", "{}");
        verify(kafka, timeout(2000)).send("request-logs", "/x", "{}");
        long deadline = System.currentTimeMillis() + 2000;
        while (meters.counter("gateway.request.log.failed").count() < 1 && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(meters.counter("gateway.request.log.failed").count() >= 1);
        publisher.stop();
    }
}
