package com.diwan.transactions.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What the user entered about one of their accounts / cards / loans (see V3__bank_positions.sql). */
@Entity
@Table(name = "bank_products", uniqueConstraints =
        @UniqueConstraint(name = "uk_bank_product", columnNames = {"user_id", "bank", "kind", "ref"}))
public class BankProduct {

    public static final String ACCOUNT = "ACCOUNT";
    public static final String CARD = "CARD";
    public static final String LOAN = "LOAN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String bank;

    @Column(nullable = false, length = 10)
    private String kind;

    @Column(nullable = false, length = 8)
    private String ref;

    @Column(length = 100)
    private String label;

    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "opening_balance", precision = 14, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "opening_at")
    private LocalDateTime openingAt;

    @Column(length = 10)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId()                          { return id; }
    public Long getUserId()                      { return userId; }
    public void setUserId(Long v)                { this.userId = v; }
    public String getBank()                      { return bank; }
    public void setBank(String v)                { this.bank = v; }
    public String getKind()                      { return kind; }
    public void setKind(String v)                { this.kind = v; }
    public String getRef()                       { return ref; }
    public void setRef(String v)                 { this.ref = v; }
    public String getLabel()                     { return label; }
    public void setLabel(String v)               { this.label = v; }
    public BigDecimal getCreditLimit()           { return creditLimit; }
    public void setCreditLimit(BigDecimal v)     { this.creditLimit = v; }
    public BigDecimal getOpeningBalance()        { return openingBalance; }
    public void setOpeningBalance(BigDecimal v)  { this.openingBalance = v; }
    public LocalDateTime getOpeningAt()          { return openingAt; }
    public void setOpeningAt(LocalDateTime v)    { this.openingAt = v; }
    public String getCurrency()                  { return currency; }
    public void setCurrency(String v)            { this.currency = v; }
}
