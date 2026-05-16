package com.auction.client.socket;

import com.auction.client.dto.event.AuctionEventDto;

public interface SocketEventListener {
    void onBidPlaced(AuctionEventDto event);

    void onLeaderChanged(AuctionEventDto event);

    void onAuctionExtended(AuctionEventDto event);

    void onAuctionFinished(AuctionEventDto event);

    void onError(AuctionEventDto event);

    void onDisconnected(String reason);
}