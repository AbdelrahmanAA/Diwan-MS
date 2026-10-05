package com.diwan.portfolio.domain;

/** Kinds of investment. Clients get localized labels from {@code GET /api/portfolio/reference}. */
public enum AssetType {
    STOCK,
    ETF,
    MUTUAL_FUND,
    BOND,
    REAL_ESTATE,
    CRYPTO,
    COMMODITY,
    CASH,
    PRIVATE_BUSINESS,
    OTHER;

    public boolean isCash() {
        return this == CASH;
    }

    /** Types that can be linked to a market symbol and priced from quotes. */
    public boolean isMarketTraded() {
        return this == STOCK || this == ETF || this == MUTUAL_FUND || this == BOND || this == CRYPTO || this == COMMODITY;
    }
}
