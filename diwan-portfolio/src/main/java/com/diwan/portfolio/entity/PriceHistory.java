package com.diwan.portfolio.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "price_history")
@IdClass(PriceHistory.Key.class)
public class PriceHistory {

    @Id
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Id
    @Column(name = "price_date")
    private LocalDate priceDate;

    @Column(name = "close_price", nullable = false, precision = 28, scale = 8)
    private BigDecimal closePrice;

    protected PriceHistory() {}

    public PriceHistory(Long instrumentId, LocalDate priceDate, BigDecimal closePrice) {
        this.instrumentId = instrumentId;
        this.priceDate = priceDate;
        this.closePrice = closePrice;
    }

    public Long getInstrumentId() { return instrumentId; }
    public LocalDate getPriceDate() { return priceDate; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }

    public static class Key implements Serializable {
        private Long instrumentId;
        private LocalDate priceDate;

        public Key() {}

        public Key(Long instrumentId, LocalDate priceDate) {
            this.instrumentId = instrumentId;
            this.priceDate = priceDate;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(instrumentId, k.instrumentId) && Objects.equals(priceDate, k.priceDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instrumentId, priceDate);
        }
    }
}
