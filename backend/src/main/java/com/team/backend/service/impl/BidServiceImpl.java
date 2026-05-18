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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BidServiceImpl - phiên bản dùng double cho tiền
 */
@Service
public class BidServiceImpl implements BidService {

    private static final Logger log = LoggerFactory.getLogger(BidServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    private final ConcurrentHashMap<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    private final double minIncrement;

    public BidServiceImpl(AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          @Value("${auction.bid.min-increment:1.0}") double minIncrement) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.minIncrement = minIncrement;
    }

    private ReentrantLock getLock(UUID auctionId) {
        return lockMap.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    @Override
    @Transactional
    public BidTransaction placeBid(UUID auctionId, UUID bidderId, double amount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId và bidderId là bắt buộc");
        }

        if (amount <= 0) {
            throw new InvalidBidException("Số tiền đặt phải lớn hơn 0");
        }

        ReentrantLock lock = getLock(auctionId);
        lock.lock();

        try {
            Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

            validateAuctionForBid(auction);

            double minAllowed = auction.getCurrentPrice() + minIncrement;
            if (amount < minAllowed) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
            }

            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
            BidTransaction saved = bidRepository.save(tx);

            log.debug("Đặt giá thành công: auction={}, bidder={}, amount={}",
                    auctionId, bidderId, amount);

            return saved;
        } finally {
            lock.unlock();
        }
    }

    private Auction tryLoadAuctionForUpdate(UUID auctionId) {
        try {
            return auctionRepository.findByIdForUpdate(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
        } catch (UnsupportedOperationException | PessimisticLockingFailureException ex) {
            throw new RuntimeException("DB lock không khả dụng: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    protected BidTransaction placeBidWithInMemoryLock(UUID auctionId, UUID bidderId, double amount) {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

            validateAuctionForBid(auction);

            double minAllowed = auction.getCurrentPrice() + minIncrement;
            if (amount < minAllowed) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minAllowed);
            }

            auction.setCurrentPrice(amount);
            auction.setLeaderId(bidderId);
            auctionRepository.save(auction);

            BidTransaction tx = new BidTransaction(auctionId, bidderId, amount, Instant.now());
            BidTransaction saved = bidRepository.save(tx);

            log.debug("Đặt giá thành công (in-memory lock): auction={}, bidder={}, amount={}", auctionId, bidderId, amount);
            return saved;
        } finally {
            lock.unlock();
        }
    }

    private void validateAuctionForBid(Auction auction) {
        if (auction == null) {
            throw new ResourceNotFoundException("Auction is null");
        }
        if (auction.getState() == AuctionState.FINISHED || auction.getState() == AuctionState.CANCELLED) {
            throw new AuctionClosedException("Auction đã đóng");
        }
        Instant now = Instant.now();
        if (now.isBefore(auction.getStartTime())) {
            throw new InvalidBidException("Auction chưa bắt đầu");
        }
        if (now.isAfter(auction.getEndTime())) {
            auction.setState(AuctionState.FINISHED);
            auction.setWinnerId(auction.getLeaderId());
            auctionRepository.save(auction);
            throw new AuctionClosedException("Auction đã kết thúc");
        }
    }

    @Override
    public List<BidTransaction> getBidHistory(UUID auctionId) {
        return bidRepository.findByAuctionIdOrderByCreatedAtAsc(auctionId);
    }
}
