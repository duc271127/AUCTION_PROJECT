package com.team.backend.scheduler;

import com.team.backend.service.AuctionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuctionStateScheduler {

    private final AuctionService auctionService;

    public AuctionStateScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    // run every 15 seconds
    @Scheduled(fixedDelayString = "${auction.scheduler.delay-ms:15000}")
    @Transactional
    public void transitionAuctions() {
        auctionService.refreshStates();
    }
}
