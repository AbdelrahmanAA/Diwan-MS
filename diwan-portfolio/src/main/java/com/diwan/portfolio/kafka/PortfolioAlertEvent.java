package com.diwan.portfolio.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code portfolio.alerts} for each triggered alert, so a notification service (push, e-mail)
 * can deliver it. Title and message are already in the user's preferred language. Schema rule: only ever
 * add optional fields. Move to diwan-common once a second service consumes it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortfolioAlertEvent(
        int version,
        String eventId,
        Instant occurredAt,
        Long alertId,
        Long userId,
        String type,
        String language,
        String title,
        String message,
        String symbol,
        Long holdingId,
        Long portfolioId,
        BigDecimal observedValue,
        BigDecimal threshold) {

    public static final int CURRENT_VERSION = 1;

    public static PortfolioAlertEvent of(Long alertId, Long userId, String type, String language, String title,
                                         String message, String symbol, Long holdingId, Long portfolioId,
                                         BigDecimal observedValue, BigDecimal threshold) {
        return new PortfolioAlertEvent(CURRENT_VERSION, UUID.randomUUID().toString(), Instant.now(), alertId, userId,
                type, language, title, message, symbol, holdingId, portfolioId, observedValue, threshold);
    }
}
