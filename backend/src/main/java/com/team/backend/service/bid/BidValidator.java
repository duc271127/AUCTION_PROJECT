package com.team.backend.service.bid;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.exception.InvalidBidException;

public class BidValidator {

    private final double minIncrement;

    public BidValidator(double minIncrement) {
        this.minIncrement = minIncrement;
    }

    public double minAllowed(Auction auction) {
        if (auction == null) throw new IllegalArgumentException("auction is null");
        double current = auction.getCurrentPrice();
        return current + minIncrement;
    }

    public void validate(Auction auction, Double amount) {
        if (auction == null) throw new InvalidBidException("Auction not found");
        if (amount == null) throw new InvalidBidException("Amount is required");
        if (amount <= 0.0) throw new InvalidBidException("Amount must be positive");

        AuctionState state = auction.getState();
        if (state == null) throw new InvalidBidException("Auction state invalid");
        switch (state) {
            case DRAFT:
            case SCHEDULED:
                throw new InvalidBidException("Auction chưa bắt đầu");
            case CANCELLED:
            case FINISHED:
                throw new InvalidBidException("Auction đã đóng");
            default:
                break;
        }

        double minAllowed = minAllowed(auction);
        if (amount < minAllowed) {
            throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
        }
    }
}
