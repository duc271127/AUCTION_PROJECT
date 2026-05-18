package com.team.backend.service.impl;

import com.team.backend.entity.AutoBid;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.service.AutoBidService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AutoBidServiceImpl implements AutoBidService {

    private final AutoBidRepository autoBidRepository;

    public AutoBidServiceImpl(AutoBidRepository autoBidRepository) {
        this.autoBidRepository = autoBidRepository;
    }

    @Override
    @Transactional
    public AutoBid setAutoBid(UUID auctionId, UUID bidderId, double maxAmount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId and bidderId are required");
        }

        if (maxAmount <= 0) {
            throw new InvalidBidException("maxAmount must be greater than 0");
        }

        AutoBid autoBid = autoBidRepository
                .findByAuctionIdAndBidderId(auctionId, bidderId)
                .orElseGet(AutoBid::new);

        autoBid.setAuctionId(auctionId);
        autoBid.setBidderId(bidderId);
        autoBid.setMaxAmount(maxAmount);
        autoBid.setActive(true);

        return autoBidRepository.save(autoBid);
    }
}