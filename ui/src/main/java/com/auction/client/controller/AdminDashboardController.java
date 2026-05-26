package com.auction.client.controller;

import com.auction.client.dto.request.CreateAuctionRequest;
import com.auction.client.dto.response.AdminStatsResponse;
import com.auction.client.dto.response.AdminWalletActivityResponse;
import com.auction.client.dto.response.AdminNotificationResponse;
import com.auction.client.model.AdminApprovalItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AdminApiService;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

public class AdminDashboardController {

    @FXML private Label totalAuctionsLabel;
    @FXML private Label activeSellersLabel;
    @FXML private Label revenueLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label closedAuctionsLabel;
    @FXML private Label newSellersLabel;
    @FXML private Label successRateLabel;
    @FXML private Label adminMessageLabel;
    @FXML private Button overviewTabButton;
    @FXML private Button auctionManagementTabButton;
    @FXML private VBox overviewPane;
    @FXML private VBox auctionManagementPane;

    @FXML private TableView<AdminApprovalItem> approvalTable;
    @FXML private TableColumn<AdminApprovalItem, String> productNameColumn;
    @FXML private TableColumn<AdminApprovalItem, String> sellerColumn;
    @FXML private TableColumn<AdminApprovalItem, String> categoryColumn;
    @FXML private TableColumn<AdminApprovalItem, String> priceColumn;
    @FXML private TableColumn<AdminApprovalItem, String> submittedDateColumn;
    @FXML private TableColumn<AdminApprovalItem, String> statusColumn;

    @FXML private TableView<AdminWalletActivityResponse> walletActivityTable;
    @FXML private TableColumn<AdminWalletActivityResponse, String> walletUserColumn;
    @FXML private TableColumn<AdminWalletActivityResponse, String> walletUserIdColumn;
    @FXML private TableColumn<AdminWalletActivityResponse, String> walletTypeColumn;
    @FXML private TableColumn<AdminWalletActivityResponse, String> walletAmountColumn;
    @FXML private TableColumn<AdminWalletActivityResponse, String> walletBalanceColumn;
    @FXML private TableColumn<AdminWalletActivityResponse, String> walletCreatedColumn;
    @FXML private TableView<AdminNotificationResponse> notificationTable;
    @FXML private TableColumn<AdminNotificationResponse, String> notificationTypeColumn;
    @FXML private TableColumn<AdminNotificationResponse, String> notificationMessageColumn;
    @FXML private TableColumn<AdminNotificationResponse, String> notificationCreatedColumn;

