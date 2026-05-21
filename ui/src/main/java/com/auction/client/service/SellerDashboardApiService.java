package com.auction.client.service;

import com.auction.client.dto.response.SellerStatsResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.session.SessionManager;

import java.util.UUID;

public class SellerDashboardApiService {
    private final ApiClient apiClient = new ApiClient();

    public SellerStatsResponse getStats() {
        UUID sellerId = SessionManager.getUserId();
        if (sellerId == null) {
            throw new ApiException("Seller id is missing. Please login again.");
        }

        String body = apiClient.get("/seller/stats?sellerId=" + sellerId);
        return new com.google.gson.Gson().fromJson(body, SellerStatsResponse.class);
    }
}
