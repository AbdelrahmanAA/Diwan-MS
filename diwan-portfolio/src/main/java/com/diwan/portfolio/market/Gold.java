package com.diwan.portfolio.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** The gold symbols: one instrument per karat, priced in USD per gram. */
public final class Gold {

    public static final String CURRENCY = "USD";
    public static final String EXCHANGE = "SPOT";
    /** Gold-backed ETF whose company news stands in for gold news, which has no symbol of its own */
    public static final String NEWS_SYMBOL = "GLD";
    public static final List<Integer> KARATS = List.of(24, 22, 21, 18);

    private static final String PREFIX = "GOLD-";
    public static final BigDecimal GRAMS_PER_TROY_OUNCE = new BigDecimal("31.1034768");
    private static final BigDecimal PURE = BigDecimal.valueOf(24);

    private Gold() {}

    public static String symbol(int karat) {
        return PREFIX + karat + "K";
    }

    public static String name(int karat) {
        return "Gold " + karat + "K (per gram)";
    }

    public static boolean isGold(String symbol) {
        return karat(symbol).isPresent();
    }

    public static Optional<Integer> karat(String symbol) {
        return KARATS.stream().filter(k -> symbol(k).equals(symbol)).findFirst();
    }

    /** Price of one gram of this karat from the spot price of a troy ounce of pure gold. */
    public static BigDecimal perGram(BigDecimal perOunce, int karat) {
        return perOunce.multiply(BigDecimal.valueOf(karat))
                .divide(GRAMS_PER_TROY_OUNCE.multiply(PURE), 4, RoundingMode.HALF_EVEN);
    }
}
