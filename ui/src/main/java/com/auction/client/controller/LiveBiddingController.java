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
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AutoBidResponse;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
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
    @FXML private VBox autoBidBanner;
    @FXML private Label autoBidBannerTitleLabel;
    @FXML private Label autoBidBannerSubtitleLabel;
    @FXML private VBox newBidToast;
    @FXML private Label toastTitleLabel;
    @FXML private Label toastMessageLabel;
    @FXML private Label toastPriceLabel;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final RealtimeAuctionService realtimeAuctionService = new RealtimeAuctionService();
    private final ObservableList<BidRecord> bidHistory = FXCollections.observableArrayList();
    private final XYChart.Series<Number, Number> bidChartSeries = new XYChart.Series<>();

    private AuctionItem selectedItem;
    private AuctionDetailResponse currentAuction;
    private Timeline refreshTimeline;
    private Timeline countdownTimeline;
    private Timeline toastTimeline;
    private boolean auctionCloseRefreshTriggered;
    private boolean isLoadingAuction = false;
    private Long realtimeRemainingSeconds;
    private AutoBidResponse currentUserAutoBid;
    private final Set<String> handledEventIds = new HashSet<>();

    @FXML
    public void initialize() {
        selectedItem = MockData.getSelectedItem();
        updateConnectionStatus("CONNECTING");
        outbidAlertLabel.setText("");
        hideAutoBidBanner();
        hideNewBidToast();
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
            refreshCurrentUserAutoBidState();

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
            case "AUCTION_CLOSED", "AUCTION_FINISHED" -> handleAuctionFinishedEvent(event);
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
        showNewBidToast(bidderName, event.getCurrentPrice());
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
        showNewBidToast(safeLeaderName(event.getLeaderName()), event.getCurrentPrice());
    }

    private void handleAuctionExtendedEvent(AuctionEventDto event) {
        applyRealtimeCountdown(event);
        showInfo(event.getMessage() == null ? "Auction time extended." : event.getMessage());
    }

    private void handleAuctionFinishedEvent(AuctionEventDto event) {
        realtimeRemainingSeconds = 0L;
        countdownLabel.setText("00:00:00");
        auctionCloseRefreshTriggered = false;
        loadAuctionDetail(false);

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

        if (!"FINISHED".equalsIgnoreCase(auction.getState()) && !"CANCELLED".equalsIgnoreCase(auction.getState())) {
            auctionCloseRefreshTriggered = false;
        }

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
        showNewBidToast(response.getBidderDisplay() == null ? "You" : response.getBidderDisplay(), response.getCurrentPrice());

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

        double bidStep = currentAuction != null && currentAuction.getMinNextBid() > currentAuction.getCurrentPrice()
                ? currentAuction.getMinNextBid() - currentAuction.getCurrentPrice()
                : 1.0;

        CompletableFuture.supplyAsync(() -> auctionApiService.setAutoBid(selectedItem.getId(), new AutoBidRequest(maxAmount, bidStep)))
                .thenAccept(response -> runOnUiThread(() -> {
                    if (autoBidStatusLabel != null) {
                        String stepText = response.getBidStep() > 0
                                ? " with step " + formatMoney(response.getBidStep())
                                : "";
                        autoBidStatusLabel.setText("Auto-bid enabled up to " + formatMoney(response.getMaxAmount()) + stepText);
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

    @FXML
    private void handleOpenAutoBidDialog() {
        if (selectedItem == null) {
            showError("No auction selected.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auto_bid_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Auto-Bid");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Window ownerWindow = resolveDialogOwnerWindow();
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }

            Scene scene = createOverlayDialogScene(root, ownerWindow);

            AutoBidDialogController controller = loader.getController();

            AuctionListResponse auctionForDialog = buildAuctionListSnapshot();
            boolean demoMode = auctionForDialog.getId() == null;

            controller.setDialogStage(stage);
            controller.setAuction(
                    auctionForDialog,
                    demoMode,
                    currentUserAutoBid,
                    this::applyAutoBidActivated,
                    this::applyAutoBidDisabled
            );

            stage.setScene(scene);
            positionOverlayDialogStage(stage, ownerWindow);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            showError("Cannot open auto-bid dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenBidHistoryDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bid_history_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Bid History");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Window ownerWindow = resolveDialogOwnerWindow();
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }

            Scene scene = createOverlayDialogScene(root, ownerWindow);

            BidHistoryDialogController controller = loader.getController();

            AuctionListResponse auctionForDialog = buildAuctionListSnapshot();
            boolean demoMode = auctionForDialog.getId() == null;

            controller.setDialogStage(stage);
            controller.setAuction(auctionForDialog, demoMode);

            stage.setScene(scene);
            positionOverlayDialogStage(stage, ownerWindow);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            showError("Cannot open bid history: " + e.getMessage());
        }
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
                if (realtimeRemainingSeconds <= 0) {
                    requestImmediateAuctionClosureSync();
                }
                return;
            }

            if (currentAuction != null) {
                String countdownText = formatCountdown(currentAuction.getEndTime());
                countdownLabel.setText(countdownText);
                if ("00:00:00".equals(countdownText)) {
                    requestImmediateAuctionClosureSync();
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void requestImmediateAuctionClosureSync() {
        if (auctionCloseRefreshTriggered || selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            return;
        }

        auctionCloseRefreshTriggered = true;
        loadAuctionDetail(false);
    }

    private String formatMoney(double value) {
        return "USD " + String.format("%,.0f", value);
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

    private void applyAutoBidActivated(AutoBidResponse response) {
        if (response == null) {
            return;
        }

        currentUserAutoBid = response;
        double maxAmount = response.getMaxAmount();

        if (autoBidStatusLabel != null) {
            String stepText = response.getBidStep() > 0
                    ? " with step " + formatMoney(response.getBidStep())
                    : "";
            autoBidStatusLabel.setText("Auto-bid enabled up to " + formatMoney(maxAmount) + stepText);
        }

        showAutoBidBanner(maxAmount);
        showSuccess("Auto-bid enabled.");
    }

    private void applyAutoBidDisabled() {
        clearCurrentUserAutoBidState();
        showSuccess("Auto-bid turned off.");
    }

    private AuctionListResponse buildAuctionListSnapshot() {
        AuctionListResponse snapshot = new AuctionListResponse();

        if (currentAuction != null) {
            snapshot.setId(currentAuction.getId());
            snapshot.setItemId(currentAuction.getItemId());
            snapshot.setTitle(currentAuction.getTitle());
            snapshot.setDescription(currentAuction.getDescription());
            snapshot.setImageUrl(currentAuction.getImageUrl());
            snapshot.setCategory(currentAuction.getCategory());
            snapshot.setSellerId(currentAuction.getSellerId());
            snapshot.setSellerName(currentAuction.getSellerName());
            snapshot.setLeaderId(currentAuction.getLeaderId());
            snapshot.setLeaderName(currentAuction.getLeaderName());
            snapshot.setBidCount(currentAuction.getBidCount());
            snapshot.setCurrentPrice(currentAuction.getCurrentPrice());
            snapshot.setMinNextBid(currentAuction.getMinNextBid());
            snapshot.setViewCount(currentAuction.getViewCount());
            snapshot.setFavoriteCount(currentAuction.getFavoriteCount());
            snapshot.setTrendingScore(currentAuction.getTrendingScore());
            snapshot.setState(currentAuction.getState());
            snapshot.setStartTime(currentAuction.getStartTime());
            snapshot.setEndTime(currentAuction.getEndTime());
            return snapshot;
        }

        snapshot.setTitle(selectedItem == null ? "Demo Auction" : selectedItem.getName());
        double currentPrice = parseMoneyText(currentBidLabel == null ? "0" : currentBidLabel.getText());
        snapshot.setCurrentPrice(currentPrice);
        snapshot.setMinNextBid(currentPrice + 50);
        return snapshot;
    }

    private void appendBidChartPoint(double amount) {
        int nextIndex = bidChartSeries.getData().size() + 1;
        bidChartSeries.getData().add(new XYChart.Data<>(nextIndex, amount));
        if (bidChartSeries.getData().size() > 50) {
            bidChartSeries.getData().remove(0);
        }
    }

    private double parseMoneyText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        String normalized = text.replaceAll("[^0-9.]", "");

        if (normalized.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            return 0;
        }
    }

    private void showAutoBidBanner(double maxAmount) {
        if (autoBidBanner == null) {
            return;
        }

        autoBidBanner.setVisible(true);
        autoBidBanner.setManaged(true);

        if (autoBidBannerTitleLabel != null) {
            autoBidBannerTitleLabel.setText("Auto-Bid Active");
        }

        if (autoBidBannerSubtitleLabel != null) {
            autoBidBannerSubtitleLabel.setText("Max: " + formatMoney(maxAmount));
        }
    }

    private void hideAutoBidBanner() {
        if (autoBidBanner != null) {
            autoBidBanner.setVisible(false);
            autoBidBanner.setManaged(false);
        }
    }

    private void refreshCurrentUserAutoBidState() {
        if (!SessionManager.isAuthenticated() || selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            clearCurrentUserAutoBidState();
            return;
        }

        CompletableFuture.supplyAsync(() -> auctionApiService.listMyAutoBids())
                .thenAccept(autoBids -> runOnUiThread(() -> applyCurrentUserAutoBidState(autoBids)))
                .exceptionally(error -> null);
    }

    private void applyCurrentUserAutoBidState(List<AutoBidResponse> autoBids) {
        if (selectedItem == null || selectedItem.getId() == null) {
            clearCurrentUserAutoBidState();
            return;
        }

        AutoBidResponse matching = null;
        if (autoBids != null) {
            for (AutoBidResponse autoBid : autoBids) {
                if (autoBid == null || !autoBid.isActive()) {
                    continue;
                }
                if (selectedItem.getId().equals(autoBid.getAuctionId())) {
                    matching = autoBid;
                    break;
                }
            }
        }

        currentUserAutoBid = matching;

        if (matching == null) {
            clearCurrentUserAutoBidState();
            return;
        }

        if (autoBidStatusLabel != null) {
            String stepText = matching.getBidStep() > 0
                    ? " with step " + formatMoney(matching.getBidStep())
                    : "";
            autoBidStatusLabel.setText("Auto-bid enabled up to " + formatMoney(matching.getMaxAmount()) + stepText);
        }

        showAutoBidBanner(matching.getMaxAmount());
    }

    private void clearCurrentUserAutoBidState() {
        currentUserAutoBid = null;

        if (autoBidStatusLabel != null) {
            autoBidStatusLabel.setText("Auto-bid inactive");
        }

        hideAutoBidBanner();
    }

    private void showNewBidToast(String bidderName, double amount) {
        if (newBidToast == null) {
            return;
        }

        if (toastTitleLabel != null) {
            toastTitleLabel.setText("New bid placed!");
        }

        if (toastMessageLabel != null) {
            toastMessageLabel.setText("by " + safeLeaderName(bidderName));
        }

        if (toastPriceLabel != null) {
            toastPriceLabel.setText(formatMoney(amount));
        }

        newBidToast.setVisible(true);
        newBidToast.setManaged(true);

        if (toastTimeline != null) {
            toastTimeline.stop();
        }

        toastTimeline = new Timeline(new KeyFrame(Duration.seconds(4), event -> hideNewBidToast()));
        toastTimeline.setCycleCount(1);
        toastTimeline.play();
    }

    private void hideNewBidToast() {
        if (newBidToast != null) {
            newBidToast.setVisible(false);
            newBidToast.setManaged(false);
        }
    }

    private void addDialogStyles(Scene scene) {
        addStylesheet(scene, "/css/app.css");
        addStylesheet(scene, "/css/components.css");
        addStylesheet(scene, "/css/live_bidding.css");
        addStylesheet(scene, "/css/product_detail.css");
    }

    private Window resolveDialogOwnerWindow() {
        return lotTitleLabel != null && lotTitleLabel.getScene() != null
                ? lotTitleLabel.getScene().getWindow()
                : null;
    }

    private Scene createOverlayDialogScene(Parent dialogCard, Window ownerWindow) {
        StackPane overlay = new StackPane(dialogCard);
        overlay.getStyleClass().add("modal-overlay");

        Scene scene = ownerWindow == null
                ? new Scene(overlay, Color.TRANSPARENT)
                : new Scene(overlay, ownerWindow.getWidth(), ownerWindow.getHeight(), Color.TRANSPARENT);

        addDialogStyles(scene);
        return scene;
    }

    private void positionOverlayDialogStage(Stage stage, Window ownerWindow) {
        if (ownerWindow == null) {
            return;
        }

        stage.setX(ownerWindow.getX());
        stage.setY(ownerWindow.getY());
    }

    private void addStylesheet(Scene scene, String path) {
        try {
            String css = getClass().getResource(path).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {
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
        shutdownLiveUpdates();
        SceneManager.goToProductDetail();
    }

    @FXML
    private void handleLogout() {
        shutdownLiveUpdates();
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    private void shutdownLiveUpdates() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (toastTimeline != null) {
            toastTimeline.stop();
        }

        realtimeAuctionService.disconnect();
    }
}
