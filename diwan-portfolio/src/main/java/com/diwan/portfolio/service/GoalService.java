package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Goal;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.repository.GoalRepository;
import com.diwan.portfolio.web.dto.Requests.GoalRequest;
import com.diwan.portfolio.web.dto.Responses.GoalResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Savings goals measured against the portfolio's current market value (no growth assumption). */
@Service
public class GoalService {

    private final GoalRepository goals;
    private final PortfolioService portfolios;
    private final SummaryService summaries;
    private final Clock clock;

    public GoalService(GoalRepository goals, PortfolioService portfolios, SummaryService summaries, Clock clock) {
        this.goals = goals;
        this.portfolios = portfolios;
        this.summaries = summaries;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(Long userId, Long portfolioId) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        BigDecimal current = currentValue(p);
        return goals.findByPortfolioIdOrderByIdAsc(p.getId()).stream().map(g -> view(g, current)).toList();
    }

    @Transactional
    public GoalResponse create(Long userId, Long portfolioId, GoalRequest req) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        if (goals.findByPortfolioIdOrderByIdAsc(p.getId()).size() >= 20) {
            throw ApiException.conflict("goal.limit_reached", 20);
        }
        Goal g = new Goal();
        g.setPortfolio(p);
        apply(g, req);
        return view(goals.save(g), currentValue(p));
    }

    @Transactional
    public GoalResponse update(Long userId, Long goalId, GoalRequest req) {
        Goal g = goals.findOwned(goalId, userId).orElseThrow(() -> ApiException.notFound("goal.not_found"));
        apply(g, req);
        return view(goals.save(g), currentValue(g.getPortfolio()));
    }

    @Transactional
    public void delete(Long userId, Long goalId) {
        goals.delete(goals.findOwned(goalId, userId).orElseThrow(() -> ApiException.notFound("goal.not_found")));
    }

    private void apply(Goal g, GoalRequest req) {
        if (req.targetDate() != null && !req.targetDate().isAfter(LocalDate.now(clock))) {
            throw ApiException.badRequest("goal.date_in_past");
        }
        g.setName(req.name().trim());
        g.setTargetAmount(req.targetAmount());
        g.setTargetDate(req.targetDate());
    }

    private BigDecimal currentValue(Portfolio p) {
        return Money.nz(summaries.totals(summaries.valued(p, null, false).values()).marketValue());
    }

    GoalResponse view(Goal g, BigDecimal current) {
        BigDecimal remaining = g.getTargetAmount().subtract(current).max(BigDecimal.ZERO);
        Integer months = null;
        BigDecimal monthly = null;
        if (g.getTargetDate() != null) {
            months = (int) Math.max(0, ChronoUnit.MONTHS.between(LocalDate.now(clock), g.getTargetDate()));
            monthly = months == 0 ? remaining : remaining.divide(BigDecimal.valueOf(months), Money.MC);
        }
        BigDecimal progress = Money.percent(current, g.getTargetAmount());
        return new GoalResponse(g.getId(), g.getPortfolio().getId(), g.getName(), Money.round(g.getTargetAmount()),
                g.getTargetDate(), g.getPortfolio().getBaseCurrency(), Money.round(current),
                progress == null ? null : progress.min(BigDecimal.valueOf(100)),
                Money.round(remaining), months, Money.round(monthly), remaining.signum() == 0);
    }
}
