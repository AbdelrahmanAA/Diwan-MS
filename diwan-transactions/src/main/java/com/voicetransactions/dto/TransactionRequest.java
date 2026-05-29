package com.voicetransactions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRequest {

    // optional — defaults to "Unknown"
    private String bank;

    @NotBlank(message = "Type is required (expense | income)")
    private String type;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    // ── Enriched optional fields ───────────────────────────────────────────────
    private String merchantName;
    private String paymentMethod;   // cash | card | transfer | wallet | cheque
    private String status;          // completed | pending | cancelled
    private String description;
    private String notes;
    private String referenceNumber;
    private String tags;
    private LocalDateTime recordedAt;

    // ── Getters & Setters ──────────────────────────────────────────────────────
    public String getBank()                        { return bank; }
    public void setBank(String v)                  { this.bank = v; }
    public String getType()                        { return type; }
    public void setType(String v)                  { this.type = v; }
    public String getCategory()                    { return category; }
    public void setCategory(String v)              { this.category = v; }
    public BigDecimal getAmount()                  { return amount; }
    public void setAmount(BigDecimal v)            { this.amount = v; }
    public String getCurrency()                    { return currency; }
    public void setCurrency(String v)              { this.currency = v; }
    public String getMerchantName()                { return merchantName; }
    public void setMerchantName(String v)          { this.merchantName = v; }
    public String getPaymentMethod()               { return paymentMethod; }
    public void setPaymentMethod(String v)         { this.paymentMethod = v; }
    public String getStatus()                      { return status; }
    public void setStatus(String v)                { this.status = v; }
    public String getDescription()                 { return description; }
    public void setDescription(String v)           { this.description = v; }
    public String getNotes()                       { return notes; }
    public void setNotes(String v)                 { this.notes = v; }
    public String getReferenceNumber()             { return referenceNumber; }
    public void setReferenceNumber(String v)       { this.referenceNumber = v; }
    public String getTags()                        { return tags; }
    public void setTags(String v)                  { this.tags = v; }
    public LocalDateTime getRecordedAt()           { return recordedAt; }
    public void setRecordedAt(LocalDateTime v)     { this.recordedAt = v; }
}

