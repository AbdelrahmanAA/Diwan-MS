package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Source of quotes, symbol lookup and company news. Implementations must not throw for an unknown symbol or
 * a provider outage: they return empty results and log, so one bad symbol never stops a job.
 * A paid provider covering more markets is added by implementing this interface (see MarketDataConfig).
 */
public interface MarketDataProvider {

    String name();

    Optional<Quote> quote(String symbol);

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
