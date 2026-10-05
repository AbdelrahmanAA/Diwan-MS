package com.diwan.portfolio.service;

import com.diwan.portfolio.domain.AlertType;
import com.diwan.portfolio.domain.AllocationDimension;
import com.diwan.portfolio.entity.AlertRule;
import com.diwan.portfolio.entity.Holding;
import com.diwan.portfolio.entity.InstrumentQuote;
import com.diwan.common.web.ApiException;
import com.diwan.portfolio.web.dto.Requests.TargetItem;
import com.diwan.portfolio.web.dto.Requests.TargetsRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertRulesTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static InstrumentQuote quote(String price, String changePercent) {
        InstrumentQuote q = new InstrumentQuote(1L);
        q.setPrice(d(price));
        q.setChangePercent(changePercent == null ? null : d(changePercent));
        return q;
    }

    @Test
    void priceRulesFireAtOrBeyondTheThreshold() {
        assertThat(AlertService.breached(AlertType.PRICE_ABOVE, d("200"), d("200"))).isTrue();
        assertThat(AlertService.breached(AlertType.PRICE_ABOVE, d("199.99"), d("200"))).isFalse();
        assertThat(AlertService.breached(AlertType.PRICE_BELOW, d("150"), d("150"))).isTrue();
        assertThat(AlertService.breached(AlertType.PRICE_BELOW, d("150.01"), d("150"))).isFalse();
    }

    @Test
    void dailyMoveWatchesBothDirections() {
        AlertRule r = new AlertRule();
        r.setType(AlertType.DAILY_MOVE_PERCENT);
        BigDecimal down = AlertService.observed(r, quote("90", "-6.5")).orElseThrow();
        assertThat(down).isEqualByComparingTo("6.5");
        assertThat(AlertService.breached(AlertType.DAILY_MOVE_PERCENT, down, d("5"))).isTrue();
    }

    @Test
    void stopLossAndTakeProfitAreMeasuredAgainstTheAverageCost() {
        Holding h = new Holding();
        h.setQuantity(d("10"));
        h.setAverageCost(d("100"));
        AlertRule loss = new AlertRule();
        loss.setType(AlertType.LOSS_PERCENT);
        loss.setHolding(h);

        BigDecimal pct = AlertService.observed(loss, quote("85", null)).orElseThrow();
        assertThat(pct).isEqualByComparingTo("-15");
        assertThat(AlertService.breached(AlertType.LOSS_PERCENT, pct, d("10"))).isTrue();
        assertThat(AlertService.breached(AlertType.LOSS_PERCENT, pct, d("20"))).isFalse();
        assertThat(AlertService.breached(AlertType.GAIN_PERCENT, AlertService.observed(loss, quote("130", null)).orElseThrow(), d("25"))).isTrue();
    }

    @Test
    void holdingRulesAreSilentForAnEmptyPosition() {
        Holding h = new Holding();
        h.setQuantity(d("0"));
        h.setAverageCost(d("0"));
        AlertRule r = new AlertRule();
        r.setType(AlertType.GAIN_PERCENT);
        r.setHolding(h);
        assertThat(AlertService.observed(r, quote("10", null))).isEmpty();
    }

    @Test
    void targetsMustBeValidKeysAddingUpTo100() {
        AllocationService.validate(new TargetsRequest(AllocationDimension.ASSET_TYPE,
                List.of(new TargetItem("STOCK", d("60")), new TargetItem("REAL_ESTATE", d("40")))));
        AllocationService.validate(new TargetsRequest(AllocationDimension.COUNTRY, List.of())); // clear

        assertThatThrownBy(() -> AllocationService.validate(new TargetsRequest(AllocationDimension.ASSET_TYPE,
                List.of(new TargetItem("STOCK", d("60")), new TargetItem("CASH", d("30"))))))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("target.sum_not_100");
        assertThatThrownBy(() -> AllocationService.validate(new TargetsRequest(AllocationDimension.COUNTRY,
                List.of(new TargetItem("XX", d("100"))))))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("target.invalid_key");
        assertThatThrownBy(() -> AllocationService.validate(new TargetsRequest(AllocationDimension.ASSET_TYPE,
                List.of(new TargetItem("STOCK", d("50")), new TargetItem("STOCK", d("50"))))))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("target.duplicate_key");
    }
}
