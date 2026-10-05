package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.domain.AssetType;
import com.diwan.portfolio.entity.AllocationTarget;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.repository.AllocationTargetRepository;
import com.diwan.portfolio.web.dto.Requests.TargetItem;
import com.diwan.portfolio.web.dto.Requests.TargetsRequest;
import com.diwan.portfolio.web.dto.Responses.AllocationResponse;
import com.diwan.portfolio.web.dto.Responses.AllocationRow;
import com.diwan.portfolio.web.dto.Responses.TargetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Target allocation ("60% stocks, 25% real estate...") and how far the portfolio has drifted from it.
 * Targets exist per dimension (asset type or country); within a dimension they must add up to 100%.
 */
@Service
public class AllocationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AllocationTargetRepository targets;
    private final PortfolioService portfolios;
    private final SummaryService summaries;
    private final Labels labels;

    public AllocationService(AllocationTargetRepository targets, PortfolioService portfolios, SummaryService summaries,
                             Labels labels) {
        this.targets = targets;
        this.portfolios = portfolios;
        this.summaries = summaries;
        this.labels = labels;
    }

    @Transactional(readOnly = true)
    public List<TargetResponse> getTargets(Long userId, Long portfolioId) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        return targets.findByPortfolioId(p.getId()).stream()
                .sorted(Comparator.comparing(AllocationTarget::getDimension).thenComparing(AllocationTarget::getTargetKey))
                .map(t -> new TargetResponse(t.getDimension(), t.getTargetKey(),
                        labels.key(t.getDimension(), t.getTargetKey()), t.getTargetPercent()))
                .toList();
    }

    /** Replaces the targets of one dimension; an empty list removes them. */
    @Transactional
    public List<TargetResponse> setTargets(Long userId, Long portfolioId, TargetsRequest req) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        validate(req);
        targets.deleteDimension(p.getId(), req.dimension());
        for (TargetItem item : req.targets()) {
            if (item.percent().signum() > 0) {
                targets.save(new AllocationTarget(p, req.dimension(), item.key(), item.percent()));
            }
        }
        targets.flush();
        return getTargets(userId, portfolioId);
    }

    static void validate(TargetsRequest req) {
        if (req.dimension() == AllocationDimension.CURRENCY) {
            throw ApiException.badRequest("target.dimension_not_supported");
        }
        Set<String> seen = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (TargetItem item : req.targets()) {
            boolean valid = switch (req.dimension()) {
                case ASSET_TYPE -> isAssetType(item.key());
                case COUNTRY -> Labels.isCountry(item.key());
                case CURRENCY -> false;
            };
            if (!valid) {
                throw ApiException.badRequest("target.invalid_key", item.key());
            }
            if (!seen.add(item.key())) {
                throw ApiException.badRequest("target.duplicate_key", item.key());
            }
            sum = sum.add(item.percent());
        }
        if (!req.targets().isEmpty() && sum.subtract(HUNDRED).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw ApiException.badRequest("target.sum_not_100", sum.stripTrailingZeros().toPlainString());
        }
    }

    private static boolean isAssetType(String key) {
        try {
            AssetType.valueOf(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public AllocationResponse allocation(Long userId, Long portfolioId, AllocationDimension dimension) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        return allocation(p, summaries.valued(p, null, false).values(), targets.findByPortfolioId(p.getId()), dimension);
    }

    /**
     * Current weight vs target per key. suggestedChange is what to add (+) or remove (-) to be on target,
     * keeping today's total value. Keys with a target but no holding appear with value 0.
     */
    AllocationResponse allocation(Portfolio p, List<HoldingValue> values, List<AllocationTarget> allTargets,
                                  AllocationDimension dimension) {
        Map<String, BigDecimal> targetByKey = new LinkedHashMap<>();
        allTargets.stream().filter(t -> t.getDimension() == dimension)
                .forEach(t -> targetByKey.put(t.getTargetKey(), t.getTargetPercent()));

        Map<String, BigDecimal> valueByKey = new LinkedHashMap<>();
        for (HoldingValue v : values) {
            if (v.convertible()) {
                valueByKey.merge(SummaryService.keyOf(v.holding(), dimension), v.marketValue(), BigDecimal::add);
            }
        }
        targetByKey.keySet().forEach(k -> valueByKey.putIfAbsent(k, BigDecimal.ZERO));
        BigDecimal total = valueByKey.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasTargets = !targetByKey.isEmpty();

        List<AllocationRow> rows = new ArrayList<>();
        BigDecimal maxDrift = BigDecimal.ZERO;
        for (var e : valueByKey.entrySet()) {
            BigDecimal current = total.signum() == 0 ? BigDecimal.ZERO : Money.percent(e.getValue(), total);
            BigDecimal target = hasTargets ? targetByKey.getOrDefault(e.getKey(), BigDecimal.ZERO) : null;
            BigDecimal drift = target == null ? null : current.subtract(target);
            BigDecimal change = target == null ? null
                    : Money.round(total.multiply(target).divide(HUNDRED, Money.MC).subtract(e.getValue()));
            if (drift != null && drift.abs().compareTo(maxDrift) > 0) {
                maxDrift = drift.abs();
            }
            rows.add(new AllocationRow(e.getKey(), labels.key(dimension, e.getKey()), Money.round(e.getValue()),
                    current, target, drift, change));
        }
        rows.sort(Comparator.comparing(AllocationRow::value).reversed().thenComparing(AllocationRow::label));
        return new AllocationResponse(dimension, labels.enumLabel(dimension), p.getBaseCurrency(),
                Money.round(total), hasTargets, hasTargets ? maxDrift : null, rows);
    }

    /** Largest drift of the portfolio over asset-type and country targets (null without targets). */
    @Transactional(readOnly = true)
    public BigDecimal maxDrift(Portfolio p) {
        List<AllocationTarget> all = targets.findByPortfolioId(p.getId());
        if (all.isEmpty()) {
            return null;
        }
        List<HoldingValue> values = summaries.valued(p, null, false).values();
        BigDecimal max = null;
        for (AllocationDimension d : List.of(AllocationDimension.ASSET_TYPE, AllocationDimension.COUNTRY)) {
            BigDecimal drift = allocation(p, values, all, d).maxDriftPercent();
            if (drift != null && (max == null || drift.compareTo(max) > 0)) {
                max = drift;
            }
        }
        return max;
    }
}
