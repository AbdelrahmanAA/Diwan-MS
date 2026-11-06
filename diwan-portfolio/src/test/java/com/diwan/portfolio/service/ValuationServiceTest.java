package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.AssetType;
import com.diwan.portfolio.domain.PriceSource;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.entity.Valuation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValuationServiceTest {

    /** 1 USD = 50 EGP */
    private final FxService.Snapshot rates = new FxService.Snapshot(Map.of("EGP", new BigDecimal("50")), LocalDate.now());

    private static Holding holding(AssetType type, String currency, String qty, String avg) {
        Holding h = new Holding();
        h.setAssetType(type);
        h.setCurrency(currency);
        h.setCountryCode("EG");
        h.setName("x");
        h.setQuantity(new BigDecimal(qty));
        h.setAverageCost(new BigDecimal(avg));
        return h;
    }

    @Test
    void quotedHoldingIsValuedAtTheQuoteAndConvertedToTheBaseCurrency() {
        Holding h = holding(AssetType.STOCK, "EGP", "100", "40");
        h.setInstrument(new Instrument("COMI.CA"));
        InstrumentQuote q = new InstrumentQuote(1L);
        q.setPrice(new BigDecimal("50"));
        q.setDayChange(new BigDecimal("2"));
        q.setChangePercent(new BigDecimal("4.17"));
        q.setQuoteTime(Instant.parse("2026-01-01T10:00:00Z"));

        HoldingValue v = ValuationService.value(h, q, null, "USD", rates);

        assertThat(v.priceSource()).isEqualTo(PriceSource.QUOTE);
        assertThat(v.marketValueLocal()).isEqualByComparingTo("5000");  // EGP
        assertThat(v.marketValue()).isEqualByComparingTo("100");        // USD
        assertThat(v.costBasis()).isEqualByComparingTo("80");
        assertThat(v.unrealizedPnl()).isEqualByComparingTo("20");
        assertThat(v.unrealizedPnlPercent()).isEqualByComparingTo("25");
        assertThat(v.dayChange()).isEqualByComparingTo("4");             // 100 * 2 EGP = 200 EGP = 4 USD
    }

    @Test
    void goldQuotedInUsdIsValuedInTheCurrencyOfTheHolding() {
        Instrument gold = new Instrument("GOLD-21K");
        gold.setCurrency("USD");
        Holding h = holding(AssetType.COMMODITY, "EGP", "10", "4000"); // 10 g bought at 4000 EGP per gram
        h.setInstrument(gold);
        InstrumentQuote q = new InstrumentQuote(1L);
        q.setPrice(new BigDecimal("100"));
        q.setDayChange(new BigDecimal("2"));
        q.setQuoteTime(Instant.parse("2026-01-01T10:00:00Z"));

        HoldingValue v = ValuationService.value(h, q, null, "USD", rates);

        assertThat(v.price()).isEqualByComparingTo("5000");            // EGP per gram
        assertThat(v.marketValueLocal()).isEqualByComparingTo("50000"); // EGP
        assertThat(v.dayChangeLocal()).isEqualByComparingTo("1000");
        assertThat(v.unrealizedPnlLocal()).isEqualByComparingTo("10000");
        assertThat(v.marketValue()).isEqualByComparingTo("1000");       // USD
        assertThat(v.unrealizedPnlPercent()).isEqualByComparingTo("25");
    }

    @Test
    void aQuoteThatCannotBeConvertedIsNotUsedAsIfItWereInTheHoldingCurrency() {
        Instrument gold = new Instrument("GOLD-21K");
        gold.setCurrency("USD");
        Holding h = holding(AssetType.COMMODITY, "KES", "10", "500");
        h.setInstrument(gold);
        InstrumentQuote q = new InstrumentQuote(1L);
        q.setPrice(new BigDecimal("100"));
        q.setQuoteTime(Instant.parse("2026-01-01T10:00:00Z"));

        HoldingValue v = ValuationService.value(h, q, null, "USD", rates);

        assertThat(v.priceSource()).isEqualTo(PriceSource.COST);
        assertThat(v.marketValueLocal()).isEqualByComparingTo("5000");
    }

    @Test
    void localTotalsOfAMarketUseNoExchangeRate() {
        Holding a = holding(AssetType.STOCK, "SAR", "10", "20");
        Holding b = holding(AssetType.STOCK, "SAR", "5", "40");
        a.setRealizedPnl(new BigDecimal("7"));
        b.setNetIncome(new BigDecimal("3"));
        var none = new FxService.Snapshot(Map.of(), LocalDate.now());
        var values = java.util.List.of(ValuationService.value(a, null, null, "USD", none),
                ValuationService.value(b, null, null, "USD", none));

        var totals = SummaryService.localTotals(values);

        assertThat(values.get(0).convertible()).isFalse();
        assertThat(totals.marketValue()).isEqualByComparingTo("400");
        assertThat(totals.realizedPnl()).isEqualByComparingTo("7");
        assertThat(totals.netIncome()).isEqualByComparingTo("3");
        assertThat(totals.totalReturn()).isEqualByComparingTo("10");
    }

    @Test
    void propertyUsesTheNewestManualValuation() {
        Holding h = holding(AssetType.REAL_ESTATE, "EGP", "1", "3000000");
        Valuation val = new Valuation();
        val.setValue(new BigDecimal("3500000"));
        val.setValuationDate(LocalDate.parse("2026-06-01"));

        HoldingValue v = ValuationService.value(h, null, val, "EGP", rates);

        assertThat(v.priceSource()).isEqualTo(PriceSource.VALUATION);
        assertThat(v.marketValue()).isEqualByComparingTo("3500000");
        assertThat(v.unrealizedPnl()).isEqualByComparingTo("500000");
    }

    @Test
    void withoutQuoteOrValuationTheCostBasisIsUsed() {
        HoldingValue v = ValuationService.value(holding(AssetType.PRIVATE_BUSINESS, "USD", "1", "20000"),
                null, null, "USD", rates);
        assertThat(v.priceSource()).isEqualTo(PriceSource.COST);
        assertThat(v.marketValue()).isEqualByComparingTo("20000");
        assertThat(v.unrealizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    void cashIsWorthItsBalance() {
        HoldingValue v = ValuationService.value(holding(AssetType.CASH, "EGP", "10000", "1"), null, null, "USD", rates);
        assertThat(v.priceSource()).isEqualTo(PriceSource.CASH);
        assertThat(v.marketValue()).isEqualByComparingTo("200");
    }

    @Test
    void soldOutPropertyIsWorthNothingEvenWithAnOldValuation() {
        Valuation val = new Valuation();
        val.setValue(new BigDecimal("3500000"));
        val.setValuationDate(LocalDate.parse("2026-06-01"));
        HoldingValue v = ValuationService.value(holding(AssetType.REAL_ESTATE, "EGP", "0", "0"), null, val, "EGP", rates);
        assertThat(v.marketValue()).isEqualByComparingTo("0");
    }

    @Test
    void unknownCurrencyIsReportedInsteadOfGuessed() {
        HoldingValue v = ValuationService.value(holding(AssetType.STOCK, "KES", "10", "5"), null, null, "USD", rates);
        assertThat(v.convertible()).isFalse();
        assertThat(v.marketValue()).isNull();
        assertThat(v.marketValueLocal()).isEqualByComparingTo("50");
    }
}
