package com.auction.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auctions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "item_id")
})
public class Auction {

    @Id
    @Column(nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private Item item;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "category")
    private String category;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionState state = AuctionState.DRAFT;

    @Column(name = "current_price", nullable = false)
    private double currentPrice = 0.0;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "reserve_price")
    private Double reservePrice = 0.0;

    @Column(name = "seller_id", columnDefinition = "uuid")
    private UUID sellerId;

    @Column(name = "leader_id", columnDefinition = "uuid")
    private UUID leaderId;

    @Column(name = "winner_payment_captured", nullable = false, columnDefinition = "boolean default false")
    private boolean winnerPaymentCaptured = false;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Version
    private Long version;

    @Transient
    private Integer bidCount;

    @Transient
    private Double minNextBid;

    @Transient
    private String sellerName;

    @Transient
    private Long favoriteCount;

    @Transient
    private Double trendingScore;

    public Auction() {
        this.id = UUID.randomUUID();
    }

    // Convenience constructor for creation
    public Auction(UUID itemId, String title, String description, String imageUrl, String category,
                   Instant startTime, Instant endTime, double currentPrice, Double reservePrice, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.reservePrice = reservePrice;
        this.createdBy = createdBy;
        this.state = AuctionState.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }

    public Item getItem() { return item; }
    public void setItem(Item item) {
        this.item = item;
        if (item != null && item.getId() != null) {
            this.itemId = item.getId();
        }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public AuctionState getState() { return state; }
    public void setState(AuctionState state) { this.state = state; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public Double getReservePrice() { return reservePrice; }
    public void setReservePrice(Double reservePrice) { this.reservePrice = reservePrice; }

    public UUID getSellerId() { return sellerId; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }

    public UUID getLeaderId() { return leaderId; }
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public boolean isWinnerPaymentCaptured() { return winnerPaymentCaptured; }
    public void setWinnerPaymentCaptured(boolean winnerPaymentCaptured) { this.winnerPaymentCaptured = winnerPaymentCaptured; }

    public UUID getWinnerId() { return winnerId; }
    public void setWinnerId(UUID winnerId) { this.winnerId = winnerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Integer getBidCount() { return bidCount; }
    public void setBidCount(Integer bidCount) { this.bidCount = bidCount; }

    public Double getMinNextBid() { return minNextBid; }
    public void setMinNextBid(Double minNextBid) { this.minNextBid = minNextBid; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public Long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }

    public Double getTrendingScore() { return trendingScore; }
    public void setTrendingScore(Double trendingScore) { this.trendingScore = trendingScore; }

    // Helper: compute default minNextBid if not set (business rule: +1.0)
    public double computeMinNextBid(double increment) {
        if (minNextBid != null) return minNextBid;
        return this.currentPrice + increment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Auction)) return false;
        Auction auction = (Auction) o;
        return Objects.equals(id, auction.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id=" + id +
                ", itemId=" + itemId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", state=" + state +
                ", currentPrice=" + currentPrice +
                ", leaderId=" + leaderId +
                ", winnerId=" + winnerId +
                ", createdAt=" + createdAt +
                ", createdBy=" + createdBy +
                ", version=" + version +
                '}';
    }
}

