package com.auction.client.controller;

import com.auction.client.model.SellerListing;
import com.auction.client.navigation.SceneManager;
import com.auction.client.util.MockData;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class SellerDashboardController {

    @FXML private Label activeCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label endedCountLabel;
    @FXML private Label sellerMessageLabel;

    @FXML private TableView<SellerListing> listingTable;
    @FXML private TableColumn<SellerListing, String> productNameColumn;
    @FXML private TableColumn<SellerListing, String> categoryColumn;
    @FXML private TableColumn<SellerListing, String> startingPriceColumn;
    @FXML private TableColumn<SellerListing, String> statusColumn;
    @FXML private TableColumn<SellerListing, String> startDateColumn;
    @FXML private TableColumn<SellerListing, String> endDateColumn;

    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField categoryField;
    @FXML private TextField startingPriceField;
    @FXML private TextField reservePriceField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    private ObservableList<SellerListing> sellerListing;

    @FXML
    public void initialize() {
        setupTable();
        loadMockListing();
        hideMessage();
    }

    private void setupTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        startingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
    }

    private void loadMockListing() {
        sellerListing = MockData.getMockSellerListing();
        listingTable.setItems(sellerListing);
        updateStats();
    }

    @FXML
    private void handleSubmitListing() {
        hideMessage();

        String productName = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category = categoryField.getText().trim();
        String startingPriceText = startingPriceField.getText().trim();
        String reservePriceText = reservePriceField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (productName.isEmpty()) {
            showError("Product name is required.");
            return;
        }

        if (startingPriceText.isEmpty()) {
            showError("Starting price is required.");
            return;
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(startingPriceText);
        } catch (NumberFormatException e) {
            showError("Starting price must be a valid number.");
            return;
        }

        if (startingPrice <= 0) {
            showError("Starting price must be greater than 0.");
            return;
        }

        if (!reservePriceText.isEmpty()) {
            try {
                double reservePrice = Double.parseDouble(reservePriceText);
                if (reservePrice < 0) {
                    showError("Reserve price cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Reserve price must be a valid number.");
                return;
            }
        }

        if (startDate == null) {
            showError("Start date is required.");
            return;
        }

        if (endDate == null) {
            showError("End date is required.");
            return;
        }

        if (endDate.isBefore(startDate)) {
            showError("End date cannot be earlier than start date.");
            return;
        }

        if (category.isEmpty()) {
            category = "General";
        }

        SellerListing newListing = new SellerListing(
                productName,
                category,
                "$" + String.format("%,.0f", startingPrice),
                "Pending Review",
                startDate.toString(),
                endDate.toString()
        );

        sellerListing.add(0, newListing);
        listingTable.refresh();
        updateStats();

        showSuccess("Listing submitted for review.");
        clearForm();
    }

    @FXML
    private void handleResetForm() {
        clearForm();
        hideMessage();
    }

    @FXML
    private void handleLogout() {
        SceneManager.goToAuth();
    }

    private void updateStats() {
        int activeCount = 0;
        int pendingCount = 0;
        int endedCount = 0;

        for (SellerListing listing : sellerListing) {
            String status = listing.getStatus();

            if ("Active".equalsIgnoreCase(status)) {
                activeCount++;
            } else if ("Pending Review".equalsIgnoreCase(status)) {
                pendingCount++;
            } else if ("Ended".equalsIgnoreCase(status)) {
                endedCount++;
            }
        }

        activeCountLabel.setText(String.valueOf(activeCount));
        pendingCountLabel.setText(String.valueOf(pendingCount));
        endedCountLabel.setText(String.valueOf(endedCount));
    }

    private void clearForm() {
        productNameField.clear();
        descriptionArea.clear();
        categoryField.clear();
        startingPriceField.clear();
        reservePriceField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }

    private void showError(String message) {
        sellerMessageLabel.setText(message);
        sellerMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        sellerMessageLabel.setManaged(true);
        sellerMessageLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        sellerMessageLabel.setText(message);
        sellerMessageLabel.setStyle("-fx-text-fill: #16a34a;");
        sellerMessageLabel.setManaged(true);
        sellerMessageLabel.setVisible(true);
    }

    private void hideMessage() {
        sellerMessageLabel.setText("");
        sellerMessageLabel.setManaged(false);
        sellerMessageLabel.setVisible(false);
    }
}