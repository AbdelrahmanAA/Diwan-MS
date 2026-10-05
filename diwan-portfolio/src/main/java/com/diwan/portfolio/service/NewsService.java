package com.diwan.portfolio.service;

import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.NewsArticle;
import com.diwan.portfolio.market.MarketDataProvider;
import com.diwan.portfolio.repository.HoldingRepository;
import com.diwan.portfolio.repository.InstrumentRepository;
import com.diwan.portfolio.repository.NewsArticleRepository;
import com.diwan.portfolio.repository.WatchlistItemRepository;
import com.diwan.portfolio.web.dto.Responses.NewsResponse;
import com.diwan.portfolio.web.dto.Responses.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Company news for the symbols a user holds or watches ("My news"). */
@Service
public class NewsService {

    private final NewsArticleRepository news;
    private final HoldingRepository holdings;
    private final WatchlistItemRepository watchlist;
    private final InstrumentRepository instruments;
    private final PortfolioService portfolios;
    private final MarketDataProvider provider;
    private final Clock clock;
    private final int lookbackDays;
    private final int retentionDays;

    public NewsService(NewsArticleRepository news, HoldingRepository holdings, WatchlistItemRepository watchlist,
                       InstrumentRepository instruments, PortfolioService portfolios, MarketDataProvider provider,
                       Clock clock,
                       @Value("${diwan.portfolio.news.lookback-days:3}") int lookbackDays,
                       @Value("${diwan.portfolio.news.retention-days:90}") int retentionDays) {
        this.news = news;
        this.holdings = holdings;
        this.watchlist = watchlist;
        this.instruments = instruments;
        this.portfolios = portfolios;
        this.provider = provider;
        this.clock = clock;
        this.lookbackDays = lookbackDays;
        this.retentionDays = retentionDays;
    }

    /**
     * @param portfolioId only symbols of this portfolio (null = all portfolios + watchlist)
     * @param symbol only this symbol (must be one the user holds or watches)
     */
    @Transactional(readOnly = true)
    public PageResponse<NewsResponse> feed(Long userId, Long portfolioId, String symbol, int page, int size) {
        Set<Long> ids = new LinkedHashSet<>();
        if (portfolioId != null) {
            portfolios.requireOwned(portfolioId, userId);
            holdings.findByPortfolioIdOrderByIdAsc(portfolioId).stream()
                    .filter(h -> !h.isClosed()).map(Holding::getInstrument).filter(Objects::nonNull)
                    .map(Instrument::getId).forEach(ids::add);
        } else {
            holdings.findOpenOfUser(userId).stream().map(Holding::getInstrument).filter(Objects::nonNull)
                    .map(Instrument::getId).forEach(ids::add);
            watchlist.findByUserIdOrderByIdAsc(userId).forEach(w -> ids.add(w.getInstrument().getId()));
        }
        if (symbol != null && !symbol.isBlank()) {
            Long only = instruments.findBySymbol(InstrumentService.normalize(symbol)).map(Instrument::getId).orElse(-1L);
            ids.retainAll(Set.of(only));
        }
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        if (ids.isEmpty()) {
            return new PageResponse<>(List.of(), pr.getPageNumber(), pr.getPageSize(), 0, 0);
        }
        Page<NewsArticle> p = news.findByInstrumentIdInOrderByPublishedAtDesc(ids, pr);
        return new PageResponse<>(p.getContent().stream().map(NewsService::view).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /** Fetches recent news of one instrument (network call, no transaction held) and stores the new items. */
    public int fetch(Instrument instrument) {
        LocalDate to = LocalDate.now(clock);
        List<MarketDataProvider.NewsItem> items = provider.news(instrument.getSymbol(), to.minusDays(lookbackDays), to);
        return store(instrument, items);
    }

    @Transactional
    public int store(Instrument instrument, List<MarketDataProvider.NewsItem> items) {
        int added = 0;
        for (MarketDataProvider.NewsItem item : items.stream().limit(30).toList()) {
            if (item.url().length() > 2048) continue;
            String hash = sha256(item.url());
            if (news.existsByInstrumentIdAndUrlHash(instrument.getId(), hash)) continue;
            NewsArticle a = new NewsArticle();
            a.setInstrument(instrument);
            a.setUrlHash(hash);
            a.setUrl(item.url());
            a.setHeadline(truncate(item.headline(), 512));
            a.setSummary(truncate(item.summary(), 4000));
            a.setSource(truncate(item.source(), 128));
            a.setImageUrl(item.imageUrl() != null && item.imageUrl().length() <= 2048 ? item.imageUrl() : null);
            a.setLanguage(item.language() == null ? "en" : item.language());
            a.setPublishedAt(item.publishedAt() != null ? item.publishedAt() : clock.instant());
            a.setFetchedAt(clock.instant());
            news.save(a);
            added++;
        }
        return added;
    }

    @Transactional
    public int deleteExpired() {
        return news.deleteOlderThan(clock.instant().minus(Duration.ofDays(retentionDays)));
    }

    private static NewsResponse view(NewsArticle a) {
        return new NewsResponse(a.getId(), a.getInstrument().getSymbol(), a.getInstrument().getName(), a.getHeadline(),
                a.getSummary(), a.getSource(), a.getUrl(), a.getImageUrl(), a.getLanguage(), a.getPublishedAt());
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
