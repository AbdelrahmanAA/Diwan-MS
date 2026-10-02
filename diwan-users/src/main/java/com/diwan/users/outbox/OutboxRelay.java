package com.diwan.users.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes pending outbox rows to Kafka in order. Delivery is at-least-once (a crash between the send and
 * the commit re-sends), which is why every event carries an eventId. Stops at the first failure so ordering
 * per user is preserved, and retries on the next tick.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final int batchSize;
    private final long sendTimeoutMs;
    private final int retentionDays;

    public OutboxRelay(OutboxEventRepository repository,
                       KafkaTemplate<String, String> outboxKafkaTemplate,
                       @Value("${diwan.outbox.batch-size:50}") int batchSize,
                       @Value("${diwan.outbox.send-timeout-ms:10000}") long sendTimeoutMs,
                       @Value("${diwan.outbox.retention-days:7}") int retentionDays) {
        this.repository = repository;
        this.kafka = outboxKafkaTemplate;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${diwan.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = repository.lockPending(PageRequest.of(0, batchSize));
        for (OutboxEvent event : pending) {
            try {
                kafka.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                event.markPublished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                event.markFailed(cause.toString());
                log.warn("[Outbox] publish of event {} failed (attempt {}): {}",
                        event.getEventId(), event.getAttempts(), cause.toString());
                return;
            }
        }
    }

    @Scheduled(cron = "${diwan.outbox.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void deleteOldPublished() {
        int deleted = repository.deletePublishedBefore(LocalDateTime.now().minusDays(retentionDays));
        if (deleted > 0) log.info("[Outbox] removed {} published events older than {} days", deleted, retentionDays);
    }
}
