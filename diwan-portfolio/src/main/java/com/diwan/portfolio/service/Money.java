package com.diwan.portfolio.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Rounding rules for API output: money with 2 decimals, percentages with 2 decimals. */
public final class Money {

    public static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {}

    public static BigDecimal round(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** part / whole * 100, or null when whole is zero */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole.abs(), MC).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
