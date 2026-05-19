package com.team.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * AutoBid - cấu trúc lưu cấu hình đặt giá tự động cho mỗi bidder trên mỗi auction.
 * - Có constructor tiện lợi, builder-style, equals/hashCode, toString.
 * - Có trường updatedAt/createdAt và @PrePersist/@PreUpdate để tự động cập nhật thời gian.
 * - Có @Version để hỗ trợ optimistic locking nếu cần.
 *
 * Lưu ý: giữ kiểu tiền tệ là double theo yêu cầu dự án.
 */
@Entity
@Table(
        name = "auto_bids",
        uniqueConstraints = @UniqueConstraint(columnNames = {"auction_id", "bidder_id"})
)
public class AutoBid implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id = UUID.randomUUID();

    @NotNull
    @Column(name = "auction_id", nullable = false, columnDefinition = "uuid")
    private UUID auctionId;

    @NotNull
    @Column(name = "bidder_id", nullable = false, columnDefinition = "uuid")
    private UUID bidderId;

    @Positive
    @Column(name = "max_amount", nullable = false)
    private double maxAmount;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /**
     * Phiên bản để hỗ trợ optimistic locking (tùy chọn).
     * Nếu bạn không muốn optimistic locking, có thể xóa trường này.
     */
    @Version
    @Column(name = "version")
    private Long version;

    public AutoBid() {
        // JPA
    }

    /**
     * Constructor tiện lợi.
     */
    public AutoBid(UUID auctionId, UUID bidderId, double maxAmount) {
        this.id = UUID.randomUUID();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Factory method tiện lợi
    public static AutoBid of(UUID auctionId, UUID bidderId, double maxAmount) {
        return new AutoBid(auctionId, bidderId, maxAmount);
    }

    // Builder-style setters (tuỳ chọn, giúp test/khởi tạo nhanh)
    public AutoBid withMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }

    public AutoBid withActive(boolean active) {
        this.active = active;
        return this;
    }

    // Getters / Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAuctionId() { return auctionId; }
    public void setAuctionId(UUID auctionId) { this.auctionId = auctionId; }

    public UUID getBidderId() { return bidderId; }
    public void setBidderId(UUID bidderId) { this.bidderId = bidderId; }

    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }

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

    // equals/hashCode dựa trên id (UUID) để phù hợp với entity identity
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutoBid)) return false;
        AutoBid autoBid = (AutoBid) o;
        return Objects.equals(id, autoBid.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AutoBid{" +
                "id=" + id +
                ", auctionId=" + auctionId +
                ", bidderId=" + bidderId +
                ", maxAmount=" + maxAmount +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
