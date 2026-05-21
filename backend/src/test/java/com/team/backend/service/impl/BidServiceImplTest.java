package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.BidTransaction;
<<<<<<< Updated upstream
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
=======
import com.team.backend.exception.InvalidBidException;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.service.EventPublisher;
>>>>>>> Stashed changes
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
<<<<<<< Updated upstream

import java.time.Instant;
import java.util.ArrayList;
=======
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
>>>>>>> Stashed changes
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BidServiceImpl adapted to the refactored design where
 * transactional core is in BidTransactionalService.
 */
@ExtendWith(MockitoExtension.class)
class BidServiceImplTest {

    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private AutoBidRepository autoBidRepository;
    private BidTransactionalService bidTransactionalService;
<<<<<<< Updated upstream
=======
    private EventPublisher eventPublisher;
>>>>>>> Stashed changes
    private BidServiceImpl bidService;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        bidRepository = mock(BidRepository.class);
        autoBidRepository = mock(AutoBidRepository.class);
<<<<<<< Updated upstream

        // Create a real transactional service instance but with mocked repositories.
        bidTransactionalService = new BidTransactionalService(auctionRepository, bidRepository, autoBidRepository);

        double minIncrement = 1.0;
        long antiSnipingThreshold = 30L;
        long antiSnipingExtend = 60L;
        int maxRetries = 3;

        // EventPublisher is optional; pass null for tests
=======
        eventPublisher = mock(EventPublisher.class);

        // Mock transactional service so that BidServiceImpl falls back to in-memory path
        bidTransactionalService = mock(BidTransactionalService.class);
        try {
            when(bidTransactionalService.placeBidTransactionalAttempt(
                    any(UUID.class),
                    any(UUID.class),
                    anyDouble(),
                    anyDouble(),
                    anyLong(),
                    anyLong(),
                    any(EventPublisher.class)
            )).thenThrow(new PessimisticLockingFailureException("force fallback for tests"));
        } catch (Exception e) {
            // won't happen for mock setup
        }

>>>>>>> Stashed changes
        bidService = new BidServiceImpl(
                auctionRepository,
                bidRepository,
                autoBidRepository,
                bidTransactionalService,
<<<<<<< Updated upstream
                minIncrement,
                antiSnipingThreshold,
                antiSnipingExtend,
                maxRetries,
                null
=======
                1.0,         // minIncrement
                30L,         // antiSnipingThresholdSeconds
                60L,         // antiSnipingExtendSeconds
                3,           // maxRetries
                eventPublisher
>>>>>>> Stashed changes
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

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(a));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
<<<<<<< Updated upstream
=======
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Collections.emptyList());
>>>>>>> Stashed changes

        BidTransaction tx = bidService.placeBid(auctionId, bidderId, 12.0);

        assertNotNull(tx);
        assertEquals(12.0, tx.getAmount(), 0.0001);
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

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(a));

        assertThrows(InvalidBidException.class, () -> bidService.placeBid(auctionId, bidderId, 100.5));
        verify(bidRepository, never()).save(any());
    }

    @Test
    void placeBid_auctionNotFound_throwsResourceNotFound() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

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

<<<<<<< Updated upstream
        // Simulate repository returning the same auction instance; synchronize to emulate DB serialization
        when(auctionRepository.findByIdForUpdate(auctionId)).thenAnswer(invocation -> {
=======
        when(auctionRepository.findById(auctionId)).thenAnswer(invocation -> {
>>>>>>> Stashed changes
            synchronized (auctionMonitor) {
                // return the same auction instance to allow concurrent threads to update it
                return Optional.of(auction);
            }
        });

        when(auctionRepository.save(any(Auction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
<<<<<<< Updated upstream
=======
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Collections.emptyList());
>>>>>>> Stashed changes

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);

            List<Exception> errors = new ArrayList<>();

            double[] bids = {11.0, 12.0, 13.0};

<<<<<<< Updated upstream
        for (double bidAmount : bids) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bidService.placeBid(auctionId, UUID.randomUUID(), bidAmount);
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }
                return null;
            });
        }

        // wait for threads to be ready and then start them simultaneously
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        start.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(finished, "Executor did not finish in time");
        // final price should be the highest bid attempted
        assertEquals(13.0, auction.getCurrentPrice());
        assertNotNull(auction.getLeaderId());
        // at most two threads may fail (others succeed)
        assertTrue(errors.size() <= 2);
        verify(bidRepository, atLeastOnce()).save(any(BidTransaction.class));
=======
            for (double bidAmount : bids) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        // ensure all threads start together
                        start.await();
                        bidService.placeBid(auctionId, UUID.randomUUID(), bidAmount);
                    } catch (Exception e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    }
                    return null;
                });
            }

            // ensure all threads are ready
            assertTrue(ready.await(3, TimeUnit.SECONDS), "threads didn't become ready in time");
            start.countDown();

            executor.shutdown();
            boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

            assertTrue(finished, "executor did not finish in time");
            assertEquals(13.0, auction.getCurrentPrice(), 0.0001);
            assertNotNull(auction.getLeaderId());
            assertTrue(errors.size() <= 2);
            verify(bidRepository, atLeastOnce()).save(any(BidTransaction.class));
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
>>>>>>> Stashed changes
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

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
<<<<<<< Updated upstream
=======
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Collections.emptyList());
>>>>>>> Stashed changes

        bidService.placeBid(auctionId, bidderId, 12.0);

        assertTrue(auction.getEndTime().isAfter(originalEndTime));
        assertEquals(12.0, auction.getCurrentPrice(), 0.0001);
        assertEquals(bidderId, auction.getLeaderId());
    }
}
