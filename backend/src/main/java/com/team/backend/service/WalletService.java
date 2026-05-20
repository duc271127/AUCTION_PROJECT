package com.team.backend.service;

import com.team.backend.dto.WalletBalanceDto;
import com.team.backend.dto.WalletTransactionDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    WalletBalanceDto getBalance(UUID userId);
    WalletBalanceDto deposit(UUID userId, BigDecimal amount);
    WalletBalanceDto withdraw(UUID userId, BigDecimal amount);
    List<WalletTransactionDto> getHistory(UUID userId);
}