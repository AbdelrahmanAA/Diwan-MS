package com.diwan.transactions.kafka;

import com.diwan.common.event.UserInvalidatedEvent;
import com.diwan.transactions.service.TransactionService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for user-invalidated events from Kafka. A DELETED user's transactions and bank products
 * (balances, card limits) are removed; auth itself is the gateway's job.
 */
@Component
public class UserInvalidatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserInvalidatedConsumer.class);

    private final TransactionService service;

    public UserInvalidatedConsumer(TransactionService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${diwan.kafka.topics.user-invalidated:user.invalidated}", groupId = "${spring.kafka.consumer.group-id}")
    public void onUserInvalidated(UserInvalidatedEvent event) {
        log.info("[Transactions] User invalidated event received: userId={} reason={}",
                event.getUserId(), event.getReason());
        if ("DELETED".equals(event.getReason())) service.deleteUserData(event.getUserId());
    }
}