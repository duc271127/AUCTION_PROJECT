package com.auction.server.service.impl;

import com.auction.server.dto.WalletBalanceDto;
import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.Wallet;
import com.auction.server.entity.WalletTransaction;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.server.service.bid.BidWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceImplTest {

    private AuctionRepository auctionRepository;
    private WalletRepository walletRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private UserRepository userRepository;
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        walletRepository = mock(WalletRepository.class);
        walletTransactionRepository = mock(WalletTransactionRepository.class);
        userRepository = mock(UserRepository.class);
        BidWalletService bidWalletService = new BidWalletService(
                auctionRepository,
                walletRepository,
                walletTransactionRepository
        );

        walletService = new WalletServiceImpl(
                walletRepository,
                walletTransactionRepository,
                userRepository,
                bidWalletService
        );

        when(userRepository.existsById(any(UUID.class))).thenReturn(true);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of());
    }

    @Test
    void getBalance_returnsStoredWalletBalanceWhenNoOutstandingWinnerPaymentExists() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletWithBalance("600.00");
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletBalanceDto balance = walletService.getBalance(userId);

        assertEquals(0, balance.getBalance().compareTo(new BigDecimal("600.00")));
        assertEquals(0, balance.getReservedAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, balance.getAvailableToWithdraw().compareTo(new BigDecimal("600.00")));
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("600.00")));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void getBalance_reconcilesFinishedAuctionWinnerPaymentBeforeReturningBalance() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletWithBalance("1000.00");

        Auction finishedAuction = new Auction();
        finishedAuction.setId(UUID.randomUUID());
        finishedAuction.setWinnerId(userId);
        finishedAuction.setLeaderId(userId);
        finishedAuction.setState(AuctionState.FINISHED);
        finishedAuction.setCurrentPrice(100.0);
        finishedAuction.setWinnerPaymentCaptured(false);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of(finishedAuction));

        WalletBalanceDto balance = walletService.getBalance(userId);

        assertEquals(0, balance.getBalance().compareTo(new BigDecimal("900.00")));
        assertEquals(0, balance.getAvailableToWithdraw().compareTo(new BigDecimal("900.00")));
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("900.00")));
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void getBalance_doesNotCreateTransactions() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletWithBalance("500.00");
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletBalanceDto balance = walletService.getBalance(userId);

        assertEquals(0, balance.getBalance().compareTo(new BigDecimal("500.00")));
        verify(walletTransactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void withdraw_blocksAmountReservedByActiveBidDebt() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = walletWithBalance("100.00");
        Auction reservedAuction = new Auction();
        reservedAuction.setId(UUID.randomUUID());
        reservedAuction.setLeaderId(userId);
        reservedAuction.setState(AuctionState.ACTIVE);
        reservedAuction.setCurrentPrice(80.0);

        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(auctionRepository.findOutstandingWalletDebtAuctions(any(UUID.class), anyCollection(), any(AuctionState.class)))
                .thenReturn(List.of(reservedAuction));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> walletService.withdraw(userId, new BigDecimal("30.00"))
        );

        assertEquals(
                "Withdraw amount exceeds your available balance because some funds are reserved for active bids",
                ex.getMessage()
        );
    }

    private Wallet walletWithBalance(String balance) {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal(balance));
        return wallet;
    }
}

