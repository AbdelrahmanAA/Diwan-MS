package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * <a href="https://gold-api.com">gold-api.com</a>: free spot price of gold in USD per troy ounce, no key.
 * Every karat is derived from one spot price, which is kept for a minute because the terms forbid several
 * requests per second. The price has no previous close; {@code QuoteService} takes it from the price history.
 */
public class GoldApiMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(GoldApiMarketDataProvider.class);
    private static final Duration SPOT_TTL = Duration.ofMinutes(1);

    private record Spot(BigDecimal perOunce, Instant time) {}

    private final RestClient http;
    private final Clock clock;
    private Spot spot;
    private Instant spotFetchedAt;

    public GoldApiMarketDataProvider(RestClient.Builder builder, String baseUrl, Clock clock) {
        this.http = builder.baseUrl(baseUrl).build();
        this.clock = clock;
    }

    @Override
    public String name() {
        return "gold-api";
    }

    @Override
    public Optional<Quote> quote(String symbol) {
        return Gold.karat(symbol).flatMap(karat -> spot().map(s -> new Quote(Gold.perGram(s.perOunce(), karat),
                null, null, null, null, null, s.time())));
    }

    /** A failed fetch also waits for the next minute, so an outage cannot turn into a burst of requests. */
    private synchronized Optional<Spot> spot() {
        Instant now = clock.instant();
        if (spotFetchedAt == null || !spotFetchedAt.plus(SPOT_TTL).isAfter(now)) {
            spotFetchedAt = now;
            spot = fetchSpot().orElse(null);
        }
        return Optional.ofNullable(spot);
    }

    private Optional<Spot> fetchSpot() {
        try {
            JsonNode n = http.get().uri("/price/XAU").retrieve().body(JsonNode.class);
            if (n == null || n.path("price").asDouble() <= 0) {
                return Optional.empty();
            }
            return Optional.of(new Spot(n.get("price").decimalValue(), time(n.path("updatedAt").asText(null))));
        } catch (RestClientException e) {
            log.warn("[GoldApi] spot price failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Instant time(String updatedAt) {
        try {
            return updatedAt == null ? clock.instant() : Instant.parse(updatedAt);
        } catch (DateTimeParseException e) {
            return clock.instant();
        }
    }

    @Override
    public List<SymbolMatch> search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        boolean all = q.contains("gold") || q.contains("xau") || q.contains("ذهب");
        List<SymbolMatch> out = new ArrayList<>();
        for (int karat : Gold.KARATS) {
            if (all || Gold.symbol(karat).toLowerCase(Locale.ROOT).contains(q)) {
                out.add(new SymbolMatch(Gold.symbol(karat), Gold.name(karat), AssetType.COMMODITY.name()));
            }
        }
        return out;
    }

    @Override
    public Optional<Profile> profile(String symbol) {
        return Gold.karat(symbol).map(karat -> new Profile(symbol, Gold.name(karat), Gold.EXCHANGE, null,
                Gold.CURRENCY, AssetType.COMMODITY));
    }

    @Override
    public List<NewsItem> news(String symbol, LocalDate from, LocalDate to) {
        return List.of();
    }
}
