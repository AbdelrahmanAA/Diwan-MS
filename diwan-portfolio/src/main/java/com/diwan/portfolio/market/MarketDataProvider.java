package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Source of quotes, symbol lookup and company news. Implementations must not throw for an unknown symbol or
 * a provider outage: they return empty results and log, so one bad symbol never stops a job.
 * A provider covering another market is added by implementing this interface and routing its symbols to it
 * (see RoutingMarketDataProvider and MarketDataConfig).
 */
public interface MarketDataProvider {

    String name();

    /** The provider that answers for this symbol (differs from {@link #name()} when routing). */
    default String nameFor(String symbol) {
        return name();
    }

    /** Credits the provider's terms require wherever its data is shown. */
    default List<String> credits() {
        return List.of();
    }

    Optional<Quote> quote(String symbol);

    /**
     * Which of these symbols should be fetched now. A provider with a small allowance returns fewer than it
     * was given, outside trading hours for example.
     * @param lastFetched symbol -> time of its stored quote, null when it has none
     */
    default Set<String> dueForRefresh(Map<String, Instant> lastFetched) {
        return lastFetched.keySet();
    }

    List<SymbolMatch> search(String query);

    Optional<Profile> profile(String symbol);

    List<NewsItem> news(String symbol, LocalDate from, LocalDate to);

    record Quote(BigDecimal price, BigDecimal previousClose, BigDecimal change, BigDecimal changePercent,
                 BigDecimal high, BigDecimal low, Instant time) {}

    record SymbolMatch(String symbol, String description, String type) {}

    /** Any field except symbol may be null when the provider does not know it. */
    record Profile(String symbol, String name, String exchange, String countryCode, String currency, AssetType assetType) {}

    record NewsItem(String url, String headline, String summary, String source, String imageUrl,
                    Instant publishedAt, String language) {}
}
