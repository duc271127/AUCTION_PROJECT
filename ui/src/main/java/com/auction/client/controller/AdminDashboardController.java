package com.auction.client.controller;

import com.auction.client.dto.request.CreateAuctionRequest;
import com.auction.client.model.AdminApprovalItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AdminApiService;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AdminDashboardController {

    @FXML private Label activeAuctionsLabel;
    @FXML private Label pendingApprovalsLabel;
    @FXML private Label reportedItemsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminMessageLabel;

    @FXML private TableView<AdminApprovalItem> approvalTable;
    @FXML private TableColumn<AdminApprovalItem, String> productNameColumn;
    @FXML private TableColumn<AdminApprovalItem, String> categoryColumn;
    @FXML private TableColumn<AdminApprovalItem, String> submittedDateColumn;
    @FXML private TableColumn<AdminApprovalItem, String> statusColumn;

    private final AdminApiService adminApiService = new AdminApiService();
    private ObservableList<AdminApprovalItem> approvalItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        approvalTable.setItems(approvalItems);
        hideMessage();
        loadPendingItems();
    }

    private void setupTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        submittedDateColumn.setCellValueFactory(new PropertyValueFactory<>("submittedDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadPendingItems() {
        try {
            approvalItems.setAll(adminApiService.getPendingItems());
            updateStats();
            hideMessage();
        } catch (Exception e) {
            approvalItems.clear();
            updateStats();
            showMessage("Cannot load pending items: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleApprove() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showMessage("Please select an item to approve.");
            return;
        }

        if (selectedItem.getItemId() == null || selectedItem.getItemId().isBlank()) {
            showMessage("Selected item does not have itemId.");
            return;
        }

        try {
            adminApiService.approveItem(selectedItem.getItemId());

            CreateAuctionRequest request = buildCreateAuctionRequest(selectedItem);
            adminApiService.createAuctionForItem(selectedItem.getItemId(), request);

            showSuccess("Item approved and auction created successfully.");
            loadPendingItems();

        } catch (Exception e) {
            showMessage("Approve failed: " + extractFriendlyMessage(e.getMessage()));
        }
    }

    @FXML
    private void handleReject() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showMessage("Please select an item to reject.");
            return;
        }

        // Backend core hiện tại chưa có reject endpoint.
        // Khi backend thêm POST /admin/items/{itemId}/reject thì nối ở đây.
        showMessage("Reject API is not available yet.");
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
    private void handleDelete() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showMessage("Please select an item to delete.");
            return;
        }

        approvalItems.remove(selectedItem);
        updateStats();
        showSuccess("Removed from current table. Backend delete API is not connected.");
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    private CreateAuctionRequest buildCreateAuctionRequest(AdminApprovalItem item) {
        String startTime = toInstantTextOrDefaultStart(item.getStartDate());
        String endTime = toInstantTextOrDefaultEnd(item.getEndDate());

        Double startingPrice = item.getStartingPrice();
        if (startingPrice == null || startingPrice <= 0) {
            startingPrice = 1.0;
        }

        Double reservePrice = item.getReservePrice();
        if (reservePrice == null || reservePrice < 0) {
            reservePrice = 0.0;
        }

        return new CreateAuctionRequest(
                startTime,
                endTime,
                startingPrice,
                reservePrice
        );
    }

    private String toInstantTextOrDefaultStart(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plus(1, ChronoUnit.MINUTES).toString();
        }

        String trimmed = value.trim();

        if (trimmed.contains("T")) {
            return trimmed.endsWith("Z") ? trimmed : trimmed + "Z";
        }

        return trimmed + "T00:00:00Z";
    }

    private String toInstantTextOrDefaultEnd(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plus(7, ChronoUnit.DAYS).toString();
        }

        String trimmed = value.trim();

        if (trimmed.contains("T")) {
            return trimmed.endsWith("Z") ? trimmed : trimmed + "Z";
        }

        return trimmed + "T23:59:59Z";
    }

    private void updateStats() {
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        for (AdminApprovalItem item : approvalItems) {
            if ("PENDING".equalsIgnoreCase(item.getStatus()) || "Pending".equalsIgnoreCase(item.getStatus())) {
                pending++;
            } else if ("APPROVED".equalsIgnoreCase(item.getStatus()) || "Approved".equalsIgnoreCase(item.getStatus())) {
                approved++;
            } else if ("REJECTED".equalsIgnoreCase(item.getStatus()) || "Rejected".equalsIgnoreCase(item.getStatus())) {
                rejected++;
            }
        }

        activeAuctionsLabel.setText(String.valueOf(approved));
        pendingApprovalsLabel.setText(String.valueOf(pending));
        reportedItemsLabel.setText(String.valueOf(rejected));
        totalUsersLabel.setText("-");
    }

    private String extractFriendlyMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Unknown error.";
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
}
