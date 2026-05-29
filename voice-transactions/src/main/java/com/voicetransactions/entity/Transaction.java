package com.voicetransactions.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_category",   columnList = "category"),
    @Index(name = "idx_bank",       columnList = "bank"),
    @Index(name = "idx_type",       columnList = "type"),
    @Index(name = "idx_status",     columnList = "status"),
    @Index(name = "idx_recordedAt", columnList = "recordedAt")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner of this transaction */
    @Column(nullable = false)
    private Long userId;

    // ── Core fields ───────────────────────────────────────────────────────────

    /** اسم البنك مثل: CIB, NBE, Al Rajhi */
    @Column(nullable = false)
    private String bank;

    /** expense | income */
    @NotBlank(message = "Type is required")
    @Column(nullable = false)
    private String type;

    /** food | transport | bills | health | shopping | entertainment | salary | general */
    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** EGP | USD | EUR | SAR | AED | GBP */
    @NotBlank(message = "Currency is required")
    @Column(nullable = false, length = 10)
    private String currency;

    // ── Enriched fields ───────────────────────────────────────────────────────

    /** اسم التاجر أو المحل: Carrefour, Amazon, مطعم الأصيل */
    @Column(length = 200)
    private String merchantName;

    /** cash | card | transfer | wallet | cheque */
    @Column(length = 50)
    private String paymentMethod;

    /** completed | pending | cancelled */
    @Column(nullable = false, length = 20)
    private String status;

    /** وصف مختصر للعملية */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** ملاحظات إضافية */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** رقم الفاتورة أو رقم مرجعي */
    @Column(length = 100)
    private String referenceNumber;

    /** tags مفصولة بفاصلة مثل: work,personal,urgent */
    @Column(columnDefinition = "TEXT")
    private String tags;

    // ── Timestamps ────────────────────────────────────────────────────────────

    /** وقت العملية الفعلي */
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    /** وقت الإدخال في النظام */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** وقت آخر تعديل */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Transaction() {}

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (recordedAt  == null) recordedAt  = now;
        if (createdAt   == null) createdAt   = now;
        updatedAt = now;
        if (bank          == null || bank.isBlank())          bank          = "Unknown";
        if (status        == null || status.isBlank())        status        = "completed";
        if (paymentMethod == null || paymentMethod.isBlank()) paymentMethod = "card";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Builder ────────────────────────────────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Transaction t = new Transaction();
        public Builder userId(Long v)             { t.userId = v;          return this; }
        public Builder bank(String v)             { t.bank = v;            return this; }
        public Builder type(String v)             { t.type = v;            return this; }
        public Builder category(String v)         { t.category = v;        return this; }
        public Builder amount(BigDecimal v)       { t.amount = v;          return this; }
        public Builder currency(String v)         { t.currency = v;        return this; }
        public Builder merchantName(String v)     { t.merchantName = v;    return this; }
        public Builder paymentMethod(String v)    { t.paymentMethod = v;   return this; }
        public Builder status(String v)           { t.status = v;          return this; }
        public Builder description(String v)      { t.description = v;     return this; }
        public Builder notes(String v)            { t.notes = v;           return this; }
        public Builder referenceNumber(String v)  { t.referenceNumber = v; return this; }
        public Builder tags(String v)             { t.tags = v;            return this; }
        public Builder recordedAt(LocalDateTime v){ t.recordedAt = v;      return this; }
        public Transaction build()                { return t; }
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public Long getUserId()                      { return userId; }
    public void setUserId(Long v)                { this.userId = v; }
    public String getBank()                      { return bank; }
    public void setBank(String v)                { this.bank = v; }
    public String getType()                      { return type; }
    public void setType(String v)                { this.type = v; }
    public String getCategory()                  { return category; }
    public void setCategory(String v)            { this.category = v; }
    public BigDecimal getAmount()                { return amount; }
    public void setAmount(BigDecimal v)          { this.amount = v; }
    public String getCurrency()                  { return currency; }
    public void setCurrency(String v)            { this.currency = v; }
    public String getMerchantName()              { return merchantName; }
    public void setMerchantName(String v)        { this.merchantName = v; }
    public String getPaymentMethod()             { return paymentMethod; }
    public void setPaymentMethod(String v)       { this.paymentMethod = v; }
    public String getStatus()                    { return status; }
    public void setStatus(String v)              { this.status = v; }
    public String getDescription()               { return description; }
    public void setDescription(String v)         { this.description = v; }
    public String getNotes()                     { return notes; }
    public void setNotes(String v)               { this.notes = v; }
    public String getReferenceNumber()           { return referenceNumber; }
    public void setReferenceNumber(String v)     { this.referenceNumber = v; }
    public String getTags()                      { return tags; }
    public void setTags(String v)                { this.tags = v; }
    public LocalDateTime getRecordedAt()         { return recordedAt; }
    public void setRecordedAt(LocalDateTime v)   { this.recordedAt = v; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
}
