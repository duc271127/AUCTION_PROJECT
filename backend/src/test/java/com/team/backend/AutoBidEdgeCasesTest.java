package com.team.backend;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.AutoBid;
import com.team.backend.entity.BidTransaction;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.WalletRepository;
import com.team.backend.repository.WalletTransactionRepository;
import com.team.backend.service.bid.BidWalletService;
import com.team.backend.service.impl.BidTransactionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test các trường hợp:
 * - maxBid bằng nhau -> ưu tiên createdAt nhỏ hơn
 * - auto-bid xung đột -> vòng lặp dừng khi ổn định
 */
class AutoBidEdgeCasesTest {

    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private AutoBidRepository autoBidRepository;
    private BidTransactionalService bidTransactionalService;
    private WalletRepository walletRepository;
    private WalletTransactionRepository walletTransactionRepository;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        bidRepository = mock(BidRepository.class);
        autoBidRepository = mock(AutoBidRepository.class);
        walletRepository = mock(WalletRepository.class);
        walletTransactionRepository = mock(WalletTransactionRepository.class);

        bidTransactionalService = new BidTransactionalService(
                auctionRepository,
                bidRepository,
                autoBidRepository,
                new BidWalletService(auctionRepository, walletRepository, walletTransactionRepository)
        );
        when(walletRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(walletWithBalance("1000.00")));
        when(walletRepository.findByUserIdForUpdate(any(UUID.class))).thenReturn(Optional.of(walletWithBalance("1000.00")));
        when(walletRepository.save(any(com.team.backend.entity.Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any(com.team.backend.entity.WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of());
    }

    @Test
    void equalMaxBid_tieBreaker_createdAtEarlierWins() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(Instant.now().plusSeconds(60));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(100.0);
        auction.setLeaderId(null);

        AutoBid a1 = new AutoBid(auctionId, UUID.randomUUID(), 150.0, 1.0);
        a1.setCreatedAt(Instant.now().minusSeconds(60)); // đăng ký sớm hơn
        AutoBid a2 = new AutoBid(auctionId, UUID.randomUUID(), 150.0, 1.0);
        a2.setCreatedAt(Instant.now().minusSeconds(30)); // đăng ký sau

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Arrays.asList(a1, a2));

        // đặt bid thủ công để kích hoạt auto-bid vòng 1
        BidTransaction tx = bidTransactionalService.placeBidTransactionalAttempt(auctionId, UUID.randomUUID(), 110.0, 1.0, 30, 60, null);

        // sau auto-bid, leader phải là a1 (createdAt sớm hơn)
        assertEquals(a1.getBidderId(), auction.getLeaderId());
        assertTrue(auction.getCurrentPrice() > 110.0);
    }

    @Test
    void autoBid_conflict_resolves_to_stable_state() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(10));
        auction.setEndTime(Instant.now().plusSeconds(60));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(100.0);
        auction.setLeaderId(null);

        // two auto-bidders with different max amounts
        AutoBid high = new AutoBid(auctionId, UUID.randomUUID(), 200.0, 1.0);
        AutoBid mid = new AutoBid(auctionId, UUID.randomUUID(), 150.0, 1.0);
        AutoBid low = new AutoBid(auctionId, UUID.randomUUID(), 120.0, 1.0);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidTransaction.class))).thenAnswer(i -> i.getArgument(0));
        // simulate repository returning list where best is high, second best is mid
        when(autoBidRepository.findByAuctionIdAndActiveTrueOrderByMaxAmountDescCreatedAtAsc(auctionId))
                .thenReturn(Arrays.asList(high, mid, low));

        // place manual bid to trigger auto-bid rounds
        bidTransactionalService.placeBidTransactionalAttempt(auctionId, UUID.randomUUID(), 110.0, 1.0, 30, 60, null);

        // final leader should be high bidder and price should be <= high.maxAmount
        assertEquals(high.getBidderId(), auction.getLeaderId());
        assertTrue(auction.getCurrentPrice() <= high.getMaxAmount());
    }

    private com.team.backend.entity.Wallet walletWithBalance(String balance) {
        com.team.backend.entity.Wallet wallet = new com.team.backend.entity.Wallet();
        wallet.setBalance(new java.math.BigDecimal(balance));
        return wallet;
    }
}
