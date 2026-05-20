package com.team.backend.service.impl;

import com.team.backend.entity.AutoBid;
import com.team.backend.service.AutoBidService;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AutoBidServiceImpl implements AutoBidService {

    @Override
    public AutoBid setAutoBid(
            UUID auctionId,
            UUID bidderId,
            double maxAmount
    ) {

        return null;
    }

    @Override
    public void cancelAutoBid(
            UUID auctionId,
            UUID bidderId
    ) {

    }
}
