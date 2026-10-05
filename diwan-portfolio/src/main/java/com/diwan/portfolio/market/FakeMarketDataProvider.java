package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Offline provider for DEV, tests and demos (used when no FINNHUB_API_KEY is set). It knows a fixed set of
 * symbols across several markets; prices move slowly and deterministically with the clock, so two calls in
 * the same 5-minute window return the same quote.
 */
public class FakeMarketDataProvider implements MarketDataProvider {

    private record Known(String name, String exchange, String country, String currency, AssetType type, double basePrice) {}

    private static final Map<String, Known> KNOWN = Map.ofEntries(
            Map.entry("AAPL", new Known("Apple Inc", "NASDAQ", "US", "USD", AssetType.STOCK, 190)),
            Map.entry("MSFT", new Known("Microsoft Corp", "NASDAQ", "US", "USD", AssetType.STOCK, 410)),
            Map.entry("NVDA", new Known("NVIDIA Corp", "NASDAQ", "US", "USD", AssetType.STOCK, 120)),
            Map.entry("GOOGL", new Known("Alphabet Inc", "NASDAQ", "US", "USD", AssetType.STOCK, 170)),
            Map.entry("AMZN", new Known("Amazon.com Inc", "NASDAQ", "US", "USD", AssetType.STOCK, 180)),
            Map.entry("TSLA", new Known("Tesla Inc", "NASDAQ", "US", "USD", AssetType.STOCK, 240)),
            Map.entry("VOO", new Known("Vanguard S&P 500 ETF", "NYSE ARCA", "US", "USD", AssetType.ETF, 480)),
            Map.entry("SPY", new Known("SPDR S&P 500 ETF Trust", "NYSE ARCA", "US", "USD", AssetType.ETF, 520)),
            Map.entry("GLD", new Known("SPDR Gold Shares", "NYSE ARCA", "US", "USD", AssetType.COMMODITY, 220)),
            Map.entry("COMI.CA", new Known("Commercial International Bank", "EGX", "EG", "EGP", AssetType.STOCK, 80)),
            Map.entry("2222.SR", new Known("Saudi Arabian Oil Co", "TADAWUL", "SA", "SAR", AssetType.STOCK, 28)),
            Map.entry("EMAAR.AE", new Known("Emaar Properties", "DFM", "AE", "AED", AssetType.STOCK, 9)),
            Map.entry("BTC-USD", new Known("Bitcoin", "CRYPTO", null, "USD", AssetType.CRYPTO, 60000)));

    private final Clock clock;

    public FakeMarketDataProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public Optional<Quote> quote(String symbol) {
        Known k = KNOWN.get(symbol);
        if (k == null) {
            return Optional.empty();
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        Instant bucket = now.minusSeconds(now.getEpochSecond() % 300);
        Instant dayStart = LocalDate.ofInstant(now, ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal price = priceAt(symbol, k.basePrice(), bucket);
        BigDecimal prev = priceAt(symbol, k.basePrice(), dayStart);
        BigDecimal change = price.subtract(prev);
        BigDecimal pct = change.multiply(BigDecimal.valueOf(100)).divide(prev, 4, RoundingMode.HALF_EVEN);
        return Optional.of(new Quote(price, prev, change, pct,
                price.max(prev), price.min(prev), bucket));
    }

    /** Slow sine wave (+-8%) with a per-symbol phase, sampled at the given instant. */
    static BigDecimal priceAt(String symbol, double base, Instant at) {
        double hours = at.getEpochSecond() / 3600.0;
        double phase = (symbol.hashCode() & 0xffff) / 1000.0;
        double value = base * (1 + 0.08 * Math.sin(hours / 30 + phase) + 0.01 * Math.sin(hours * 1.7 + phase));
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_EVEN);
    }

    @Override
    public List<SymbolMatch> search(String query) {
        String q = query.trim().toUpperCase(Locale.ROOT);
        List<SymbolMatch> out = new ArrayList<>();
        KNOWN.forEach((symbol, k) -> {
            if (symbol.contains(q) || k.name().toUpperCase(Locale.ROOT).contains(q)) {
                out.add(new SymbolMatch(symbol, k.name(), k.type().name()));
            }
        });
        out.sort((a, b) -> a.symbol().compareTo(b.symbol()));
        return out;
    }

    @Override
    public Optional<Profile> profile(String symbol) {
        Known k = KNOWN.get(symbol);
        return k == null ? Optional.empty()
                : Optional.of(new Profile(symbol, k.name(), k.exchange(), k.country(), k.currency(), k.type()));
    }

    @Override
    public List<NewsItem> news(String symbol, LocalDate from, LocalDate to) {
        Known k = KNOWN.get(symbol);
        if (k == null) {
            return List.of();
        }
        Instant day = to.atTime(9, 0).toInstant(ZoneOffset.UTC);
        String slug = symbol.toLowerCase(Locale.ROOT);
        return List.of(
                new NewsItem("https://example.com/news/" + slug + "/" + to + "/results",
                        k.name() + " reports quarterly results",
                        "Demo article generated by the offline market data provider.", "Diwan Demo", null, day, "en"),
                new NewsItem("https://example.com/news/" + slug + "/" + to + "/outlook",
                        "Analysts update their outlook on " + k.name(),
                        "Demo article generated by the offline market data provider.", "Diwan Demo", null,
                        day.minus(3, ChronoUnit.HOURS), "en"));
    }
}
