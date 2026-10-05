package com.diwan.portfolio.jobs;

import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.repository.PortfolioAlertRepository;
import com.diwan.portfolio.service.AlertService;
import com.diwan.portfolio.service.FxRefreshService;
import com.diwan.portfolio.service.NewsService;
import com.diwan.portfolio.service.QuoteService;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * The monitoring mechanism. Only instruments somebody holds, watches or has an alert on are fetched, each
 * once per run, so the provider's free quota goes a long way. One failing symbol never stops a run.
 * Metrics: portfolio.jobs.quotes.updated / .failed, portfolio.jobs.alerts.fired, portfolio.jobs.news.added.
 */
@Component
@ConditionalOnProperty(name = "diwan.portfolio.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataJobs {

    private static final Logger log = LoggerFactory.getLogger(MarketDataJobs.class);

    private final QuoteService quotes;
    private final AlertService alerts;
    private final NewsService news;
    private final FxRefreshService fx;
    private final PortfolioAlertRepository alertHistory;
    private final MeterRegistry metrics;
    private final TransactionTemplate tx;
    private final Clock clock;

    public MarketDataJobs(QuoteService quotes, AlertService alerts, NewsService news, FxRefreshService fx,
                          PortfolioAlertRepository alertHistory, MeterRegistry metrics, TransactionTemplate tx,
                          Clock clock) {
        this.quotes = quotes;
        this.alerts = alerts;
        this.news = news;
        this.fx = fx;
        this.alertHistory = alertHistory;
        this.metrics = metrics;
        this.tx = tx;
        this.clock = clock;
    }

    /** First start (empty fx_rate table): load exchange rates right away instead of waiting for the cron. */
    @EventListener(ApplicationReadyEvent.class)
    public void loadRatesOnFirstStart() {
        try {
            if (!fx.hasRates()) {
                log.info("[Jobs] no exchange rates yet, loading them now: {} currencies", fx.refresh());
            }
        } catch (RuntimeException e) {
            log.warn("[Jobs] initial exchange rate load failed: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "${diwan.portfolio.jobs.quotes-cron}", zone = "UTC")
    @SchedulerLock(name = "quotes", lockAtMostFor = "PT20M", lockAtLeastFor = "PT30S")
    public void refreshQuotes() {
        List<Instrument> tracked = quotes.trackedInstruments();
        int updated = 0, failed = 0, fired = 0;
        for (Instrument i : tracked) {
            try {
                var q = quotes.refresh(i);
                if (q.isPresent()) {
                    updated++;
                    fired += alerts.evaluateQuote(i, q.get());
                } else {
                    failed++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("[Jobs] quote of {} failed: {}", i.getSymbol(), e.getMessage());
            }
        }
        metrics.counter("portfolio.jobs.quotes.updated").increment(updated);
        metrics.counter("portfolio.jobs.quotes.failed").increment(failed);
        metrics.counter("portfolio.jobs.alerts.fired").increment(fired);
        log.info("[Jobs] quotes: {} tracked, {} updated, {} failed, {} alerts", tracked.size(), updated, failed, fired);
    }

    @Scheduled(cron = "${diwan.portfolio.jobs.daily-close-cron}", zone = "UTC")
    @SchedulerLock(name = "daily-close", lockAtMostFor = "PT10M")
    public void recordDailyCloses() {
        log.info("[Jobs] daily close recorded for {} instruments", quotes.recordDailyCloses());
    }

    @Scheduled(cron = "${diwan.portfolio.jobs.fx-cron}", zone = "UTC")
    @SchedulerLock(name = "fx", lockAtMostFor = "PT10M")
    public void refreshFx() {
        log.info("[Jobs] exchange rates refreshed: {} currencies", fx.refresh());
    }

    @Scheduled(cron = "${diwan.portfolio.jobs.news-cron}", zone = "UTC")
    @SchedulerLock(name = "news", lockAtMostFor = "PT30M", lockAtLeastFor = "PT30S")
    public void fetchNews() {
        int added = 0;
        for (Instrument i : quotes.trackedInstruments()) {
            try {
                added += news.fetch(i);
            } catch (RuntimeException e) {
                log.warn("[Jobs] news of {} failed: {}", i.getSymbol(), e.getMessage());
            }
        }
        metrics.counter("portfolio.jobs.news.added").increment(added);
        log.info("[Jobs] news: {} new articles", added);
    }

    @Scheduled(cron = "${diwan.portfolio.jobs.drift-cron}", zone = "UTC")
    @SchedulerLock(name = "drift", lockAtMostFor = "PT10M")
    public void checkDrift() {
        int fired = alerts.evaluateDrift();
        metrics.counter("portfolio.jobs.alerts.fired").increment(fired);
        log.info("[Jobs] allocation drift: {} alerts", fired);
    }

    @Scheduled(cron = "${diwan.portfolio.jobs.cleanup-cron}", zone = "UTC")
    @SchedulerLock(name = "cleanup", lockAtMostFor = "PT10M")
    public void cleanup() {
        int newsDeleted = news.deleteExpired();
        Integer alertsDeleted = tx.execute(s -> alertHistory.deleteOlderThan(clock.instant().minus(Duration.ofDays(365))));
        log.info("[Jobs] cleanup: {} old articles, {} old alerts removed", newsDeleted, alertsDeleted);
    }
}
