package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AutoBid;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.service.AutoBidService;
import com.team.backend.service.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AutoBidServiceImpl implements AutoBidService {

    private static final Logger log = LoggerFactory.getLogger(AutoBidServiceImpl.class);

    private final AutoBidRepository autoBidRepository;
    private final AuctionRepository auctionRepository;
    private final EventPublisher eventPublisher;

    public AutoBidServiceImpl(AutoBidRepository autoBidRepository,
                              AuctionRepository auctionRepository,
                              ObjectProvider<EventPublisher> eventPublisherProvider) {
        this.autoBidRepository = autoBidRepository;
        this.auctionRepository = auctionRepository;
        this.eventPublisher = eventPublisherProvider.getIfAvailable();
    }

    @Override
    @Transactional
    public AutoBid setAutoBid(UUID auctionId, UUID bidderId, double maxAmount) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId and bidderId are required");
        }
        if (maxAmount <= 0.0) {
            throw new InvalidBidException("maxAmount must be greater than 0");
        }

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        Instant now = Instant.now();
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            throw new InvalidBidException("Cannot enable auto-bid on a finished auction");
        }
        if (auction.getState() != null &&
                ("FINISHED".equalsIgnoreCase(auction.getState().name())
                        || "CANCELLED".equalsIgnoreCase(auction.getState().name()))) {
            throw new InvalidBidException("Cannot enable auto-bid on a closed auction");
        }

        List<AutoBid> existing = autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId);
        AutoBid autoBid;
        if (existing != null && !existing.isEmpty()) {
            autoBid = existing.get(0);
            autoBid.setMaxAmount(maxAmount);
            autoBid.setActive(true);
            autoBid.setUpdatedAt(now);
        } else {
            autoBid = new AutoBid(auctionId, bidderId, maxAmount);
            autoBid.setCreatedAt(now);
            autoBid.setUpdatedAt(now);
        }

        AutoBid saved = autoBidRepository.save(autoBid);
        if (eventPublisher != null) {
            try {
                eventPublisher.publishAutoBidPlaced(auctionId, bidderId, saved.getMaxAmount(), auction.getLeaderId(), Instant.now());
            } catch (Exception e) {
                log.warn("Failed to publish auto-bid setup event for auction {}", auctionId, e);
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public void cancelAutoBid(UUID auctionId, UUID bidderId) {
        if (auctionId == null || bidderId == null) {
            throw new InvalidBidException("auctionId and bidderId are required");
        }

        List<AutoBid> existing = autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId);
        if (existing == null || existing.isEmpty()) {
            throw new ResourceNotFoundException("Auto-bid not found for this auction and user");
        }

        Instant now = Instant.now();
        for (AutoBid autoBid : existing) {
            autoBid.setActive(false);
            autoBid.setUpdatedAt(now);
            autoBidRepository.save(autoBid);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutoBid> listAutoBidsForAuction(UUID auctionId) {
        if (auctionId == null) {
            throw new InvalidBidException("auctionId is required");
        }
        return autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutoBid> listAutoBidsByUser(UUID bidderId) {
        if (bidderId == null) {
            throw new InvalidBidException("bidderId is required");
        }
        return autoBidRepository.findByBidderIdAndActiveTrueOrderByCreatedAtAsc(bidderId);
    }
}
