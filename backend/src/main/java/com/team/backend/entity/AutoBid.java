package com.team.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "auto_bids",
        uniqueConstraints = @UniqueConstraint(columnNames = {"auction_id", "bidder_id"})
)
public class AutoBid implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(nullable = false)
    private UUID id = UUID.randomUUID();

    @NotNull
    @Column(name = "auction_id", nullable = false)
    private UUID auctionId;

    @NotNull
    @Column(name = "bidder_id", nullable = false)
    private UUID bidderId;

    @Positive
    @Column(name = "max_amount", nullable = false)
    private double maxAmount;

    @Positive
    @Column(name = "bid_step")
    private Double bidStep = 1.0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version")
    private Long version;

    public AutoBid() {}

    public AutoBid(UUID auctionId, UUID bidderId, double maxAmount, double bidStep) {
        this.id = UUID.randomUUID();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
        this.bidStep = bidStep;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static AutoBid of(UUID auctionId, UUID bidderId, double maxAmount, double bidStep) {
        return new AutoBid(auctionId, bidderId, maxAmount, bidStep);
    }

    public AutoBid withMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }

    public AutoBid withBidStep(double bidStep) {
        this.bidStep = bidStep;
        return this;
    }

    public AutoBid withActive(boolean active) {
        this.active = active;
        return this;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAuctionId() { return auctionId; }
    public void setAuctionId(UUID auctionId) { this.auctionId = auctionId; }

    public UUID getBidderId() { return bidderId; }
    public void setBidderId(UUID bidderId) { this.bidderId = bidderId; }

    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }

    public Double getBidStep() { return bidStep; }
    public void setBidStep(Double bidStep) { this.bidStep = bidStep; }

    public double resolveBidStep(double fallback) {
        return bidStep == null || bidStep <= 0.0 ? fallback : bidStep;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutoBid autoBid)) return false;
        return Objects.equals(id, autoBid.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
