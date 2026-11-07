package com.diwan.portfolio.service;

import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarketServiceTest {

    /** 1 USD = 3.75 SAR */
    private final FxService.Snapshot rates = new FxService.Snapshot(Map.of("SAR", new BigDecimal("3.75")), LocalDate.now());

    private static Instrument gold() {
        Instrument i = new Instrument("GOLD-24K");
        i.setCurrency("USD");
        return i;
    }

    private static InstrumentQuote quote(String price) {
        InstrumentQuote q = new InstrumentQuote(1L);
        q.setPrice(new BigDecimal(price));
        return q;
    }

    @Test
    void priceIsConvertedToTheCurrencyAskedFor() {
        assertThat(MarketService.priceIn("SAR", gold(), quote("130.9211"), rates)).isEqualByComparingTo("490.9541");
    }

    @Test
    void priceInTheInstrumentCurrencyIsTheQuote() {
        assertThat(MarketService.priceIn("USD", gold(), quote("130.9211"), rates)).isEqualByComparingTo("130.9211");
    }

    @Test
    void goldPricesAreOneGramOfEachKaratAndOneOunceOfPureGold() {
        Instrument karat21 = new Instrument("GOLD-21K");
        karat21.setCurrency("USD");

        var prices = MarketService.goldPrices("SAR", gold(), quote("100"), karat21, quote("87.5"), rates).prices();

        assertThat(prices).extracting(p -> p.karat() + p.unit()).containsExactly("24GRAM", "21GRAM", "24OUNCE");
        assertThat(prices.get(0).price()).isEqualByComparingTo("375");
        assertThat(prices.get(1).price()).isEqualByComparingTo("328.125");
        assertThat(prices.get(2).price()).isEqualByComparingTo("11663.80");
    }

    @Test
    void goldPricesAreEmptyWithoutAnExchangeRate() {
        var prices = MarketService.goldPrices("EGP", gold(), quote("100"), gold(), quote("87.5"), rates).prices();

        assertThat(prices).allSatisfy(p -> assertThat(p.price()).isNull());
    }

    @Test
    void noPriceWithoutAQuoteOrARate() {
        assertThat(MarketService.priceIn("SAR", gold(), null, rates)).isNull();
        assertThat(MarketService.priceIn("EGP", gold(), quote("130.9211"), rates)).isNull();
    }
}
