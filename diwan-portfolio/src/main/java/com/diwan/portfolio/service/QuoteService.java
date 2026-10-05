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

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
        return provider.quote(instrument.getSymbol()).map(q -> store(instrument.getId(), q));
    }

    @Transactional
    public InstrumentQuote store(Long instrumentId, MarketDataProvider.Quote q) {
        InstrumentQuote row = quotes.findById(instrumentId).orElseGet(() -> new InstrumentQuote(instrumentId));
        row.setPrice(q.price());
        row.setPreviousClose(q.previousClose());
        row.setDayChange(q.change());
        row.setChangePercent(q.changePercent());
        row.setDayHigh(q.high());
        row.setDayLow(q.low());
        row.setQuoteTime(q.time() != null ? q.time() : clock.instant());
        row.setFetchedAt(clock.instant());
        row.setProvider(provider.name());
        return quotes.save(row);
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
