package com.auction.client.service;

import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.dto.request.AutoBidRequest;
import com.auction.client.dto.response.BidResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.util.List;

public class AuctionApiService {
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    public AuctionApiService() {
        this.apiClient = new ApiClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<AuctionListResponse> getAuctions() {
        try {
            String responseBody = apiClient.get("/api/auctions");
            return objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<AuctionListResponse>>() {}
            );
        } catch (Exception e) {
            throw new ApiException("Load auction list failed: " + e.getMessage(), e);
        }
    }

    public AuctionListResponse getAuctionById(String auctionId) {
        try {
            String responseBody = apiClient.get("/api/auctions/" + auctionId);
            return objectMapper.readValue(responseBody, AuctionListResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load auction detail failed: " + e.getMessage(), e);
        }
    }
    public AuctionListResponse placeBid(String auctionId, BidRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            String responseBody = apiClient.post("/api/auctions/" + auctionId + "/bids", jsonBody);
            return objectMapper.readValue(responseBody, AuctionListResponse.class);
        } catch (Exception e) {
            throw new ApiException("Place bid failed: " + e.getMessage(), e);
        }
    }
    public void setAutoBid(String auctionId, AutoBidRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            apiClient.post("/api/auctions/" + auctionId + "/auto-bid", jsonBody);
        } catch (Exception e) {
            throw new ApiException("Enable auto-bid failed: " + e.getMessage(), e);
        }
    }
    public List<BidResponse> getBidHistory(String auctionId) {
        try {
            String responseBody = apiClient.get("/api/auctions/" + auctionId + "/bids");
            return objectMapper.readValue(
                    responseBody,
                    new TypeReference<List<BidResponse>>() {}
            );
        } catch (Exception e) {
            throw new ApiException("Load bid history failed: " + e.getMessage(), e);
        }
    }
}