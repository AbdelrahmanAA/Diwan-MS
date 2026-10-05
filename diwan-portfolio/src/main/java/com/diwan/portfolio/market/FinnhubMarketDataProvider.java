package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * <a href="https://finnhub.io/docs/api">Finnhub</a>: /quote, /search, /stock/profile2 and /company-news.
 * The free plan covers US listings well and other markets poorly; it is limited to 60 calls per minute, so
 * calls are spaced out ({@link RateLimiter}). The API key goes in the X-Finnhub-Token header (never in the
 * URL, so it cannot end up in access logs).
 */
public class FinnhubMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(FinnhubMarketDataProvider.class);

    private final RestClient http;
    private final RateLimiter limiter;

    public FinnhubMarketDataProvider(RestClient.Builder builder, String baseUrl, String apiKey, int callsPerMinute) {
        this.http = builder.baseUrl(baseUrl).defaultHeader("X-Finnhub-Token", apiKey).build();
        this.limiter = new RateLimiter(callsPerMinute);
    }

    @Override
    public String name() {
        return "finnhub";
    }

    @Override
    public Optional<Quote> quote(String symbol) {
        return call("quote " + symbol, client -> client.get()
                .uri(b -> b.path("/quote").queryParam("symbol", symbol).build())
                .retrieve().body(JsonNode.class))
                .filter(n -> n.path("t").asLong() > 0 && n.path("c").asDouble() > 0) // unknown symbol: all zeros
                .map(n -> new Quote(
                        decimal(n, "c"), decimal(n, "pc"), decimal(n, "d"), decimal(n, "dp"),
                        decimal(n, "h"), decimal(n, "l"), Instant.ofEpochSecond(n.path("t").asLong())));
    }

    @Override
    public List<SymbolMatch> search(String query) {
        return call("search", client -> client.get()
                .uri(b -> b.path("/search").queryParam("q", query).build())
                .retrieve().body(JsonNode.class))
                .map(n -> {
                    List<SymbolMatch> out = new ArrayList<>();
                    for (JsonNode r : n.path("result")) {
                        out.add(new SymbolMatch(r.path("symbol").asText(), r.path("description").asText(),
                                r.path("type").asText(null)));
                        if (out.size() == 20) break;
                    }
                    return out;
                }).orElse(List.of());
    }

    @Override
    public Optional<Profile> profile(String symbol) {
        Optional<JsonNode> node = call("profile " + symbol, client -> client.get()
                .uri(b -> b.path("/stock/profile2").queryParam("symbol", symbol).build())
                .retrieve().body(JsonNode.class));
        if (node.isPresent() && node.get().hasNonNull("name")) {
            JsonNode n = node.get();
            String country = n.path("country").asText(null);
            return Optional.of(new Profile(symbol, n.path("name").asText(null), n.path("exchange").asText(null),
                    country == null ? null : country.toUpperCase(Locale.ROOT),
                    n.path("currency").asText(null), AssetType.STOCK));
        }
        // ETFs, funds and most non-US symbols have no free profile: accept the symbol when it has a quote
        return quote(symbol).map(q -> new Profile(symbol, null, null, null, null, null));
    }

    @Override
    public List<NewsItem> news(String symbol, LocalDate from, LocalDate to) {
        return call("news " + symbol, client -> client.get()
                .uri(b -> b.path("/company-news").queryParam("symbol", symbol)
                        .queryParam("from", from.toString()).queryParam("to", to.toString()).build())
                .retrieve().body(JsonNode.class))
                .map(n -> {
                    List<NewsItem> out = new ArrayList<>();
                    for (JsonNode a : n) {
                        String url = a.path("url").asText("");
                        String headline = a.path("headline").asText("");
                        if (url.isBlank() || headline.isBlank()) continue;
                        out.add(new NewsItem(url, headline, blankToNull(a.path("summary").asText(null)),
                                a.path("source").asText(null), blankToNull(a.path("image").asText(null)),
                                Instant.ofEpochSecond(a.path("datetime").asLong()), "en"));
                    }
                    return out;
                }).orElse(List.of());
    }

    private Optional<JsonNode> call(String what, Function<RestClient, JsonNode> request) {
        limiter.acquire();
        try {
            return Optional.ofNullable(request.apply(http));
        } catch (RestClientResponseException e) {
            log.warn("[Finnhub] {} failed: HTTP {}", what, e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("[Finnhub] {} failed: {}", what, e.getMessage());
        }
        return Optional.empty();
    }

    private static BigDecimal decimal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.decimalValue();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
