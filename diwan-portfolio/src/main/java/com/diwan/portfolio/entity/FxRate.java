package com.diwan.portfolio.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** 1 USD = {@code rate} units of {@code currency} on {@code rateDate}. */
@Entity
@Table(name = "fx_rate")
@IdClass(FxRate.Key.class)
public class FxRate {

    @Id
    @Column(length = 3)
    private String currency;

    @Id
    @Column(name = "rate_date")
    private LocalDate rateDate;

    @Column(nullable = false, precision = 24, scale = 10)
    private BigDecimal rate;

    protected FxRate() {}

    public FxRate(String currency, LocalDate rateDate, BigDecimal rate) {
        this.currency = currency;
        this.rateDate = rateDate;
        this.rate = rate;
    }

    public String getCurrency() { return currency; }
    public LocalDate getRateDate() { return rateDate; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public static class Key implements Serializable {
        private String currency;
        private LocalDate rateDate;

        public Key() {}

        public Key(String currency, LocalDate rateDate) {
            this.currency = currency;
            this.rateDate = rateDate;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(currency, k.currency) && Objects.equals(rateDate, k.rateDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(currency, rateDate);
        }
    }
}
