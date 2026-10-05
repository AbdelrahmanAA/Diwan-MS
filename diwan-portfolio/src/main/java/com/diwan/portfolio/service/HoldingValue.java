package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.PriceSource;
import com.diwan.portfolio.entity.Holding;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A holding valued at the current price. {@code price} and {@code marketValueLocal} are in the holding's
 * currency; every other amount is converted to the portfolio's base currency. The converted amounts are
 * null when no exchange rate is known for the holding's currency ({@code convertible == false}).
 */
public record HoldingValue(
        Holding holding,
        BigDecimal price,
        PriceSource priceSource,
        Instant priceAsOf,
        BigDecimal marketValueLocal,
        boolean convertible,
        BigDecimal marketValue,
        BigDecimal costBasis,
        BigDecimal unrealizedPnl,
        BigDecimal realizedPnl,
        BigDecimal netIncome,
        BigDecimal dayChange,
        BigDecimal dayChangePercent) {

    public BigDecimal unrealizedPnlPercent() {
        return Money.percent(unrealizedPnl, costBasis);
    }

    /** Unrealized + realized P/L + net income */
    public BigDecimal totalReturn() {
        if (!convertible) {
            return null;
        }
        return unrealizedPnl.add(realizedPnl).add(netIncome);
    }
}
