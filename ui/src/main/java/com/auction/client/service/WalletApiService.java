package com.auction.client.service;

import com.auction.client.dto.request.WalletAmountRequest;
import com.auction.client.dto.response.WalletBalanceResponse;
import com.auction.client.dto.response.WalletTransactionResponse;

import java.util.List;

public class WalletApiService {
    private final ApiClient apiClient = new ApiClient();

    public WalletBalanceResponse getBalance() {
        String body = apiClient.get("/api/wallet/balance");
        return new com.google.gson.Gson().fromJson(body, WalletBalanceResponse.class);
    }

    public WalletBalanceResponse deposit(WalletAmountRequest request) {
        return apiClient.post("/api/wallet/deposit", request, WalletBalanceResponse.class);
    }

    public WalletBalanceResponse withdraw(WalletAmountRequest request) {
        return apiClient.post("/api/wallet/withdraw", request, WalletBalanceResponse.class);
    }

    public List<WalletTransactionResponse> getHistory() {
        return apiClient.getList("/api/wallet/history", WalletTransactionResponse.class);
    }
}
