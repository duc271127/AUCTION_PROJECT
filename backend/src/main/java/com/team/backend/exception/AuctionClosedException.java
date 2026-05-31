package com.team.backend.exception;

public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) { super(message); }
}
