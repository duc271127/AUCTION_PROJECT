package com.auction.client.socket;

import com.auction.client.dto.event.AuctionEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class AuctionSocketClient {
    public boolean connectBlockingToServer() {
        try {
            client = new WebSocketClient(new URI(WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    connected = true;
                    System.out.println("WebSocket connected");
                }

                @Override
                public void onMessage(String message) {
                    onMessageReceived(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    notifyDisconnected(reason);
                }

                @Override
                public void onError(Exception ex) {
                    notifyError(ex.getMessage());
                }
            };

            boolean ok = client.connectBlocking();
            connected = ok;
            return ok;

        } catch (Exception e) {
            notifyError(e.getMessage());
            return false;
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocketEventListener listener;

    private boolean connected;
    private String subscribedAuctionId;

    private WebSocketClient client;

    // DOMAIN PLAYIT CỦA SERVER
    private static final String WS_URL =
            "ws://lungs-decree.with.playit.plus:1125/ws";

    public void connect() {

        try {

            client = new WebSocketClient(new URI(WS_URL)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {

                    connected = true;

                    System.out.println("WebSocket connected");

                }

                @Override
                public void onMessage(String message) {

                    onMessageReceived(message);

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {

                    connected = false;

                    notifyDisconnected(reason);

                }

                @Override
                public void onError(Exception ex) {

                    notifyError(ex.getMessage());

                }
            };

            // QUAN TRỌNG:
            // đợi websocket connect xong rồi mới chạy tiếp
            client.connectBlocking();

        } catch (Exception e) {

            notifyError(e.getMessage());

        }
    }

    public void subscribeAuction(String auctionId) {

        if (!connected) {
            notifyError("Socket not connected");
            return;
        }

        subscribedAuctionId = auctionId;

        try {

            client.send("SUBSCRIBE:" + auctionId);

            System.out.println("Subscribed auction: " + auctionId);

        } catch (Exception e) {

            notifyError(e.getMessage());

        }
    }

    public void onMessageReceived(String jsonMessage) {

        try {

            AuctionEventDto event =
                    objectMapper.readValue(jsonMessage, AuctionEventDto.class);

            dispatch(event);

        } catch (Exception e) {

            notifyError("Cannot parse realtime event: " + e.getMessage());

        }
    }

    public void disconnect() {

        try {

            if (client != null) {
                client.close();
            }

        } catch (Exception ignored) {
        }

        connected = false;
        subscribedAuctionId = null;
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

        if (event == null || event.getType() == null) {
            return;
        }

        if (listener == null) {
            return;
        }

        switch (event.getType()) {

            case "BID_PLACED" ->
                    listener.onBidPlaced(event);

            case "LEADER_CHANGED" ->
                    listener.onLeaderChanged(event);

            case "AUCTION_EXTENDED" ->
                    listener.onAuctionExtended(event);

            case "AUCTION_FINISHED" ->
                    listener.onAuctionFinished(event);

            case "ERROR" ->
                    listener.onError(event);

            default ->
                    notifyError("Unknown realtime event type");

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