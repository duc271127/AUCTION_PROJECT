package com.team.backend.service.impl;

import com.team.backend.dto.BidHistoryDto;
import com.team.backend.entity.Auction;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.realtime.RealtimeEvent;
import com.team.backend.realtime.RealtimeEventFactory;
import com.team.backend.realtime.RealtimeNotifier;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.service.AuctionHelper;
import com.team.backend.service.EventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Primary
public class StompEventPublisher implements EventPublisher {

    private final RealtimeNotifier realtimeNotifier;
    private final AuctionRepository auctionRepository;
    private final AuctionHelper auctionHelper;

    public StompEventPublisher(RealtimeNotifier realtimeNotifier,
                               AuctionRepository auctionRepository,
                               AuctionHelper auctionHelper) {
        this.realtimeNotifier = realtimeNotifier;
        this.auctionRepository = auctionRepository;
        this.auctionHelper = auctionHelper;
    }

    @Override
    public void publishBidPlaced(UUID auctionId, UUID bidderId, double amount, UUID previousLeader, Instant timestamp) {
        Auction auction = loadAuction(auctionId);
        String bidderName = auctionHelper.lookupUserName(bidderId);
        String leaderName = auctionHelper.lookupUserName(auction.getLeaderId());
        BidHistoryDto latestBid = auctionHelper.toBidHistoryItem(bidderId, amount, timestamp);
        latestBid.setAuctionId(auctionId);
        latestBid.setSource("MANUAL_BID");
        latestBid.setAutoBid(false);

        RealtimeEvent bidPlaced = RealtimeEventFactory.bidPlaced(
                auctionId,
                null,
                bidderId,
                bidderName,
                auction.getLeaderId(),
                leaderName,
                auction.getCurrentPrice(),
                stateName(auction),
                auctionHelper.computeRemainingSeconds(auctionId),
                auction.getEndTime(),
                latestBid,
                bidderName + " placed a new bid."
        );
        realtimeNotifier.broadcastToAuction(auctionId, bidPlaced);

        if (previousLeader == null || !previousLeader.equals(auction.getLeaderId())) {
            RealtimeEvent leaderChanged = RealtimeEventFactory.leaderChanged(
                    auctionId,
                    bidderId,
                    bidderName,
                    auction.getLeaderId(),
                    leaderName,
                    auction.getCurrentPrice(),
                    stateName(auction),
                    auctionHelper.computeRemainingSeconds(auctionId),
                    auction.getEndTime(),
                    leaderName == null ? "Leader changed." : leaderName + " is now leading."
            );
            realtimeNotifier.broadcastToAuction(auctionId, leaderChanged);
        }
    }

    @Override
    public void publishAuctionExtended(UUID auctionId, double newPrice, Instant newEndTime) {
        Auction auction = loadAuction(auctionId);
        RealtimeEvent event = RealtimeEventFactory.auctionExtended(
                auctionId,
                auction.getLeaderId(),
                auctionHelper.lookupUserName(auction.getLeaderId()),
                newPrice,
                stateName(auction),
                auctionHelper.computeRemainingSeconds(auctionId),
                newEndTime,
                "Auction time extended."
        );
        realtimeNotifier.broadcastToAuction(auctionId, event);
    }

    @Override
    public void publishAutoBidPlaced(UUID auctionId, UUID bidderId, double amount, UUID previousLeader, Instant timestamp) {
        Auction auction = loadAuction(auctionId);
        String bidderName = auctionHelper.lookupUserName(bidderId);
        String leaderName = auctionHelper.lookupUserName(auction.getLeaderId());
        BidHistoryDto latestBid = auctionHelper.toBidHistoryItem(bidderId, amount, timestamp);
        latestBid.setAuctionId(auctionId);
        latestBid.setSource("AUTO_BID");
        latestBid.setAutoBid(true);

        RealtimeEvent bidPlaced = RealtimeEventFactory.bidPlaced(
                auctionId,
                null,
                bidderId,
                bidderName,
                auction.getLeaderId(),
                leaderName,
                auction.getCurrentPrice(),
                stateName(auction),
                auctionHelper.computeRemainingSeconds(auctionId),
                auction.getEndTime(),
                latestBid,
                bidderName + " placed an auto-bid."
        );
        realtimeNotifier.broadcastToAuction(auctionId, bidPlaced);

        if (previousLeader == null || !previousLeader.equals(auction.getLeaderId())) {
            RealtimeEvent leaderChanged = RealtimeEventFactory.leaderChanged(
                    auctionId,
                    bidderId,
                    bidderName,
                    auction.getLeaderId(),
                    leaderName,
                    auction.getCurrentPrice(),
                    stateName(auction),
                    auctionHelper.computeRemainingSeconds(auctionId),
                    auction.getEndTime(),
                    leaderName == null ? "Leader changed." : leaderName + " is now leading."
            );
            realtimeNotifier.broadcastToAuction(auctionId, leaderChanged);
        }
    }

    private Auction loadAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));
    }

    private String stateName(Auction auction) {
        return auction.getState() == null ? null : auction.getState().name();
    }
}
