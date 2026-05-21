package com.auction.client.controller;

import com.auction.client.dto.event.AuctionEventDto;
import com.auction.client.dto.request.AutoBidRequest;
import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.BidPlacementResponse;
import com.auction.client.dto.response.BidResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.model.BidRecord;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.RealtimeAuctionService;
import com.auction.client.session.SessionManager;
import com.auction.client.util.MockData;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LiveBiddingController {

    @FXML private Label lotTitleLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label leaderLabel;
    @FXML private Label countdownLabel;
    @FXML private Label outbidAlertLabel;
    @FXML private Button placeBidButton;
    @FXML private Button autoBidButton;
    @FXML private TextField bidInputField;
    @FXML private TextField autoBidMaxInput;
    @FXML private Label autoBidStatusLabel;
    @FXML private ListView<BidRecord> bidHistoryListView;
    @FXML private LineChart<Number, Number> bidChart;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final RealtimeAuctionService realtimeAuctionService = new RealtimeAuctionService();
    private final ObservableList<BidRecord> bidHistory = FXCollections.observableArrayList();
    private final XYChart.Series<Number, Number> bidChartSeries = new XYChart.Series<>();

    private AuctionItem selectedItem;
    private AuctionDetailResponse currentAuction;
    private Timeline refreshTimeline;
    private Timeline countdownTimeline;
    private boolean isLoadingAuction = false;
    private Long realtimeRemainingSeconds;
    private final Set<String> handledEventIds = new HashSet<>();

    @FXML
    public void initialize() {
        selectedItem = MockData.getSelectedItem();
        updateConnectionStatus("CONNECTING");
        outbidAlertLabel.setText("");
        bidHistoryListView.setItems(bidHistory);
        setupBidHistoryListView();
        setupRealtimeService();
        setupBidChart();

        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            showError("No selected auction.");
            showEmptyState();
            return;
        }

        loadAuctionDetail(true);
        loadBidHistory();
        realtimeAuctionService.connect(selectedItem.getId());
        startPollingRefresh();
        startCountdownTimer();
    }

    private void loadAuctionDetail(boolean firstLoad) {
        if (isLoadingAuction) {
            return;
        }
        isLoadingAuction = true;

        CompletableFuture.supplyAsync(() -> auctionApiService.getAuctionDetail(selectedItem.getId()))
                .thenAccept(latest -> applyAuctionUpdate(latest, firstLoad))
                .exceptionally(error -> {
                    showConnectionError();
                    return null;
                });
    }

    private void applyAuctionUpdate(AuctionDetailResponse latest, boolean firstLoad) {
        runOnUiThread(() -> {
            AuctionDetailResponse previous = currentAuction;

            if (previous != null && latest.getCurrentPrice() < previous.getCurrentPrice()) {
                isLoadingAuction = false;
                return;
            }

            currentAuction = latest;
            bindAuctionToScreen(latest);

            if (!firstLoad && previous != null) {
                handleAuctionChange(previous, latest);
                detectOutbid(previous, latest);
            }

            isLoadingAuction = false;
        });
    }

    private void handleAuctionChange(AuctionDetailResponse previous, AuctionDetailResponse latest) {
        if (latest.getCurrentPrice() <= previous.getCurrentPrice()) {
            return;
        }

        playCurrentBidPulse();

        UUID currentUserId = SessionManager.getUserId();
        boolean iAmLeader = currentUserId != null && currentUserId.equals(latest.getLeaderId());

        if (iAmLeader) {
            showSuccess("You are leading this auction.");
        } else {
            addBidHistory(latest.getLeaderName() == null ? "Other bidder" : latest.getLeaderName(), latest.getCurrentPrice());
            showInfo("Current bid updated to " + formatMoney(latest.getCurrentPrice()) + ".");
        }
    }

    private void playCurrentBidPulse() {
        ScaleTransition transition = new ScaleTransition(Duration.millis(160), currentBidLabel);
        transition.setFromX(1.0);
        transition.setFromY(1.0);
        transition.setToX(1.08);
        transition.setToY(1.08);
        transition.setAutoReverse(true);
        transition.setCycleCount(2);
        transition.play();
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
        realtimeAuctionService.setOnAuctionEvent(event -> runOnUiThread(() -> handleRealtimeEvent(event)));
        realtimeAuctionService.setOnConnectionStatusChanged(status -> runOnUiThread(() -> updateConnectionStatus(status)));
        realtimeAuctionService.setOnError(message -> runOnUiThread(() -> showError(message)));
    }

    private void updateConnectionStatus(String status) {
        String safeStatus = (status == null || status.isBlank()) ? "UNKNOWN" : status;
        connectionStatusLabel.setText(safeStatus);

        if ("SUBSCRIBED".equalsIgnoreCase(safeStatus)
                || "SOCKET CONNECTED".equalsIgnoreCase(safeStatus)
                || "CONNECTED".equalsIgnoreCase(safeStatus)) {
            connectionStatusLabel.setStyle("-fx-background-color: #dcfce7;-fx-text-fill: #15803d;-fx-padding: 6 12;-fx-background-radius: 999;-fx-font-weight: bold;");
        } else if ("CONNECTING".equalsIgnoreCase(safeStatus)
                || "RECONNECTING".equalsIgnoreCase(safeStatus)
                || "POLLING".equalsIgnoreCase(safeStatus)
                || "POLLING ONLY".equalsIgnoreCase(safeStatus)) {
            connectionStatusLabel.setStyle("-fx-background-color: #fef9c3;-fx-text-fill: #854d0e;-fx-padding: 6 12;-fx-background-radius: 999;-fx-font-weight: bold;");
        } else if ("DISCONNECTED".equalsIgnoreCase(safeStatus) || "ERROR".equalsIgnoreCase(safeStatus)) {
            connectionStatusLabel.setStyle("-fx-background-color: #fee2e2;-fx-text-fill: #dc2626;-fx-padding: 6 12;-fx-background-radius: 999;-fx-font-weight: bold;");
        } else {
            connectionStatusLabel.setStyle("-fx-background-color: #e2e8f0;-fx-text-fill: #334155;-fx-padding: 6 12;-fx-background-radius: 999;-fx-font-weight: bold;");
        }
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
            case "AUCTION_CLOSED" -> handleAuctionFinishedEvent(event);
            case "ERROR" -> showError(event.getMessage() == null ? "Realtime error." : event.getMessage());
            default -> showInfo(event.getMessage() == null ? "Unknown realtime event: " + event.getType() : event.getMessage());
        }
    }

    private void handleBidPlacedEvent(AuctionEventDto event) {
        if (event.getCurrentPrice() == null) {
            showInfo(event.getMessage() == null ? "New bid placed." : event.getMessage());
            loadAuctionDetail(false);
            return;
        }

        if (currentAuction != null) {
            currentAuction.setCurrentPrice(event.getCurrentPrice());
        }

        String bidderName = event.getLeaderName();
        if (bidderName == null || bidderName.isBlank()) {
            bidderName = "Other bidder";
        }

        currentBidLabel.setText(formatMoney(event.getCurrentPrice()));
        leaderLabel.setText("Leader: " + bidderName);
        addBidHistory(bidderName, event.getCurrentPrice());
        playCurrentBidPulse();
        applyRealtimeCountdown(event);
        showInfo(event.getMessage() == null ? bidderName + " placed a new bid." : event.getMessage());
        loadAuctionDetail(false);
    }

    private void handleLeaderChangedEvent(AuctionEventDto event) {
        if (event.getCurrentPrice() == null) {
            showInfo(event.getMessage() == null ? "Leader changed." : event.getMessage());
            return;
        }

        if (currentAuction != null) {
            currentAuction.setCurrentPrice(event.getCurrentPrice());
        }

        currentBidLabel.setText(formatMoney(event.getCurrentPrice()));
        leaderLabel.setText("Leader: " + safeLeaderName(event.getLeaderName()));
        playCurrentBidPulse();
        applyRealtimeCountdown(event);
        showInfo(event.getMessage() == null ? "Leader changed." : event.getMessage());
    }

    private void handleAuctionExtendedEvent(AuctionEventDto event) {
        applyRealtimeCountdown(event);
        showInfo(event.getMessage() == null ? "Auction time extended." : event.getMessage());
    }

    private void handleAuctionFinishedEvent(AuctionEventDto event) {
        realtimeRemainingSeconds = 0L;
        countdownLabel.setText("00:00:00");

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        lockBiddingControls();
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            String winnerName = event.getLeaderName();
            message = winnerName == null || winnerName.isBlank()
                    ? "Auction finished."
                    : "Auction finished. Winner: " + winnerName + ".";
        }
        showInfo(message);
    }

    private void lockBiddingControls() {
        bidInputField.setDisable(true);
        if (autoBidMaxInput != null) {
            autoBidMaxInput.setDisable(true);
        }
        if (placeBidButton != null) {
            placeBidButton.setDisable(true);
        }
        if (autoBidButton != null) {
            autoBidButton.setDisable(true);
        }
    }

    private void bindAuctionToScreen(AuctionDetailResponse auction) {
        lotTitleLabel.setText("Lot - " + (auction.getTitle() == null || auction.getTitle().isBlank() ? "Unnamed Auction" : auction.getTitle()));
        currentBidLabel.setText(formatMoney(auction.getCurrentPrice()));
        countdownLabel.setText(formatCountdown(auction.getEndTime()));
        leaderLabel.setText("Leader: " + safeLeaderName(auction.getLeaderName()));

        if ("FINISHED".equalsIgnoreCase(auction.getState()) || "CANCELLED".equalsIgnoreCase(auction.getState())) {
            lockBiddingControls();
        }
    }

    private void detectOutbid(AuctionDetailResponse previous, AuctionDetailResponse latest) {
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

    private void setupBidChart() {
        if (bidChart == null) {
            return;
        }
        bidChart.setLegendVisible(false);
        bidChart.setAnimated(false);
        bidChart.getData().clear();
        bidChart.getData().add(bidChartSeries);
    }

    private void loadBidHistory() {
        if (selectedItem == null || selectedItem.getId() == null) {
            return;
        }

        CompletableFuture.supplyAsync(() -> auctionApiService.getBidHistory(selectedItem.getId()))
                .thenAccept(this::applyBidHistory)
                .exceptionally(error -> null);
    }

    private void applyBidHistory(List<BidResponse> history) {
        runOnUiThread(() -> {
            bidHistory.clear();
            bidChartSeries.getData().clear();

            if (history == null || history.isEmpty()) {
                return;
            }

            int chartIndex = 1;
            for (BidResponse bid : history) {
                String bidder = bid.getBidderName();
                if (bidder == null || bidder.isBlank()) {
                    bidder = bid.getBidderId() == null ? "Bidder" : shortId(bid.getBidderId());
                }
                if (bid.isAutoBid()) {
                    bidder += " (auto)";
                }

                String time = bid.getCreatedAt() == null ? "" : formatHistoryTime(bid.getCreatedAt());
                bidHistory.add(new BidRecord(bidder, formatMoney(bid.getAmount()), time));
                bidChartSeries.getData().add(new XYChart.Data<>(chartIndex++, bid.getAmount()));
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

        double minNextBid = currentAuction.getMinNextBid() > 0 ? currentAuction.getMinNextBid() : currentAuction.getCurrentPrice() + 1;
        if (newBid < minNextBid) {
            showError("Your bid must be at least " + formatMoney(minNextBid) + ".");
            return;
        }

        if (placeBidButton != null) {
            placeBidButton.setDisable(true);
        }

        CompletableFuture.supplyAsync(() -> auctionApiService.placeBid(selectedItem.getId(), new BidRequest(newBid)))
                .thenAccept(response -> runOnUiThread(() -> applyBidPlacementResponse(response)))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        showError(extractFriendlyMessage(error.getMessage()));
                        loadAuctionDetail(false);
                        if (placeBidButton != null) {
                            placeBidButton.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    private void applyBidPlacementResponse(BidPlacementResponse response) {
        if (currentAuction != null) {
            currentAuction.setCurrentPrice(response.getCurrentPrice());
            currentAuction.setMinNextBid(response.getMinNextBid());
            currentAuction.setLeaderName(response.getLeaderName());
            currentAuction.setEndTime(response.getEndTime());
            currentAuction.setState(response.getState());
        }

        currentBidLabel.setText(formatMoney(response.getCurrentPrice()));
        leaderLabel.setText("Leader: " + safeLeaderName(response.getLeaderName()));
        countdownLabel.setText(formatCountdown(response.getEndTime()));
        addBidHistory(response.getBidderDisplay() == null ? "You" : response.getBidderDisplay(), response.getCurrentPrice());
        bidInputField.clear();
        showSuccess("Bid placed successfully.");

        if ("FINISHED".equalsIgnoreCase(response.getState())) {
            lockBiddingControls();
        }

        if (placeBidButton != null) {
            placeBidButton.setDisable(false);
        }
    }

    @FXML
    private void handleEnableAutoBid() {
        clearAlert();

        if (selectedItem == null || selectedItem.getId() == null) {
            showError("No auction selected.");
            return;
        }

        if (SessionManager.getUserId() == null) {
            showError("Please login again before enabling auto-bid.");
            return;
        }

        String input = autoBidMaxInput.getText().trim();
        if (input.isEmpty()) {
            showError("Please enter your auto-bid max amount.");
            return;
        }

        double maxAmount;
        try {
            maxAmount = Double.parseDouble(input.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            showError("Auto-bid max amount must be a valid number.");
            return;
        }

        if (currentAuction != null && maxAmount <= currentAuction.getCurrentPrice()) {
            showError("Auto-bid max must be higher than current price.");
            return;
        }

        if (autoBidButton != null) {
            autoBidButton.setDisable(true);
        }

        CompletableFuture.supplyAsync(() -> auctionApiService.setAutoBid(selectedItem.getId(), new AutoBidRequest(maxAmount)))
                .thenAccept(response -> runOnUiThread(() -> {
                    if (autoBidStatusLabel != null) {
                        autoBidStatusLabel.setText("Auto-bid enabled up to " + formatMoney(response.getMaxAmount()));
                    }
                    showSuccess("Auto-bid enabled.");
                    if (autoBidButton != null) {
                        autoBidButton.setDisable(false);
                    }
                }))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        showError(extractFriendlyMessage(error.getMessage()));
                        if (autoBidButton != null) {
                            autoBidButton.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    private void startPollingRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> loadAuctionDetail(false)));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void startCountdownTimer() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (realtimeRemainingSeconds != null) {
                realtimeRemainingSeconds = Math.max(0, realtimeRemainingSeconds - 1);
                countdownLabel.setText(formatSeconds(realtimeRemainingSeconds));
                return;
            }

            if (currentAuction != null) {
                countdownLabel.setText(formatCountdown(currentAuction.getEndTime()));
            }
        }));
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

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "Unknown";
        }
        return id.length() > 8 ? id.substring(0, 8) + "..." : id;
    }

    private String formatHistoryTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return "";
        }
        try {
            return DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(rawTime));
        } catch (Exception e) {
            return rawTime;
        }
    }

    private void addBidHistory(String bidderName, double amount) {
        bidHistory.add(0, new BidRecord(bidderName, formatMoney(amount), getCurrentTimeText()));
        if (bidHistory.size() > 50) {
            bidHistory.remove(50, bidHistory.size());
        }
        appendBidChartPoint(amount);
    }

    private void applyRealtimeCountdown(AuctionEventDto event) {
        if (event.getRemainingSeconds() != null) {
            realtimeRemainingSeconds = Math.max(0, event.getRemainingSeconds());
            countdownLabel.setText(formatSeconds(realtimeRemainingSeconds));
            return;
        }

        if (event.getEndTime() != null && !event.getEndTime().isBlank()) {
            countdownLabel.setText(formatCountdown(event.getEndTime()));
        }
    }

    private void appendBidChartPoint(double amount) {
        int nextIndex = bidChartSeries.getData().size() + 1;
        bidChartSeries.getData().add(new XYChart.Data<>(nextIndex, amount));
        if (bidChartSeries.getData().size() > 50) {
            bidChartSeries.getData().remove(0);
        }
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

    private String safeLeaderName(String leaderName) {
        if (leaderName == null || leaderName.isBlank()) {
            return "No leader yet";
        }
        return leaderName;
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
        realtimeAuctionService.disconnect();
        SceneManager.goToProductDetail();
    }
}
