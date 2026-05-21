package com.team.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "bid_transactions", indexes = {
        @Index(name = "idx_bid_tx_auction", columnList = "auction_id"),
        @Index(name = "idx_bid_tx_bidder", columnList = "bidder_id")
})
public class BidTransaction {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id = UUID.randomUUID();

    @Column(name = "auction_id", nullable = false, columnDefinition = "uuid")
    private UUID auctionId;

    @Column(name = "bidder_id",  nullable = false, columnDefinition = "uuid")
    private UUID bidderId;

    @Column(nullable = false)
    private double amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public BidTransaction() {}

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
