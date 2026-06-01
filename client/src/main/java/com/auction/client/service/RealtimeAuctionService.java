package com.auction.client.service;

import com.auction.client.dto.event.AuctionEventDto;
import com.auction.client.socket.AuctionSocketClient;
import com.auction.client.socket.SocketEventListener;

import java.util.function.Consumer;

public class RealtimeAuctionService {

    private final AuctionSocketClient socketClient = new AuctionSocketClient();

    private Consumer<AuctionEventDto> onAuctionEvent;
    private Consumer<String> onConnectionStatusChanged;
    private Consumer<String> onError;

    public RealtimeAuctionService() {
        socketClient.setListener(new SocketEventListener() {
            @Override
            public void onBidPlaced(AuctionEventDto event) {
                notifyAuctionEvent(event);
            }

            @Override
            public void onLeaderChanged(AuctionEventDto event) {
                notifyAuctionEvent(event);
            }

            @Override
            public void onFavoriteChanged(AuctionEventDto event) {
                notifyAuctionEvent(event);
            }

            @Override
            public void onAuctionExtended(AuctionEventDto event) {
                notifyAuctionEvent(event);
            }

            @Override
            public void onAuctionFinished(AuctionEventDto event) {
                notifyAuctionEvent(event);
            }

            @Override
            public void onError(AuctionEventDto event) {
                String message = event.getMessage() == null
                        ? "Realtime error."
                        : event.getMessage();

                notifyError(message);
            }

            @Override
            public void onDisconnected(String reason) {
                notifyConnectionStatus("DISCONNECTED");
                notifyError(reason);
            }
        });
    }

    public void connect(String auctionId) {
        new Thread(() -> {
            int maxAttempts = 3;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                notifyConnectionStatus(attempt == 1 ? "CONNECTING" : "RECONNECTING");

                boolean connected = socketClient.connectBlockingToServer();

                if (connected) {
                    notifyConnectionStatus("SOCKET CONNECTED");
                    socketClient.subscribeAuction(auctionId);
                    notifyConnectionStatus("SUBSCRIBED");
                    return;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            notifyConnectionStatus("POLLING ONLY");
            notifyError("Realtime disconnected. Using polling fallback.");
        }).start();
    }

    public void disconnect() {
        socketClient.disconnect();
    }

    public void setOnAuctionEvent(Consumer<AuctionEventDto> onAuctionEvent) {
        this.onAuctionEvent = onAuctionEvent;
    }

    public void setOnConnectionStatusChanged(Consumer<String> onConnectionStatusChanged) {
        this.onConnectionStatusChanged = onConnectionStatusChanged;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    public void simulateIncomingMessage(String jsonMessage) {
        socketClient.onMessageReceived(jsonMessage);
    }

    private void notifyAuctionEvent(AuctionEventDto event) {
        if (onAuctionEvent != null) {
            onAuctionEvent.accept(event);
        }
    }

    private void notifyConnectionStatus(String status) {
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(status);
        }
    }

    private void notifyError(String message) {
        if (onError != null && message != null && !message.isBlank()) {
            onError.accept(message);
        }
    }
}
