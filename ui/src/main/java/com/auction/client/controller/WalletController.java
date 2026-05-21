package com.auction.client.controller;

import com.auction.client.dto.request.WalletAmountRequest;
import com.auction.client.dto.response.WalletBalanceResponse;
import com.auction.client.dto.response.WalletTransactionResponse;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.WalletApiService;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;

public class WalletController {
    @FXML private Label walletOwnerLabel;
    @FXML private Label walletBalanceLabel;
    @FXML private Label walletMessageLabel;
    @FXML private TextField amountField;
    @FXML private TableView<WalletTransactionResponse> historyTable;
    @FXML private TableColumn<WalletTransactionResponse, String> typeColumn;
    @FXML private TableColumn<WalletTransactionResponse, String> amountColumn;
    @FXML private TableColumn<WalletTransactionResponse, String> balanceColumn;
    @FXML private TableColumn<WalletTransactionResponse, String> createdColumn;

    private final WalletApiService walletApiService = new WalletApiService();
    private final ObservableList<WalletTransactionResponse> history = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!SessionManager.isAuthenticated()) {
            SceneManager.goToAuth();
            return;
        }

        walletOwnerLabel.setText(SessionManager.getUsername() == null ? "Wallet" : SessionManager.getUsername());
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        historyTable.setItems(history);
        hideMessage();
        refreshWallet();
    }

    @FXML
    private void handleDeposit() {
        mutateWallet(true);
    }

    @FXML
    private void handleWithdraw() {
        mutateWallet(false);
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

    private void mutateWallet(boolean deposit) {
        try {
            BigDecimal amount = parseAmount();
            WalletAmountRequest request = new WalletAmountRequest(amount);
            WalletBalanceResponse balance = deposit
                    ? walletApiService.deposit(request)
                    : walletApiService.withdraw(request);
            walletBalanceLabel.setText(formatMoney(balance.getBalance()));
            amountField.clear();
            showSuccess(deposit ? "Deposit completed." : "Withdrawal completed.");
            loadHistory();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private BigDecimal parseAmount() {
        String amountText = amountField.getText() == null ? "" : amountField.getText().trim();
        if (amountText.isEmpty()) {
            throw new IllegalArgumentException("Amount is required.");
        }

        BigDecimal amount = new BigDecimal(amountText);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        return amount;
    }

    private void refreshWallet() {
        try {
            WalletBalanceResponse balance = walletApiService.getBalance();
            walletBalanceLabel.setText(formatMoney(balance.getBalance()));
            loadHistory();
        } catch (Exception e) {
            walletBalanceLabel.setText("-");
            showError("Cannot load wallet: " + e.getMessage());
        }
    }

    private void loadHistory() {
        history.setAll(walletApiService.getHistory());
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "$0" : "$" + amount;
    }

    private void showError(String message) {
        walletMessageLabel.setText(message == null || message.isBlank() ? "Wallet operation failed." : message);
        walletMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        walletMessageLabel.setManaged(true);
        walletMessageLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        walletMessageLabel.setText(message);
        walletMessageLabel.setStyle("-fx-text-fill: #15803d;");
        walletMessageLabel.setManaged(true);
        walletMessageLabel.setVisible(true);
    }

    private void hideMessage() {
        walletMessageLabel.setText("");
        walletMessageLabel.setManaged(false);
        walletMessageLabel.setVisible(false);
    }
}
