package com.auction.client.controller;

import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.model.BidRecord;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.util.MockData;
import com.auction.client.service.RealtimeAuctionService;
import com.auction.client.dto.event.AuctionEventDto;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.animation.ScaleTransition;



import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.HashSet;
import java.util.Set;

public class LiveBiddingController {

    @FXML private Label lotTitleLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label leaderLabel;
    @FXML private Label countdownLabel;
    @FXML private Label outbidAlertLabel;
    @FXML private Button placeBidButton;

    @FXML private TextField bidInputField;
    @FXML private ListView<BidRecord> bidHistoryListView;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final RealtimeAuctionService realtimeAuctionService = new RealtimeAuctionService();
    private final ObservableList<BidRecord> bidHistory = FXCollections.observableArrayList();

    private AuctionItem selectedItem;
    private AuctionListResponse currentAuction;
    private Timeline refreshTimeline;
    private Timeline countdownTimeline;
    private boolean isLoadingAuction = false;
    private Long realtimeRemainingSeconds;
    private final Set<String> handledEventIds = new HashSet<>();

    @FXML
    public void initialize() {

        selectedItem = MockData.getSelectedItem();

        connectionStatusLabel.setText("CONNECTED");
        outbidAlertLabel.setText("");

        bidHistoryListView.setItems(bidHistory);

        setupBidHistoryListView();

        // setup realtime service
        setupRealtimeService();

        if (selectedItem == null
                || selectedItem.getId() == null
                || selectedItem.getId().isBlank()) {

            showError("No selected auction.");
            showEmptyState();
            return;
        }

        // load auction lần đầu
        loadAuctionDetail(true);

        // TẮT WEBSOCKET TẠM THỜI
        // realtimeAuctionService.connect(selectedItem.getId());

        // polling refresh mỗi 5 giây
        startPollingRefresh();

        // countdown timer
        startCountdownTimer();
    }

