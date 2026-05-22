package com.auction.client.controller;

import com.auction.client.dto.request.AutoBidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AutoBidResponse;
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
import javafx.stage.Window;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AutoBidDialogController {

    @FXML private Label lotTitleLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label minimumBidBoxLabel;
    @FXML private Label minimumBidLabel;
    @FXML private TextField maxBidInputField;
    @FXML private CheckBox confirmCheckBox;
    @FXML private Button activateButton;
    @FXML private Label errorLabel;

    @FXML private Label summaryCurrentBidLabel;
    @FXML private Label summaryMaxBidLabel;
    @FXML private Label summarySaveLabel;

    private final AuctionApiService auctionApiService = new AuctionApiService();

    private Stage dialogStage;
    private AuctionListResponse auction;
    private Consumer<AutoBidResponse> onAutoBidActivated;
    private boolean demoMode;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAuction(AuctionListResponse auction,
                           boolean demoMode,
                           Consumer<AutoBidResponse> onAutoBidActivated) {
        this.auction = auction;
        this.demoMode = demoMode;
        this.onAutoBidActivated = onAutoBidActivated;

        bindAuction();
    }

    @FXML
    public void initialize() {
        if (confirmCheckBox != null) {
            confirmCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateSubmitState());
        }

        if (maxBidInputField != null) {
            maxBidInputField.textProperty().addListener((obs, oldValue, newValue) -> {
                updateSubmitState();
                updateSummary();
            });
        }

        hideError();
    }

    private void bindAuction() {
        if (auction == null) {
            return;
        }

        String title = firstNonBlank(auction.getTitle(), auction.getItemName(), "Unnamed Auction");
        double currentPrice = auction.getCurrentPrice();
        double minimumBid = getMinimumBid();

        lotTitleLabel.setText(title);
        currentPriceLabel.setText(formatMoney(currentPrice));
        minimumBidBoxLabel.setText(formatMoney(minimumBid));
        minimumBidLabel.setText("Minimum: " + formatMoney(minimumBid));
        maxBidInputField.setPromptText(String.format("%.0f", minimumBid));

        summaryCurrentBidLabel.setText(formatMoney(currentPrice));
        summaryMaxBidLabel.setText(formatMoney(minimumBid));
        summarySaveLabel.setText(formatMoney(Math.max(0, minimumBid - currentPrice)));
    }

    private void updateSubmitState() {
        boolean hasInput = maxBidInputField != null && !maxBidInputField.getText().trim().isEmpty();
        boolean confirmed = confirmCheckBox != null && confirmCheckBox.isSelected();

        if (activateButton != null) {
            activateButton.setDisable(!hasInput || !confirmed);
        }
    }

    private void updateSummary() {
        double amount;

        try {
            amount = parseMoney(maxBidInputField.getText());
        } catch (Exception e) {
            amount = getMinimumBid();
        }

        double currentPrice = auction == null ? 0 : auction.getCurrentPrice();

        summaryCurrentBidLabel.setText(formatMoney(currentPrice));
        summaryMaxBidLabel.setText(formatMoney(amount));
        summarySaveLabel.setText(formatMoney(Math.max(0, amount - currentPrice)));
    }

    @FXML
    private void handleQuickAdd50() {
        quickAdd(50);
    }

    @FXML
    private void handleQuickAdd100() {
        quickAdd(100);
    }

    @FXML
    private void handleQuickAdd200() {
        quickAdd(200);
    }

    @FXML
    private void handleQuickAdd500() {
        quickAdd(500);
    }

    private void quickAdd(double amount) {
        double base;

        try {
            base = parseMoney(maxBidInputField.getText());
        } catch (Exception e) {
            base = getMinimumBid();
        }

        maxBidInputField.setText(String.format("%.0f", base + amount));
    }

    @FXML
    private void handleActivateAutoBid() {
        hideError();

        if (auction == null) {
            showError("Auction data is unavailable.");
            return;
        }

        double maxAmount;

        try {
            maxAmount = parseMoney(maxBidInputField.getText());
        } catch (NumberFormatException e) {
            showError("Maximum bid must be a valid number.");
            return;
        }

        double minimumBid = getMinimumBid();

        if (maxAmount < minimumBid) {
            showError("Your maximum bid must be at least " + formatMoney(minimumBid) + ".");
            return;
        }

        activateButton.setDisable(true);

        Window owner = dialogStage == null ? null : dialogStage.getOwner();

        if (demoMode || auction.getId() == null) {
            AutoBidResponse response = new AutoBidResponse();
            response.setActive(true);
            response.setMaxAmount(maxAmount);
            response.setAuctionId("DEMO");

            if (onAutoBidActivated != null) {
                onAutoBidActivated.accept(response);
            }

            closeCurrentDialog();
            openSuccessDialog(maxAmount, owner);
            return;
        }

        CompletableFuture
                .supplyAsync(() -> auctionApiService.setAutoBid(
                        auction.getId().toString(),
                        new AutoBidRequest(maxAmount)
                ))
                .thenAccept(response -> runOnUiThread(() -> {
                    if (onAutoBidActivated != null) {
                        onAutoBidActivated.accept(response);
                    }

                    closeCurrentDialog();
                    openSuccessDialog(response.getMaxAmount(), owner);
                }))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        showError(extractFriendlyMessage(error.getMessage()));
                        activateButton.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void handleClose() {
        closeCurrentDialog();
    }

    private void openSuccessDialog(double maxAmount, Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auto_bid_success_dialog.fxml"));
            Parent root = loader.load();

            Stage successStage = new Stage();
            successStage.setTitle("Auto-Bid activated");
            successStage.initModality(Modality.WINDOW_MODAL);

            if (owner != null) {
                successStage.initOwner(owner);
            }

            Scene scene = new Scene(root);
            addStyles(scene);

            AutoBidSuccessDialogController controller = loader.getController();
            controller.setDialogStage(successStage);
            controller.setMaxAmount(maxAmount);

            successStage.setScene(scene);
            successStage.setResizable(false);
            successStage.showAndWait();

        } catch (Exception e) {
            System.out.println("Cannot open auto-bid success dialog: " + e.getMessage());
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

        return auction.getCurrentPrice() + 50;
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
            return "Enable auto-bid failed.";
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