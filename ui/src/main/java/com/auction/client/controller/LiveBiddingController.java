package com.auction.client.controller;

import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.model.BidRecord;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.util.MockData;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LiveBiddingController {

    @FXML private Label lotTitleLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label leaderLabel;
    @FXML private Label countdownLabel;
    @FXML private Label outbidAlertLabel;

    @FXML private TextField bidInputField;
    @FXML private ListView<BidRecord> bidHistoryListView;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ObservableList<BidRecord> bidHistory = FXCollections.observableArrayList();

    private AuctionItem selectedItem;
    private AuctionListResponse currentAuction;
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        selectedItem = MockData.getSelectedItem();

        connectionStatusLabel.setText("CONNECTED");
        outbidAlertLabel.setText("");
        bidHistoryListView.setItems(bidHistory);

        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            showError("No selected auction.");
            showEmptyState();
            return;
        }

        loadAuctionDetail(true);
        startFakeRefresh();
    }

    private void loadAuctionDetail(boolean firstLoad) {
        try {
            AuctionListResponse previous = currentAuction;
            AuctionListResponse latest = auctionApiService.getAuctionById(selectedItem.getId());

            currentAuction = latest;
            bindAuctionToScreen(latest);

            if (!firstLoad && previous != null) {
                detectOutbid(previous, latest);
            }
        } catch (Exception e) {
            connectionStatusLabel.setText("DISCONNECTED");
            if (currentAuction == null) {
                showError("Cannot load live auction.");
                showEmptyState();
            }
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

            bidHistory.add(0, new BidRecord(
                    "You",
                    formatMoney(newBid),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())
            ));

            bidInputField.clear();
            showSuccess("Bid placed successfully.");

        } catch (Exception e) {
            showError(extractFriendlyMessage(e.getMessage()));
            loadAuctionDetail(false);
        }
    }

    private void startFakeRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> loadAuctionDetail(false))
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private String formatMoney(double value) {
        return "$" + String.format("%,.0f", value);
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

    private void clearAlert() {
        outbidAlertLabel.setText("");
    }

    @FXML
    private void handleBack() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        SceneManager.goToProductDetail();
    }
}