package com.auction.server.service.impl;

import com.auction.server.dto.WalletBalanceDto;
import com.auction.server.dto.WalletTransactionDto;
import com.auction.server.entity.Wallet;
import com.auction.server.entity.WalletTransaction;
import com.auction.server.entity.WalletTransactionType;
import com.auction.server.exception.BusinessRuleException;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.WalletRepository;
import com.auction.server.repository.WalletTransactionRepository;
import com.auction.server.service.WalletService;
import com.auction.server.service.bid.BidWalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BidWalletService bidWalletService;

    public WalletServiceImpl(
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            UserRepository userRepository,
            BidWalletService bidWalletService
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.bidWalletService = bidWalletService;
    }

    @Override
    @Transactional
    public WalletBalanceDto getBalance(UUID userId) {
        bidWalletService.reconcileWinnerPayments(userId);
        Wallet wallet = getOrCreateWallet(userId);
        return toBalanceDto(userId, wallet.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceDto deposit(UUID userId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        Wallet saved = walletRepository.save(wallet);

        saveTransaction(saved, WalletTransactionType.DEPOSIT, amount);

        return toBalanceDto(userId, saved.getBalance());
    }

    @Override
    @Transactional
    public WalletBalanceDto withdraw(UUID userId, BigDecimal amount) {
        validateAmount(amount);

        Wallet wallet = getOrCreateWalletForUpdate(userId);
        BigDecimal availableToWithdraw = bidWalletService.calculateAvailableToWithdraw(userId, wallet.getBalance());

        if (availableToWithdraw.compareTo(amount) < 0) {
            throw new BusinessRuleException("Withdraw amount exceeds your available balance because some funds are reserved for active bids");
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        Wallet saved = walletRepository.save(wallet);

        saveTransaction(saved, WalletTransactionType.WITHDRAW, amount);

        return toBalanceDto(userId, saved.getBalance());
    }

    @Override
    @Transactional
    public List<WalletTransactionDto> getHistory(UUID userId) {
        validateUser(userId);

        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Wallet getOrCreateWallet(UUID userId) {
        validateUser(userId);

        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setCreatedAt(Instant.now());
                    return walletRepository.save(wallet);
                });
    }

    private Wallet getOrCreateWalletForUpdate(UUID userId) {
        validateUser(userId);

        return walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setBalance(BigDecimal.ZERO);
                    wallet.setCreatedAt(Instant.now());
                    return walletRepository.save(wallet);
                });
    }

    private void validateUser(UUID userId) {
        if (userId == null) {
            throw new BusinessRuleException("userId is required");
        }
        if (!userRepository.existsById(userId)) {
            throw new BusinessRuleException("User not found: " + userId);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("amount must be positive");
        }
    }

    private void saveTransaction(Wallet wallet, WalletTransactionType type, BigDecimal amount) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setUserId(wallet.getUserId());
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setCreatedAt(Instant.now());
        transactionRepository.save(tx);
    }

    private WalletBalanceDto toBalanceDto(UUID userId, BigDecimal balance) {
        BigDecimal safeBalance = balance == null ? BigDecimal.ZERO : balance;
        BigDecimal reservedAmount = bidWalletService.calculateOutstandingDebt(userId);
        BigDecimal availableToWithdraw = bidWalletService.calculateAvailableToWithdraw(userId, safeBalance);
        return new WalletBalanceDto(userId, safeBalance, reservedAmount, availableToWithdraw);
    }

    private WalletTransactionDto toDto(WalletTransaction tx) {
        WalletTransactionDto dto = new WalletTransactionDto();
        dto.setId(tx.getId());
        dto.setType(tx.getType());
        dto.setAmount(tx.getAmount());
        dto.setBalanceAfter(tx.getBalanceAfter());
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}

