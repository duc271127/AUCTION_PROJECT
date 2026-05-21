package com.team.backend.dto;

import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

/**
 * Backward compatible AuctionCreateDto.
 * If itemId is provided, server will use existing item.
 * Otherwise itemName + startPrice will be used to create a new item.
 */
public class AuctionCreateDto {
    public String itemName;
    public String itemDescription;
    public double startPrice;
    public UUID itemId;
    public String title;
    public String description;
    public String imageUrl;
    public String category;

    @NotNull
    public Instant startTime;

    @NotNull
    public Instant endTime;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }

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

}
