package com.team.backend.service.impl;

import com.team.backend.entity.Auction;
import com.team.backend.entity.AuctionState;
import com.team.backend.entity.Wallet;
import com.team.backend.entity.WalletTransaction;
import com.team.backend.repository.AuctionRepository;
import com.team.backend.repository.AutoBidRepository;
import com.team.backend.repository.BidRepository;
import com.team.backend.repository.FavoriteRepository;
import com.team.backend.repository.ItemRepository;
import com.team.backend.repository.WalletRepository;
import com.team.backend.repository.WalletTransactionRepository;
import com.team.backend.service.AuctionHelper;
import com.team.backend.service.bid.BidWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuctionServiceImplTest {

    private AuctionRepository auctionRepository;
    private ItemRepository itemRepository;
    private AutoBidRepository autoBidRepository;
    private BidRepository bidRepository;
    private FavoriteRepository favoriteRepository;
    private AuctionHelper auctionHelper;
    private WalletRepository walletRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private AuctionServiceImpl auctionService;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        itemRepository = mock(ItemRepository.class);
        autoBidRepository = mock(AutoBidRepository.class);
        bidRepository = mock(BidRepository.class);
        favoriteRepository = mock(FavoriteRepository.class);
        auctionHelper = mock(AuctionHelper.class);
        walletRepository = mock(WalletRepository.class);
        walletTransactionRepository = mock(WalletTransactionRepository.class);

        BidWalletService bidWalletService = new BidWalletService(
                auctionRepository,
                walletRepository,
                walletTransactionRepository
        );
        auctionService = new AuctionServiceImpl(
                auctionRepository,
                itemRepository,
                autoBidRepository,
                bidRepository,
                favoriteRepository,
                auctionHelper,
                bidWalletService,
                Optional.empty()
        );

        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByAuctionIds(any())).thenReturn(List.of());
        when(favoriteRepository.countByAuctionIds(any())).thenReturn(List.of());
        when(auctionHelper.lookupUserNames(any())).thenReturn(new HashMap<>());
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of());
    }

    @Test
    void closeAuction_deductsWinnerBalanceOnce() {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        Wallet winnerWallet = walletWithBalance("600.00");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(30));
        auction.setEndTime(Instant.now().plusSeconds(30));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(200.0);
        auction.setLeaderId(winnerId);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUserIdForUpdate(winnerId)).thenReturn(Optional.of(winnerWallet));

        auctionService.closeAuction(auctionId);

        assertEquals(AuctionState.FINISHED, auction.getState());
        assertEquals(winnerId, auction.getWinnerId());
        assertTrue(auction.isWinnerPaymentCaptured());
        assertEquals(0, winnerWallet.getBalance().compareTo(new BigDecimal("400.00")));
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void getAuction_expiredAuctionSynchronizesStateWithoutChargingWinner() {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        Wallet winnerWallet = walletWithBalance("600.00");

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(60));
        auction.setEndTime(Instant.now().minusSeconds(5));
        auction.setState(AuctionState.ACTIVE);
        auction.setCurrentPrice(200.0);
        auction.setLeaderId(winnerId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUserIdForUpdate(winnerId)).thenReturn(Optional.of(winnerWallet));

        auctionService.getAuction(auctionId);

        assertEquals(AuctionState.FINISHED, auction.getState());
        assertEquals(winnerId, auction.getWinnerId());
        assertEquals(0, winnerWallet.getBalance().compareTo(new BigDecimal("600.00")));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void getAuction_doesNotChargeAgainWhenWinnerAlreadyCaptured() {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStartTime(Instant.now().minusSeconds(60));
        auction.setEndTime(Instant.now().minusSeconds(5));
        auction.setState(AuctionState.FINISHED);
        auction.setCurrentPrice(200.0);
        auction.setLeaderId(winnerId);
        auction.setWinnerId(winnerId);
        auction.setWinnerPaymentCaptured(true);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        auctionService.getAuction(auctionId);

        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    private Wallet walletWithBalance(String balance) {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal(balance));
        return wallet;
    }
}
