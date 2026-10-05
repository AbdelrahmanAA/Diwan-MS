package com.diwan.portfolio.service;

import com.diwan.portfolio.entity.FxRate;
import com.diwan.portfolio.market.FxProvider;
import com.diwan.portfolio.repository.FxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class FxRefreshService {

    private final FxProvider provider;
    private final FxRateRepository rates;

    public FxRefreshService(FxProvider provider, FxRateRepository rates) {
        this.provider = provider;
        this.rates = rates;
    }

    /** Stores today's rates (one row per currency and date; re-running the same day overwrites). */
    public int refresh() {
        return provider.latest().map(r -> store(r.date(), r.perUsd())).orElse(0);
    }

    @Transactional
    public int store(java.time.LocalDate date, Map<String, java.math.BigDecimal> perUsd) {
        perUsd.forEach((ccy, rate) -> {
            FxRate row = rates.findById(new FxRate.Key(ccy, date)).orElseGet(() -> new FxRate(ccy, date, rate));
            row.setRate(rate);
            rates.save(row);
        });
        return perUsd.size();
    }

    public boolean hasRates() {
        return rates.count() > 0;
    }
}
