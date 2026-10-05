package com.diwan.portfolio.kafka;

import com.diwan.common.event.UserInvalidatedEvent;
import com.diwan.portfolio.service.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** A deleted user's portfolio data is erased; a deactivated user's alerts are paused. */
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
            log.warn("[Portfolio] user.invalidated event {} without userId ignored", event.getEventId());
            return;
        }
        if ("DELETED".equals(event.getReason())) {
            userData.deleteAll(event.getUserId());
        } else if ("DEACTIVATED".equals(event.getReason())) {
            userData.deactivate(event.getUserId());
        } else {
            log.info("[Portfolio] user.invalidated reason '{}' needs no action", event.getReason());
        }
    }
}
