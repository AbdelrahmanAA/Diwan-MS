package com.diwan.portfolio.service;

import com.diwan.portfolio.entity.FxRate;
import com.diwan.portfolio.repository.FxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Currency conversion with the newest stored USD cross rates (filled by the FX job). Callers take one
 * {@link Snapshot} per calculation so every number in a response uses the same rates.
 */
@Service
public class FxService {

    private final FxRateRepository rates;

    public FxService(FxRateRepository rates) {
        this.rates = rates;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        Map<String, BigDecimal> perUsd = new HashMap<>();
        LocalDate asOf = null;
        for (FxRate r : rates.findLatestRates()) {
            perUsd.put(r.getCurrency(), r.getRate());
            if (asOf == null || r.getRateDate().isAfter(asOf)) {
                asOf = r.getRateDate();
            }
        }
        return new Snapshot(perUsd, asOf);
    }

    public static final class Snapshot {
        private final Map<String, BigDecimal> perUsd;
        private final LocalDate asOf;

        public Snapshot(Map<String, BigDecimal> perUsd, LocalDate asOf) {
            this.perUsd = new HashMap<>(perUsd);
            this.perUsd.put("USD", BigDecimal.ONE);
            this.asOf = asOf;
        }

        public LocalDate asOf() {
            return asOf;
        }

        public boolean supports(String currency) {
            return perUsd.containsKey(currency);
        }

        /** Empty when either currency has no known rate. */
        public Optional<BigDecimal> convert(BigDecimal amount, String from, String to) {
            if (amount == null) {
                return Optional.empty();
            }
            if (from.equals(to)) {
                return Optional.of(amount);
            }
            BigDecimal fromRate = perUsd.get(from);
            BigDecimal toRate = perUsd.get(to);
            if (fromRate == null || toRate == null || fromRate.signum() == 0) {
                return Optional.empty();
            }
            return Optional.of(amount.divide(fromRate, Money.MC).multiply(toRate, Money.MC));
        }
    }
}
