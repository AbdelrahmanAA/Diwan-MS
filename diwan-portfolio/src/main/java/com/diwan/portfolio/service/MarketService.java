package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.market.Gold;
import com.diwan.portfolio.web.dto.Responses.GoldPriceResponse;
import com.diwan.portfolio.web.dto.Responses.GoldPricesResponse;
import com.diwan.portfolio.web.dto.Responses.InstrumentResponse;
import com.diwan.portfolio.web.dto.Responses.PricePoint;
import com.diwan.portfolio.web.dto.Responses.SymbolMatchResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** Read-only market information for the app: symbol search, instrument + quote, price history. */
@Service
public class MarketService {

    /** A quote older than this is refreshed when someone opens the instrument. */
    private static final Duration STALE = Duration.ofMinutes(15);

    private final InstrumentService instruments;
    private final QuoteService quotes;
    private final FxService fx;
    private final PreferenceService preferences;
    private final Views views;
    private final Clock clock;

    public MarketService(InstrumentService instruments, QuoteService quotes, FxService fx,
                         PreferenceService preferences, Views views, Clock clock) {
        this.instruments = instruments;
        this.quotes = quotes;
        this.fx = fx;
        this.preferences = preferences;
        this.views = views;
        this.clock = clock;
    }

    public List<SymbolMatchResponse> search(String query) {
        return instruments.search(query).stream()
                .map(m -> new SymbolMatchResponse(m.symbol(), m.description(), m.type())).toList();
    }

    /** @param currency optional: also answers the price in this currency, for gold held in any currency */
    public InstrumentResponse instrument(String symbol, String currency) {
        Instrument i = instruments.resolve(symbol);
        InstrumentQuote q = currentQuote(i);
        return views.instrument(i, q, currency == null ? null : priceIn(currency, i, q, fx.snapshot()));
    }

    private InstrumentQuote currentQuote(Instrument i) {
        return quotes.latest(i.getId())
                .filter(x -> x.getFetchedAt().plus(STALE).isAfter(clock.instant()))
                .or(() -> quotes.refreshIfDue(i))
                .or(() -> quotes.latest(i.getId()))
                .orElse(null);
    }

    /** One gram of 24K and 21K and one troy ounce of pure gold. Needs no holding or watchlist entry. */
    public GoldPricesResponse goldPrices(Long userId, String currency) {
        String target = currency != null ? currency : preferences.baseCurrencyOf(userId);
        if (!Labels.isCurrency(target)) {
            throw ApiException.badRequest("currency.unknown", target);
        }
        Instrument pure = instruments.resolve(Gold.symbol(24));
        Instrument karat21 = instruments.resolve(Gold.symbol(21));
        return goldPrices(target, pure, currentQuote(pure), karat21, currentQuote(karat21), fx.snapshot());
    }

    static GoldPricesResponse goldPrices(String currency, Instrument pure, InstrumentQuote pureQuote,
                                         Instrument karat21, InstrumentQuote karat21Quote, FxService.Snapshot rates) {
        BigDecimal gram = priceIn(currency, pure, pureQuote, rates);
        BigDecimal ounce = gram == null ? null
                : gram.multiply(Gold.GRAMS_PER_TROY_OUNCE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal change = pureQuote == null ? null : pureQuote.getChangePercent();
        return new GoldPricesResponse(currency, pureQuote == null ? null : pureQuote.getQuoteTime(), List.of(
                new GoldPriceResponse(24, "GRAM", gram, change),
                new GoldPriceResponse(21, "GRAM", priceIn(currency, karat21, karat21Quote, rates),
                        karat21Quote == null ? null : karat21Quote.getChangePercent()),
                new GoldPriceResponse(24, "OUNCE", ounce, change)));
    }

    /** Null when there is no quote or no exchange rate for the currency. */
    static BigDecimal priceIn(String currency, Instrument i, InstrumentQuote q, FxService.Snapshot rates) {
        if (q == null || i.getCurrency() == null) {
            return null;
        }
        return rates.convert(q.getPrice(), i.getCurrency(), currency)
                .map(p -> p.setScale(4, RoundingMode.HALF_EVEN)).orElse(null);
    }

    public List<PricePoint> history(String symbol, int days) {
        if (days < 1 || days > 3650) {
            throw ApiException.badRequest("market.invalid_range");
        }
        Instrument i = instruments.find(symbol);
        return quotes.history(i.getId(), days).stream()
                .map(h -> new PricePoint(h.getPriceDate(), h.getClosePrice().stripTrailingZeros())).toList();
    }
}
