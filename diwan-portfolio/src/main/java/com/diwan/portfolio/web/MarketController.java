package com.diwan.portfolio.web;

import com.diwan.portfolio.service.AlertService;
import com.diwan.portfolio.service.MarketService;
import com.diwan.portfolio.service.NewsService;
import com.diwan.portfolio.service.WatchlistService;
import com.diwan.portfolio.web.dto.Requests.AlertRuleRequest;
import com.diwan.portfolio.web.dto.Requests.WatchRequest;
import com.diwan.portfolio.web.dto.Responses.AlertResponse;
import com.diwan.portfolio.web.dto.Responses.AlertRuleResponse;
import com.diwan.portfolio.web.dto.Responses.InstrumentResponse;
import com.diwan.portfolio.web.dto.Responses.NewsResponse;
import com.diwan.portfolio.web.dto.Responses.PageResponse;
import com.diwan.portfolio.web.dto.Responses.PricePoint;
import com.diwan.portfolio.web.dto.Responses.SymbolMatchResponse;
import com.diwan.portfolio.web.dto.Responses.WatchlistItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Market data, watchlist, news and alerts. */
@RestController
@RequestMapping("/api/portfolio")
public class MarketController {

    private final MarketService market;
    private final WatchlistService watchlist;
    private final NewsService news;
    private final AlertService alerts;

    public MarketController(MarketService market, WatchlistService watchlist, NewsService news, AlertService alerts) {
        this.market = market;
        this.watchlist = watchlist;
        this.news = news;
        this.alerts = alerts;
    }

    @GetMapping("/instruments/search")
    public List<SymbolMatchResponse> search(@RequestParam String q) {
        return market.search(q);
    }

    @GetMapping("/instruments/{symbol}")
    public InstrumentResponse instrument(@PathVariable String symbol) {
        return market.instrument(symbol);
    }

    @GetMapping("/instruments/{symbol}/history")
    public List<PricePoint> history(@PathVariable String symbol, @RequestParam(defaultValue = "365") int days) {
        return market.history(symbol, days);
    }

    @GetMapping("/watchlist")
    public List<WatchlistItemResponse> watchlist(@RequestHeader("X-User-Id") Long userId) {
        return watchlist.list(userId);
    }

    @PostMapping("/watchlist")
    public List<WatchlistItemResponse> watch(@RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody WatchRequest req) {
        return watchlist.add(userId, req.symbol());
    }

    @DeleteMapping("/watchlist/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unwatch(@RequestHeader("X-User-Id") Long userId, @PathVariable String symbol) {
        watchlist.remove(userId, symbol);
    }

    /** News about the user's holdings and watchlist (optionally one portfolio or one symbol). */
    @GetMapping("/news")
    public PageResponse<NewsResponse> news(@RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(required = false) Long portfolioId,
                                           @RequestParam(required = false) String symbol,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return news.feed(userId, portfolioId, symbol, page, size);
    }

    @GetMapping("/alerts/rules")
    public List<AlertRuleResponse> rules(@RequestHeader("X-User-Id") Long userId) {
        return alerts.listRules(userId);
    }

    @PostMapping("/alerts/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertRuleResponse createRule(@RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody AlertRuleRequest req) {
        return alerts.createRule(userId, req);
    }

    @PutMapping("/alerts/rules/{id}")
    public AlertRuleResponse updateRule(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id,
                                        @Valid @RequestBody AlertRuleRequest req) {
        return alerts.updateRule(userId, id, req);
    }

    @DeleteMapping("/alerts/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        alerts.deleteRule(userId, id);
    }

    @GetMapping("/alerts")
    public PageResponse<AlertResponse> alerts(@RequestHeader("X-User-Id") Long userId,
                                              @RequestParam(defaultValue = "false") boolean unreadOnly,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return alerts.list(userId, unreadOnly, page, size);
    }

    @GetMapping("/alerts/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("unread", alerts.unreadCount(userId));
    }

    @PostMapping("/alerts/{id}/read")
    public AlertResponse markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return alerts.markRead(userId, id);
    }

    @PostMapping("/alerts/read-all")
    public Map<String, Integer> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("updated", alerts.markAllRead(userId));
    }
}
