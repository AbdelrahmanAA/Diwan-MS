package com.diwan.portfolio.domain;

/**
 * BUY/SELL change the position (quantity x price); DIVIDEND/INCOME/EXPENSE only change the income
 * (amount); DEPOSIT/WITHDRAWAL are the cash-holding equivalent of buy/sell (amount, price 1).
 */
public enum TransactionType {
    BUY,
    SELL,
    DIVIDEND,
    INCOME,
    EXPENSE,
    DEPOSIT,
    WITHDRAWAL;

    public boolean isTrade() {
        return this == BUY || this == SELL;
    }

    public boolean allowedFor(AssetType type) {
        if (type.isCash()) {
            return this == DEPOSIT || this == WITHDRAWAL || this == INCOME || this == EXPENSE;
        }
        return this != DEPOSIT && this != WITHDRAWAL;
    }
}
