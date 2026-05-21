package com.auction.client.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletBalanceResponse {
    private UUID userId;
    private BigDecimal balance;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
