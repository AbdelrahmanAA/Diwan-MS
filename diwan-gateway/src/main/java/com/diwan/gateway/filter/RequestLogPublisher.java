package com.diwan.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sends request-log events to Kafka without ever slowing down or exhausting the gateway.
 * Events go into a bounded in-memory queue and are sent one at a time with a short timeout. When Kafka is
 * slow or down the queue fills up and NEW events are dropped (counted in the
 * {@code gateway.request.log.dropped} metric); request handling is never affected. Request logs are
 * best-effort by design.
 */
@Component
public class RequestLogPublisher {

    private static final Logger log = LoggerFactory.getLogger(RequestLogPublisher.class);
    private static final long WARN_EVERY_MS = 10_000;

    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final Duration sendTimeout;
    private final Sinks.Many<Event> queue;
    private final Disposable worker;
    private final Counter dropped;
    private final Counter failed;
    private final AtomicLong lastWarn = new AtomicLong();

    private record Event(String key, String json) {}

    public RequestLogPublisher(KafkaTemplate<String, String> kafka,
                               MeterRegistry meters,
                               @Value("${diwan.kafka.topics.request-logs:request-logs}") String topic,
                               @Value("${diwan.gateway.request-log.queue-capacity:10000}") int capacity,
                               @Value("${diwan.gateway.request-log.send-timeout-ms:2000}") long sendTimeoutMs) {
        this.kafka = kafka;
        this.topic = topic;
        this.sendTimeout = Duration.ofMillis(sendTimeoutMs);
        this.dropped = Counter.builder("gateway.request.log.dropped").description("Request logs dropped because the queue was full").register(meters);
        this.failed = Counter.builder("gateway.request.log.failed").description("Request logs that could not be sent to Kafka").register(meters);
        this.queue = Sinks.many().unicast().onBackpressureBuffer(new ArrayBlockingQueue<>(capacity));
        this.worker = queue.asFlux().concatMap(this::send, 1).subscribe();
    }

    /** Never blocks, never throws. */
    public void publish(String key, String json) {
        Sinks.EmitResult result = queue.tryEmitNext(new Event(key, json));
        if (result.isFailure()) {
            dropped.increment();
            warnThrottled("queue full, dropping request logs (dropped so far: " + (long) dropped.count() + ")");
        }
    }

    private Mono<Void> send(Event event) {
        return Mono.fromCallable(() -> kafka.send(topic, event.key(), event.json()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(Mono::fromFuture)
                .timeout(sendTimeout)
                .then()
                .onErrorResume(e -> {
                    failed.increment();
                    warnThrottled("Kafka publish failed: " + e);
                    return Mono.empty();
                });
    }

    private void warnThrottled(String message) {
        long now = System.currentTimeMillis();
        long last = lastWarn.get();
        if (now - last >= WARN_EVERY_MS && lastWarn.compareAndSet(last, now)) {
            log.warn("[RequestLog] {}", message);
        }
    }

    @PreDestroy
    void stop() {
        queue.tryEmitComplete();
        worker.dispose();
    }
}
