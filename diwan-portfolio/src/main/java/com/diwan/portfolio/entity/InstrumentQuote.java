package com.diwan.portfolio.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** The latest known quote of an instrument (one row per instrument, overwritten by the quote job). */
@Entity
@Table(name = "instrument_quote")
public class InstrumentQuote {

    @Id
    @Column(name = "instrument_id")
    private Long instrumentId;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal price;

    @Column(name = "previous_close", precision = 28, scale = 8)
    private BigDecimal previousClose;

    @Column(name = "day_change", precision = 28, scale = 8)
    private BigDecimal dayChange;

    @Column(name = "change_percent", precision = 12, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "day_high", precision = 28, scale = 8)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 28, scale = 8)
    private BigDecimal dayLow;

    @Column(name = "quote_time", nullable = false)
    private Instant quoteTime;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(nullable = false, length = 16)
    private String provider;

    protected InstrumentQuote() {}

    public InstrumentQuote(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Long getInstrumentId() { return instrumentId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }
    public BigDecimal getDayChange() { return dayChange; }
    public void setDayChange(BigDecimal dayChange) { this.dayChange = dayChange; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
    public BigDecimal getDayHigh() { return dayHigh; }
    public void setDayHigh(BigDecimal dayHigh) { this.dayHigh = dayHigh; }
    public BigDecimal getDayLow() { return dayLow; }
    public void setDayLow(BigDecimal dayLow) { this.dayLow = dayLow; }
    public Instant getQuoteTime() { return quoteTime; }
    public void setQuoteTime(Instant quoteTime) { this.quoteTime = quoteTime; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
