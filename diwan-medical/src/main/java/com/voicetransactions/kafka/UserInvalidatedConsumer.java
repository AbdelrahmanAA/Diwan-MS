package com.voicetransactions.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for user-invalidated events from Kafka.
 * Auth is now centralized in the Gateway (JWT validation + X-Gateway-Validated header),
 * so no Redis cache to evict here. We just log the event for audit purposes.
 */
@Component
public class UserInvalidatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserInvalidatedConsumer.class);

    @KafkaListener(topics = "user.invalidated", groupId = "medical-group")
    public void onUserInvalidated(UserInvalidatedEvent event) {
        log.info("[Medical] User invalidated event received: userId={} reason={}",
                event.getUserId(), event.getReason());
        // Gateway handles auth - no local cache to evict
    }
}