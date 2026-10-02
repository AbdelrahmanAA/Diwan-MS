package com.diwan.gateway.filter;

import com.diwan.gateway.config.ClientIp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Records every request that passes through the gateway (method, path, status, duration, user id, client IP)
 * and hands it to {@link RequestLogPublisher}. Deliberately NOT logged: query strings, headers (tokens),
 * request/response bodies. Runs AFTER JwtAuthFilter (Order 2).
 */
@Component
@Order(2)
public class LoggingWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingWebFilter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RequestLogPublisher publisher;
    private final ClientIp clientIp;
    private final int managementPort;

    public LoggingWebFilter(RequestLogPublisher publisher, ClientIp clientIp,
                            @org.springframework.beans.factory.annotation.Value("${management.server.port:-1}") int managementPort) {
        this.publisher = publisher;
        this.clientIp = clientIp;
        this.managementPort = managementPort;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (ManagementPort.isManagementRequest(exchange, managementPort)) return chain.filter(exchange);
        long startTime = System.currentTimeMillis();
        String method  = exchange.getRequest().getMethod().name();
        String path    = exchange.getRequest().getURI().getPath();
        String ip      = clientIp.of(exchange);
        String requestId = exchange.getResponse().getHeaders().getFirst(RequestIdWebFilter.HEADER);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration   = System.currentTimeMillis() - startTime;
                    int  statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;

                    // Set by JwtAuthFilter; a client-supplied value was stripped there
                    String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                    Long userId = null;
                    try { if (userIdHeader != null) userId = Long.parseLong(userIdHeader); }
                    catch (NumberFormatException ignored) {}

                    String targetService = resolveTargetService(path);

                    Map<String, Object> logData = new HashMap<>();
                    logData.put("method",        method);
                    logData.put("path",          path);
                    logData.put("statusCode",    statusCode);
                    logData.put("durationMs",    duration);
                    logData.put("userId",        userId);
                    logData.put("sourceService", "GATEWAY");
                    logData.put("targetService", targetService);
                    logData.put("clientIp",      ip);
                    logData.put("requestId",     requestId);
                    logData.put("requestTime",   LocalDateTime.now().toString());
                    if (statusCode >= 400) {
                        logData.put("errorMessage", "HTTP " + statusCode);
                    }

                    try {
                        publisher.publish(path, MAPPER.writeValueAsString(logData));
                    } catch (Exception e) {
                        log.warn("[LoggingFilter] could not serialize request log: {}", e.getMessage());
                    }

                    log.info("[{}] {} {} -> {} ({}ms) user={} requestId={}",
                            targetService, method, path, statusCode, duration, userId, requestId);
                });
    }

    /** Map URL path prefix to service name */
    static String resolveTargetService(String path) {
        if (path.startsWith("/api/transactions")) return "TRANSACTIONS";
        if (path.startsWith("/api/medical"))      return "MEDICAL";
        if (path.startsWith("/api/users"))        return "USERS";
        if (path.startsWith("/api/smart-home"))   return "SMARTHOME";
        if (path.startsWith("/api/logs"))         return "LOGGING";
        if (path.startsWith("/api/features"))     return "GATEWAY";
        return "UNKNOWN";
    }
}
