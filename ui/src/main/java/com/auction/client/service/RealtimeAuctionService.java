package com.auction.client.service;

import com.auction.client.dto.response.AuctionListResponse;

import java.util.function.Consumer;

public class RealtimeAuctionService {

    private Consumer<AuctionListResponse> onAuctionUpdated;
    private Consumer<String> onConnectionStatusChanged;
    private Consumer<String> onError;

    public void connect(String auctionId) {
        notifyConnectionStatus("POLLING MODE");

        // Hiện tại server chưa có WebSocket.
        // Sau này nếu server có WebSocket thì code connect sẽ viết ở đây.
    }

    public void disconnect() {
        notifyConnectionStatus("DISCONNECTED");

        // Sau này nếu có WebSocket thì đóng kết nối ở đây.
    }

    public void setOnAuctionUpdated(Consumer<AuctionListResponse> onAuctionUpdated) {
        this.onAuctionUpdated = onAuctionUpdated;
    }

    public void setOnConnectionStatusChanged(Consumer<String> onConnectionStatusChanged) {
        this.onConnectionStatusChanged = onConnectionStatusChanged;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    public void notifyAuctionUpdated(AuctionListResponse latestAuction) {
        if (onAuctionUpdated != null) {
            onAuctionUpdated.accept(latestAuction);
        }
    }

    private void notifyConnectionStatus(String status) {
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(status);
        }
    }

    private void notifyError(String message) {
        if (onError != null) {
            onError.accept(message);
        }
    }
}