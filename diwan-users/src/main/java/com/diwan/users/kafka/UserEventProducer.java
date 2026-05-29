package com.diwan.users.kafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {
    public static final String TOPIC = "user.invalidated";
    private final KafkaTemplate<String, UserInvalidatedEvent> kafkaTemplate;
    public UserEventProducer(KafkaTemplate<String, UserInvalidatedEvent> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }
    public void publishInvalidated(Long userId, String reason){
        kafkaTemplate.send(TOPIC, String.valueOf(userId), new UserInvalidatedEvent(userId, reason));
    }
}