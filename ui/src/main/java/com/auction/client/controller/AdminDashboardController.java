package com.auction.client.controller;

import com.auction.client.model.AdminApprovalItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.util.MockData;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminDashboardController {

    @FXML private Label activeAuctionsLabel;
    @FXML private Label pendingApprovalsLabel;
    @FXML private Label reportedItemsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminMessageLabel;

    @FXML private TableView<AdminApprovalItem> approvalTable;
    @FXML private TableColumn<AdminApprovalItem, String> productNameColumn;
    @FXML private TableColumn<AdminApprovalItem, String> sellerNameColumn;
    @FXML private TableColumn<AdminApprovalItem, String> categoryColumn;
    @FXML private TableColumn<AdminApprovalItem, String> submittedDateColumn;
    @FXML private TableColumn<AdminApprovalItem, String> statusColumn;

    private ObservableList<AdminApprovalItem> approvalItems;

    @FXML
    public void initialize() {
        setupTable();
        loadMockData();
        hideMessage();
    }

    private void setupTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        sellerNameColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        submittedDateColumn.setCellValueFactory(new PropertyValueFactory<>("submittedDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadMockData() {
        approvalItems = MockData.getMockAdminApprovalItems();
        approvalTable.setItems(approvalItems);
        updateStats();
    }

    @FXML
    private void handleApprove() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showMessage("Please select an item to approve.");
            return;
        }

        selectedItem.setStatus("Approved");
        approvalTable.refresh();
        updateStats();
        showSuccess("Item approved successfully.");
    }

    @FXML
    private void handleReject() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showMessage("Please select an item to reject.");
            return;
        }

        selectedItem.setStatus("Rejected");
        approvalTable.refresh();
        updateStats();
        showSuccess("Item rejected successfully.");
    }

    @FXML
    private void handleReview() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showMessage("Please select an item to review.");
            return;
        }

        showSuccess("Review opened for: " + selectedItem.getProductName() + " (demo only).");
    }

    @FXML
    private void handleDelete() {
        AdminApprovalItem selectedItem = approvalTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showMessage("Please select an item to delete.");
            return;
        }

        approvalItems.remove(selectedItem);
        approvalTable.refresh();
        updateStats();
        showSuccess("Item deleted successfully.");
    }

    @FXML
    private void handleLogout() {
        SceneManager.goToAuth();
    }

    private void updateStats() {
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        for (AdminApprovalItem item : approvalItems) {
            if ("Pending".equalsIgnoreCase(item.getStatus())) {
                pending++;
            } else if ("Approved".equalsIgnoreCase(item.getStatus())) {
                approved++;
            } else if ("Rejected".equalsIgnoreCase(item.getStatus())) {
                rejected++;
            }
        }

        activeAuctionsLabel.setText("12");
        pendingApprovalsLabel.setText(String.valueOf(pending));
        reportedItemsLabel.setText(String.valueOf(rejected));
        totalUsersLabel.setText("248");
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