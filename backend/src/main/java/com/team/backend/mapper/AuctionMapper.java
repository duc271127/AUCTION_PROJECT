package com.team.backend.mapper;

import com.team.backend.dto.AuctionDetailResponse;
import com.team.backend.entity.Auction;

public class AuctionMapper {
    public static AuctionDetailResponse toDetail(Auction a, int bidCount, double minNextBid, String leaderName) {
        AuctionDetailResponse r = new AuctionDetailResponse();
        r.id = a.getId();
        r.title = a.getTitle();
        r.description = a.getDescription();
        r.imageUrl = a.getImageUrl();
        r.category = a.getCategory();
        // Nếu entity có sellerId riêng, thay a.getCreatedBy() bằng a.getSellerId()
        r.sellerId = a.getCreatedBy();
        r.bidCount = bidCount;
        r.currentPrice = a.getCurrentPrice();
        r.minNextBid = minNextBid;
        r.leaderName = leaderName;
        // Auction.getState() trả AuctionState (enum) -> chuyển sang String
        r.state = a.getState() == null ? null : a.getState().name();
        r.startTime = a.getStartTime();
        r.endTime = a.getEndTime();

        return r;
    }
}
