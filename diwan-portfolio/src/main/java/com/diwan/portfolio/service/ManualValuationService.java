package com.diwan.portfolio.service;

import com.diwan.common.web.ApiException;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.Valuation;
import com.diwan.portfolio.repository.ValuationRepository;
import com.diwan.portfolio.web.dto.Requests.ValuationRequest;
import com.diwan.portfolio.web.dto.Responses.ValuationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Customer-entered values for assets without a market price (property, private business, funds...). */
@Service
public class ManualValuationService {

    private final ValuationRepository valuations;
    private final HoldingService holdings;
    private final Views views;
    private final Clock clock;

    public ManualValuationService(ValuationRepository valuations, HoldingService holdings, Views views, Clock clock) {
        this.valuations = valuations;
        this.holdings = holdings;
        this.views = views;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ValuationResponse> list(Long userId, Long holdingId) {
        Holding h = holdings.requireOwned(holdingId, userId);
        return valuations.findByHoldingIdOrderByValuationDateDescIdDesc(h.getId()).stream().map(views::valuation).toList();
    }

    @Transactional
    public ValuationResponse add(Long userId, Long holdingId, ValuationRequest req) {
        Holding h = holdings.requireOwned(holdingId, userId);
        if (h.getAssetType().isCash()) {
            throw ApiException.badRequest("valuation.not_for_cash");
        }
        if (req.valuationDate().isAfter(LocalDate.now(clock).plusDays(1))) {
            throw ApiException.badRequest("valuation.future_date");
        }
        Valuation v = new Valuation();
        v.setHolding(h);
        v.setValuationDate(req.valuationDate());
        v.setValue(req.value());
        v.setNote(req.note());
        return views.valuation(valuations.save(v));
    }

    @Transactional
    public void delete(Long userId, Long valuationId) {
        valuations.delete(valuations.findOwned(valuationId, userId)
                .orElseThrow(() -> ApiException.notFound("valuation.not_found")));
    }
}
