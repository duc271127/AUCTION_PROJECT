package com.auction.client.socket;

import com.auction.client.config.EndpointConfig;
import com.auction.client.dto.event.AuctionEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionSocketClient {

    private static final Logger LOGGER = Logger.getLogger(AuctionSocketClient.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocketEventListener listener;
    private boolean connected;
    private boolean stompConnected;
    private String subscribedAuctionId;

    private WebSocketClient client;

    public boolean connectBlockingToServer() {
        String wsUrl = EndpointConfig.getWebSocketUrl();

        try {
            client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    connected = true;
                    LOGGER.info("WebSocket transport connected");

                    sendStompFrame(
                            "CONNECT\n" +
                                    "accept-version:1.2\n" +
                                    "heart-beat:10000,10000\n\n" +
                                    "\u0000"
                    );
                }

                @Override
                public void onMessage(String message) {
                    onSockJsMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    stompConnected = false;
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
            LOGGER.log(Level.WARNING, "Cannot connect to realtime server at " + wsUrl, e);
            notifyError("Cannot connect to realtime server at " + wsUrl + ": " + safeMessage(e));
            return false;
        }
    }

    public void connect() {
        connectBlockingToServer();
    }

    public void subscribeAuction(String auctionId) {
        if (!connected || client == null || !client.isOpen()) {
            notifyError("Socket not connected");
            return;
        }

        subscribedAuctionId = auctionId;

        String frame =
                "SUBSCRIBE\n" +
                        "id:auction-" + auctionId + "\n" +
                        "destination:/topic/auctions/" + auctionId + "\n\n" +
                        "\u0000";

        sendStompFrame(frame);
        LOGGER.fine("Subscribed auction topic: /topic/auctions/" + auctionId);
    }

    public void disconnect() {
        try {
            if (client != null && client.isOpen()) {
                sendStompFrame("DISCONNECT\n\n\u0000");
                client.close();
            }
        } catch (Exception ignored) {
        }

        connected = false;
        stompConnected = false;
        subscribedAuctionId = null;
    }

    public void setListener(SocketEventListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected && stompConnected;
    }

    public String getSubscribedAuctionId() {
        return subscribedAuctionId;
    }

    private void sendStompFrame(String stompFrame) {
        try {
            if (client != null && client.isOpen()) {
                String sockJsFrame = objectMapper.writeValueAsString(List.of(stompFrame));
                client.send(sockJsFrame);
            }
        } catch (Exception e) {
            notifyError(safeMessage(e));
        }
    }

    private void onSockJsMessage(String message) {
        try {
            if (message == null || message.isBlank()) {
                return;
            }

            // SockJS open frame
            if ("o".equals(message)) {
                return;
            }

            // SockJS heartbeat
            if ("h".equals(message)) {
                return;
            }

            // SockJS message array
            if (message.startsWith("a")) {
                String payload = message.substring(1);
                String[] frames = objectMapper.readValue(payload, String[].class);

                for (String frame : frames) {
                    onStompFrame(frame);
                }

                return;
            }

            // SockJS close frame
            if (message.startsWith("c")) {
                connected = false;
                stompConnected = false;
                notifyDisconnected(message);
            }

        } catch (Exception e) {
            notifyError("Cannot parse realtime frame: " + safeMessage(e));
        }
    }

    private void onStompFrame(String frame) {
        if (frame == null || frame.isBlank()) {
            return;
        }

        if (frame.startsWith("CONNECTED")) {
            stompConnected = true;

            if (subscribedAuctionId != null) {
                subscribeAuction(subscribedAuctionId);
            }

            return;
        }

        if (frame.startsWith("MESSAGE")) {
            String body = extractBody(frame);

            if (body == null || body.isBlank()) {
                return;
            }

            onMessageReceived(body);
            return;
        }

        if (frame.startsWith("ERROR")) {
            notifyError(extractBody(frame));
        }
    }

    private String extractBody(String frame) {
        int bodyStart = frame.indexOf("\n\n");

        if (bodyStart < 0) {
            return "";
        }

        String body = frame.substring(bodyStart + 2);
        return body.replace("\u0000", "").trim();
    }

    public void onMessageReceived(String jsonMessage) {
        try {
            AuctionEventDto event =
                    objectMapper.readValue(jsonMessage, AuctionEventDto.class);

            dispatch(event);

        } catch (Exception e) {
            notifyError("Cannot parse realtime event: " + safeMessage(e));
        }
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

            case "AUCTION_FINISHED", "AUCTION_CLOSED" ->
                    listener.onAuctionFinished(event);

            case "ERROR" ->
                    listener.onError(event);

            default ->
                    notifyError("Unknown realtime event type: " + event.getType());
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

    private String safeMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception == null ? "Unknown error" : exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
