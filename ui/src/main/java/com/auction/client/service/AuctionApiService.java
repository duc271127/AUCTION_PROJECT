package com.auction.client.service;

import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
}