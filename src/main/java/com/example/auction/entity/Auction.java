package com.example.auction.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double startPrice;
    private Double currentPrice;

    private Timestamp startTime;
    private Timestamp endTime;

    private String status;

    // getter setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getStartPrice() { return startPrice; }
    public void setStartPrice(Double startPrice) { this.startPrice = startPrice; }

    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
