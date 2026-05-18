package com.team.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class ItemDto {
    private UUID id;

    @NotNull
    private UUID sellerId;

    @NotBlank
    private String title;

    private String description;

    private Double startingPrice;

    private Instant CreatedAt;

    // getters / setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSellerId() { return sellerId; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Double startingPrice) { this.startingPrice = startingPrice; }

    public Instant getCreatedAt() { return CreatedAt; }
    public void setCreatedAt(Instant createdAt) { this.CreatedAt = CreatedAt; }
}
