package com.team.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id = UUID.randomUUID();

    @Column(name = "item_id", columnDefinition = "uuid", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private Item item;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionState state = AuctionState.DRAFT;

    @Column(name = "current_price", nullable = false)
    private double currentPrice = 0.0;

    @Column(name = "reserve_price")
    private Double reservePrice = 0.0;

    @Column(name = "leader_id", columnDefinition = "uuid")
    private UUID leaderId;

    @Column(name = "winner_id", columnDefinition = "uuid")
    private UUID winnerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Version
    private Long version;

    public Auction() {this.id = UUID.randomUUID();}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public AuctionState getState() { return state; }
    public void setState(AuctionState state) { this.state = state; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public Double getReservePrice() { return reservePrice; }
    public void setReservePrice(Double reservePrice) { this.reservePrice = reservePrice; }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public UUID getWinnerId() { return winnerId; }
    public void setWinnerId(UUID winnerId) { this.winnerId = winnerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
