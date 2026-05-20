package com.team.backend.service;

import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.BidTransaction;

import java.util.List;
import java.util.UUID;

public interface BidService {

    BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount);

    List<BidHistoryDto> getBidHistory(UUID auctionId);
}
