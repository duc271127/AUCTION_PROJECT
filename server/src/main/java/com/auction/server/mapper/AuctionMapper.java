package com.auction.server.mapper;

import com.auction.server.dto.AuctionDetailResponse;
import com.auction.server.entity.Auction;
import com.auction.server.util.AuctionImageResolver;

public final class AuctionMapper {

    private AuctionMapper() {
    }

    public static AuctionDetailResponse toDetail(Auction auction,
                                                 int bidCount,
                                                 double minNextBid,
                                                 String leaderName,
                                                 String sellerName) {
        AuctionDetailResponse response = new AuctionDetailResponse();
        response.id = auction.getId();
        response.itemId = auction.getItemId();
        response.title = auction.getTitle();
        response.description = auction.getDescription();
        response.imageUrl = AuctionImageResolver.resolvePrimaryImage(auction);
        response.category = auction.getCategory();
        response.sellerId = auction.getSellerId() != null ? auction.getSellerId() : auction.getCreatedBy();
        response.sellerName = sellerName;
        response.leaderId = auction.getLeaderId();
        response.bidCount = bidCount;
        response.currentPrice = auction.getCurrentPrice();
        double reserve = auction.getReservePrice() == null ? 0.0 : auction.getReservePrice();
        response.reservePrice = reserve;
        response.reserveMet = reserve <= 0.0 || auction.getCurrentPrice() >= reserve;
        response.viewCount = Math.max(0L, auction.getViewCount());
        response.favoriteCount = auction.getFavoriteCount() == null ? 0L : auction.getFavoriteCount();
        response.trendingScore = auction.getTrendingScore() == null ? 0.0 : auction.getTrendingScore();
        response.minNextBid = minNextBid;
        response.leaderName = leaderName;
        response.state = auction.getState() == null ? null : auction.getState().name();
        response.startTime = auction.getStartTime();
        response.endTime = auction.getEndTime();
        return response;
    }
}

