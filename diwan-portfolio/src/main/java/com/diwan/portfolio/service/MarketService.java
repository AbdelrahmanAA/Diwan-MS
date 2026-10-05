package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.web.dto.Responses.InstrumentResponse;
import com.diwan.portfolio.web.dto.Responses.PricePoint;
import com.diwan.portfolio.web.dto.Responses.SymbolMatchResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** Read-only market information for the app: symbol search, instrument + quote, price history. */
@Service
public class MarketService {

    /** A quote older than this is refreshed when someone opens the instrument. */
    private static final Duration STALE = Duration.ofMinutes(15);

    private final InstrumentService instruments;
    private final QuoteService quotes;
    private final Views views;
    private final Clock clock;

    public MarketService(InstrumentService instruments, QuoteService quotes, Views views, Clock clock) {
        this.instruments = instruments;
        this.quotes = quotes;
        this.views = views;
        this.clock = clock;
    }

    public List<SymbolMatchResponse> search(String query) {
        return instruments.search(query).stream()
                .map(m -> new SymbolMatchResponse(m.symbol(), m.description(), m.type())).toList();
    }

    public InstrumentResponse instrument(String symbol) {
        Instrument i = instruments.resolve(symbol);
        InstrumentQuote q = quotes.latest(i.getId())
                .filter(x -> x.getFetchedAt().plus(STALE).isAfter(clock.instant()))
                .or(() -> quotes.refresh(i))
                .or(() -> quotes.latest(i.getId()))
                .orElse(null);
        return views.instrument(i, q);
    }

    public List<PricePoint> history(String symbol, int days) {
        if (days < 1 || days > 3650) {
            throw ApiException.badRequest("market.invalid_range");
        }
        Instrument i = instruments.find(symbol);
        return quotes.history(i.getId(), days).stream()
                .map(h -> new PricePoint(h.getPriceDate(), h.getClosePrice().stripTrailingZeros())).toList();
    }
}
