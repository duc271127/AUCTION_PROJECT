package com.team.backend.repository;

import java.util.UUID;

public interface AuctionCountProjection {
    UUID getAuctionId();
    long getCount();
}
