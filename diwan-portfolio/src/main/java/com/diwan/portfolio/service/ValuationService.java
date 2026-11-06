package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.PriceSource;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.entity.Valuation;
import com.diwan.portfolio.repository.InstrumentQuoteRepository;
import com.diwan.portfolio.repository.ValuationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Values holdings at their current price. Price priority: market quote (holding linked to an instrument),
 * then the newest manual valuation, then the cost basis. Cash is worth its balance. A quote is in the
 * instrument's currency, which can differ from the holding's (gold is quoted in USD and held in any currency).
 */
@Service
public class ValuationService {

    private final InstrumentQuoteRepository quotes;
    private final ValuationRepository valuations;
    private final FxService fx;

    public ValuationService(InstrumentQuoteRepository quotes, ValuationRepository valuations, FxService fx) {
        this.quotes = quotes;
        this.valuations = valuations;
        this.fx = fx;
    }

    public FxService.Snapshot fxSnapshot() {
        return fx.snapshot();
    }

    @Transactional(readOnly = true)
    public List<HoldingValue> value(List<Holding> holdings, String baseCurrency, FxService.Snapshot rates) {
        List<Long> instrumentIds = holdings.stream()
                .filter(h -> h.getInstrument() != null).map(h -> h.getInstrument().getId()).distinct().toList();
        Map<Long, InstrumentQuote> quoteById = instrumentIds.isEmpty() ? Map.of()
                : quotes.findAllById(instrumentIds).stream()
                    .collect(Collectors.toMap(InstrumentQuote::getInstrumentId, Function.identity()));
        List<Long> holdingIds = holdings.stream().map(Holding::getId).filter(Objects::nonNull).toList();
        Map<Long, Valuation> valuationByHolding = holdingIds.isEmpty() ? Map.of()
                : valuations.findLatestFor(holdingIds).stream()
                    .collect(Collectors.toMap(v -> v.getHolding().getId(), Function.identity(), (a, b) -> a));

        return holdings.stream()
                .map(h -> value(h, quoteOf(h, quoteById), valuationByHolding.get(h.getId()), baseCurrency, rates))
                .toList();
    }

    private static InstrumentQuote quoteOf(Holding h, Map<Long, InstrumentQuote> quoteById) {
        return h.getInstrument() == null ? null : quoteById.get(h.getInstrument().getId());
    }

    /** A quoted amount in the holding's currency; empty when the amount or the exchange rate is missing. */
    static Optional<BigDecimal> inHoldingCurrency(BigDecimal quoted, Holding h, FxService.Snapshot rates) {
        String quoteCurrency = h.getInstrument() == null ? null : h.getInstrument().getCurrency();
        return quoteCurrency == null ? Optional.ofNullable(quoted) : rates.convert(quoted, quoteCurrency, h.getCurrency());
    }

    /** Pure valuation of one holding; package-visible for tests. */
    static HoldingValue value(Holding h, InstrumentQuote quote, Valuation valuation,
                              String baseCurrency, FxService.Snapshot rates) {
        BigDecimal qty = h.getQuantity();
        BigDecimal price;
        PriceSource source;
        java.time.Instant asOf = null;
        BigDecimal local;
        BigDecimal dayChangeLocal = BigDecimal.ZERO;
        BigDecimal dayChangePercent = null;

        if (h.getAssetType().isCash()) {
            price = BigDecimal.ONE;
            source = PriceSource.CASH;
            local = qty;
        } else if (quote != null && inHoldingCurrency(quote.getPrice(), h, rates).isPresent()) {
            price = inHoldingCurrency(quote.getPrice(), h, rates).orElseThrow();
            source = PriceSource.QUOTE;
            asOf = quote.getQuoteTime();
            local = qty.multiply(price);
            dayChangeLocal = inHoldingCurrency(quote.getDayChange(), h, rates).map(qty::multiply).orElse(BigDecimal.ZERO);
            dayChangePercent = quote.getChangePercent();
        } else if (valuation != null && qty.signum() != 0) { // a sold-out holding is worth nothing
            local = valuation.getValue();
            price = local.divide(qty, Money.MC);
            source = PriceSource.VALUATION;
            asOf = valuation.getValuationDate().atStartOfDay().toInstant(ZoneOffset.UTC);
        } else {
            price = h.getAverageCost();
            source = PriceSource.COST;
            local = h.costBasis();
        }

        String ccy = h.getCurrency();
        var market = rates.convert(local, ccy, baseCurrency);
        if (market.isEmpty()) {
            return new HoldingValue(h, price, source, asOf, local, dayChangeLocal, false,
                    null, null, null, null, null, null, dayChangePercent);
        }
        BigDecimal cost = rates.convert(h.costBasis(), ccy, baseCurrency).orElseThrow();
        return new HoldingValue(h, price, source, asOf, local, dayChangeLocal, true,
                market.get(),
                cost,
                market.get().subtract(cost),
                rates.convert(h.getRealizedPnl(), ccy, baseCurrency).orElseThrow(),
                rates.convert(h.getNetIncome(), ccy, baseCurrency).orElseThrow(),
                rates.convert(dayChangeLocal, ccy, baseCurrency).orElseThrow(),
                dayChangePercent);
    }
}
