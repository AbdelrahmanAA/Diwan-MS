package com.diwan.portfolio.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "watchlist_item")
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WatchlistItem() {}

    public WatchlistItem(Long userId, Instrument instrument) {
        this.userId = userId;
        this.instrument = instrument;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instrument getInstrument() { return instrument; }
    public Instant getCreatedAt() { return createdAt; }
}
