package com.diwan.portfolio.market;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Sends each symbol to the provider that covers it: {@code GOLD-*} to the gold provider, {@code *.SR} to the
 * Saudi provider, everything else to the default one. Without a gold or Saudi provider those symbols go to
 * the default provider too. News always comes from the default provider, the only one that has any.
 */
public class RoutingMarketDataProvider implements MarketDataProvider {

    private final MarketDataProvider fallback;
    private final MarketDataProvider gold;
    private final MarketDataProvider saudi;

    /** @param gold may be null @param saudi may be null */
    public RoutingMarketDataProvider(MarketDataProvider fallback, MarketDataProvider gold, MarketDataProvider saudi) {
        this.fallback = fallback;
        this.gold = gold;
        this.saudi = saudi;
    }

    private MarketDataProvider route(String symbol) {
        if (gold != null && Gold.isGold(symbol)) {
            return gold;
        }
        if (saudi != null && symbol.endsWith(SahmkMarketDataProvider.SUFFIX)) {
            return saudi;
        }
        return fallback;
    }

    private Stream<MarketDataProvider> all() {
        return Stream.of(gold, saudi, fallback).filter(Objects::nonNull);
    }

    @Override
    public String name() {
        return fallback.name();
    }

    @Override
    public String nameFor(String symbol) {
        return route(symbol).name();
    }

    @Override
    public List<String> credits() {
        return all().flatMap(p -> p.credits().stream()).toList();
    }

    @Override
    public Optional<Quote> quote(String symbol) {
        return route(symbol).quote(symbol);
    }

    @Override
    public Set<String> dueForRefresh(Map<String, Instant> lastFetched) {
        Map<MarketDataProvider, Map<String, Instant>> byProvider = new LinkedHashMap<>();
        lastFetched.forEach((symbol, fetched) ->
                byProvider.computeIfAbsent(route(symbol), p -> new HashMap<>()).put(symbol, fetched));
        Set<String> due = new LinkedHashSet<>();
        byProvider.forEach((provider, symbols) -> due.addAll(provider.dueForRefresh(symbols)));
        return due;
    }

    @Override
    public List<SymbolMatch> search(String query) {
        Map<String, SymbolMatch> bySymbol = new LinkedHashMap<>();
        all().forEach(p -> p.search(query).forEach(m -> bySymbol.putIfAbsent(m.symbol(), m)));
        return bySymbol.values().stream().limit(20).toList();
    }

    @Override
    public Optional<Profile> profile(String symbol) {
        return route(symbol).profile(symbol);
    }

    @Override
    public List<NewsItem> news(String symbol, LocalDate from, LocalDate to) {
        return fallback.news(Gold.isGold(symbol) ? Gold.NEWS_SYMBOL : symbol, from, to);
    }
}
