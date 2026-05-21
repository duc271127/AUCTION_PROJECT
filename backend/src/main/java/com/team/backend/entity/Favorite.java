package com.team.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","auction_id"}))
public class Favorite {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "auction_id", columnDefinition = "uuid", nullable = false)
    private UUID auctionId;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Favorite() {}
    public Favorite(UUID userId, UUID auctionId) { this.userId = userId; this.auctionId = auctionId; }

    // getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getAuctionId() { return auctionId; }
    public Instant getCreatedAt() { return createdAt; }
}
