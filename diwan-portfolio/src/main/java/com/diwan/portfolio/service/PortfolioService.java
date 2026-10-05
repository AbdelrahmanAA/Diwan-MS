package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.repository.PortfolioRepository;
import com.diwan.portfolio.web.dto.Requests.PortfolioRequest;
import com.diwan.portfolio.web.dto.Responses.PortfolioResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortfolioService {

    /** Keeps one user from filling the database; far above any real use. */
    static final int MAX_PORTFOLIOS = 50;

    private final PortfolioRepository repository;
    private final PreferenceService preferences;
    private final Views views;

    public PortfolioService(PortfolioRepository repository, PreferenceService preferences, Views views) {
        this.repository = repository;
        this.preferences = preferences;
        this.views = views;
    }

    /** Someone else's portfolio and a missing one both answer 404, so ids cannot be probed. */
    @Transactional(readOnly = true)
    public Portfolio requireOwned(Long portfolioId, Long userId) {
        return repository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> ApiException.notFound("portfolio.not_found"));
    }

    @Transactional(readOnly = true)
    public List<Portfolio> ownedBy(Long userId) {
        return repository.findByUserIdOrderBySortOrderAscIdAsc(userId);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse get(Long userId, Long id) {
        return views.portfolio(requireOwned(id, userId));
    }

    @Transactional
    public PortfolioResponse create(Long userId, PortfolioRequest req) {
        if (repository.countByUserId(userId) >= MAX_PORTFOLIOS) {
            throw ApiException.conflict("portfolio.limit_reached", MAX_PORTFOLIOS);
        }
        Portfolio p = new Portfolio();
        p.setUserId(userId);
        apply(p, req);
        if (p.getBaseCurrency() == null) {
            p.setBaseCurrency(preferences.baseCurrencyOf(userId));
        }
        return views.portfolio(repository.save(p));
    }

    @Transactional
    public PortfolioResponse update(Long userId, Long id, PortfolioRequest req) {
        Portfolio p = requireOwned(id, userId);
        apply(p, req);
        return views.portfolio(repository.saveAndFlush(p));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(requireOwned(id, userId));
    }

    private static void apply(Portfolio p, PortfolioRequest req) {
        if (req.baseCurrency() != null && !Labels.isCurrency(req.baseCurrency())) {
            throw ApiException.badRequest("currency.unknown", req.baseCurrency());
        }
        p.setName(req.name().trim());
        p.setDescription(req.description());
        if (req.baseCurrency() != null) p.setBaseCurrency(req.baseCurrency());
        p.setColor(req.color());
        p.setIcon(req.icon());
        if (req.sortOrder() != null) p.setSortOrder(req.sortOrder());
    }
}
