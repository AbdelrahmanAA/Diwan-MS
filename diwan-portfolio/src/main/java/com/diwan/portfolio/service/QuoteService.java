package com.diwan.portfolio.service;

import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.entity.PriceHistory;
import com.diwan.portfolio.market.MarketDataProvider;
import com.diwan.portfolio.repository.InstrumentQuoteRepository;
import com.diwan.portfolio.repository.PriceHistoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Fetches and stores quotes; keeps the daily close history. */
@Service
public class QuoteService {

    private final MarketDataProvider provider;
    private final InstrumentQuoteRepository quotes;
    private final PriceHistoryRepository history;
    private final EntityManager em;
    private final Clock clock;

    public QuoteService(MarketDataProvider provider, InstrumentQuoteRepository quotes, PriceHistoryRepository history,
                        EntityManager em, Clock clock) {
        this.provider = provider;
        this.quotes = quotes;
        this.history = history;
        this.em = em;
        this.clock = clock;
    }

    /** Fetches the current quote (outside any transaction: it is a network call) and stores it. */
    public Optional<InstrumentQuote> refresh(Instrument instrument) {
        String symbol = instrument.getSymbol();
        return provider.quote(symbol).map(q -> store(instrument.getId(), q, provider.nameFor(symbol)));
    }

    /** Refreshes the instrument only if its provider's allowance has room for it right now. */
    public Optional<InstrumentQuote> refreshIfDue(Instrument instrument) {
        List<Instrument> all = new ArrayList<>(trackedInstruments());
        if (all.stream().noneMatch(i -> i.getId().equals(instrument.getId()))) {
            all.add(instrument);
        }
        boolean due = dueForRefresh(all).stream().anyMatch(i -> i.getId().equals(instrument.getId()));
        return due ? refresh(instrument) : Optional.empty();
    }

    /** The instruments, out of these, that their providers want fetched now. */
    public List<Instrument> dueForRefresh(List<Instrument> instruments) {
        Map<Long, Instant> fetched = new HashMap<>();
        quotes.findAllById(instruments.stream().map(Instrument::getId).toList())
                .forEach(q -> fetched.put(q.getInstrumentId(), q.getFetchedAt()));
        Map<String, Instant> lastFetched = new LinkedHashMap<>();
        instruments.forEach(i -> lastFetched.put(i.getSymbol(), fetched.get(i.getId())));
        Set<String> due = provider.dueForRefresh(lastFetched);
        return instruments.stream().filter(i -> due.contains(i.getSymbol())).toList();
    }

    public InstrumentQuote store(Long instrumentId, MarketDataProvider.Quote q) {
        return store(instrumentId, q, provider.name());
    }

    @Transactional
    public InstrumentQuote store(Long instrumentId, MarketDataProvider.Quote quote, String providerName) {
        MarketDataProvider.Quote q = withDayChange(instrumentId, quote);
        InstrumentQuote row = quotes.findById(instrumentId).orElseGet(() -> new InstrumentQuote(instrumentId));
        row.setPrice(q.price());
        row.setPreviousClose(q.previousClose());
        row.setDayChange(q.change());
        row.setChangePercent(q.changePercent());
        row.setDayHigh(q.high());
        row.setDayLow(q.low());
        row.setQuoteTime(q.time() != null ? q.time() : clock.instant());
        row.setFetchedAt(clock.instant());
        row.setProvider(providerName);
        return quotes.save(row);
    }

    /** A provider without a previous close (gold) gets its day change from the newest close in the history. */
    private MarketDataProvider.Quote withDayChange(Long instrumentId, MarketDataProvider.Quote q) {
        if (q.previousClose() != null && q.change() != null) {
            return q;
        }
        BigDecimal previous = q.previousClose() != null ? q.previousClose()
                : history.findFirstByInstrumentIdAndPriceDateBeforeOrderByPriceDateDesc(instrumentId, LocalDate.now(clock))
                        .map(PriceHistory::getClosePrice).orElse(null);
        if (previous == null || previous.signum() == 0) {
            return q;
        }
        BigDecimal change = q.price().subtract(previous);
        BigDecimal percent = change.multiply(BigDecimal.valueOf(100)).divide(previous, 4, RoundingMode.HALF_EVEN);
        return new MarketDataProvider.Quote(q.price(), previous, change, percent, q.high(), q.low(), q.time());
    }

    @Transactional(readOnly = true)
    public Optional<InstrumentQuote> latest(Long instrumentId) {
        return quotes.findById(instrumentId);
    }

    /** Instruments someone cares about: open holdings, watchlists and active alert rules. */
    @Transactional(readOnly = true)
    public List<Instrument> trackedInstruments() {
        return em.createQuery("""
                select distinct i from Instrument i where
                  exists (select 1 from Holding h where h.instrument = i and h.closed = false)
                  or exists (select 1 from WatchlistItem w where w.instrument = i)
                  or exists (select 1 from AlertRule r where r.instrument = i and r.active = true)
                order by i.symbol""", Instrument.class).getResultList();
    }

    /** Copies every stored quote into the history as that quote's trading-day close. */
    @Transactional
    public int recordDailyCloses() {
        int n = 0;
        for (InstrumentQuote q : quotes.findAll()) {
            LocalDate day = LocalDate.ofInstant(q.getQuoteTime(), ZoneOffset.UTC);
            PriceHistory row = history.findById(new PriceHistory.Key(q.getInstrumentId(), day))
                    .orElseGet(() -> new PriceHistory(q.getInstrumentId(), day, q.getPrice()));
            row.setClosePrice(q.getPrice());
            history.save(row);
            n++;
        }
        return n;
    }

    @Transactional(readOnly = true)
    public List<PriceHistory> history(Long instrumentId, int days) {
        return history.findByInstrumentIdAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                instrumentId, LocalDate.now(clock).minusDays(days));
    }
}
