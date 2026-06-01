package com.auction.server.service.bid;

import com.auction.server.entity.Auction;
import com.auction.server.entity.AuctionState;
import com.auction.server.entity.Wallet;
import com.auction.server.entity.WalletTransaction;
import com.auction.server.entity.WalletTransactionType;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BidWalletService {

    private static final List<AuctionState> RESERVED_BID_STATES = List.of(AuctionState.SCHEDULED, AuctionState.ACTIVE);

    private final AuctionRepository auctionRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public BidWalletService(AuctionRepository auctionRepository,
                            WalletRepository walletRepository,
                            WalletTransactionRepository walletTransactionRepository) {
        this.auctionRepository = auctionRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public void ensureSufficientBalanceForBid(UUID bidderId, UUID auctionId, double requestedAmount) {
        ensureSufficientCapacity(bidderId, auctionId, BigDecimal.valueOf(requestedAmount), "Insufficient wallet balance for this bid");
    }

    public void ensureSufficientBalanceForAutoBid(UUID bidderId, UUID auctionId, double maxAmount) {
        ensureSufficientCapacity(bidderId, auctionId, BigDecimal.valueOf(maxAmount), "Insufficient wallet balance for this auto-bid limit");
    }

    public BigDecimal calculateOutstandingDebt(UUID userId) {
        return calculateOutstandingDebtExcludingAuction(userId, null);
    }

    public BigDecimal calculateOutstandingDebtExcludingAuction(UUID userId, UUID excludedAuctionId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }

        return auctionRepository.findOutstandingWalletDebtAuctions(userId, RESERVED_BID_STATES, AuctionState.FINISHED)
                .stream()
                .filter(auction -> auction != null
                        && auction.getId() != null
                        && !auction.isWinnerPaymentCaptured()
                        && (excludedAuctionId == null || !excludedAuctionId.equals(auction.getId()))
                        && auction.getCurrentPrice() > 0.0d)
                .map(auction -> BigDecimal.valueOf(auction.getCurrentPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateAvailableToWithdraw(UUID userId, BigDecimal walletBalance) {
        BigDecimal reservedDebt = calculateOutstandingDebt(userId);
        BigDecimal safeBalance = walletBalance == null ? BigDecimal.ZERO : walletBalance;
        BigDecimal available = safeBalance.subtract(reservedDebt);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    public void reconcileWinnerPayments(UUID userId) {
        if (userId == null) {
            return;
        }

        auctionRepository.findOutstandingWalletDebtAuctions(userId, RESERVED_BID_STATES, AuctionState.FINISHED)
                .stream()
                .filter(auction -> auction != null
                        && auction.getState() == AuctionState.FINISHED
                        && !auction.isWinnerPaymentCaptured()
                        && userId.equals(auction.getWinnerId())
                        && auction.getCurrentPrice() > 0.0d)
                .forEach(this::captureWinnerPayment);
    }

    private void ensureSufficientCapacity(UUID bidderId, UUID auctionId, BigDecimal requiredAmount, String message) {
        BigDecimal walletBalance = walletRepository.findByUserId(bidderId)
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);
        BigDecimal reservedDebt = calculateOutstandingDebtExcludingAuction(bidderId, auctionId);
        BigDecimal availableBalance = walletBalance.subtract(reservedDebt);

        if (availableBalance.compareTo(requiredAmount) < 0) {
            throw new InvalidBidException(message);
        }
    }

    public void captureWinnerPayment(Auction auction) {
        if (auction == null
                || auction.isWinnerPaymentCaptured()
                || auction.getWinnerId() == null
                || auction.getCurrentPrice() <= 0.0d) {
            return;
        }

        debitWallet(auction.getWinnerId(), auction.getCurrentPrice(), WalletTransactionType.WITHDRAW);
        auction.setWinnerPaymentCaptured(true);
    }

    private void debitWallet(UUID userId, double amount, WalletTransactionType type) {
        BigDecimal delta = BigDecimal.valueOf(amount);
        if (userId == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(delta) < 0) {
            throw new InvalidBidException("Insufficient wallet balance for this bid");
        }

        wallet.setBalance(balance.subtract(delta));
        Wallet saved = walletRepository.save(wallet);
        saveTransaction(saved, type, delta);
    }

    private Wallet getOrCreateWalletForUpdate(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setCreatedAt(Instant.now());
                    return walletRepository.save(wallet);
                });
    }

    private void saveTransaction(Wallet wallet, WalletTransactionType type, BigDecimal amount) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(wallet.getUserId());
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setCreatedAt(Instant.now());
        walletTransactionRepository.save(tx);
    }
}

