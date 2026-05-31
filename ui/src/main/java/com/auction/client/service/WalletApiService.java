package com.auction.client.service;

import com.auction.client.dto.request.WalletAmountRequest;
import com.auction.client.dto.response.WalletBalanceResponse;
import com.auction.client.dto.response.WalletTransactionResponse;
import com.auction.client.exception.ApiException;
import com.google.gson.Gson;

import java.util.List;

public class WalletApiService {
    private final ApiClient apiClient = new ApiClient();
    private final Gson gson = new Gson();

    public WalletBalanceResponse getBalance() {
        String body = apiClient.get("/api/wallet/balance");
        return parseBalanceResponse(body, "load wallet balance");
    }

    public WalletBalanceResponse deposit(WalletAmountRequest request) {
        WalletBalanceResponse response = apiClient.post("/api/wallet/deposit", request, WalletBalanceResponse.class);
        return requireBalance(response, "deposit funds");
    }

    public WalletBalanceResponse withdraw(WalletAmountRequest request) {
        WalletBalanceResponse response = apiClient.post("/api/wallet/withdraw", request, WalletBalanceResponse.class);
        return requireBalance(response, "withdraw funds");
    }

    public List<WalletTransactionResponse> getHistory() {
        return apiClient.getList("/api/wallet/history", WalletTransactionResponse.class);
    }

    private WalletBalanceResponse parseBalanceResponse(String body, String action) {
        WalletBalanceResponse response = gson.fromJson(body, WalletBalanceResponse.class);
        return requireBalance(response, action);
    }

    private WalletBalanceResponse requireBalance(WalletBalanceResponse response, String action) {
        if (response == null || response.getBalance() == null) {
            throw new ApiException("Wallet API returned no balance for " + action + ".");
        }
        return response;
    }
}
