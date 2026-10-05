package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.domain.TransactionType;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.PortfolioTransaction;
import com.diwan.portfolio.repository.HoldingRepository;
import com.diwan.portfolio.repository.TransactionRepository;
import com.diwan.portfolio.web.dto.Requests.TransactionRequest;
import com.diwan.portfolio.web.dto.Responses.TransactionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Every change to a holding's transactions locks the holding, applies the change and replays the full
 * history ({@link HoldingCalculator}); an impossible history (selling more than held) rolls everything back.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final HoldingRepository holdings;
    private final Views views;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, HoldingRepository holdings, Views views, Clock clock) {
        this.transactions = transactions;
        this.holdings = holdings;
        this.views = views;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long userId, Long holdingId) {
        Holding h = holdings.findOwned(holdingId, userId).orElseThrow(() -> ApiException.notFound("holding.not_found"));
        return transactions.findByHoldingIdOrderByTradeDateAscIdAsc(h.getId()).stream()
                .sorted(Comparator.comparing(PortfolioTransaction::getTradeDate)
                        .thenComparing(PortfolioTransaction::getId).reversed())
                .map(views::transaction).toList();
    }

    @Transactional
    public TransactionResponse add(Long userId, Long holdingId, TransactionRequest req) {
        Holding h = lock(userId, holdingId);
        PortfolioTransaction t = new PortfolioTransaction();
        t.setHolding(h);
        apply(t, req, h);
        transactions.save(t);
        recompute(h);
        return views.transaction(t);
    }

    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest req) {
        PortfolioTransaction t = transactions.findOwned(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("transaction.not_found"));
        Holding h = lock(userId, t.getHolding().getId());
        apply(t, req, h);
        transactions.save(t);
        recompute(h);
        return views.transaction(t);
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        PortfolioTransaction t = transactions.findOwned(transactionId, userId)
                .orElseThrow(() -> ApiException.notFound("transaction.not_found"));
        Holding h = lock(userId, t.getHolding().getId());
        transactions.delete(t);
        transactions.flush();
        recompute(h);
    }

    /** Called inside the caller's transaction after the holding was created (initial transaction). */
    void addInitial(Holding h, TransactionRequest req) {
        PortfolioTransaction t = new PortfolioTransaction();
        t.setHolding(h);
        apply(t, req, h);
        transactions.save(t);
        recompute(h);
    }

    private Holding lock(Long userId, Long holdingId) {
        return holdings.lockOwned(holdingId, userId).orElseThrow(() -> ApiException.notFound("holding.not_found"));
    }

    private void recompute(Holding h) {
        transactions.flush();
        HoldingCalculator.Position p = HoldingCalculator.replay(transactions.findByHoldingIdOrderByTradeDateAscIdAsc(h.getId()));
        h.setQuantity(p.quantity());
        h.setAverageCost(p.averageCost());
        h.setRealizedPnl(p.realizedPnl());
        h.setNetIncome(p.netIncome());
        holdings.save(h);
    }

    /** Validates the request against the holding's asset type and copies only the fields the type uses. */
    private void apply(PortfolioTransaction t, TransactionRequest req, Holding h) {
        TransactionType type = req.type();
        if (!type.allowedFor(h.getAssetType())) {
            throw ApiException.badRequest("transaction.type_not_allowed", h.getAssetType());
        }
        if (req.tradeDate().isAfter(LocalDate.now(clock).plusDays(1))) {
            throw ApiException.badRequest("transaction.future_date");
        }
        t.setType(type);
        t.setTradeDate(req.tradeDate());
        t.setFees(req.fees() == null ? BigDecimal.ZERO : req.fees());
        t.setNote(req.note());
        if (type.isTrade()) {
            if (req.quantity() == null || req.price() == null) {
                throw ApiException.badRequest("transaction.quantity_price_required");
            }
            t.setQuantity(req.quantity());
            t.setPrice(req.price());
            t.setAmount(null);
        } else {
            if (req.amount() == null) {
                throw ApiException.badRequest("transaction.amount_required");
            }
            t.setAmount(req.amount());
            t.setQuantity(null);
            t.setPrice(null);
        }
    }
}
