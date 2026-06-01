package com.auction.server.repository;

import java.util.UUID;

public interface AuctionCountProjection {
    UUID getAuctionId();
    long getCount();
}

