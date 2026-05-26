package com.auction.client.service;

import com.auction.client.dto.request.AutoBidRequest;
import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AuctionPageResponse;
import com.auction.client.dto.response.AutoBidResponse;
import com.auction.client.dto.response.BidPlacementResponse;
import com.auction.client.dto.response.BidResponse;
import com.auction.client.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuctionApiService {
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;

    public AuctionApiService() {
        this.apiClient = new ApiClient();
        this.objectMapper = new ObjectMapper();
    }

    public AuctionPageResponse getAuctions() {
        return searchAuctions(null, null, null, 0, 12, "endTime,asc");
    }

    public AuctionPageResponse getTrendingAuctions(String category, String q, String state, int page, int size) {
        try {
            StringBuilder endpoint = new StringBuilder("/api/auctions/trending?page=")
                    .append(page)
                    .append("&size=")
                    .append(size);

            if (category != null && !category.isBlank()) {
                endpoint.append("&category=").append(encode(category));
            }
            if (q != null && !q.isBlank()) {
                endpoint.append("&q=").append(encode(q));
            }
            if (state != null && !state.isBlank()) {
                endpoint.append("&state=").append(encode(state));
            }

            String responseBody = apiClient.get(endpoint.toString());
            return objectMapper.readValue(responseBody, AuctionPageResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load trending auctions failed: " + e.getMessage(), e);
        }
    }

    public AuctionPageResponse getForYouAuctions(String category, String q, String state, int page, int size) {
        try {
            StringBuilder endpoint = new StringBuilder("/api/auctions/for-you?page=")
                    .append(page)
                    .append("&size=")
                    .append(size);

            if (category != null && !category.isBlank()) {
                endpoint.append("&category=").append(encode(category));
            }
            if (q != null && !q.isBlank()) {
                endpoint.append("&q=").append(encode(q));
            }
            if (state != null && !state.isBlank()) {
                endpoint.append("&state=").append(encode(state));
            }

            String responseBody = apiClient.get(endpoint.toString());
            return objectMapper.readValue(responseBody, AuctionPageResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load personalized auctions failed: " + e.getMessage(), e);
        }
    }

    public AuctionPageResponse searchAuctions(String category, String q, String state, int page, int size, String sort) {
        try {
            StringBuilder endpoint = new StringBuilder("/api/auctions?page=")
                    .append(page)
                    .append("&size=")
                    .append(size)
                    .append("&sort=")
                    .append(encode(sort));

            if (category != null && !category.isBlank()) {
                endpoint.append("&category=").append(encode(category));
            }
            if (q != null && !q.isBlank()) {
                endpoint.append("&q=").append(encode(q));
            }
            if (state != null && !state.isBlank()) {
                endpoint.append("&state=").append(encode(state));
            }

            String responseBody = apiClient.get(endpoint.toString());
            return objectMapper.readValue(responseBody, AuctionPageResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load auction list failed: " + e.getMessage(), e);
        }
    }

    public AuctionDetailResponse getAuctionDetail(String auctionId) {
        try {
            String responseBody = apiClient.get("/api/auctions/" + auctionId + "/detail");
            return objectMapper.readValue(responseBody, AuctionDetailResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load auction detail failed: " + e.getMessage(), e);
        }
    }

    public AuctionListResponse getAuctionById(String auctionId) {
        try {
            String responseBody = apiClient.get("/api/auctions/" + auctionId);
            return objectMapper.readValue(responseBody, AuctionListResponse.class);
        } catch (Exception e) {
            throw new ApiException("Load auction snapshot failed: " + e.getMessage(), e);
        }
    }

    public BidPlacementResponse placeBid(String auctionId, BidRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            String responseBody = apiClient.post("/api/auctions/" + auctionId + "/bids", jsonBody);
            return objectMapper.readValue(responseBody, BidPlacementResponse.class);
        } catch (Exception e) {
            throw new ApiException("Place bid failed: " + e.getMessage(), e);
        }
    }

    public AutoBidResponse setAutoBid(String auctionId, AutoBidRequest request) {
        try {
            String jsonBody = objectMapper.writeValueAsString(request);
            String responseBody = apiClient.post("/api/auctions/" + auctionId + "/auto-bid", jsonBody);
            return objectMapper.readValue(responseBody, AutoBidResponse.class);
        } catch (Exception e) {
            throw new ApiException("Enable auto-bid failed: " + e.getMessage(), e);
        }
    }

    public void cancelAutoBid(String auctionId) {
        try {
            apiClient.delete("/api/auctions/" + auctionId + "/auto-bid");
        } catch (Exception e) {
            throw new ApiException("Disable auto-bid failed: " + e.getMessage(), e);
        }
    }

    public List<AutoBidResponse> listMyAutoBids() {
        try {
            String responseBody = apiClient.get("/api/auctions/me/auto-bids");
            return objectMapper.readValue(responseBody, new TypeReference<List<AutoBidResponse>>() {});
        } catch (Exception e) {
            throw new ApiException("Load my auto-bids failed: " + e.getMessage(), e);
        }
    }

    public List<BidResponse> getBidHistory(String auctionId) {
        try {
            String responseBody = apiClient.get("/api/auctions/" + auctionId + "/bids");
            return objectMapper.readValue(responseBody, new TypeReference<List<BidResponse>>() {});
        } catch (Exception e) {
            throw new ApiException("Load bid history failed: " + e.getMessage(), e);
        }
    }

    public void trackView(String auctionId) {
        try {
            apiClient.post("/api/auctions/" + auctionId + "/view", "");
        } catch (Exception e) {
            throw new ApiException("Track auction view failed: " + e.getMessage(), e);
        }
    }

    public double getMinIncrement() {
        try {
            String responseBody = apiClient.get("/api/auctions/config/min-increment");
            return objectMapper.readValue(responseBody, Double.class);
        } catch (Exception e) {
            throw new ApiException("Load minimum increment failed: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
