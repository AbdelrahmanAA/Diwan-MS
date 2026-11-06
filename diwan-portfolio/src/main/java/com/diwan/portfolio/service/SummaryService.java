package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.repository.HoldingRepository;
import com.diwan.portfolio.web.dto.Responses.CombinedSummaryResponse;
import com.diwan.portfolio.web.dto.Responses.CurrencyTotals;
import com.diwan.portfolio.web.dto.Responses.GroupNode;
import com.diwan.portfolio.web.dto.Responses.HoldingResponse;
import com.diwan.portfolio.web.dto.Responses.PortfolioOverview;
import com.diwan.portfolio.web.dto.Responses.PortfolioShare;
import com.diwan.portfolio.web.dto.Responses.SummaryResponse;
import com.diwan.portfolio.web.dto.Responses.Totals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The portfolio views: totals, nested groups (country -> type by default) and per-holding P/L. */
@Service
public class SummaryService {

    public static final List<AllocationDimension> DEFAULT_GROUPING =
            List.of(AllocationDimension.COUNTRY, AllocationDimension.ASSET_TYPE);

    private final HoldingRepository holdings;
    private final ValuationService valuation;
    private final PortfolioService portfolios;
    private final PreferenceService preferences;
    private final Labels labels;
    private final Views views;

    public SummaryService(HoldingRepository holdings, ValuationService valuation, PortfolioService portfolios,
                          PreferenceService preferences, Labels labels, Views views) {
        this.holdings = holdings;
        this.valuation = valuation;
        this.portfolios = portfolios;
        this.preferences = preferences;
        this.labels = labels;
        this.views = views;
    }

    /** Open holdings of the portfolio, valued in its base currency. */
    @Transactional(readOnly = true)
    public Valued valued(Portfolio p, Long tagId, boolean includeClosed) {
        List<Holding> list = holdings.findByPortfolioIdOrderByIdAsc(p.getId()).stream()
                .filter(h -> includeClosed || !h.isClosed())
                .filter(h -> tagId == null || h.getTags().stream().anyMatch(t -> t.getId().equals(tagId)))
                .toList();
        FxService.Snapshot rates = valuation.fxSnapshot();
        return new Valued(p, valuation.value(list, p.getBaseCurrency(), rates), rates);
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(Long userId, Long portfolioId, List<AllocationDimension> groupBy, Long tagId) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        List<AllocationDimension> dims = dimensions(groupBy);
        Valued v = valued(p, tagId, false);
        Totals totals = totals(v.values());
        BigDecimal total = Money.nz(sum(v.values(), HoldingValue::marketValue));
        return new SummaryResponse(views.portfolio(p), dims, totals, currencyTotals(v.values()),
                group(v.values(), dims, 0, total), missingRates(v.values()), v.rates().asOf());
    }

    /** All open holdings of the user, whatever portfolio they are in, valued in the user's preferred currency. */
    @Transactional(readOnly = true)
    public CombinedSummaryResponse combined(Long userId, List<AllocationDimension> groupBy) {
        String currency = preferences.baseCurrencyOf(userId);
        List<AllocationDimension> dims = dimensions(groupBy);
        FxService.Snapshot rates = valuation.fxSnapshot();
        List<HoldingValue> values = valuation.value(holdings.findOpenOfUser(userId), currency, rates);
        BigDecimal total = Money.nz(sum(values, HoldingValue::marketValue));

        Map<Long, List<HoldingValue>> byPortfolio = new LinkedHashMap<>();
        values.forEach(v -> byPortfolio.computeIfAbsent(v.holding().getPortfolio().getId(), k -> new ArrayList<>()).add(v));
        List<PortfolioShare> shares = portfolios.ownedBy(userId).stream().map(p -> {
            List<HoldingValue> members = byPortfolio.getOrDefault(p.getId(), List.of());
            return new PortfolioShare(views.portfolio(p), members.size(),
                    Money.percent(sum(members, HoldingValue::marketValue), total), totals(members));
        }).sorted(Comparator.comparing((PortfolioShare s) -> s.totals().marketValue()).reversed()).toList();

        return new CombinedSummaryResponse(currency, dims, totals(values), currencyTotals(values), shares,
                values.isEmpty() ? List.of() : group(values, dims, 0, total), missingRates(values), rates.asOf());
    }

    private static List<AllocationDimension> dimensions(List<AllocationDimension> groupBy) {
        return groupBy == null || groupBy.isEmpty() ? DEFAULT_GROUPING : groupBy.stream().distinct().limit(3).toList();
    }