    private void loadAuctionDetail(boolean firstLoad) {
        if (isLoadingAuction) {
            return;
        }

        isLoadingAuction = true;

        CompletableFuture
                .supplyAsync(() -> auctionApiService.getAuctionById(selectedItem.getId()))
                .thenAccept(latest -> applyAuctionUpdate(latest, firstLoad))
                .exceptionally(error -> {
                    showConnectionError();
                    return null;
                });
    }
    private void applyAuctionUpdate(AuctionListResponse latest, boolean firstLoad) {
        runOnUiThread(() -> {
            AuctionListResponse previous = currentAuction;

            currentAuction = latest;
            bindAuctionToScreen(latest);

            if (!firstLoad && previous != null) {
                handleAuctionChange(previous, latest);
                detectOutbid(previous, latest);
            }

            isLoadingAuction = false;
        });
    }
    private void handleAuctionChange(AuctionListResponse previous, AuctionListResponse latest) {
        if (latest.getCurrentPrice() <= previous.getCurrentPrice()) {
            return;
        }

        playCurrentBidPulse();

        UUID currentUserId = SessionManager.getUserId();
        boolean iAmLeader = currentUserId != null && currentUserId.equals(latest.getLeaderId());

        if (iAmLeader) {
            showSuccess("You are leading this auction.");
        } else {
            addBidHistory("Other bidder", latest.getCurrentPrice());
            showInfo("Current bid updated to " + formatMoney(latest.getCurrentPrice()) + ".");
        }
    }
    private void playCurrentBidPulse() {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(160), currentBidLabel);

        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.08);
        scaleTransition.setToY(1.08);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);

        scaleTransition.play();
    }

    private void showConnectionError() {
        runOnUiThread(() -> {
            connectionStatusLabel.setText("DISCONNECTED");

            if (currentAuction == null) {
                showError("Cannot load live auction.");
                showEmptyState();
            }

            isLoadingAuction = false;
        });
    }

    private void runOnUiThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
    private void setupRealtimeService() {
        realtimeAuctionService.setOnAuctionEvent(event -> {
            runOnUiThread(() -> handleRealtimeEvent(event));
        });

        realtimeAuctionService.setOnConnectionStatusChanged(status -> {
            runOnUiThread(() -> connectionStatusLabel.setText(status));
        });

        realtimeAuctionService.setOnError(message -> {
            runOnUiThread(() -> showError(message));
        });
    }
    private boolean shouldSkipEvent(AuctionEventDto event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            return false;
        }

        if (handledEventIds.contains(event.getEventId())) {
            return true;
        }

        handledEventIds.add(event.getEventId());
        return false;
    }
    private void handleRealtimeEvent(AuctionEventDto event) {
        if (event == null || event.getType() == null || event.getType().isBlank()) {
            return;
        }

        if (shouldSkipEvent(event)) {
            return;
        }


        switch (event.getType()) {
            case "BID_PLACED" -> handleBidPlacedEvent(event);
            case "LEADER_CHANGED" -> handleLeaderChangedEvent(event);
            case "AUCTION_EXTENDED" -> handleAuctionExtendedEvent(event);
            case "AUCTION_FINISHED" -> handleAuctionFinishedEvent(event);
            case "ERROR" -> showError(
                    event.getMessage() == null ? "Realtime error." : event.getMessage()
            );
            default -> showInfo(
                    event.getMessage() == null ? "Unknown realtime event: " + event.getType() : event.getMessage()
            );
        }
    }
    private void handleBidPlacedEvent(AuctionEventDto event) {
        if (event.getCurrentPrice() == null) {
            showInfo(event.getMessage() == null ? "New bid placed." : event.getMessage());
            return;
        }

        String bidderName = event.getLeaderName();

        if (bidderName == null || bidderName.isBlank()) {
            bidderName = "Other bidder";
        }

        addBidHistory(bidderName, event.getCurrentPrice());

        showInfo(event.getMessage() == null
                ? bidderName + " placed a new bid."
                : event.getMessage());
    }

    private void handleLeaderChangedEvent(AuctionEventDto event) {
        if (event.getCurrentPrice() == null) {
            showInfo(event.getMessage() == null ? "Leader changed." : event.getMessage());
            return;
        }

        if (currentAuction != null && event.getCurrentPrice() < currentAuction.getCurrentPrice()) {
            return;
        }

        if (currentAuction != null) {
            currentAuction.setCurrentPrice(event.getCurrentPrice());
        }

        currentBidLabel.setText(formatMoney(event.getCurrentPrice()));

        String leaderName = event.getLeaderName();
        if (leaderName == null || leaderName.isBlank()) {
            leaderName = "Unknown bidder";
        }

        leaderLabel.setText("Leader: " + leaderName);

        playCurrentBidPulse();
        if (event.getRemainingSeconds() != null) {
            realtimeRemainingSeconds = Math.max(0, event.getRemainingSeconds());
            countdownLabel.setText(formatSeconds(realtimeRemainingSeconds));
        }

        showInfo(event.getMessage() == null
                ? "Current bid updated to " + formatMoney(event.getCurrentPrice()) + "."
                : event.getMessage());
    }

    private void handleAuctionExtendedEvent(AuctionEventDto event) {
        if (event.getRemainingSeconds() != null) {
            realtimeRemainingSeconds = Math.max(0, event.getRemainingSeconds());
            countdownLabel.setText(formatSeconds(realtimeRemainingSeconds));
        }

        showInfo(event.getMessage() == null
                ? "Auction time extended."
                : event.getMessage());
    }

    private void handleAuctionFinishedEvent(AuctionEventDto event) {
        realtimeRemainingSeconds = 0L;
        countdownLabel.setText("00:00:00");

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        lockBiddingControls();

        String winnerName = event.getLeaderName();
        String message = event.getMessage();

        if (message == null || message.isBlank()) {
            if (winnerName != null && !winnerName.isBlank()) {
                message = "Auction finished. Winner: " + winnerName + ".";
            } else {
                message = "Auction finished.";
            }
        }

        showInfo(message);
    }

    private void lockBiddingControls() {
        bidInputField.setDisable(true);

        if (placeBidButton != null) {
            placeBidButton.setDisable(true);
        }
    }

    private void bindAuctionToScreen(AuctionListResponse auction) {
        String title = auction.getItemName() == null || auction.getItemName().isBlank()
                ? "Unnamed Auction"
                : auction.getItemName();

        lotTitleLabel.setText("Lot - " + title);
        currentBidLabel.setText(formatMoney(auction.getCurrentPrice()));
        countdownLabel.setText(formatCountdown(auction.getEndTime()));
        leaderLabel.setText("Leader: " + formatLeader(auction.getLeaderId()));
        connectionStatusLabel.setText("CONNECTED");

        if (auction.getState() != null && auction.getState().equalsIgnoreCase("FINISHED")) {
            lockBiddingControls();
        }
    }

    private void detectOutbid(AuctionListResponse previous, AuctionListResponse latest) {
        UUID currentUserId = SessionManager.getUserId();

        if (currentUserId == null) {
            return;
        }

        boolean iWasLeader = currentUserId.equals(previous.getLeaderId());
        boolean iAmLeader = currentUserId.equals(latest.getLeaderId());

        if (iWasLeader && !iAmLeader) {
            showError("You have been outbid.");
        }
    }
    private void setupBidHistoryListView() {
        bidHistoryListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(BidRecord bidRecord, boolean empty) {
                super.updateItem(bidRecord, empty);

                if (empty || bidRecord == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label bidderLabel = new Label(bidRecord.getBidderName());
                bidderLabel.getStyleClass().add("bid-history-bidder");

                Label amountLabel = new Label(bidRecord.getBidAmount());
                amountLabel.getStyleClass().add("bid-history-amount");

                Label timeLabel = new Label(bidRecord.getBidTime());
                timeLabel.getStyleClass().add("bid-history-time");

                VBox box = new VBox(3, bidderLabel, amountLabel, timeLabel);
                box.getStyleClass().add("bid-history-item");

                setText(null);
                setGraphic(box);
            }
        });
    }

    @FXML
    private void handleAdd500() {
        increaseBidBy(500);
    }

    @FXML
    private void handleAdd1000() {
        increaseBidBy(1000);
    }

    @FXML
    private void handleAdd5000() {
        increaseBidBy(5000);
    }

    private void increaseBidBy(int increment) {
        double baseValue = currentAuction != null ? currentAuction.getCurrentPrice() : 0;

        String input = bidInputField.getText().trim();
        if (!input.isEmpty()) {
            try {
                baseValue = Double.parseDouble(input.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ignored) {
                baseValue = currentAuction != null ? currentAuction.getCurrentPrice() : 0;
            }
        }

        bidInputField.setText(String.valueOf((int) baseValue + increment));
        clearAlert();
    }

    @FXML
    private void handlePlaceBid() {
        clearAlert();

        if (currentAuction == null) {
            showError("Auction data is unavailable.");
            return;
        }

        if (SessionManager.getUserId() == null) {
            showError("Please login again before bidding.");
            return;
        }

        String input = bidInputField.getText().trim();

        if (input.isEmpty()) {
            showError("Please enter a bid amount.");
            return;
        }

        double newBid;
        try {
            newBid = Double.parseDouble(input.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            showError("Bid amount must be a valid number.");
            return;
        }

        if (newBid <= currentAuction.getCurrentPrice()) {
            showError("Your bid must be higher than current highest bid.");
            return;
        }

        try {
            BidRequest request = new BidRequest(SessionManager.getUserId(), newBid);
            AuctionListResponse updatedAuction = auctionApiService.placeBid(selectedItem.getId(), request);

            currentAuction = updatedAuction;
            bindAuctionToScreen(updatedAuction);

            addBidHistory("You", newBid);

            bidInputField.clear();
            showSuccess("Bid placed successfully.");

        } catch (Exception e) {
            showError(extractFriendlyMessage(e.getMessage()));
            loadAuctionDetail(false);
        }
    }

    private void startPollingRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> loadAuctionDetail(false))
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }
    private void startCountdownTimer() {
        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    if (currentAuction != null) {
                        countdownLabel.setText(formatCountdown(currentAuction.getEndTime()));
                    }
                })
        );

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private String formatMoney(double value) {
        return "$" + String.format("%,.0f", value);
    }
    private String getCurrentTimeText() {
        return DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }

    private void addBidHistory(String bidderName, double amount) {
        bidHistory.add(0, new BidRecord(
                bidderName,
                formatMoney(amount),
                getCurrentTimeText()
        ));

        if (bidHistory.size() > 50) {
            bidHistory.remove(50, bidHistory.size());
        }
    }

    private String formatLeader(UUID leaderId) {
        if (leaderId == null) {
            return "No leader yet";
        }

        UUID currentUserId = SessionManager.getUserId();
        if (currentUserId != null && currentUserId.equals(leaderId)) {
            return "You";
        }

        String text = leaderId.toString();
        return text.length() > 8 ? text.substring(0, 8) + "..." : text;
    }

    private String formatCountdown(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return "N/A";
        }

        try {
            Instant end = Instant.parse(endTime);
            java.time.Duration remaining = java.time.Duration.between(Instant.now(), end);

            if (remaining.isNegative() || remaining.isZero()) {
                return "00:00:00";
            }

            long totalSeconds = remaining.getSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } catch (Exception e) {
            return endTime;
        }
    }
    private String formatSeconds(long totalSeconds) {
        long safeSeconds = Math.max(0, totalSeconds);

        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        long seconds = safeSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String extractFriendlyMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Bid failed.";
        }

        int idx = rawMessage.indexOf("\"message\":\"");
        if (idx >= 0) {
            int start = idx + 11;
            int end = rawMessage.indexOf("\"", start);
            if (end > start) {
                return rawMessage.substring(start, end);
            }
        }

        return rawMessage;
    }

    private void showEmptyState() {
        lotTitleLabel.setText("No auction selected");
        currentBidLabel.setText("-");
        leaderLabel.setText("Leader: -");
        countdownLabel.setText("-");
    }

    private void showError(String message) {
        outbidAlertLabel.setText(message);
        outbidAlertLabel.setStyle("-fx-text-fill: #dc2626;");
    }

    private void showSuccess(String message) {
        outbidAlertLabel.setText(message);
        outbidAlertLabel.setStyle("-fx-text-fill: #16a34a;");
    }
    private void showInfo(String message) {
        outbidAlertLabel.setText(message);
        outbidAlertLabel.setStyle("-fx-text-fill: #2563eb;");
    }

    private void clearAlert() {
        outbidAlertLabel.setText("");
    }

    @FXML
    private void handleBack() {

        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        // TẮT WEBSOCKET
        // realtimeAuctionService.disconnect();

        SceneManager.goToProductDetail();
    }
}