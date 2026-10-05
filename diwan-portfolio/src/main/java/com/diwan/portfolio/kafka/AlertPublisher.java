package com.diwan.portfolio.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best-effort delivery to Kafka: the alert itself is already stored (and visible in the app) before this
 * runs, so a Kafka outage only delays nothing but the push notification, and never fails the job.
 */
@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public AlertPublisher(KafkaTemplate<String, Object> kafka,
                          @Value("${diwan.kafka.topics.portfolio-alerts:portfolio.alerts}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public void publish(PortfolioAlertEvent event) {
        try {
            kafka.send(topic, String.valueOf(event.userId()), event).whenComplete((r, e) -> {
                if (e != null) {
                    log.warn("[Alerts] could not publish alert {} to Kafka: {}", event.alertId(), e.getMessage());
                }
            });
        } catch (RuntimeException e) {
            log.warn("[Alerts] could not publish alert {} to Kafka: {}", event.alertId(), e.getMessage());
        }
    }
}