    @Transactional(readOnly = true)
    public List<PortfolioOverview> overviews(Long userId) {
        return portfolios.ownedBy(userId).stream().map(p -> {
            Valued v = valued(p, null, false);
            return new PortfolioOverview(views.portfolio(p), v.values().size(), totals(v.values()),
                    missingRates(v.values()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> holdingViews(Long userId, Long portfolioId, Long tagId, boolean includeClosed) {
        Portfolio p = portfolios.requireOwned(portfolioId, userId);
        Valued v = valued(p, tagId, includeClosed);
        BigDecimal total = sum(v.values().stream().filter(x -> !x.holding().isClosed()).toList(), HoldingValue::marketValue);
        return v.values().stream()
                .sorted(byValueDesc())
                .map(x -> views.holding(x, x.holding().isClosed() ? null : total)).toList();
    }

    public Totals totals(List<HoldingValue> values) {
        List<HoldingValue> ok = values.stream().filter(HoldingValue::convertible).toList();
        BigDecimal market = sum(ok, HoldingValue::marketValue);
        BigDecimal cost = sum(ok, HoldingValue::costBasis);
        BigDecimal unrealized = sum(ok, HoldingValue::unrealizedPnl);
        BigDecimal realized = sum(ok, HoldingValue::realizedPnl);
        BigDecimal income = sum(ok, HoldingValue::netIncome);
        BigDecimal day = sum(ok, HoldingValue::dayChange);
        BigDecimal totalReturn = unrealized.add(realized).add(income);
        return new Totals(Money.round(market), Money.round(cost), Money.round(unrealized),
                Money.percent(unrealized, cost), Money.round(realized), Money.round(income),
                Money.round(totalReturn), Money.percent(totalReturn, cost), Money.round(day),
                Money.percent(day, market.subtract(day)));
    }

    /** Totals of holdings in one currency, in that currency. */
    static Totals localTotals(List<HoldingValue> values) {
        BigDecimal market = sum(values, HoldingValue::marketValueLocal);
        BigDecimal cost = sum(values, v -> v.holding().costBasis());
        BigDecimal unrealized = sum(values, HoldingValue::unrealizedPnlLocal);
        BigDecimal realized = sum(values, v -> v.holding().getRealizedPnl());
        BigDecimal income = sum(values, v -> v.holding().getNetIncome());
        BigDecimal day = sum(values, HoldingValue::dayChangeLocal);
        BigDecimal totalReturn = unrealized.add(realized).add(income);
        return new Totals(Money.round(market), Money.round(cost), Money.round(unrealized),
                Money.percent(unrealized, cost), Money.round(realized), Money.round(income),
                Money.round(totalReturn), Money.percent(totalReturn, cost), Money.round(day),
                Money.percent(day, market.subtract(day)));
    }

    private List<CurrencyTotals> currencyTotals(List<HoldingValue> values) {
        Map<String, List<HoldingValue>> byCurrency = new LinkedHashMap<>();
        values.forEach(v -> byCurrency.computeIfAbsent(v.holding().getCurrency(), k -> new ArrayList<>()).add(v));
        return byCurrency.entrySet().stream()
                .map(e -> new CurrencyTotals(e.getKey(), labels.currency(e.getKey()), localTotals(e.getValue())))
                .sorted(Comparator.comparing(CurrencyTotals::currency))
                .toList();
    }

    private List<GroupNode> group(List<HoldingValue> values, List<AllocationDimension> dims, int level,
                                  BigDecimal portfolioTotal) {
        AllocationDimension dim = dims.get(level);
        Map<String, List<HoldingValue>> byKey = new LinkedHashMap<>();
        for (HoldingValue v : values) {
            byKey.computeIfAbsent(keyOf(v.holding(), dim), k -> new ArrayList<>()).add(v);
        }
        boolean leaf = level == dims.size() - 1;
        List<GroupNode> nodes = new ArrayList<>();
        byKey.forEach((key, members) -> {
            BigDecimal value = sum(members, HoldingValue::marketValue);
            boolean oneCurrency = members.stream().map(m -> m.holding().getCurrency()).distinct().count() == 1;
            nodes.add(new GroupNode(dim, key, labels.key(dim, key), Money.percent(value, portfolioTotal),
                    totals(members),
                    oneCurrency ? members.get(0).holding().getCurrency() : null,
                    oneCurrency ? localTotals(members) : null,
                    leaf ? List.of() : group(members, dims, level + 1, portfolioTotal),
                    leaf ? members.stream().sorted(byValueDesc()).map(m -> views.holding(m, portfolioTotal)).toList()
                         : List.of()));
        });
        nodes.sort(Comparator.comparing((GroupNode n) -> Money.nz(n.totals().marketValue())).reversed()
                .thenComparing(GroupNode::label));
        return nodes;
    }

    public static String keyOf(Holding h, AllocationDimension dim) {
        return switch (dim) {
            case ASSET_TYPE -> h.getAssetType().name();
            case COUNTRY -> h.getCountryCode();
            case CURRENCY -> h.getCurrency();
        };
    }

    public static List<String> missingRates(List<HoldingValue> values) {
        return values.stream().filter(v -> !v.convertible()).map(v -> v.holding().getCurrency()).distinct().sorted().toList();
    }

    public static BigDecimal sum(List<HoldingValue> values, Function<HoldingValue, BigDecimal> field) {
        return values.stream().map(field).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Comparator<HoldingValue> byValueDesc() {
        return Comparator.comparing((HoldingValue v) -> Money.nz(v.marketValue())).reversed();
    }

    public record Valued(Portfolio portfolio, List<HoldingValue> values, FxService.Snapshot rates) {}
}
