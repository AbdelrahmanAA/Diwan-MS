package com.diwan.gateway.controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Target of the circuit breaker's {@code forward:/fallback/<service>}. Handles every HTTP method because the
 * original request (POST, PATCH...) is forwarded as is.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service, ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        HttpStatusCode status = HttpStatus.SERVICE_UNAVAILABLE;
        String message = "Service '" + service + "' is temporarily unavailable.";

        if (cause instanceof CallNotPermittedException) {
            message = "Service '" + service + "' is failing; requests are paused for a few seconds.";
        } else if (cause instanceof TimeoutException || (cause != null && cause.getCause() instanceof TimeoutException)
                || (cause instanceof ResponseStatusException r && r.getStatusCode().value() == 504)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Service '" + service + "' did not answer in time.";
        }
        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "message", message,
                "service", service));
    }
}
