package com.diwan.portfolio.market;

import com.diwan.portfolio.domain.AssetType;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * <a href="https://www.sahmk.sa/en/developers">SAHMK</a>: Saudi Exchange (Tadawul) prices, about 15 minutes
 * delayed on the free plan. Symbols are stored as {@code 2222.SR}; SAHMK itself uses {@code 2222}.
 * <p>
 * The free plan allows 100 requests per day for the whole account and has no bulk quotes, so every refresh of
 * every symbol costs one request. Three things keep it inside that: {@link DailyQuota} is a hard limit,
 * {@link #dueForRefresh} spreads the allowance over the trading day, and search and profiles run on a company
 * list loaded once a week instead of a request per keystroke. SAHMK's terms require the {@link #credits() credit}
 * wherever its data is shown. The key goes in the X-API-Key header, never in the URL.
 */
public class SahmkMarketDataProvider implements MarketDataProvider {

    public static final String SUFFIX = ".SR";

    private static final Logger log = LoggerFactory.getLogger(SahmkMarketDataProvider.class);
    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");
    private static final Set<DayOfWeek> TRADING_DAYS = Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);
    private static final Duration COMPANIES_TTL = Duration.ofDays(7);
    private static final Duration COMPANIES_RETRY = Duration.ofHours(1);
    private static final int COMPANIES_PAGE = 500;
    private static final int COMPANIES_MAX_PAGES = 10;

    /**
     * @param dailyRequests hard limit per day
     * @param reservedRequests part of it kept for users adding new symbols, not used by the scheduled refresh
     * @param open start of the refresh window, Riyadh time
     * @param close end of the window; later than the market close so the delayed closing price is picked up
     * @param minRefresh shortest time between refreshes of a symbol (no use going below the data delay)
     */
    public record Settings(int dailyRequests, int reservedRequests, int callsPerMinute, LocalTime open, LocalTime close,
                           Duration minRefresh) {}

    private record Company(String code, String nameEn, String nameAr, AssetType type) {}

    private final RestClient http;
    private final Settings settings;
    private final Clock clock;
    private final DailyQuota quota;
    private final RateLimiter limiter;
    private final Map<String, Instant> lastAttempt = new ConcurrentHashMap<>();
    private Map<String, Company> companies = Map.of();
    private Instant companiesLoadedAt;
    private Instant companiesTriedAt;

    public SahmkMarketDataProvider(RestClient.Builder builder, String baseUrl, String apiKey, Settings settings, Clock clock) {
        this.http = builder.baseUrl(baseUrl).defaultHeader("X-API-Key", apiKey).build();
        this.settings = settings;
        this.clock = clock;
        this.quota = new DailyQuota(settings.dailyRequests(), RIYADH, clock);
        this.limiter = new RateLimiter(settings.callsPerMinute());
    }

    @Override
    public String name() {
        return "sahmk";
    }

    @Override
    public List<String> credits() {
        return List.of("Saudi market data provided by SAHMK");
    }

    @Override
    public Optional<Quote> quote(String symbol) {
        lastAttempt.put(symbol, clock.instant());
        return call("quote " + symbol, client -> client.get().uri("/quote/{symbol}/", code(symbol))
                .retrieve().toEntity(JsonNode.class))
                .filter(n -> n.path("price").asDouble() > 0)
                .map(n -> new Quote(decimal(n, "price"), decimal(n, "previous_close"), decimal(n, "change"),
                        decimal(n, "change_percent"), decimal(n, "high"), decimal(n, "low"),
                        time(n.path("updated_at").asText(null))));
    }

    /**
     * Symbols never priced are always due. Others are due while the market is open, once per interval: the
     * window divided by what the allowance can pay for, so more tracked symbols means fewer refreshes each.
     */
    @Override
    public Set<String> dueForRefresh(Map<String, Instant> lastFetched) {
        Instant now = clock.instant();
        boolean open = isRefreshWindow(now);
        Duration interval = refreshInterval(lastFetched.size());
        Map<String, Instant> last = new HashMap<>();
        lastFetched.forEach((symbol, fetched) -> last.put(symbol, latest(fetched, lastAttempt.get(symbol))));

        Set<String> due = new LinkedHashSet<>();
        last.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter(e -> e.getValue() == null || (open && !e.getValue().plus(interval).isAfter(now)))
                .forEach(e -> due.add(e.getKey()));
        return due;
    }

    Duration refreshInterval(int symbols) {
        Duration window = Duration.between(settings.open(), settings.close());
        int budget = Math.max(1, settings.dailyRequests() - settings.reservedRequests());
        Duration spread = window.multipliedBy(Math.max(1, symbols)).dividedBy(budget);
        return spread.compareTo(settings.minRefresh()) > 0 ? spread : settings.minRefresh();
    }

    boolean isRefreshWindow(Instant at) {
        ZonedDateTime riyadh = at.atZone(RIYADH);
        LocalTime time = riyadh.toLocalTime();
        return TRADING_DAYS.contains(riyadh.getDayOfWeek())
                && !time.isBefore(settings.open()) && time.isBefore(settings.close());
    }

    @Override
    public List<SymbolMatch> search(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        return companies().values().stream()
                .filter(c -> c.code().contains(q) || contains(c.nameEn(), q) || contains(c.nameAr(), q))
                .sorted(Comparator.comparing(Company::code))
                .limit(20)
                .map(c -> new SymbolMatch(c.code() + SUFFIX, displayName(c), c.type() == null ? null : c.type().name()))
                .toList();
    }

    /** Without the company list (outage, or the allowance is gone) a symbol that has a price is still accepted. */
    @Override
    public Optional<Profile> profile(String symbol) {
        Map<String, Company> known = companies();
        if (known.isEmpty()) {
            return quote(symbol).map(q -> saudi(symbol, null, null));
        }
        return Optional.ofNullable(known.get(code(symbol))).map(c -> saudi(symbol, displayName(c), c.type()));
    }

    private static Profile saudi(String symbol, String name, AssetType type) {
        return new Profile(symbol, name, "TADAWUL", "SA", "SAR", type);
    }

    /** The free plan has no news; {@link RoutingMarketDataProvider} asks the default provider instead. */
    @Override
    public List<NewsItem> news(String symbol, LocalDate from, LocalDate to) {
        return List.of();
    }

    private synchronized Map<String, Company> companies() {
        Instant now = clock.instant();
        boolean fresh = companiesLoadedAt != null && companiesLoadedAt.plus(COMPANIES_TTL).isAfter(now);
        boolean tried = companiesTriedAt != null && companiesTriedAt.plus(COMPANIES_RETRY).isAfter(now);
        if (fresh || tried) {
            return companies;
        }
        companiesTriedAt = now;
        Map<String, Company> loaded = new HashMap<>();
        for (int page = 0; page < COMPANIES_MAX_PAGES; page++) {
            int offset = loaded.size();
            Optional<JsonNode> body = call("companies", client -> client.get()
                    .uri(b -> b.path("/companies/").queryParam("limit", COMPANIES_PAGE).queryParam("offset", offset).build())
                    .retrieve().toEntity(JsonNode.class));
            if (body.isEmpty()) {
                return companies; // keep the previous list rather than a partial one
            }
            for (JsonNode c : body.get().path("results")) {
                String code = c.path("symbol").asText("");
                if (!code.isBlank()) {
                    loaded.put(code, new Company(code, c.path("name_en").asText(null), c.path("name_ar").asText(null),
                            assetType(c)));
                }
            }
            if (loaded.size() == offset || loaded.size() >= body.get().path("total").asInt(0)) {
                break;
            }
        }
        companies = Map.copyOf(loaded);
        companiesLoadedAt = now;
        log.info("[Sahmk] company list loaded: {} symbols", companies.size());
        return companies;
    }

    private static AssetType assetType(JsonNode c) {
        if (c.path("is_etf").asBoolean()) {
            return AssetType.ETF;
        }
        return switch (c.path("security_type").asText("")) {
            case "Equity" -> AssetType.STOCK;
            case "ETF" -> AssetType.ETF;
            case "Closed-End Fund" -> AssetType.MUTUAL_FUND;
            case "Sukuk" -> AssetType.BOND;
            default -> null;
        };
    }

    private Optional<JsonNode> call(String what, Function<RestClient, ResponseEntity<JsonNode>> request) {
        if (!quota.tryAcquire()) {
            log.warn("[Sahmk] {} skipped: daily allowance used up", what);
            return Optional.empty();
        }
        limiter.acquire();
        try {
            ResponseEntity<JsonNode> response = request.apply(http);
            reportUsage(response);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientResponseException e) {
            // A 429 with an "upgrade" object means the daily quota; without it, only the per-minute limit
            if (e.getStatusCode().value() == 429 && e.getResponseBodyAsString().contains("\"upgrade\"")) {
                quota.exhaust();
            }
            log.warn("[Sahmk] {} failed: HTTP {}", what, e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("[Sahmk] {} failed: {}", what, e.getMessage());
        }
        return Optional.empty();
    }

    private void reportUsage(ResponseEntity<?> response) {
        String limit = response.getHeaders().getFirst("X-RateLimit-Limit");
        String remaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
        if (limit != null && remaining != null) {
            try {
                quota.reportUsed(Integer.parseInt(limit.trim()) - Integer.parseInt(remaining.trim()));
            } catch (NumberFormatException e) {
                log.debug("[Sahmk] unreadable rate limit headers: {} / {}", limit, remaining);
            }
        }
    }

    private Instant time(String updatedAt) {
        try {
            return updatedAt == null ? clock.instant() : OffsetDateTime.parse(updatedAt).toInstant();
        } catch (DateTimeParseException e) {
            return clock.instant();
        }
    }

    private static String code(String symbol) {
        return symbol.endsWith(SUFFIX) ? symbol.substring(0, symbol.length() - SUFFIX.length()) : symbol;
    }

    private static String displayName(Company c) {
        return Objects.requireNonNullElse(c.nameEn(), Objects.requireNonNullElse(c.nameAr(), c.code()));
    }

    private static boolean contains(String text, String lowerCaseQuery) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null || b == null) {
            return a == null ? b : a;
        }
        return a.isAfter(b) ? a : b;
    }

    private static BigDecimal decimal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.decimalValue();
    }
}
