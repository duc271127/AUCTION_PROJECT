package com.auction.client.controller;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.dto.response.ItemResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.model.SellerListing;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.SellerItemApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.List;

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

    private final SellerItemApiService sellerItemApiService = new SellerItemApiService();
    private final ObservableList<SellerListing> sellerListing = FXCollections.observableArrayList();

    private SellerListing selectedListing;

    @FXML
    public void initialize() {
        setupTable();
        setupSelectionListener();
        listingTable.setItems(sellerListing);
        loadSellerItems();
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

    private void setupSelectionListener() {
        listingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            selectedListing = newItem;
            if (newItem != null) {
                fillFormFromSelectedItem(newItem);
            }
        });
    }

    private void fillFormFromSelectedItem(SellerListing item) {
        productNameField.setText(item.getProductName());
        descriptionArea.setText(item.getDescription());
        categoryField.setText(item.getCategory());
        startingPriceField.setText(item.getStartingPrice());
        reservePriceField.setText(item.getReservePrice());

        if (item.getStartDate() != null && !item.getStartDate().isBlank()) {
            startDatePicker.setValue(LocalDate.parse(item.getStartDate()));
        } else {
            startDatePicker.setValue(null);
        }

        if (item.getEndDate() != null && !item.getEndDate().isBlank()) {
            endDatePicker.setValue(LocalDate.parse(item.getEndDate()));
        } else {
            endDatePicker.setValue(null);
        }
    }

    private void loadSellerItems() {
        try {
            List<ItemResponse> responseList = sellerItemApiService.getMyItems();

            sellerListing.clear();
            for (ItemResponse item : responseList) {
                sellerListing.add(mapToSellerListing(item));
            }

            updateStats();
        } catch (ApiException e) {
            showError("Cannot load seller items: " + e.getMessage());
        } catch (Exception e) {
            showError("Cannot load seller items.");
        }
    }

    @FXML
    private void handleSubmitListing() {
        hideMessage();

        ValidationResult validation = validateForm();
        if (!validation.isValid()) {
            showError(validation.getMessage());
            return;
        }

        try {
            CreateItemRequest request = new CreateItemRequest(
                    productNameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    getCategoryOrDefault(),
                    Double.parseDouble(startingPriceField.getText().trim()),
                    getReservePriceValue(),
                    startDatePicker.getValue().toString(),
                    endDatePicker.getValue().toString()
            );

            sellerItemApiService.createItem(request);
            showSuccess("Listing created successfully.");
            clearForm();
            loadSellerItems();

        } catch (ApiException e) {
            showError("Create failed: " + e.getMessage());
        } catch (Exception e) {
            showError("Create failed.");
        }
    }

    @FXML
    private void handleUpdateListing() {
        hideMessage();

        if (selectedListing == null || selectedListing.getId() == null) {
            showError("Please select an item to update.");
            return;
        }

        ValidationResult validation = validateForm();
        if (!validation.isValid()) {
            showError(validation.getMessage());
            return;
        }

        try {
            UpdateItemRequest request = new UpdateItemRequest(
                    productNameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    getCategoryOrDefault(),
                    Double.parseDouble(startingPriceField.getText().trim()),
                    getReservePriceValue(),
                    startDatePicker.getValue().toString(),
                    endDatePicker.getValue().toString()
            );

            sellerItemApiService.updateItem(selectedListing.getId(), request);
            showSuccess("Listing updated successfully.");
            clearForm();
            loadSellerItems();

        } catch (ApiException e) {
            showError("Update failed: " + e.getMessage());
        } catch (Exception e) {
            showError("Update failed.");
        }
    }

    @FXML
    private void handleDeleteListing() {
        hideMessage();

        if (selectedListing == null || selectedListing.getId() == null) {
            showError("Please select an item to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete listing");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this listing?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            sellerItemApiService.deleteItem(selectedListing.getId());
            showSuccess("Listing deleted successfully.");
            clearForm();
            loadSellerItems();

        } catch (ApiException e) {
            showError("Delete failed: " + e.getMessage());
        } catch (Exception e) {
            showError("Delete failed.");
        }
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

    private ValidationResult validateForm() {
        String productName = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String startingPriceText = startingPriceField.getText().trim();
        String reservePriceText = reservePriceField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (productName.isEmpty()) {
            return ValidationResult.invalid("Product name is required.");
        }

        if (description.isEmpty()) {
            return ValidationResult.invalid("Description is required.");
        }

        if (startingPriceText.isEmpty()) {
            return ValidationResult.invalid("Starting price is required.");
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(startingPriceText);
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Starting price must be a valid number.");
        }

        if (startingPrice <= 0) {
            return ValidationResult.invalid("Starting price must be greater than 0.");
        }

        if (!reservePriceText.isEmpty()) {
            try {
                double reservePrice = Double.parseDouble(reservePriceText);
                if (reservePrice < 0) {
                    return ValidationResult.invalid("Reserve price cannot be negative.");
                }

                if (reservePrice < startingPrice) {
                    return ValidationResult.invalid("Reserve price cannot be smaller than starting price.");
                }
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("Reserve price must be a valid number.");
            }
        }

        if (startDate == null) {
            return ValidationResult.invalid("Start date is required.");
        }

        if (endDate == null) {
            return ValidationResult.invalid("End date is required.");
        }

        if (endDate.isBefore(startDate)) {
            return ValidationResult.invalid("End date cannot be earlier than start date.");
        }

        return ValidationResult.valid();
    }

    private String getCategoryOrDefault() {
        String category = categoryField.getText().trim();
        return category.isEmpty() ? "General" : category;
    }

    private double getReservePriceValue() {
        String reservePriceText = reservePriceField.getText().trim();
        if (reservePriceText.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(reservePriceText);
    }

    private SellerListing mapToSellerListing(ItemResponse item) {
        return new SellerListing(
                item.getId(),
                item.getProductName(),
                item.getDescription(),
                item.getCategory(),
                String.valueOf(item.getStartingPrice()),
                String.valueOf(item.getReservePrice()),
                item.getStatus(),
                item.getStartDate(),
                item.getEndDate()
        );
    }

    private void updateStats() {
        int activeCount = 0;
        int pendingCount = 0;
        int endedCount = 0;

        for (SellerListing listing : sellerListing) {
            String status = listing.getStatus();

            if ("Active".equalsIgnoreCase(status)) {
                activeCount++;
            } else if ("Pending Review".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
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
        listingTable.getSelectionModel().clearSelection();
        selectedListing = null;
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

    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}