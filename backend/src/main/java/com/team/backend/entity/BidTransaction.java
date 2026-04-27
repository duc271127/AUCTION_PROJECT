package com.team.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * BidTransaction entity (extended).
 * - Uses a single timestamp field (createdAt) for clarity; keeps backward-compatible
 *   column name "timestamp" if your DB already uses it.
 * - Adds @PrePersist to ensure timestamp is set when saving.
 * - Adds equals/hashCode/toString for easier testing and logging.
 */
@Entity
@Table(name = "bid_transactions", indexes = {
        @Index(name = "idx_bid_tx_auction", columnList = "auction_id"),
        @Index(name = "idx_bid_tx_bidder", columnList = "bidder_id")
})
public class BidTransaction {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "auction_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID auctionId;

    @Column(name = "bidder_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID bidderId;

    @Column(nullable = false)
    private double amount;

    /**
     * Persisted column name kept as "timestamp" for compatibility.
     * Use createdAt in code for clearer semantics.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant createdAt;

    public BidTransaction() {
        this.id = UUID.randomUUID();
    }

    public BidTransaction(UUID auctionId, UUID bidderId, double amount, Instant createdAt) {
        this();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    // Getters / Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Alias getter/setter names: keep getTimestamp/setTimestamp for compatibility
     * with existing code that referenced "timestamp", and also provide getCreatedAt/setCreatedAt.
     */
    public Instant getTimestamp() {
        return createdAt;
    }

    public void setTimestamp(Instant timestamp) {
        this.createdAt = timestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // equals / hashCode / toString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidTransaction)) return false;
        BidTransaction that = (BidTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BidTransaction{" +
                "id=" + id +
                ", auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                ", amount=" + amount +
                ", createdAt=" + createdAt +
                '}';
    }
}
