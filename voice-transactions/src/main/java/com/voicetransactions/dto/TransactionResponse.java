package com.voicetransactions.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String bank;
    private String type;
    private String category;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String paymentMethod;
    private String status;
    private String description;
    private String notes;
    private String referenceNumber;
    private String tags;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long v)                    { this.id = v; }
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
    public void setCreatedAt(LocalDateTime v)    { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)    { this.updatedAt = v; }
}

