package com.diwan.portfolio.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import com.diwan.portfolio.domain.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Amounts are in the holding's currency. */
@Entity
@Table(name = "portfolio_transaction")
public class PortfolioTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holding_id", nullable = false)
    private Holding holding;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /** BUY/SELL only */
    @Column(precision = 28, scale = 8)
    private BigDecimal quantity;

    /** BUY/SELL only: price per unit */
    @Column(precision = 28, scale = 8)
    private BigDecimal price;

    /** DIVIDEND/INCOME/EXPENSE/DEPOSIT/WITHDRAWAL only */
    @Column(precision = 24, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 24, scale = 4)
    private BigDecimal fees = BigDecimal.ZERO;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Holding getHolding() { return holding; }
    public void setHolding(Holding holding) { this.holding = holding; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
}
