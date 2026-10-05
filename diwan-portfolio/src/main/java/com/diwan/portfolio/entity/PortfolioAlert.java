package com.diwan.portfolio.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import com.diwan.portfolio.domain.AlertType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** A triggered alert. Title and message are rendered at read time in the reader's language. */
@Entity
@Table(name = "portfolio_alert")
public class PortfolioAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private AlertType type;

    @Column(length = 32)
    private String symbol;

    @Column(name = "holding_id")
    private Long holdingId;

    @Column(name = "portfolio_id")
    private Long portfolioId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 1024)
    private String subject;

    @Column(name = "observed_value", nullable = false, precision = 28, scale = 8)
    private BigDecimal observedValue;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal threshold;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "read_at")
    private Instant readAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Long getHoldingId() { return holdingId; }
    public void setHoldingId(Long holdingId) { this.holdingId = holdingId; }
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public BigDecimal getObservedValue() { return observedValue; }
    public void setObservedValue(BigDecimal observedValue) { this.observedValue = observedValue; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
    public Instant getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(Instant triggeredAt) { this.triggeredAt = triggeredAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
