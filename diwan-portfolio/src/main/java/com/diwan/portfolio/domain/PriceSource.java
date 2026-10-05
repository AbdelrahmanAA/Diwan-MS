package com.diwan.portfolio.domain;

/** Where a holding's current value came from. */
public enum PriceSource {
    /** Latest market quote of the linked instrument */
    QUOTE,
    /** The newest manual valuation */
    VALUATION,
    /** Cash is worth its balance */
    CASH,
    /** Nothing better known: valued at what was paid */
    COST
}
