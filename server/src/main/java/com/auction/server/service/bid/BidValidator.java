package com.auction.server.service.bid;

import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.exception.InvalidBidException;

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
                throw new InvalidBidException("Auction has not started");
            case CANCELLED:
            case FINISHED:
            case REJECTED:
            case DELETED:
                throw new InvalidBidException("Auction is closed");
            default:
                break;
        }

        double minAllowed = minAllowed(auction);
        if (amount < minAllowed) {
            throw new InvalidBidException("Bid must be at least " + minAllowed);
        }
    }
}

