package com.auction.server.dto;

import java.math.BigDecimal;

public class WalletAmountRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
