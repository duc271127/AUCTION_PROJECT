package com.auction.client.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletBalanceResponse {
    private UUID userId;
    private BigDecimal balance;
    private BigDecimal reservedAmount;
    private BigDecimal availableToWithdraw;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(BigDecimal reservedAmount) { this.reservedAmount = reservedAmount; }

    public BigDecimal getAvailableToWithdraw() { return availableToWithdraw; }
    public void setAvailableToWithdraw(BigDecimal availableToWithdraw) { this.availableToWithdraw = availableToWithdraw; }
}
