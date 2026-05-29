package com.diwan.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts every request passing through the Gateway,
 * measures duration, then publishes a log event to Kafka topic "request-logs".
 * Runs AFTER JwtAuthFilter (Order 2).
 */
@Component
@Order(2)
public class LoggingWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingWebFilter.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "request-logs";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public LoggingWebFilter(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method  = exchange.getRequest().getMethod().name();
        String path    = exchange.getRequest().getURI().getPath();
        String clientIp = getClientIp(exchange);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration   = System.currentTimeMillis() - startTime;
                    int  statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;

                    // Extract userId if present (set by JwtAuthFilter)
                    String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                    Long userId = null;
                    try { if (userIdHeader != null) userId = Long.parseLong(userIdHeader); }
                    catch (NumberFormatException ignored) {}

                    // Resolve target service from path
                    String targetService = resolveTargetService(path);

                    Map<String, Object> logData = new HashMap<>();
                    logData.put("method",        method);
                    logData.put("path",          path);
                    logData.put("statusCode",    statusCode);
                    logData.put("durationMs",    duration);
                    logData.put("userId",        userId);
                    logData.put("sourceService", "GATEWAY");
                    logData.put("targetService", targetService);
                    logData.put("clientIp",      clientIp);
                    logData.put("requestTime",   LocalDateTime.now().toString());
                    if (statusCode >= 400) {
                        logData.put("errorMessage", "HTTP " + statusCode);
                    }

                    // Publish to Kafka asynchronously (don't block response)
                    Mono.fromCallable(() -> {
                        try {
                            String json = MAPPER.writeValueAsString(logData);
                            kafkaTemplate.send(TOPIC, path, json);
                        } catch (Exception e) {
                            log.warn("[LoggingFilter] Kafka publish failed: {}", e.getMessage());
                        }
                        return null;
                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();

                    log.info("[{}] {} {} -> {} ({}ms) user={}",
                            targetService, method, path, statusCode, duration, userId);
                });
    }

    /** Map URL path prefix to service name */
    private String resolveTargetService(String path) {
        if (path.startsWith("/api/transactions")) return "TRANSACTIONS";
        if (path.startsWith("/api/medical"))      return "MEDICAL";
        if (path.startsWith("/api/users"))        return "USERS";
        if (path.startsWith("/api/logs"))         return "LOGGING";
        if (path.startsWith("/api/features"))     return "GATEWAY";
        return "UNKNOWN";
    }

    /** Get real client IP (handles X-Forwarded-For) */
    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
