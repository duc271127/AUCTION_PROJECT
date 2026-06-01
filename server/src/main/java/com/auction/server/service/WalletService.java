package com.auction.server.service;

import com.auction.server.dto.WalletBalanceDto;
import com.auction.server.dto.WalletTransactionDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    WalletBalanceDto getBalance(UUID userId);
    WalletBalanceDto deposit(UUID userId, BigDecimal amount);
    WalletBalanceDto withdraw(UUID userId, BigDecimal amount);
    List<WalletTransactionDto> getHistory(UUID userId);
}
