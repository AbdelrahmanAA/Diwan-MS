package com.diwan.users.outbox;

import com.diwan.common.event.UserInvalidatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records events in the caller's transaction; {@link OutboxRelay} publishes them to Kafka. */
@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final String userInvalidatedTopic;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper,
                         @Value("${diwan.kafka.topics.user-invalidated:" + UserInvalidatedEvent.TOPIC + "}") String userInvalidatedTopic) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.userInvalidatedTopic = userInvalidatedTopic;
    }

    /** Must run inside the transaction that changes the user, otherwise the guarantee is lost. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void userInvalidated(Long userId, String reason) {
        UserInvalidatedEvent event = new UserInvalidatedEvent(userId, reason);
        try {
            repository.save(new OutboxEvent(event.getEventId(), userInvalidatedTopic,
                    String.valueOf(userId), objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + event, e);
        }
    }
}
