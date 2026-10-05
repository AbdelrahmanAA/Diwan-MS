package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.market.MarketDataProvider;
import com.diwan.portfolio.repository.InstrumentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Turns a symbol typed by a user into a known instrument (looked up once at the provider, then stored). */
@Service
public class InstrumentService {

    private static final Pattern SYMBOL = Pattern.compile("[A-Z0-9.:^=/_-]{1,32}");

    private final InstrumentRepository instruments;
    private final MarketDataProvider provider;
    private final QuoteService quotes;

    public InstrumentService(InstrumentRepository instruments, MarketDataProvider provider, QuoteService quotes) {
        this.instruments = instruments;
        this.provider = provider;
        this.quotes = quotes;
    }

    public static String normalize(String symbol) {
        String s = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (!SYMBOL.matcher(s).matches()) {
            throw ApiException.badRequest("instrument.invalid_symbol", symbol);
        }
        return s;
    }

    public Instrument find(String symbol) {
        return instruments.findBySymbol(normalize(symbol))
                .orElseThrow(() -> ApiException.notFound("instrument.not_found", symbol));
    }

    /** Finds or registers the symbol; unknown symbols are rejected. Fetches a first quote for new ones. */
    public Instrument resolve(String rawSymbol) {
        String symbol = normalize(rawSymbol);
        var existing = instruments.findBySymbol(symbol);
        if (existing.isPresent()) {
            return existing.get();
        }
        MarketDataProvider.Profile p = provider.profile(symbol)
                .orElseThrow(() -> ApiException.badRequest("instrument.unknown", symbol));
        Instrument i = new Instrument(symbol);
        i.setName(p.name());
        i.setExchange(p.exchange());
        i.setCountryCode(Labels.isCountry(p.countryCode()) ? p.countryCode() : null);
        i.setCurrency(Labels.isCurrency(p.currency()) ? p.currency() : null);
        i.setAssetType(p.assetType());
        Instrument saved;
        try {
            saved = instruments.saveAndFlush(i);
        } catch (DataIntegrityViolationException e) { // registered concurrently by another request
            return instruments.findBySymbol(symbol).orElseThrow(() -> e);
        }
        quotes.refresh(saved);
        return saved;
    }

    public List<MarketDataProvider.SymbolMatch> search(String query) {
        if (query == null || query.isBlank() || query.length() > 40) {
            throw ApiException.badRequest("instrument.invalid_query");
        }
        return provider.search(query.trim());
    }

    public String providerName() {
        return provider.name();
    }
}
