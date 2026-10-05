package com.diwan.portfolio.market;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** Exchange rates as units of each currency per 1 USD. */
public interface FxProvider {

    String name();

    Optional<Rates> latest();

    record Rates(LocalDate date, Map<String, BigDecimal> perUsd) {}
}
