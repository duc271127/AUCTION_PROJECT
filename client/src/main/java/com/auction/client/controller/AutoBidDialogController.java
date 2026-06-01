package com.auction.client.controller;

import com.auction.client.dto.request.AutoBidRequest;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AutoBidResponse;
import com.auction.client.service.AuctionApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AutoBidDialogController {

    @FXML private Label lotTitleLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label minimumBidBoxLabel;
    @FXML private Label minimumBidLabel;
    @FXML private TextField maxBidInputField;
    @FXML private TextField bidStepInputField;
    @FXML private Label stepHintLabel;
    @FXML private Label autoBidModeLabel;
    @FXML private CheckBox confirmCheckBox;
    @FXML private Button deactivateButton;
    @FXML private Button activateButton;
    @FXML private Label errorLabel;

    @FXML private Label summaryCurrentBidLabel;
    @FXML private Label summaryMaxBidLabel;
    @FXML private Label summaryStepLabel;
    @FXML private Label summarySaveLabel;
    @FXML private Button quickAddFirstButton;
    @FXML private Button quickAddSecondButton;
    @FXML private Button quickAddThirdButton;
    @FXML private Button quickAddFourthButton;

    private final AuctionApiService auctionApiService = new AuctionApiService();

    private Stage dialogStage;
    private AuctionListResponse auction;
    private AutoBidResponse currentAutoBid;
    private Consumer<AutoBidResponse> onAutoBidActivated;
    private Runnable onAutoBidDisabled;
    private boolean demoMode;
    private double minIncrement = 1.0;
    private double recommendedStep = 10.0;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setAuction(AuctionListResponse auction,
                           boolean demoMode,
                           Consumer<AutoBidResponse> onAutoBidActivated) {
        setAuction(auction, demoMode, null, onAutoBidActivated, null);
    }

    public void setAuction(AuctionListResponse auction,
                           boolean demoMode,
                           AutoBidResponse currentAutoBid,
                           Consumer<AutoBidResponse> onAutoBidActivated,
                           Runnable onAutoBidDisabled) {
        this.auction = auction;
        this.demoMode = demoMode;
        this.currentAutoBid = currentAutoBid;
        this.onAutoBidActivated = onAutoBidActivated;
        this.onAutoBidDisabled = onAutoBidDisabled;

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

        if (bidStepInputField != null) {
            bidStepInputField.textProperty().addListener((obs, oldValue, newValue) -> {
                updateQuickAddButtons();
                updateSummary();
            });
        }

        hideError();
        updateQuickAddButtons();
    }

    private void bindAuction() {
        if (auction == null) {
            return;
        }

        String title = firstNonBlank(auction.getTitle(), auction.getItemName(), "Unnamed Auction");
        double currentPrice = auction.getCurrentPrice();
        double minimumBid = getMinimumBid();

        recommendedStep = Math.max(resolveDefaultBidStep(), 1.0);

        lotTitleLabel.setText(title);
        currentPriceLabel.setText(formatMoney(currentPrice));
        minimumBidBoxLabel.setText(formatMoney(minimumBid));
        minimumBidLabel.setText("Minimum: " + formatMoney(minimumBid));
        maxBidInputField.setPromptText(String.format("%.0f", minimumBid));
        bidStepInputField.setText(String.format("%.0f", recommendedStep));
        stepHintLabel.setText("Minimum increment: " + formatMoney(minIncrement));

        summaryCurrentBidLabel.setText(formatMoney(currentPrice));
        summaryMaxBidLabel.setText(formatMoney(minimumBid));
        summaryStepLabel.setText(formatMoney(recommendedStep));
        summarySaveLabel.setText(formatMoney(Math.max(0, minimumBid - currentPrice)));
        applyExistingAutoBidState(minimumBid);
        updateQuickAddButtons();

        CompletableFuture.supplyAsync(() -> {
            try {
                return auctionApiService.getMinIncrement();
            } catch (Exception ignored) {
                return null;
            }
        }).thenAccept(serverStep -> runOnUiThread(() -> applyServerStep(serverStep)));
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
        double step = getBidStep();

        summaryCurrentBidLabel.setText(formatMoney(currentPrice));
        summaryMaxBidLabel.setText(formatMoney(amount));
        summaryStepLabel.setText(formatMoney(step));
        summarySaveLabel.setText(formatMoney(Math.max(0, amount - currentPrice)));
    }

    @FXML
    private void handleQuickAdd50() {
        quickAdd(getBidStep());
    }

    @FXML
    private void handleQuickAdd100() {
        quickAdd(getBidStep() * 2);
    }

    @FXML
    private void handleQuickAdd200() {
        quickAdd(getBidStep() * 5);
    }

    @FXML
    private void handleQuickAdd500() {
        quickAdd(getBidStep() * 10);
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
        double bidStep = getBidStep();

        if (maxAmount < minimumBid) {
            showError("Your maximum bid must be at least " + formatMoney(minimumBid) + ".");
            return;
        }

        if (bidStep < minIncrement) {
            showError("Bid step must be at least " + formatMoney(minIncrement) + ".");
            return;
        }

        activateButton.setDisable(true);
        if (deactivateButton != null) {
            deactivateButton.setDisable(true);
        }

        if (demoMode || auction.getId() == null) {
            AutoBidResponse response = new AutoBidResponse();
            response.setActive(true);
            response.setMaxAmount(maxAmount);
            response.setBidStep(bidStep);
            response.setAuctionId("DEMO");

            if (onAutoBidActivated != null) {
                onAutoBidActivated.accept(response);
            }

            closeCurrentDialog();
            return;
        }

        CompletableFuture
                .supplyAsync(() -> auctionApiService.setAutoBid(
                        auction.getId().toString(),
                        new AutoBidRequest(maxAmount, bidStep)
                ))
                .thenAccept(response -> runOnUiThread(() -> {
                    if (onAutoBidActivated != null) {
                        onAutoBidActivated.accept(response);
                    }

                    closeCurrentDialog();
                }))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        showError(extractFriendlyMessage(error.getMessage()));
                        activateButton.setDisable(false);
                        if (deactivateButton != null) {
                            deactivateButton.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void handleDisableAutoBid() {
        hideError();

        if (!hasActiveAutoBid()) {
            showError("Auto-bid is not active.");
            return;
        }

        if (auction == null || auction.getId() == null) {
            if (demoMode) {
                if (onAutoBidDisabled != null) {
                    onAutoBidDisabled.run();
                }
                closeCurrentDialog();
                return;
            }

            showError("Auction data is unavailable.");
            return;
        }

        if (activateButton != null) {
            activateButton.setDisable(true);
        }
        if (deactivateButton != null) {
            deactivateButton.setDisable(true);
        }

        CompletableFuture.runAsync(() -> auctionApiService.cancelAutoBid(auction.getId().toString()))
                .thenRun(() -> runOnUiThread(() -> {
                    if (onAutoBidDisabled != null) {
                        onAutoBidDisabled.run();
                    }
                    closeCurrentDialog();
                }))
                .exceptionally(error -> {
                    runOnUiThread(() -> {
                        showError(extractFriendlyMessage(error.getMessage()));
                        updateSubmitState();
                        if (deactivateButton != null) {
                            deactivateButton.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void handleClose() {
        closeCurrentDialog();
    }

    private void closeCurrentDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private double getMinimumBid() {
        if (auction == null) {
            return 0;
        }

        if (auction.getMinNextBid() > 0) {
            return auction.getMinNextBid();
        }

        return auction.getCurrentPrice() + Math.max(minIncrement, 1.0);
    }

    private double getBidStep() {
        if (bidStepInputField == null) {
            return Math.max(minIncrement, 1.0);
        }

        try {
            double parsed = parseMoney(bidStepInputField.getText());
            return Math.max(parsed, minIncrement);
        } catch (Exception e) {
            return Math.max(minIncrement, 1.0);
        }
    }

    private void applyServerStep(Double serverStep) {
        if (serverStep == null || serverStep <= 0) {
            return;
        }

        minIncrement = Math.max(serverStep, 1.0);

        if (bidStepInputField != null) {
            double currentStep = getBidStep();
            if (bidStepInputField.getText() == null || bidStepInputField.getText().isBlank()) {
                bidStepInputField.setText(String.format("%.0f", Math.max(recommendedStep, minIncrement)));
            } else if (currentStep < minIncrement) {
                bidStepInputField.setText(String.format("%.0f", minIncrement));
            }
        }

        if (stepHintLabel != null) {
            stepHintLabel.setText("Minimum increment: " + formatMoney(minIncrement));
        }

        updateQuickAddButtons();
        updateSummary();
    }

    private void applyExistingAutoBidState(double minimumBid) {
        boolean active = hasActiveAutoBid();

        if (autoBidModeLabel != null) {
            autoBidModeLabel.setVisible(active);
            autoBidModeLabel.setManaged(active);
            autoBidModeLabel.setText(active
                    ? "Auto-bid is active. Saving here will replace the current auto-bid."
                    : "");
        }

        if (deactivateButton != null) {
            deactivateButton.setVisible(active);
            deactivateButton.setManaged(active);
            deactivateButton.setDisable(false);
        }

        if (activateButton != null) {
            activateButton.setText(active ? "Place Auto-Bid" : "Activate Auto-Bid");
        }

        if (confirmCheckBox != null) {
            confirmCheckBox.setText(active
                    ? "I want to replace the current auto-bid."
                    : "Confirm auto-bid");
            confirmCheckBox.setSelected(false);
        }

        if (!active) {
            updateSubmitState();
            return;
        }

        if (maxBidInputField != null) {
            double value = currentAutoBid.getMaxAmount() > 0 ? currentAutoBid.getMaxAmount() : minimumBid;
            maxBidInputField.setText(formatWholeNumber(value));
        }

        if (bidStepInputField != null && currentAutoBid.getBidStep() > 0) {
            bidStepInputField.setText(formatWholeNumber(currentAutoBid.getBidStep()));
        }

        updateSummary();
        updateSubmitState();
    }

    private double resolveDefaultBidStep() {
        if (auction == null) {
            return recommendedStep;
        }

        double referencePrice = Math.max(auction.getCurrentPrice(), auction.getMinNextBid());

        if (referencePrice >= 100_000) {
            return 2_000;
        }
        if (referencePrice >= 50_000) {
            return 1_000;
        }
        if (referencePrice >= 20_000) {
            return 500;
        }
        if (referencePrice >= 10_000) {
            return 200;
        }
        if (referencePrice >= 5_000) {
            return 100;
        }
        if (referencePrice >= 1_000) {
            return 50;
        }
        if (referencePrice >= 100) {
            return 10;
        }
        if (referencePrice >= 20) {
            return 5;
        }

        return Math.max(minIncrement, 1.0);
    }

    private void updateQuickAddButtons() {
        double step = getBidStep();

        if (quickAddFirstButton != null) {
            quickAddFirstButton.setText("+ " + formatMoney(step));
        }
        if (quickAddSecondButton != null) {
            quickAddSecondButton.setText("+ " + formatMoney(step * 2));
        }
        if (quickAddThirdButton != null) {
            quickAddThirdButton.setText("+ " + formatMoney(step * 5));
        }
        if (quickAddFourthButton != null) {
            quickAddFourthButton.setText("+ " + formatMoney(step * 10));
        }
    }

    private boolean hasActiveAutoBid() {
        return currentAutoBid != null && currentAutoBid.isActive();
    }

    private String formatWholeNumber(double value) {
        return String.format("%.0f", value);
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
        return "USD " + String.format("%,.0f", value);
    }

    private void runOnUiThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}
