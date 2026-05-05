package com.auction.client.socket;

import com.auction.client.dto.event.AuctionEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuctionSocketClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocketEventListener listener;
    private boolean connected;
    private String subscribedAuctionId;

    public void connect() {
        // Block 11: skeleton only.
        // Block 20: replace this with real Spring Boot WebSocket + STOMP connection.
        connected = true;
    }

    public void subscribeAuction(String auctionId) {
        if (!connected) {
            notifyDisconnected("Socket is not connected.");
            return;
        }

        if (auctionId == null || auctionId.isBlank()) {
            notifyError("Cannot subscribe auction because auction id is empty.");
            return;
        }

        subscribedAuctionId = auctionId;

        // Block 20: subscribe to topic, for example:
        // /topic/auctions/{auctionId}
    }

    public void onMessage(String jsonMessage) {
        try {
            AuctionEventDto event = objectMapper.readValue(jsonMessage, AuctionEventDto.class);
            dispatch(event);
        } catch (Exception e) {
            notifyError("Cannot parse realtime event: " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        subscribedAuctionId = null;
        notifyDisconnected("Disconnected by client.");
    }

    public void setListener(SocketEventListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getSubscribedAuctionId() {
        return subscribedAuctionId;
    }

    private void dispatch(AuctionEventDto event) {
        if (event == null || event.getType() == null || event.getType().isBlank()) {
            notifyError("Received realtime event without type.");
            return;
        }

        if (listener == null) {
            return;
        }

        switch (event.getType()) {
            case "BID_PLACED" -> listener.onBidPlaced(event);
            case "LEADER_CHANGED" -> listener.onLeaderChanged(event);
            case "AUCTION_EXTENDED" -> listener.onAuctionExtended(event);
            case "AUCTION_FINISHED" -> listener.onAuctionFinished(event);
            case "ERROR" -> listener.onError(event);
            default -> notifyError("Unknown realtime event type: " + event.getType());
        }
    }

    private void notifyError(String message) {
        if (listener == null) {
            return;
        }

        AuctionEventDto errorEvent = new AuctionEventDto();
        errorEvent.setType("ERROR");
        errorEvent.setMessage(message);

        listener.onError(errorEvent);
    }

    private void notifyDisconnected(String reason) {
        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }
}