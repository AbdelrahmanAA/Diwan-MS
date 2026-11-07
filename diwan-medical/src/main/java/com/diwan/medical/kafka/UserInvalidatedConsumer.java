package com.diwan.medical.kafka;

import com.diwan.common.event.UserInvalidatedEvent;
import com.diwan.medical.service.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** A deleted user's medical data is erased. A deactivated account keeps it: the user can come back. */
@Component
public class UserInvalidatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserInvalidatedConsumer.class);

    private final UserDataService userData;

    public UserInvalidatedConsumer(UserDataService userData) {
        this.userData = userData;
    }

    @KafkaListener(topics = "${diwan.kafka.topics.user-invalidated:user.invalidated}", groupId = "${spring.kafka.consumer.group-id}")
    public void onUserInvalidated(UserInvalidatedEvent event) {
        if (event.getUserId() == null) {
            log.warn("[Medical] user.invalidated event {} without userId ignored", event.getEventId());
            return;
        }
        if ("DELETED".equals(event.getReason())) {
            userData.deleteAll(event.getUserId());
        } else {
            log.info("[Medical] user.invalidated reason '{}' needs no action", event.getReason());
        }
    }
}
