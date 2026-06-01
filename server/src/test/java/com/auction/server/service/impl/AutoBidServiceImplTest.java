package com.auction.server.service.impl;

import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.AutoBid;
import com.auction.server.entity.Wallet;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.server.service.EventPublisher;
import com.auction.server.service.bid.BidWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoBidServiceImplTest {

    private AutoBidRepository autoBidRepository;
    private AuctionRepository auctionRepository;
    private WalletRepository walletRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private AutoBidServiceImpl autoBidService;

    @BeforeEach
    void setUp() {
        autoBidRepository = mock(AutoBidRepository.class);
        auctionRepository = mock(AuctionRepository.class);
        walletRepository = mock(WalletRepository.class);
        walletTransactionRepository = mock(WalletTransactionRepository.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<EventPublisher> eventPublisherProvider = mock(ObjectProvider.class);
        BidWalletService bidWalletService = new BidWalletService(
                auctionRepository,
                walletRepository,
                walletTransactionRepository
        );

        autoBidService = new AutoBidServiceImpl(
                autoBidRepository,
                auctionRepository,
                bidWalletService,
                eventPublisherProvider
        );
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of());
    }

    @Test
    void setAutoBid_replacesExistingCommandsBeforeSavingNewOne() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setCurrentPrice(100.0);
        auction.setState(AuctionState.ACTIVE);
        auction.setEndTime(Instant.now().plusSeconds(600));

        AutoBid existing = new AutoBid(auctionId, bidderId, 120.0, 5.0);
        UUID previousId = existing.getId();

        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(1_000.0));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUserId(bidderId)).thenReturn(Optional.of(wallet));
        when(autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId)).thenReturn(List.of(existing));
        when(autoBidRepository.save(any(AutoBid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AutoBid saved = autoBidService.setAutoBid(auctionId, bidderId, 250.0, 10.0);

        assertEquals(auctionId, saved.getAuctionId());
        assertEquals(bidderId, saved.getBidderId());
        assertEquals(250.0, saved.getMaxAmount());
        assertEquals(10.0, saved.getBidStep());
        assertTrue(saved.isActive());
        assertNotEquals(previousId, saved.getId());

        var inOrder = inOrder(autoBidRepository);
        inOrder.verify(autoBidRepository).findByAuctionIdAndBidderId(auctionId, bidderId);
        inOrder.verify(autoBidRepository).deleteAll(List.of(existing));
        inOrder.verify(autoBidRepository).flush();
        inOrder.verify(autoBidRepository).save(any(AutoBid.class));
    }
}

