package com.auction.client.controller;

import com.auction.client.dto.request.WalletAmountRequest;
import com.auction.client.dto.response.WalletBalanceResponse;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.WalletApiService;
import com.auction.client.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WalletController {

    @FXML private Label walletBalanceLabel;
    @FXML private Label walletMessageLabel;
    @FXML private Label amountTitleLabel;
    @FXML private TextField amountField;

    @FXML private Button depositModeButton;
    @FXML private Button withdrawModeButton;
    @FXML private Button primaryActionButton;

    @FXML private VBox formContentBox;
    @FXML private VBox successBox;

    @FXML private Label successTitleLabel;
    @FXML private Label successDescriptionLabel;

    private final WalletApiService walletApiService = new WalletApiService();

    private boolean depositMode = true;
    private BigDecimal currentBalance = BigDecimal.ZERO;
    private BigDecimal currentAvailableToWithdraw = BigDecimal.ZERO;
    private boolean walletBalanceLoaded;

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneManager.goToAuth();
            return;
        }

        hideMessage();
        selectMode(true);
        refreshWallet();
    }

    @FXML
    private void handleSelectDeposit() {
        selectMode(true);
    }

    @FXML
    private void handleSelectWithdraw() {
        selectMode(false);
    }

    @FXML
    private void handleSubmit() {
        hideMessage();

        BigDecimal amount;

        try {
            amount = parseAmount();
        } catch (Exception e) {
            showError("Amount must be greater than 0.");
            return;
        }

        if (!depositMode && amount.compareTo(currentAvailableToWithdraw) > 0) {
            showError("Withdraw amount cannot exceed your available balance.");
            return;
        }

        try {
            WalletAmountRequest request = new WalletAmountRequest(amount);

            WalletBalanceResponse balance = depositMode
                    ? walletApiService.deposit(request)
                    : walletApiService.withdraw(request);

            updateCurrentBalance(balance);

            showSuccessState(amount);

        } catch (Exception e) {
            showError(extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleResetForm() {
        amountField.clear();
        hideMessage();

        successBox.setVisible(false);
        successBox.setManaged(false);

        formContentBox.setVisible(true);
        formContentBox.setManaged(true);
    }

    @FXML
    private void handleBack() {
        if (SessionManager.hasRole("SELLER")) {
            SceneManager.goToSellerDashboard();
        } else if (SessionManager.hasRole("ADMIN")) {
            SceneManager.goToAdminDashboard();
        } else {
            SceneManager.goToShowroom();
        }
    }

    private void selectMode(boolean deposit) {
        this.depositMode = deposit;

        setModeButtonStyle(depositModeButton, deposit);
        setModeButtonStyle(withdrawModeButton, !deposit);

        amountTitleLabel.setText(deposit ? "Deposit Amount" : "Withdraw Amount");
        primaryActionButton.setText(deposit ? "Deposit Now" : "Withdraw Now");

        amountField.clear();
        hideMessage();
    }

    private void setModeButtonStyle(Button button, boolean active) {
        button.getStyleClass().remove("segment-button");
        button.getStyleClass().remove("segment-button-active");
        button.getStyleClass().add(active ? "segment-button-active" : "segment-button");
    }

    private void refreshWallet() {
        boolean hadLoadedBalance = walletBalanceLoaded;

        try {
            WalletBalanceResponse balance = walletApiService.getBalance();
            updateCurrentBalance(balance);
        } catch (Exception e) {
            if (!hadLoadedBalance) {
                walletBalanceLoaded = false;
                walletBalanceLabel.setText(formatUnavailableMoney());
            }
            showError(extractFriendlyMessage(e.getMessage()));
        }
    }

    private BigDecimal parseAmount() {
        String raw = amountField.getText() == null ? "" : amountField.getText().trim();

        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Amount is required.");
        }

        String normalized = raw.replaceAll("[^0-9.]", "");

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Amount is required.");
        }

        BigDecimal amount = new BigDecimal(normalized);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        return amount;
    }

    private void showSuccessState(BigDecimal amount) {
        if (depositMode) {
            successTitleLabel.setText("Deposit Successful!");
            successDescriptionLabel.setText(formatMoney(amount) + " has been added to your wallet.");
        } else {
            successTitleLabel.setText("Withdrawal Initiated!");
            successDescriptionLabel.setText(formatMoney(amount) + " will be transferred to your account.");
        }

        formContentBox.setVisible(false);
        formContentBox.setManaged(false);

        successBox.setVisible(true);
        successBox.setManaged(true);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return formatUnavailableMoney();
        }

        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return "USD " + String.format("%,.0f", rounded.doubleValue());
    }

    private String formatUnavailableMoney() {
        return "USD --";
    }

    private void updateCurrentBalance(WalletBalanceResponse balanceResponse) {
        currentBalance = balanceResponse == null || balanceResponse.getBalance() == null
                ? BigDecimal.ZERO
                : balanceResponse.getBalance();
        currentAvailableToWithdraw = balanceResponse == null || balanceResponse.getAvailableToWithdraw() == null
                ? currentBalance
                : balanceResponse.getAvailableToWithdraw();
        walletBalanceLoaded = true;
        walletBalanceLabel.setText(formatMoney(currentBalance));
    }

    private void showError(String message) {
        walletMessageLabel.setText(message == null || message.isBlank()
                ? "Wallet operation failed."
                : message);
        walletMessageLabel.setVisible(true);
        walletMessageLabel.setManaged(true);
    }

    private void hideMessage() {
        walletMessageLabel.setText("");
        walletMessageLabel.setVisible(false);
        walletMessageLabel.setManaged(false);
    }

    private String extractFriendlyMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Wallet operation failed.";
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
}
