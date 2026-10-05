package com.diwan.portfolio.domain;

/** What an alert rule watches. The threshold is a price, or a percentage for the *_PERCENT and DRIFT types. */
public enum AlertType {
    /** Instrument price rises to or above the threshold */
    PRICE_ABOVE,
    /** Instrument price falls to or below the threshold (also a stop-loss on a symbol) */
    PRICE_BELOW,
    /** Instrument moves at least threshold % (up or down) since the previous close */
    DAILY_MOVE_PERCENT,
    /** Holding's unrealized gain reaches threshold % of its cost (take profit) */
    GAIN_PERCENT,
    /** Holding's unrealized loss reaches threshold % of its cost (stop loss) */
    LOSS_PERCENT,
    /** Any asset type or country of the portfolio is more than threshold points away from its target */
    ALLOCATION_DRIFT;

    public boolean needsSymbol() {
        return this == PRICE_ABOVE || this == PRICE_BELOW || this == DAILY_MOVE_PERCENT;
    }

    public boolean needsHolding() {
        return this == GAIN_PERCENT || this == LOSS_PERCENT;
    }

    public boolean needsPortfolio() {
        return this == ALLOCATION_DRIFT;
    }
}
