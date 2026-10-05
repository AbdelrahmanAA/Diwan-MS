package com.diwan.portfolio.entity;

import com.diwan.portfolio.domain.AllocationDimension;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** "I want X% of this portfolio in <key>" where key is an asset type or a country code. */
@Entity
@Table(name = "allocation_target")
public class AllocationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private AllocationDimension dimension;

    @Column(name = "target_key", nullable = false, length = 32)
    private String targetKey;

    @Column(name = "target_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPercent;

    protected AllocationTarget() {}

    public AllocationTarget(Portfolio portfolio, AllocationDimension dimension, String targetKey, BigDecimal targetPercent) {
        this.portfolio = portfolio;
        this.dimension = dimension;
        this.targetKey = targetKey;
        this.targetPercent = targetPercent;
    }

    public Long getId() { return id; }
    public Portfolio getPortfolio() { return portfolio; }
    public AllocationDimension getDimension() { return dimension; }
    public String getTargetKey() { return targetKey; }
    public BigDecimal getTargetPercent() { return targetPercent; }
}
