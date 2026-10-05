package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.domain.AssetType;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.entity.Tag;
import com.diwan.portfolio.repository.HoldingRepository;
import com.diwan.portfolio.repository.TagRepository;
import com.diwan.portfolio.web.dto.Requests.HoldingRequest;
import com.diwan.portfolio.web.dto.Responses.HoldingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;

@Service
public class HoldingService {

    static final int MAX_HOLDINGS_PER_PORTFOLIO = 500;

    private final HoldingRepository holdings;
    private final TagRepository tags;
    private final PortfolioService portfolios;
    private final InstrumentService instruments;
    private final TransactionService transactions;
    private final ValuationService valuation;
    private final Views views;
    private final TransactionTemplate tx;

    public HoldingService(HoldingRepository holdings, TagRepository tags, PortfolioService portfolios,
                          InstrumentService instruments, TransactionService transactions, ValuationService valuation,
                          Views views, TransactionTemplate tx) {
        this.holdings = holdings;
        this.tags = tags;
        this.portfolios = portfolios;
        this.instruments = instruments;
        this.transactions = transactions;
        this.valuation = valuation;
        this.views = views;
        this.tx = tx;
    }

    public Holding requireOwned(Long holdingId, Long userId) {
        return holdings.findOwned(holdingId, userId).orElseThrow(() -> ApiException.notFound("holding.not_found"));
    }

    @Transactional(readOnly = true)
    public HoldingResponse get(Long userId, Long holdingId) {
        return view(requireOwned(holdingId, userId));
    }

    /**
     * The symbol is resolved first (it may call the market data provider, so it runs outside the database
     * transaction); then the holding and its optional first transaction are saved together.
     */
    public HoldingResponse create(Long userId, Long portfolioId, HoldingRequest req) {
        portfolios.requireOwned(portfolioId, userId);
        Instrument instrument = resolveInstrument(req);
        Long id = tx.execute(status -> {
            Portfolio p = portfolios.requireOwned(portfolioId, userId);
            if (holdings.countByPortfolioId(p.getId()) >= MAX_HOLDINGS_PER_PORTFOLIO) {
                throw ApiException.conflict("holding.limit_reached", MAX_HOLDINGS_PER_PORTFOLIO);
            }
            Holding h = new Holding();
            h.setPortfolio(p);
            apply(h, req, instrument);
            holdings.saveAndFlush(h);
            if (req.initialTransaction() != null) {
                transactions.addInitial(h, req.initialTransaction());
            }
            return h.getId();
        });
        return tx.execute(status -> view(requireOwned(id, userId)));
    }

    public HoldingResponse update(Long userId, Long holdingId, HoldingRequest req) {
        requireOwned(holdingId, userId);
        Instrument instrument = resolveInstrument(req);
        return tx.execute(status -> {
            Holding h = requireOwned(holdingId, userId);
            boolean typeChanged = h.getAssetType() != req.assetType();
            if (typeChanged && h.getQuantity().signum() != 0) {
                throw ApiException.conflict("holding.type_change_not_allowed");
            }
            apply(h, req, instrument);
            holdings.saveAndFlush(h);
            return view(h);
        });
    }

    @Transactional
    public HoldingResponse setClosed(Long userId, Long holdingId, boolean closed) {
        Holding h = requireOwned(holdingId, userId);
        h.setClosed(closed);
        return view(holdings.saveAndFlush(h));
    }

    @Transactional
    public void delete(Long userId, Long holdingId) {
        holdings.delete(requireOwned(holdingId, userId));
    }

    @Transactional
    public HoldingResponse setTags(Long userId, Long holdingId, List<Long> tagIds) {
        Holding h = requireOwned(holdingId, userId);
        List<Tag> found = tagIds.isEmpty() ? List.of() : tags.findByUserIdAndIdIn(userId, new HashSet<>(tagIds));
        if (found.size() != new HashSet<>(tagIds).size()) {
            throw ApiException.notFound("tag.not_found");
        }
        h.getTags().clear();
        h.getTags().addAll(found);
        return view(holdings.saveAndFlush(h));
    }

    private HoldingResponse view(Holding h) {
        var rates = valuation.fxSnapshot();
        HoldingValue v = valuation.value(List.of(h), h.getPortfolio().getBaseCurrency(), rates).get(0);
        return views.holding(v, null);
    }

    private Instrument resolveInstrument(HoldingRequest req) {
        if (req.symbol() == null || req.symbol().isBlank()) {
            return null;
        }
        if (!req.assetType().isMarketTraded()) {
            throw ApiException.badRequest("holding.symbol_not_allowed", req.assetType());
        }
        return instruments.resolve(req.symbol());
    }

    private void apply(Holding h, HoldingRequest req, Instrument instrument) {
        AssetType type = req.assetType();
        String country = req.countryCode() != null ? req.countryCode()
                : instrument != null ? instrument.getCountryCode() : null;
        String currency = req.currency() != null ? req.currency()
                : instrument != null ? instrument.getCurrency() : null;
        if (country == null) {
            throw ApiException.badRequest("holding.country_required");
        }
        if (!Labels.isCountry(country)) {
            throw ApiException.badRequest("country.unknown", country);
        }
        if (currency == null) {
            throw ApiException.badRequest("holding.currency_required");
        }
        if (!Labels.isCurrency(currency)) {
            throw ApiException.badRequest("currency.unknown", currency);
        }
        if (h.getId() != null && !currency.equals(h.getCurrency()) && h.getQuantity().signum() != 0) {
            throw ApiException.conflict("holding.currency_change_not_allowed");
        }
        h.setAssetType(type);
        h.setName(req.name().trim());
        h.setCountryCode(country);
        h.setCurrency(currency);
        h.setInstrument(instrument);
        h.setNotes(req.notes());
        h.setAttributes(views.writeAttributes(req.attributes()));
    }
}
