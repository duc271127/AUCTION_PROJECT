package com.team.backend.scheduler;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.service.impl.BidServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class AuctionStateScheduler {

    private final AuctionRepository auctionRepository;

    public AuctionStateScheduler(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    // run every 15 seconds
    @Scheduled(fixedDelayString = "${auction.scheduler.delay-ms:15000}")
    @Transactional
    public void transitionAuctions() {
        Instant now = Instant.now();

        // SCHEDULED -> ACTIVE
        List<Auction> toStart = auctionRepository.findByStateAndStartTimeBefore(AuctionState.SCHEDULED, now);
        for (Auction a : toStart) {
            a.setState(AuctionState.ACTIVE);
            auctionRepository.save(a);
            // optionally publish AuctionStartedEvent via messaging
        }

        // ACTIVE -> FINISHED
        List<Auction> toFinish = auctionRepository.findByStateAndEndTimeBefore(AuctionState.ACTIVE, now);
        for (Auction a : toFinish) {
            a.setState(AuctionState.FINISHED);
            auctionRepository.save(a);
            // determine winner: a.getLeaderId(); publish AuctionFinishedEvent
        }
    }
}
