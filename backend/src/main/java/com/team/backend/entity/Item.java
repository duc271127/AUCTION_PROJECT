package com.team.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private double startPrice;

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID sellerId;

    public Item() { }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    public UUID getSellerId() { return sellerId; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }
}
