package com.diwan.portfolio.entity;

import com.diwan.common.crypto.EncryptedStringConverter;
import com.diwan.portfolio.domain.AssetType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One investment inside a portfolio. The position fields (quantity, averageCost, realizedPnl, netIncome)
 * are never set by clients: {@code TransactionService} recomputes them from the transaction history.
 */
@Entity
@Table(name = "holding")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "asset_type", nullable = false, length = 32)
    private AssetType assetType;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 3)
    private String currency;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 1024)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 28, scale = 8)
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(name = "realized_pnl", nullable = false, precision = 24, scale = 4)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "net_income", nullable = false, precision = 24, scale = 4)
    private BigDecimal netIncome = BigDecimal.ZERO;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Free-form JSON (address, area, rent, account number...), encrypted as a whole */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String attributes;

    @Column(nullable = false)
    private boolean closed;

    @ManyToMany
    @JoinTable(name = "holding_tag",
            joinColumns = @JoinColumn(name = "holding_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Cost basis of the open position in the holding's currency. */
    public BigDecimal costBasis() {
        return quantity.multiply(averageCost);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Portfolio getPortfolio() { return portfolio; }
    public void setPortfolio(Portfolio portfolio) { this.portfolio = portfolio; }
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAverageCost() { return averageCost; }
    public void setAverageCost(BigDecimal averageCost) { this.averageCost = averageCost; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public BigDecimal getNetIncome() { return netIncome; }
    public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public Set<Tag> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
