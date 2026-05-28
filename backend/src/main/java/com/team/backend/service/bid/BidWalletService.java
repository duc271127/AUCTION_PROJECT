package com.team.backend.service.bid;

import com.team.backend.entity.Auction;
import com.team.backend.entity.Wallet;
import com.team.backend.entity.WalletTransaction;
import com.team.backend.entity.WalletTransactionType;
import com.team.backend.exception.InvalidBidException;
import com.team.backend.repository.WalletRepository;
import com.team.backend.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class BidWalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public BidWalletService(WalletRepository walletRepository,
                            WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public void ensureSufficientBalanceForBid(UUID bidderId, double requestedAmount) {
        BigDecimal availableBalance = walletRepository.findByUserId(bidderId)
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        BigDecimal required = BigDecimal.valueOf(requestedAmount);
        if (availableBalance.compareTo(required) < 0) {
            throw new InvalidBidException("Insufficient wallet balance for this bid");
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
