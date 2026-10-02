package com.diwan.logging.kafka;

import com.diwan.logging.entity.RequestLog;
import com.diwan.logging.repository.RequestLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RequestLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(RequestLogConsumer.class);

    private final RequestLogRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // tolerate fields from newer gateways

    public RequestLogConsumer(RequestLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Exceptions are deliberately NOT caught: the shared Kafka error handler (diwan-common) retries with
     * backoff and then moves the record to request-logs.DLT, so a database outage no longer drops log entries.
     */
    @KafkaListener(topics = "${diwan.kafka.topics.request-logs:request-logs}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) throws JsonProcessingException {
        RequestLog logEntry = objectMapper.readValue(message, RequestLog.class);
        repository.save(logEntry);
        log.debug("[Logging] Saved log: {} {} -> {} {}ms",
                logEntry.getMethod(), logEntry.getPath(),
                logEntry.getStatusCode(), logEntry.getDurationMs());
    }
}
