package com.diwan.portfolio.market;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** Fixed, roughly realistic rates for DEV and tests. */
public class FakeFxProvider implements FxProvider {

    static final Map<String, BigDecimal> RATES = Map.of(
            "USD", BigDecimal.ONE,
            "EUR", new BigDecimal("0.92"),
            "GBP", new BigDecimal("0.79"),
            "EGP", new BigDecimal("48.50"),
            "SAR", new BigDecimal("3.75"),
            "AED", new BigDecimal("3.6725"),
            "KWD", new BigDecimal("0.307"),
            "QAR", new BigDecimal("3.64"),
            "JPY", new BigDecimal("150.0"),
            "CHF", new BigDecimal("0.88"));

    private final Clock clock;

    public FakeFxProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public Optional<Rates> latest() {
        return Optional.of(new Rates(LocalDate.now(clock), RATES));
    }
}
