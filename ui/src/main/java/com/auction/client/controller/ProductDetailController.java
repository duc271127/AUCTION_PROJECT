package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ProductDetailController {

    @FXML private ImageView mainImageView;
    @FXML private ImageView thumb1ImageView;
    @FXML private ImageView thumb2ImageView;

    @FXML private Label productNameLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label countdownLabel;
    @FXML private Label statusLabel;
    @FXML private Label specsLabel;
    @FXML private Label bidMessageLabel;

    @FXML private TextField bidAmountField;

    private AuctionItem currentItem;

    @FXML
    public void initialize() {
        currentItem = MockData.getSelectedItem();

        if (currentItem != null) {
            productNameLabel.setText(currentItem.getName());
            currentBidLabel.setText("Current Highest Bid: " + currentItem.getCurrentBid());
            countdownLabel.setText("Countdown: " + currentItem.getTimeLeft());
            statusLabel.setText("Status: " + currentItem.getStatus());
            specsLabel.setText(
                    "Provenance / Specs:\n" +
                            "- Premium curated auction item\n" +
                            "- Demo detail for phase 1\n" +
                            "- Seller verified (mock)\n" +
                            "- Real-time room available in next screen"
            );

            try {
                Image image = new Image(getClass().getResourceAsStream(currentItem.getImagePath()));
                mainImageView.setImage(image);
                thumb1ImageView.setImage(image);
                thumb2ImageView.setImage(image);
            } catch (Exception e) {
                System.out.println("Image not found: " + currentItem.getImagePath());
            }
        }

        hideBidMessage();
    }

    @FXML
    private void handleBack() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleJoinLiveBidding() {
        SceneManager.goToLiveBidding();
    }

    @FXML
    private void handlePlaceBid() {
        hideBidMessage();

        String bidText = bidAmountField.getText().trim();

        if (bidText.isEmpty()) {
            showBidMessage("Please enter a bid amount.");
            return;
        }

        String numericText = bidText.replaceAll("[^0-9]", "");

        if (numericText.isEmpty()) {
            showBidMessage("Bid amount must be numeric.");
            return;
        }

        int enteredBid = Integer.parseInt(numericText);

        String currentBidText = currentItem.getCurrentBid().replaceAll("[^0-9]", "");
        int currentBid = Integer.parseInt(currentBidText);

        if (enteredBid <= currentBid) {
            showBidMessage("Your bid must be higher than the current highest bid.");
            return;
        }

        showBidSuccess("Quick bid submitted successfully (demo only).");
    }

    private void showBidMessage(String message) {
        bidMessageLabel.setText(message);
        bidMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        bidMessageLabel.setManaged(true);
        bidMessageLabel.setVisible(true);
    }

    private void showBidSuccess(String message) {
        bidMessageLabel.setText(message);
        bidMessageLabel.setStyle("-fx-text-fill: #16a34a;");
        bidMessageLabel.setManaged(true);
        bidMessageLabel.setVisible(true);
    }

    private void hideBidMessage() {
        bidMessageLabel.setText("");
        bidMessageLabel.setManaged(false);
        bidMessageLabel.setVisible(false);
    }
}