package com.auction.client.controller;

import com.auction.client.model.BidRecord;
import com.auction.client.navigation.SceneManager;
import com.auction.client.util.MockData;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class LiveBiddingController {

    @FXML private Label lotTitleLabel;
    @FXML private Label connectionStatusLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label countdownLabel;
    @FXML private Label outbidAlertLabel;

    @FXML private TextField bidInputField;
    @FXML private ListView<BidRecord> bidHistoryListView;

    private int currentHighestBid = 14000;
    private ObservableList<BidRecord> bidHistory;

    @FXML
    public void initialize() {
        lotTitleLabel.setText("Lot #A102 - Vintage Rolex Submariner");
        connectionStatusLabel.setText("CONNECTED");
        currentBidLabel.setText("$" + currentHighestBid);
        countdownLabel.setText("02:14:35");
        outbidAlertLabel.setText("");

        bidHistory = MockData.getMockBidHistory();
        bidHistoryListView.setItems(bidHistory);
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
        int baseValue = currentHighestBid;

        String input = bidInputField.getText().trim();
        if (!input.isEmpty()) {
            try {
                baseValue = Integer.parseInt(input);
            } catch (NumberFormatException ignored) {
                baseValue = currentHighestBid;
            }
        }

        bidInputField.setText(String.valueOf(baseValue + increment));
        outbidAlertLabel.setText("");
    }

    @FXML
    private void handlePlaceBid() {
        String input = bidInputField.getText().trim();

        if (input.isEmpty()) {
            outbidAlertLabel.setText("Please enter a bid amount.");
            return;
        }

        int newBid;
        try {
            newBid = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            outbidAlertLabel.setText("Bid amount must be a valid number.");
            return;
        }

        if (newBid <= currentHighestBid) {
            outbidAlertLabel.setText("Your bid must be higher than current highest bid.");
            return;
        }

        currentHighestBid = newBid;
        currentBidLabel.setText("$" + currentHighestBid);

        bidHistory.add(0, new BidRecord("You", "$" + currentHighestBid, "Now"));

        bidInputField.clear();
        outbidAlertLabel.setText("Bid placed successfully (demo only).");
    }

    @FXML
    private void handleBack() {
        SceneManager.goToProductDetail();
    }
}