package com.diwan.portfolio.service;

import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.entity.PortfolioTransaction;
import com.diwan.portfolio.entity.Tag;
import com.diwan.portfolio.entity.Valuation;
import com.diwan.portfolio.web.dto.Responses.HoldingResponse;
import com.diwan.portfolio.web.dto.Responses.InstrumentResponse;
import com.diwan.portfolio.web.dto.Responses.PortfolioResponse;
import com.diwan.portfolio.web.dto.Responses.QuoteResponse;
import com.diwan.portfolio.web.dto.Responses.TagResponse;
import com.diwan.portfolio.web.dto.Responses.TransactionResponse;
import com.diwan.portfolio.web.dto.Responses.ValuationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Entity -> response mapping, with labels in the request language and money rounded for display. */
@Component
public class Views {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final Labels labels;
    private final ObjectMapper json;

    public Views(Labels labels, ObjectMapper json) {
        this.labels = labels;
        this.json = json;
    }

    public PortfolioResponse portfolio(Portfolio p) {
        return new PortfolioResponse(p.getId(), p.getName(), p.getDescription(), p.getBaseCurrency(), p.getColor(),
                p.getIcon(), p.getSortOrder(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public TagResponse tag(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getColor());
    }

    /** @param weightBase total market value the weight is relative to (null = no weight) */
    public HoldingResponse holding(HoldingValue v, BigDecimal weightBase) {
        Holding h = v.holding();
        Instrument i = h.getInstrument();
        return new HoldingResponse(
                h.getId(), h.getPortfolio().getId(), h.getName(),
                h.getAssetType(), labels.enumLabel(h.getAssetType()),
                h.getCountryCode(), labels.country(h.getCountryCode()),
                h.getCurrency(), i == null ? null : i.getSymbol(), i == null ? null : i.getName(),
                h.getQuantity().stripTrailingZeros(), h.getAverageCost().stripTrailingZeros(),
                Money.round(h.costBasis()),
                v.price() == null ? null : v.price().stripTrailingZeros(), v.priceSource(), v.priceAsOf(),
                Money.round(v.marketValueLocal()),
                v.convertible(), Money.round(v.marketValue()), Money.round(v.costBasis()),
                Money.round(v.unrealizedPnl()), v.unrealizedPnlPercent(), Money.round(v.realizedPnl()),
                Money.round(v.netIncome()), Money.round(v.totalReturn()), Money.round(v.dayChange()),
                v.dayChangePercent() == null ? null : Money.round(v.dayChangePercent()),
                weightBase == null || !v.convertible() ? null : Money.percent(v.marketValue(), weightBase),
                h.getNotes(), attributes(h.getAttributes()), h.isClosed(),
                h.getTags().stream().sorted(Comparator.comparing(Tag::getId)).map(this::tag).toList(),
                h.getCreatedAt());
    }

    public TransactionResponse transaction(PortfolioTransaction t) {
        return new TransactionResponse(t.getId(), t.getHolding().getId(), t.getType(), labels.enumLabel(t.getType()),
                t.getTradeDate(), strip(t.getQuantity()), strip(t.getPrice()), Money.round(t.getAmount()),
                Money.round(t.getFees()), t.getNote(), t.getCreatedAt());
    }

    public ValuationResponse valuation(Valuation v) {
        return new ValuationResponse(v.getId(), v.getHolding().getId(), v.getValuationDate(),
                Money.round(v.getValue()), v.getNote(), v.getCreatedAt());
    }

    public QuoteResponse quote(String symbol, InstrumentQuote q) {
        return q == null ? null : new QuoteResponse(symbol, strip(q.getPrice()), strip(q.getPreviousClose()),
                strip(q.getDayChange()), q.getChangePercent() == null ? null : Money.round(q.getChangePercent()),
                strip(q.getDayHigh()), strip(q.getDayLow()), q.getQuoteTime(), q.getProvider());
    }

    public InstrumentResponse instrument(Instrument i, InstrumentQuote q) {
        return new InstrumentResponse(i.getSymbol(), i.getName(), i.getExchange(), i.getCountryCode(),
                labels.country(i.getCountryCode()), i.getCurrency(), i.getAssetType(),
                labels.enumLabel(i.getAssetType()), quote(i.getSymbol(), q));
    }

    public String writeAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        try {
            String s = json.writeValueAsString(attributes);
            if (s.length() > 8000) {
                throw com.diwan.common.web.ApiException.badRequest("holding.attributes_too_large");
            }
            return s;
        } catch (JsonProcessingException e) {
            throw com.diwan.common.web.ApiException.badRequest("holding.attributes_invalid");
        }
    }

    private Map<String, Object> attributes(String s) {
        if (s == null || s.isBlank()) {
            return Map.of();
        }
        try {
            return json.readValue(s, MAP);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static BigDecimal strip(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros();
    }

    public static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
