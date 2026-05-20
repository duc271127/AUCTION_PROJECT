package com.team.backend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.BidTransaction;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.team.backend.service.EventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.backend.repository.AutoBidRepository;

/**
 * Unit tests for BidServiceImpl (basic scenarios).
 */
@ExtendWith(MockitoExtension.class)
class BidServiceImplTest {

    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private BidServiceImpl bidService;
    private AutoBidRepository autoBidRepository;
    private BidTransactionalService bidTransactionalService;
    private EventPublisher eventPublisher;


    @BeforeEach
    void setUp() {
    auctionRepository = mock(AuctionRepository.class);
    bidRepository = mock(BidRepository.class);
    autoBidRepository = mock(AutoBidRepository.class);
    eventPublisher = mock(EventPublisher.class);

    bidTransactionalService = new BidTransactionalService(
            auctionRepository,
            bidRepository,
            autoBidRepository
    );

    bidService = new BidServiceImpl(
            auctionRepository,
            bidRepository,
            autoBidRepository,
            bidTransactionalService,
            1.0,
            30,
            60,
            3,
            eventPublisher
        );
    }

    @Test
    void placeBid_success_updatesAuctionAndCreatesTx() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        Auction a = new Auction();
        a.setId(auctionId);
        a.setStartTime(Instant.now().minusSeconds(10));
        a.setEndTime(Instant.now().plusSeconds(60));
        a.setState(AuctionState.ACTIVE);
        a.setCurrentPrice(10.0);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(a));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(List.of());

        BidTransaction tx = bidService.placeBid(auctionId, bidderId, 12.0);

        assertNotNull(tx);
        assertEquals(12.0, tx.getAmount());
        verify(auctionRepository).save(any(Auction.class));
        verify(bidRepository).save(any(BidTransaction.class));
    }

    @Test
    void placeBid_tooLow_throwsInvalidBid() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        Auction a = new Auction();
        a.setId(auctionId);
        a.setStartTime(Instant.now().minusSeconds(10));
        a.setEndTime(Instant.now().plusSeconds(60));
        a.setState(AuctionState.ACTIVE);
        a.setCurrentPrice(100.0);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(a));

        assertThrows(InvalidBidException.class, () -> bidService.placeBid(auctionId, bidderId, 100.5));
        verify(bidRepository, never()).save(any());
    }

    @Test
    void placeBid_auctionNotFound_throwsResourceNotFound() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bidService.placeBid(auctionId, bidderId, 50.0));
        verify(bidRepository, never()).save(any());
    }
    @Test
    void placeBid_concurrentBids_highestBidWins() throws Exception {
        UUID auctionId = UUID.randomUUID();

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(Instant.now().plusSeconds(60));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(10.0);

        Object auctionMonitor = new Object();

        when(auctionRepository.findByIdForUpdate(auctionId)).thenAnswer(invocation -> {
            synchronized (auctionMonitor) {
                return Optional.of(auction);
            }
        });

        when(auctionRepository.save(any(Auction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(List.of());

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Exception> errors = new ArrayList<>();

        double[] bids = {11.0, 12.0, 13.0};

        for (double bidAmount : bids) {
            executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    bidService.placeBid(auctionId, UUID.randomUUID(), bidAmount);
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }

                return null;
            });
        }

        ready.await(3, TimeUnit.SECONDS);
        start.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(finished);
        assertEquals(13.0, auction.getCurrentPrice());
        assertNotNull(auction.getLeaderId());
        assertTrue(errors.size() <= 2);
        verify(bidRepository, atLeastOnce()).save(any(BidTransaction.class));
    }
    @Test
    void placeBid_nearEndTime_extendsAuction() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        Instant originalEndTime = Instant.now().plusSeconds(10);

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(originalEndTime);
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(10.0);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(List.of());

        bidService.placeBid(auctionId, bidderId, 12.0);

        assertTrue(auction.getEndTime().isAfter(originalEndTime));
        assertEquals(12.0, auction.getCurrentPrice());
        assertEquals(bidderId, auction.getLeaderId());
    }


}


