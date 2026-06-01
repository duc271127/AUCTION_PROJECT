package com.auction.server.exception;

public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) { super(message); }
}

