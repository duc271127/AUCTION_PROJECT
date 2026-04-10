package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.BidTransaction;
import com.team.backend.exception.AuctionClosedException;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.service.BidService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BidServiceImpl implements BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    // per-auction lock to avoid lost update
    private final Map<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public BidServiceImpl(AuctionRepository auctionRepository, BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    private ReentrantLock getLock(UUID auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    @Override
    @Transactional
    public BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            // check state
            if (auction.getState() != AuctionState.RUNNING && auction.getState() != AuctionState.OPEN) {
                throw new AuctionClosedException("Auction is not open for bidding");
            }

            double current = auction.getCurrentPrice();
            if (amount <= current) {
                throw new InvalidBidException("Bid must be higher than current price (" + current + ")");
            }

            // update auction
            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            // create bid transaction
            BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
            bidRepository.save(tx);

            return tx;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<BidTransaction> getBidHistory(UUID auctionId) {
        return bidRepository.findByAuctionIdOrderByTimestampAsc(auctionId);
    }
}
