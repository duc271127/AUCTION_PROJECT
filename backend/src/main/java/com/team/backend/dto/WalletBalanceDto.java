package com.team.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletBalanceDto {
    private UUID userId;
    private BigDecimal balance;

    public WalletBalanceDto() {
    }

    public WalletBalanceDto(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}