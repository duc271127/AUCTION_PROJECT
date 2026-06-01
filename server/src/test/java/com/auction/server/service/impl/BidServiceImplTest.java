package com.auction.server.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.BidTransaction;
import com.auction.server.entity.Wallet;
import com.auction.server.entity.WalletTransaction;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.exception.ResourceNotFoundException;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.server.service.bid.BidWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.auction.server.service.EventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.server.repository.AutoBidRepository;

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
    private WalletRepository walletRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private BidWalletService bidWalletService;


    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        bidRepository = mock(BidRepository.class);
        autoBidRepository = mock(AutoBidRepository.class);
        eventPublisher = mock(EventPublisher.class);
        walletRepository = mock(WalletRepository.class);
        walletTransactionRepository = mock(WalletTransactionRepository.class);

        bidWalletService = new BidWalletService(auctionRepository, walletRepository, walletTransactionRepository);

        BidTransactionalService realTransactionalService = new BidTransactionalService(
                auctionRepository,
                bidRepository,
                autoBidRepository,
                bidWalletService
        );

        Object transactionalLock = new Object();
        bidTransactionalService = mock(BidTransactionalService.class);
        when(bidTransactionalService.placeBidTransactionalAttempt(
                any(UUID.class),
                any(UUID.class),
                anyDouble(),
                anyDouble(),
                anyLong(),
                anyLong(),
                any()
        )).thenAnswer(invocation -> {
            synchronized (transactionalLock) {
                return realTransactionalService.placeBidTransactionalAttempt(
                        invocation.getArgument(0, UUID.class),
                        invocation.getArgument(1, UUID.class),
                        invocation.getArgument(2, Double.class),
                        invocation.getArgument(3, Double.class),
                        invocation.getArgument(4, Long.class),
                        invocation.getArgument(5, Long.class),
                        invocation.getArgument(6, EventPublisher.class)
                );
            }
        });

        double minIncrement = 1.0;
        bidService = new BidServiceImpl(
                auctionRepository,
                bidRepository,
                autoBidRepository,
                bidTransactionalService,
                bidWalletService,
                minIncrement,
                30,
                60,
                3,
                Optional.of(eventPublisher)
        );

        lenient().when(walletRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(walletWithBalance("1000.00")));
        lenient().when(walletRepository.findByUserIdForUpdate(any(UUID.class))).thenReturn(Optional.of(walletWithBalance("1000.00")));
        lenient().when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of());
    }

    @Test
    void placeBid_success_updatesAuctionAndCreatesTx() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Wallet bidderWallet = walletWithBalance("1000.00");

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
        when(walletRepository.findByUserId(bidderId)).thenReturn(Optional.of(bidderWallet));
        BidTransaction tx = bidService.placeBid(auctionId, bidderId, 12.0);

        assertNotNull(tx);
        assertEquals(12.0, tx.getAmount());
        assertEquals(0, bidderWallet.getBalance().compareTo(new java.math.BigDecimal("1000.00")));
        verify(auctionRepository).save(any(Auction.class));
        verify(bidRepository).save(any(BidTransaction.class));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
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

    @Test
    void placeBid_outbidsPreviousLeader_doesNotTouchWalletBeforeAuctionEnds() {
        UUID auctionId = UUID.randomUUID();
        UUID previousLeaderId = UUID.randomUUID();
        UUID newBidderId = UUID.randomUUID();
        Wallet previousLeaderWallet = walletWithBalance("85.00");
        Wallet newBidderWallet = walletWithBalance("100.00");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(Instant.now().plusSeconds(60));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(15.0);
        auction.setLeaderId(previousLeaderId);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(List.of());
        when(walletRepository.findByUserId(newBidderId)).thenReturn(Optional.of(newBidderWallet));
        bidService.placeBid(auctionId, newBidderId, 20.0);

        assertEquals(0, previousLeaderWallet.getBalance().compareTo(new java.math.BigDecimal("85.00")));
        assertEquals(0, newBidderWallet.getBalance().compareTo(new java.math.BigDecimal("100.00")));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void placeBid_sameLeaderRaisesBid_doesNotDeductWalletImmediately() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Wallet bidderWallet = walletWithBalance("100.00");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(Instant.now().plusSeconds(60));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(50.0);
        auction.setLeaderId(bidderId);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(List.of());
        when(walletRepository.findByUserId(bidderId)).thenReturn(Optional.of(bidderWallet));
        bidService.placeBid(auctionId, bidderId, 70.0);

        assertEquals(0, bidderWallet.getBalance().compareTo(new java.math.BigDecimal("100.00")));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void placeBid_rejectsWhenOtherReservedDebtConsumesAvailableBalance() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Wallet bidderWallet = walletWithBalance("100.00");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(Instant.now().plusSeconds(60));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(10.0);

        Auction reservedAuction = new Auction();
        reservedAuction.setId(UUID.randomUUID());
        reservedAuction.setLeaderId(bidderId);
        reservedAuction.setState(AuctionState.ACTIVE);
        reservedAuction.setCurrentPrice(80.0);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUserId(bidderId)).thenReturn(Optional.of(bidderWallet));
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of(reservedAuction));

        assertThrows(InvalidBidException.class, () -> bidService.placeBid(auctionId, bidderId, 30.0));
        verify(bidRepository, never()).save(any(BidTransaction.class));
    }

    private Wallet walletWithBalance(String balance) {
        Wallet wallet = new Wallet();
        wallet.setBalance(new java.math.BigDecimal(balance));
        return wallet;
    }
}

