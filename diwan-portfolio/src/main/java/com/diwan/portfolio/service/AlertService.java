package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.domain.AlertType;
import com.diwan.portfolio.entity.AlertRule;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Instrument;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.portfolio.entity.Portfolio;
import com.diwan.portfolio.entity.PortfolioAlert;
import com.diwan.portfolio.kafka.AlertPublisher;
import com.diwan.portfolio.kafka.PortfolioAlertEvent;
import com.diwan.portfolio.repository.AlertRuleRepository;
import com.diwan.portfolio.repository.PortfolioAlertRepository;
import com.diwan.portfolio.web.dto.Requests.AlertRuleRequest;
import com.diwan.portfolio.web.dto.Responses.AlertResponse;
import com.diwan.portfolio.web.dto.Responses.AlertRuleResponse;
import com.diwan.portfolio.web.dto.Responses.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Alert rules, their evaluation (after each quote refresh, and daily for allocation drift) and the alert
 * inbox. A rule that fired stays quiet for the cooldown period (default 24 h) so a price hovering around
 * the threshold does not produce a stream of alerts.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    static final int MAX_RULES = 100;

    private final AlertRuleRepository rules;
    private final PortfolioAlertRepository alerts;
    private final InstrumentService instruments;
    private final HoldingService holdings;
    private final PortfolioService portfolios;
    private final AllocationService allocation;
    private final PreferenceService preferences;
    private final AlertPublisher publisher;
    private final Labels labels;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Duration cooldown;

    public AlertService(AlertRuleRepository rules, PortfolioAlertRepository alerts, InstrumentService instruments,
                        HoldingService holdings, PortfolioService portfolios, AllocationService allocation,
                        PreferenceService preferences, AlertPublisher publisher, Labels labels,
                        TransactionTemplate tx, Clock clock,
                        @Value("${diwan.portfolio.alerts.cooldown-hours:24}") long cooldownHours) {
        this.rules = rules;
        this.alerts = alerts;
        this.instruments = instruments;
        this.holdings = holdings;
        this.portfolios = portfolios;
        this.allocation = allocation;
        this.preferences = preferences;
        this.publisher = publisher;
        this.labels = labels;
        this.tx = tx;
        this.clock = clock;
        this.cooldown = Duration.ofHours(cooldownHours);
    }

    // ---- rules ---------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> listRules(Long userId) {
        return rules.findByUserIdOrderByIdAsc(userId).stream().map(this::view).toList();
    }

    public AlertRuleResponse createRule(Long userId, AlertRuleRequest req) {
        Instrument instrument = req.type().needsSymbol() ? instruments.resolve(requireSymbol(req)) : null;
        return tx.execute(s -> {
            if (rules.countByUserId(userId) >= MAX_RULES) {
                throw ApiException.conflict("alert.limit_reached", MAX_RULES);
            }
            AlertRule r = new AlertRule();
            r.setUserId(userId);
            apply(r, req, userId, instrument);
            return view(rules.save(r));
        });
    }

    public AlertRuleResponse updateRule(Long userId, Long ruleId, AlertRuleRequest req) {
        Instrument instrument = req.type().needsSymbol() ? instruments.resolve(requireSymbol(req)) : null;
        return tx.execute(s -> {
            AlertRule r = rules.findByIdAndUserId(ruleId, userId)
                    .orElseThrow(() -> ApiException.notFound("alert.rule_not_found"));
            apply(r, req, userId, instrument);
            r.setLastTriggeredAt(null); // a changed rule may fire again right away
            return view(rules.save(r));
        });
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        rules.delete(rules.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> ApiException.notFound("alert.rule_not_found")));
    }

    private static String requireSymbol(AlertRuleRequest req) {
        if (req.symbol() == null || req.symbol().isBlank()) {
            throw ApiException.badRequest("alert.symbol_required");
        }
        return req.symbol();
    }

    private void apply(AlertRule r, AlertRuleRequest req, Long userId, Instrument instrument) {
        AlertType type = req.type();
        r.setType(type);
        r.setInstrument(instrument);
        r.setHolding(null);
        r.setPortfolio(null);
        if (type.needsHolding()) {
            if (req.holdingId() == null) throw ApiException.badRequest("alert.holding_required");
            r.setHolding(holdings.requireOwned(req.holdingId(), userId));
        }
        if (type.needsPortfolio()) {
            if (req.portfolioId() == null) throw ApiException.badRequest("alert.portfolio_required");
            r.setPortfolio(portfolios.requireOwned(req.portfolioId(), userId));
        }
        if ((type == AlertType.LOSS_PERCENT || type == AlertType.ALLOCATION_DRIFT || type == AlertType.DAILY_MOVE_PERCENT)
                && req.threshold().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("alert.threshold_too_large");
        }
        r.setThreshold(req.threshold());
        r.setActive(req.active() == null || req.active());
        r.setNote(req.note());
    }

    // ---- inbox ---------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> list(Long userId, boolean unreadOnly, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<PortfolioAlert> p = unreadOnly
                ? alerts.findByUserIdAndReadAtIsNullOrderByTriggeredAtDescIdDesc(userId, pr)
                : alerts.findByUserIdOrderByTriggeredAtDescIdDesc(userId, pr);
        Locale locale = Labels.locale();
        return new PageResponse<>(p.getContent().stream().map(a -> view(a, locale)).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return alerts.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public AlertResponse markRead(Long userId, Long alertId) {
        PortfolioAlert a = alerts.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> ApiException.notFound("alert.not_found"));
        if (a.getReadAt() == null) {
            a.setReadAt(clock.instant());
        }
        return view(a, Labels.locale());
    }

    @Transactional
    public int markAllRead(Long userId) {
        return alerts.markAllRead(userId, clock.instant());
    }

    // ---- evaluation ----------------------------------------------------------------------------------

    /** Checks the rules affected by a fresh quote of the instrument. Returns the number of alerts raised. */
    public int evaluateQuote(Instrument instrument, InstrumentQuote quote) {
        List<Triggered> fired = tx.execute(s -> {
            List<Triggered> out = new ArrayList<>();
            for (AlertRule r : rules.findActiveForInstrument(instrument.getId())) {
                observed(r, quote).filter(v -> breached(r.getType(), v, r.getThreshold()))
                        .ifPresent(v -> fire(r, v, instrument.getSymbol()).ifPresent(out::add));
            }
            return out;
        });
        fired.forEach(this::publish);
        return fired.size();
    }

    /** Daily: portfolios whose allocation drifted past the rule's threshold. */
    public int evaluateDrift() {
        List<Triggered> fired = tx.execute(s -> {
            List<Triggered> out = new ArrayList<>();
            for (AlertRule r : rules.findByActiveTrueAndType(AlertType.ALLOCATION_DRIFT)) {
                BigDecimal drift = allocation.maxDrift(r.getPortfolio());
                if (drift != null && breached(r.getType(), drift, r.getThreshold())) {
                    fire(r, drift, null).ifPresent(out::add);
                }
            }
            return out;
        });
        fired.forEach(this::publish);
        return fired.size();
    }

    /** The value a rule compares with its threshold, if it can be computed from this quote. */
    static Optional<BigDecimal> observed(AlertRule r, InstrumentQuote q) {
        return switch (r.getType()) {
            case PRICE_ABOVE, PRICE_BELOW -> Optional.of(q.getPrice());
            case DAILY_MOVE_PERCENT -> Optional.ofNullable(q.getChangePercent()).map(BigDecimal::abs);
            case GAIN_PERCENT, LOSS_PERCENT -> {
                Holding h = r.getHolding();
                if (h == null || h.isClosed() || h.getQuantity().signum() <= 0 || h.getAverageCost().signum() <= 0) {
                    yield Optional.empty();
                }
                yield Optional.of(q.getPrice().subtract(h.getAverageCost())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(h.getAverageCost(), MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_EVEN));
            }
            case ALLOCATION_DRIFT -> Optional.empty();
        };
    }

    static boolean breached(AlertType type, BigDecimal observed, BigDecimal threshold) {
        return switch (type) {
            case PRICE_ABOVE, DAILY_MOVE_PERCENT, GAIN_PERCENT, ALLOCATION_DRIFT -> observed.compareTo(threshold) >= 0;
            case PRICE_BELOW -> observed.compareTo(threshold) <= 0;
            case LOSS_PERCENT -> observed.compareTo(threshold.negate()) <= 0;
        };
    }

    private Optional<Triggered> fire(AlertRule r, BigDecimal observed, String symbol) {
        Instant now = clock.instant();
        if (r.getLastTriggeredAt() != null && r.getLastTriggeredAt().plus(cooldown).isAfter(now)) {
            return Optional.empty();
        }
        r.setLastTriggeredAt(now);
        PortfolioAlert a = new PortfolioAlert();
        a.setUserId(r.getUserId());
        a.setRuleId(r.getId());
        a.setType(r.getType());
        a.setSymbol(symbol != null ? symbol : r.getHolding() != null && r.getHolding().getInstrument() != null
                ? r.getHolding().getInstrument().getSymbol() : null);
        a.setHoldingId(r.getHolding() == null ? null : r.getHolding().getId());
        a.setPortfolioId(r.getPortfolio() != null ? r.getPortfolio().getId()
                : r.getHolding() != null ? r.getHolding().getPortfolio().getId() : null);
        a.setSubject(subjectOf(r, a.getSymbol()));
        a.setObservedValue(observed);
        a.setThreshold(r.getThreshold());
        a.setTriggeredAt(now);
        alerts.save(a);
        log.info("[Alerts] rule {} ({}) fired for user {}", r.getId(), r.getType(), r.getUserId());
        return Optional.of(new Triggered(a));
    }

    private static String subjectOf(AlertRule r, String symbol) {
        if (r.getHolding() != null) return r.getHolding().getName();
        if (r.getPortfolio() != null) return r.getPortfolio().getName();
        return symbol;
    }

    private void publish(Triggered t) {
        PortfolioAlert a = t.alert();
        Locale locale = preferences.localeOf(a.getUserId());
        AlertResponse v = view(a, locale);
        publisher.publish(PortfolioAlertEvent.of(a.getId(), a.getUserId(), a.getType().name(),
                locale.getLanguage(), v.title(), v.message(), a.getSymbol(), a.getHoldingId(), a.getPortfolioId(),
                a.getObservedValue(), a.getThreshold()));
    }

    // ---- views ---------------------------------------------------------------------------------------

    private AlertRuleResponse view(AlertRule r) {
        return new AlertRuleResponse(r.getId(), r.getType(), labels.enumLabel(r.getType()),
                r.getInstrument() == null ? null : r.getInstrument().getSymbol(),
                r.getHolding() == null ? null : r.getHolding().getId(),
                r.getPortfolio() == null ? null : r.getPortfolio().getId(),
                r.getThreshold().stripTrailingZeros(), r.isActive(), r.getNote(), r.getLastTriggeredAt(), r.getCreatedAt());
    }

    AlertResponse view(PortfolioAlert a, Locale locale) {
        String key = "alert." + a.getType().name();
        String observed = a.getObservedValue().setScale(2, RoundingMode.HALF_EVEN).toPlainString();
        String threshold = a.getThreshold().stripTrailingZeros().toPlainString();
        return new AlertResponse(a.getId(), a.getRuleId(), a.getType(),
                labels.message(locale, "AlertType." + a.getType().name()), a.getSymbol(), a.getHoldingId(),
                a.getPortfolioId(),
                labels.message(locale, key + ".title", a.getSubject()),
                labels.message(locale, key + ".message", a.getSubject(), observed, threshold),
                a.getObservedValue().stripTrailingZeros(), a.getThreshold().stripTrailingZeros(),
                a.getTriggeredAt(), a.getReadAt());
    }

    private record Triggered(PortfolioAlert alert) {}
}
