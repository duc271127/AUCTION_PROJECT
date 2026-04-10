package com.team.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids")
public class BidTransaction {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID auctionId;

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID bidderId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private Instant timestamp;

    public BidTransaction() { this.id = UUID.randomUUID(); }

    public BidTransaction(UUID auctionId, UUID bidderId, double amount, Instant timestamp) {
        this();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // getters/setters
    public UUID getId() { return id; }
    public UUID getAuctionId() { return auctionId; }
    public void setAuctionId(UUID auctionId) { this.auctionId = auctionId; }
    public UUID getBidderId() { return bidderId; }
    public void setBidderId(UUID bidderId) { this.bidderId = bidderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
