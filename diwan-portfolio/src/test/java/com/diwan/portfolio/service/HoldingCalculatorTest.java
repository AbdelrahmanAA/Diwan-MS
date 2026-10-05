package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.domain.TransactionType;
import com.diwan.portfolio.entity.PortfolioTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingCalculatorTest {

    private static PortfolioTransaction trade(TransactionType type, String qty, String price, String fees, String date) {
        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(type);
        t.setQuantity(new BigDecimal(qty));
        t.setPrice(new BigDecimal(price));
        t.setFees(new BigDecimal(fees));
        t.setTradeDate(LocalDate.parse(date));
        return t;
    }

    private static PortfolioTransaction cash(TransactionType type, String amount, String fees, String date) {
        PortfolioTransaction t = new PortfolioTransaction();
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setFees(new BigDecimal(fees));
        t.setTradeDate(LocalDate.parse(date));
        return t;
    }

    @Test
    void averageCostIncludesFeesAndSellsRealizeAgainstIt() {
        var p = HoldingCalculator.replay(List.of(
                trade(TransactionType.BUY, "10", "100", "5", "2024-01-01"),   // cost 1005
                trade(TransactionType.BUY, "10", "120", "5", "2024-02-01"),   // cost 1205 -> avg 110.5
                trade(TransactionType.SELL, "5", "130", "2", "2024-03-01"))); // 5*(130-110.5) - 2 = 95.5

        assertThat(p.quantity()).isEqualByComparingTo("15");
        assertThat(p.averageCost()).isEqualByComparingTo("110.5");
        assertThat(p.realizedPnl()).isEqualByComparingTo("95.5");
    }

    @Test
    void sellingEverythingResetsTheAverageCost() {
        var p = HoldingCalculator.replay(List.of(
                trade(TransactionType.BUY, "3", "50", "0", "2024-01-01"),
                trade(TransactionType.SELL, "3", "40", "0", "2024-01-02")));

        assertThat(p.quantity()).isEqualByComparingTo("0");
        assertThat(p.averageCost()).isEqualByComparingTo("0");
        assertThat(p.realizedPnl()).isEqualByComparingTo("-30");
    }

    @Test
    void sellingMoreThanHeldIsRejected() {
        assertThatThrownBy(() -> HoldingCalculator.replay(List.of(
                trade(TransactionType.BUY, "1", "10", "0", "2024-01-01"),
                trade(TransactionType.SELL, "2", "10", "0", "2024-01-02"))))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("transaction.exceeds_quantity");
    }

    @Test
    void incomeAndExpensesOnlyChangeNetIncome() {
        var p = HoldingCalculator.replay(List.of(
                trade(TransactionType.BUY, "1", "1000000", "0", "2024-01-01"),
                cash(TransactionType.INCOME, "5000", "0", "2024-02-01"),   // rent
                cash(TransactionType.EXPENSE, "800", "0", "2024-02-15"),   // maintenance
                cash(TransactionType.DIVIDEND, "100", "10", "2024-03-01")));

        assertThat(p.quantity()).isEqualByComparingTo("1");
        assertThat(p.averageCost()).isEqualByComparingTo("1000000");
        assertThat(p.netIncome()).isEqualByComparingTo("4290");
        assertThat(p.realizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    void cashDepositsAndWithdrawalsMoveTheBalance() {
        var p = HoldingCalculator.replay(List.of(
                cash(TransactionType.DEPOSIT, "1000", "0", "2024-01-01"),
                cash(TransactionType.WITHDRAWAL, "250", "0", "2024-01-05"),
                cash(TransactionType.INCOME, "12.5", "0", "2024-02-01")));  // interest

        assertThat(p.quantity()).isEqualByComparingTo("750");
        assertThat(p.averageCost()).isEqualByComparingTo("1");
        assertThat(p.netIncome()).isEqualByComparingTo("12.5");
    }

    @Test
    void emptyHistoryIsAnEmptyPosition() {
        var p = HoldingCalculator.replay(List.of());
        assertThat(p.quantity()).isEqualByComparingTo("0");
        assertThat(p.realizedPnl()).isEqualByComparingTo("0");
    }
}
