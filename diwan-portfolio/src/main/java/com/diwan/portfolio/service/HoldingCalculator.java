package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.domain.TransactionType;
import com.diwan.portfolio.entity.PortfolioTransaction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Rebuilds a holding's position from its transactions (average-cost method):
 * <ul>
 *   <li>BUY: average cost = (old cost basis + quantity x price + fees) / new quantity</li>
 *   <li>SELL: realized P/L += quantity x (price - average cost) - fees; average cost is unchanged</li>
 *   <li>DEPOSIT/WITHDRAWAL (cash): the amount moves the balance at a cost of 1 per unit</li>
 *   <li>DIVIDEND/INCOME: net income += amount - fees; EXPENSE: net income -= amount + fees</li>
 * </ul>
 * Replaying the whole history (instead of applying one change) keeps edits and deletes of old
 * transactions correct, and rejects any history that would sell more than was held at that point.
 */
public final class HoldingCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;

    public record Position(BigDecimal quantity, BigDecimal averageCost, BigDecimal realizedPnl, BigDecimal netIncome) {
        public static final Position EMPTY = new Position(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private HoldingCalculator() {}

    /** @param transactions in replay order (trade date, then id) */
    public static Position replay(List<PortfolioTransaction> transactions) {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal avg = BigDecimal.ZERO;
        BigDecimal realized = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;

        for (PortfolioTransaction t : transactions) {
            BigDecimal fees = t.getFees() == null ? BigDecimal.ZERO : t.getFees();
            TransactionType type = t.getType();
            switch (type) {
                case BUY, DEPOSIT -> {
                    BigDecimal q = type == TransactionType.BUY ? t.getQuantity() : t.getAmount();
                    BigDecimal unitPrice = type == TransactionType.BUY ? t.getPrice() : BigDecimal.ONE;
                    BigDecimal newQty = qty.add(q);
                    BigDecimal cost = qty.multiply(avg).add(q.multiply(unitPrice)).add(fees);
                    avg = cost.divide(newQty, MC);
                    qty = newQty;
                }
                case SELL, WITHDRAWAL -> {
                    BigDecimal q = type == TransactionType.SELL ? t.getQuantity() : t.getAmount();
                    BigDecimal unitPrice = type == TransactionType.SELL ? t.getPrice() : BigDecimal.ONE;
                    if (q.compareTo(qty) > 0) {
                        throw ApiException.badRequest("transaction.exceeds_quantity", t.getTradeDate());
                    }
                    realized = realized.add(q.multiply(unitPrice.subtract(avg))).subtract(fees);
                    qty = qty.subtract(q);
                    if (qty.signum() == 0) {
                        avg = BigDecimal.ZERO;
                    }
                }
                case DIVIDEND, INCOME -> income = income.add(t.getAmount()).subtract(fees);
                case EXPENSE -> income = income.subtract(t.getAmount()).subtract(fees);
            }
        }
        return new Position(
                qty.setScale(8, RoundingMode.HALF_EVEN),
                avg.setScale(8, RoundingMode.HALF_EVEN),
                realized.setScale(4, RoundingMode.HALF_EVEN),
                income.setScale(4, RoundingMode.HALF_EVEN));
    }
}
