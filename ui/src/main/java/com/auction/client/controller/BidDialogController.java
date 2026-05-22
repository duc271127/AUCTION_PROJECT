package com.auction.client.controller;

import com.auction.client.dto.request.BidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.BidPlacementResponse;
import com.auction.client.service.AuctionApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BidDialogController {

    @FXML private Label lotTitleLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label minimumBidLabel;
    @FXML private TextField bidInputField;
    @FXML private CheckBox confirmCheckBox;
    @FXML private Button submitBidButton;
    @FXML private Label errorLabel;

    private final AuctionApiService auctionApiService = new AuctionApiService();

    private Stage dialogStage;
    private AuctionListResponse auction;
    private Consumer<BidPlacementResponse> onBidPlaced;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAuction(AuctionListResponse auction, Consumer<BidPlacementResponse> onBidPlaced) {
        this.auction = auction;
        this.onBidPlaced = onBidPlaced;

        bindAuction();
    }

    @FXML
    public void initialize() {
        if (confirmCheckBox != null) {
            confirmCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateSubmitState());
        }

        if (bidInputField != null) {
            bidInputField.textProperty().addListener((obs, oldValue, newValue) -> updateSubmitState());
        }

        hideError();
    }

    private void bindAuction() {
        if (auction == null) {
            return;
        }

        String title = firstNonBlank(auction.getTitle(), auction.getItemName(), "Unnamed Auction");

        lotTitleLabel.setText(title);
        currentBidLabel.setText(formatMoney(auction.getCurrentPrice()));
        minimumBidLabel.setText("Minimum bid: " + formatMoney(getMinimumBid()));

        bidInputField.setPromptText(String.format("%.0f", getMinimumBid()));
    }

    private void updateSubmitState() {
        boolean hasInput = bidInputField != null && !bidInputField.getText().trim().isEmpty();
        boolean confirmed = confirmCheckBox != null && confirmCheckBox.isSelected();

        if (submitBidButton != null) {
            submitBidButton.setDisable(!hasInput || !confirmed);
        }
    }

    @FXML
    private void handleSubmitBid() {
        hideError();

        if (auction == null || auction.getId() == null) {
            showError("Auction data is unavailable.");
            return;
        }

        double amount;
        try {
            amount = parseMoney(bidInputField.getText());
        } catch (NumberFormatException e) {
            showError("Bid amount must be a valid number.");
            return;
        }

        double minimumBid = getMinimumBid();

        if (amount < minimumBid) {
            showError("Your bid must be at least " + formatMoney(minimumBid) + ".");
            return;
        }

        submitBidButton.setDisable(true);

        CompletableFuture
                .supplyAsync(() -> auctionApiService.placeBid(
                        auction.getId().toString(),
                        new BidRequest(amount)
                ))
                .thenAccept(response -> runOnUiThread(() -> {
                    if (onBidPlaced != null) {
                        onBidPlaced.accept(response);
                    }

                    closeCurrentDialog();
                    openSuccessDialog(response.getCurrentPrice());
                }))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        showError(extractFriendlyMessage(error.getMessage()));
                        submitBidButton.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void handleClose() {
        closeCurrentDialog();
    }

    private void openSuccessDialog(double bidAmount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bid_success_dialog.fxml"));
            Parent root = loader.load();

            Stage successStage = new Stage();
            successStage.setTitle("Bid placed");
            successStage.initModality(Modality.WINDOW_MODAL);

            if (dialogStage != null && dialogStage.getOwner() != null) {
                successStage.initOwner(dialogStage.getOwner());
            }

            Scene scene = new Scene(root);
            addStyles(scene);

            BidSuccessDialogController controller = loader.getController();
            controller.setDialogStage(successStage);
            controller.setBidAmount(bidAmount);

            successStage.setScene(scene);
            successStage.setResizable(false);
            successStage.showAndWait();

        } catch (Exception e) {
            System.out.println("Cannot open bid success dialog: " + e.getMessage());
        }
    }

    private void closeCurrentDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void addStyles(Scene scene) {
        addStylesheet(scene, "/css/app.css");
        addStylesheet(scene, "/css/components.css");
        addStylesheet(scene, "/css/product_detail.css");
    }

    private void addStylesheet(Scene scene, String path) {
        try {
            String css = getClass().getResource(path).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {
        }
    }

    private double getMinimumBid() {
        if (auction == null) {
            return 0;
        }

        if (auction.getMinNextBid() > 0) {
            return auction.getMinNextBid();
        }

        return auction.getCurrentPrice() + 1;
    }

    private double parseMoney(String text) {
        if (text == null || text.isBlank()) {
            throw new NumberFormatException("empty amount");
        }

        String normalized = text.replaceAll("[^0-9.]", "");

        if (normalized.isBlank()) {
            throw new NumberFormatException("empty amount");
        }

        return Double.parseDouble(normalized);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        if (errorLabel == null) {
            return;
        }

        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private String formatMoney(double value) {
        return "€ " + String.format("%,.0f", value);
    }

    private void runOnUiThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}