    private final AdminApiService adminApiService = new AdminApiService();
    private final ObservableList<AdminApprovalItem> approvalItems = FXCollections.observableArrayList();
    private final ObservableList<AdminWalletActivityResponse> walletActivity = FXCollections.observableArrayList();
    private final ObservableList<AdminNotificationResponse> notifications = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private final DateTimeFormatter walletDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        setupTables();
        approvalTable.setItems(approvalItems);
        walletActivityTable.setItems(walletActivity);
        notificationTable.setItems(notifications);
        adminMessageLabel.setWrapText(true);
        adminMessageLabel.setMaxWidth(Double.MAX_VALUE);
        hideMessage();
        showOverviewTab();
        loadPendingItems();
        loadAdminStats();
        loadWalletActivity();
        loadNotifications();
    }

    private void setupTables() {
        approvalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        walletActivityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        notificationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        sellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("startingPriceText"));
        submittedDateColumn.setCellValueFactory(new PropertyValueFactory<>("submittedDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        productNameColumn.setMinWidth(220);
        sellerColumn.setMinWidth(220);
        categoryColumn.setMinWidth(150);
        priceColumn.setMinWidth(160);
        submittedDateColumn.setMinWidth(190);
        statusColumn.setMinWidth(130);

        walletUserColumn.setCellValueFactory(cell -> new SimpleStringProperty(firstNonBlank(
                cell.getValue() == null ? null : cell.getValue().getUserDisplayName(),
                "Unknown user"
        )));
        walletUserIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue() == null || cell.getValue().getUserId() == null
                        ? "-"
                        : cell.getValue().getUserId().toString()
        ));
        walletTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue() == null ? "-" : formatWalletType(cell.getValue().getType())
        ));
        walletAmountColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue() == null ? "-" : formatCurrency(cell.getValue().getAmount())
        ));
        walletBalanceColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue() == null ? "-" : formatCurrency(cell.getValue().getBalanceAfter())
        ));
        walletCreatedColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue() == null ? "-" : formatWalletCreatedAt(cell.getValue().getCreatedAt())
        ));
        notificationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        notificationMessageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        notificationCreatedColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        walletUserColumn.setMinWidth(160);
        walletUserIdColumn.setMinWidth(260);
        walletTypeColumn.setMinWidth(120);
        walletAmountColumn.setMinWidth(140);
        walletBalanceColumn.setMinWidth(150);
        walletCreatedColumn.setMinWidth(180);

        walletUserColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : firstNonBlank(item, "Unknown user"));
            }
        });
        walletUserIdColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : shortenUuid(item));
            }
        });
        walletTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatWalletType(item));
            }
        });
        walletCreatedColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatWalletCreatedAt(item));
            }
        });
    }

    private void loadPendingItems() {
        try {
            approvalItems.setAll(adminApiService.getPendingItems());
            hideMessage();
        } catch (Exception e) {
            approvalItems.clear();
            showMessage("Cannot load pending items: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    private void loadAdminStats() {
        try {
            AdminStatsResponse stats = adminApiService.getStats();
            totalUsersLabel.setText(String.valueOf(stats.getTotalUsers()));
            activeSellersLabel.setText(String.valueOf(stats.getActiveSellers()));
            totalAuctionsLabel.setText(String.valueOf(stats.getTotalAuctions()));
            activeAuctionsLabel.setText(String.valueOf(stats.getActiveAuctions()));
            closedAuctionsLabel.setText(String.valueOf(stats.getClosedAuctions()));
            newSellersLabel.setText(String.valueOf(stats.getNewSellersThisMonth()));
            successRateLabel.setText(String.format("%.0f%%", stats.getAuctionSuccessRate()));
            revenueLabel.setText("USD " + String.format("%,.1fk", stats.getRevenue() / 1000.0));
        } catch (Exception e) {
            totalUsersLabel.setText("-");
            activeSellersLabel.setText("-");
            totalAuctionsLabel.setText("-");
            activeAuctionsLabel.setText("-");
            closedAuctionsLabel.setText("-");
            newSellersLabel.setText("-");
            successRateLabel.setText("-");
            revenueLabel.setText("-");
        }
    }

    private void loadWalletActivity() {
        try {
            walletActivity.setAll(adminApiService.getRecentWalletActivity(5));
        } catch (Exception e) {
            walletActivity.clear();
        }
    }

    private void loadNotifications() {
        try {
            notifications.setAll(adminApiService.getRecentNotifications(6));
        } catch (Exception e) {
            notifications.clear();
        }
    }

    @FXML
    private void handleApprove() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (!hasItemId(selectedItem, "approve")) {
            return;
        }

        try {
            adminApiService.approveAndCreateAuction(
                    selectedItem.getItemId(),
                    buildCreateAuctionRequest(selectedItem)
            );
            showSuccess("Item approved and auction created successfully.");
            reloadDashboardData();
        } catch (Exception e) {
            showMessage("Approve failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleReject() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (!hasItemId(selectedItem, "reject")) {
            return;
        }

        try {
            adminApiService.rejectItem(selectedItem.getItemId());
            showSuccess("Item rejected.");
            reloadDashboardData();
        } catch (Exception e) {
            showMessage("Reject failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleReview() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showMessage("Please select an item to review.");
            return;
        }

        showSuccess("Reviewing: " + selectedItem.getProductName());
    }

    @FXML
    private void handleShowOverview() {
        showOverviewTab();
    }

    @FXML
    private void handleShowAuctionManagement() {
        loadPendingItems();
        showAuctionManagementTab();
    }

    @FXML
    private void handleDelete() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (!hasItemId(selectedItem, "delete")) {
            return;
        }

        try {
            adminApiService.deleteItem(selectedItem.getItemId());
            showSuccess("Item deleted.");
            reloadDashboardData();
        } catch (Exception e) {
            showMessage("Delete failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    private boolean hasItemId(AdminApprovalItem item, String action) {
        if (item == null) {
            showMessage("Please select an item to " + action + ".");
            return false;
        }

        if (item.getItemId() == null || item.getItemId().isBlank()) {
            showMessage("Selected item does not have itemId.");
            return false;
        }

        return true;
    }

    private void reloadDashboardData() {
        loadPendingItems();
        loadAdminStats();
        loadWalletActivity();
        loadNotifications();
    }

    private void showOverviewTab() {
        overviewPane.setManaged(true);
        overviewPane.setVisible(true);
        auctionManagementPane.setManaged(false);
        auctionManagementPane.setVisible(false);

        overviewTabButton.getStyleClass().remove("admin-tab-button-active");
        auctionManagementTabButton.getStyleClass().remove("admin-tab-button-active");
        if (!overviewTabButton.getStyleClass().contains("admin-tab-button-active")) {
            overviewTabButton.getStyleClass().add("admin-tab-button-active");
        }
    }

    private void showAuctionManagementTab() {
        overviewPane.setManaged(false);
        overviewPane.setVisible(false);
        auctionManagementPane.setManaged(true);
        auctionManagementPane.setVisible(true);

        overviewTabButton.getStyleClass().remove("admin-tab-button-active");
        auctionManagementTabButton.getStyleClass().remove("admin-tab-button-active");
        if (!auctionManagementTabButton.getStyleClass().contains("admin-tab-button-active")) {
            auctionManagementTabButton.getStyleClass().add("admin-tab-button-active");
        }
    }

    private CreateAuctionRequest buildCreateAuctionRequest(AdminApprovalItem item) {
        String startTime = toInstantTextOrDefaultStart(item.getStartDate());
        String endTime = toInstantTextOrDefaultEnd(item.getEndDate());
        Double startingPrice = item.getStartingPrice();
        Double reservePrice = item.getReservePrice();

        if (startingPrice == null || startingPrice <= 0) {
            startingPrice = 1.0;
        }
        if (reservePrice == null || reservePrice < 0) {
            reservePrice = 0.0;
        }

        return new CreateAuctionRequest(startTime, endTime, startingPrice, reservePrice);
    }

    private String toInstantTextOrDefaultStart(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plus(1, ChronoUnit.MINUTES).toString();
        }

        String trimmed = value.trim();
        return trimmed.contains("T") ? (trimmed.endsWith("Z") ? trimmed : trimmed + "Z") : trimmed + "T00:00:00Z";
    }

    private String toInstantTextOrDefaultEnd(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plus(7, ChronoUnit.DAYS).toString();
        }

        String trimmed = value.trim();
        return trimmed.contains("T") ? (trimmed.endsWith("Z") ? trimmed : trimmed + "Z") : trimmed + "T23:59:59Z";
    }

    private String extractFriendlyMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Unknown error.";
        }

        String extractedJsonMessage = extractJsonMessage(rawMessage);
        if (extractedJsonMessage != null && !extractedJsonMessage.isBlank()) {
            return extractedJsonMessage;
        }

        return rawMessage;
    }

    private String extractJsonMessage(String rawMessage) {
        int idx = rawMessage.indexOf("\"message\":\"");
        if (idx < 0) {
            return null;
        }

        int start = idx + 11;
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < rawMessage.length(); i++) {
            char current = rawMessage.charAt(i);

            if (escaped) {
                switch (current) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    default -> builder.append(current);
                }
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                return builder.toString();
            }

            builder.append(current);
        }

        return null;
    }

    private void showMessage(String message) {
        adminMessageLabel.setText(message);
        adminMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        adminMessageLabel.setManaged(true);
        adminMessageLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        adminMessageLabel.setText(message);
        adminMessageLabel.setStyle("-fx-text-fill: #16a34a;");
        adminMessageLabel.setManaged(true);
        adminMessageLabel.setVisible(true);
    }

    private void hideMessage() {
        adminMessageLabel.setText("");
        adminMessageLabel.setManaged(false);
        adminMessageLabel.setVisible(false);
    }

    private String formatCurrency(BigDecimal amount) {
        return amount == null ? "-" : currencyFormat.format(amount);
    }

    private String shortenUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "-";
        }

        try {
            UUID uuid = UUID.fromString(rawValue);
            return uuid.toString();
        } catch (Exception ignored) {
            return rawValue;
        }
    }

    private String formatWalletType(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replace('_', ' ');
    }

    private String formatWalletCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            return walletDateFormat.format(Instant.parse(value));
        } catch (Exception ignored) {
            return value;
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
