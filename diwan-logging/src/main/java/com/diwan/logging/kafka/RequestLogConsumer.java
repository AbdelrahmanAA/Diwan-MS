package com.diwan.logging.kafka;

import com.diwan.logging.entity.RequestLog;
import com.diwan.logging.repository.RequestLogRepository;
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
            .registerModule(new JavaTimeModule());

    public RequestLogConsumer(RequestLogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "request-logs", groupId = "logging-group")
    public void consume(String message) {
        try {
            RequestLog logEntry = objectMapper.readValue(message, RequestLog.class);
            repository.save(logEntry);
            log.debug("[Logging] Saved log: {} {} -> {} {}ms",
                    logEntry.getMethod(), logEntry.getPath(),
                    logEntry.getStatusCode(), logEntry.getDurationMs());
        } catch (Exception e) {
            log.error("[Logging] Failed to parse log message: {}", e.getMessage());
        }
    }
}
