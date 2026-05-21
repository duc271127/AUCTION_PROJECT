package com.auction.client.controller;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.dto.response.ItemResponse;
import com.auction.client.dto.response.SellerStatsResponse;
import com.auction.client.dto.response.WalletBalanceResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.model.SellerListing;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.SellerItemApiService;
import com.auction.client.service.SellerDashboardApiService;
import com.auction.client.service.WalletApiService;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class SellerDashboardController {

    @FXML private Label activeCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label totalListingsLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label rejectedCountLabel;
    @FXML private Label walletBalanceLabel;
    @FXML private Label sellerMessageLabel;
    @FXML private TilePane recentListingsGrid;

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
    @FXML private TextField skuField;
    @FXML private TextField quantityField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private ImageView productImagePreview;
    @FXML private Label selectedImageLabel;
    @FXML private ComboBox<String> primaryImageChoice;
    @FXML private HBox imagePreviewStrip;

    private final SellerItemApiService sellerItemApiService = new SellerItemApiService();
    private final SellerDashboardApiService sellerDashboardApiService = new SellerDashboardApiService();
    private final WalletApiService walletApiService = new WalletApiService();
    private final ObservableList<SellerListing> sellerListing = FXCollections.observableArrayList();

    private SellerListing selectedListing;
    private final List<File> selectedImageFiles = new ArrayList<>();
    private final List<String> currentImageUrls = new ArrayList<>();

    @FXML
    public void initialize() {
        setupTable();
        setupSelectionListener();
        listingTable.setItems(sellerListing);
        loadSellerItems();
        loadSellerStats();
        loadWalletBalance();
        loadRecentListings();
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
        skuField.setText(item.getSku());
        quantityField.setText(item.getQuantity() == null ? "1" : String.valueOf(item.getQuantity()));

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

        selectedImageFiles.clear();
        currentImageUrls.clear();
        if (item.getImageUrls() != null) {
            currentImageUrls.addAll(item.getImageUrls());
        } else if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            currentImageUrls.add(item.getImagePath());
        }
        bindPrimaryImageChoices(currentImageUrls);
        renderRemoteImagePreviews(currentImageUrls);

        if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            selectedImageLabel.setText(item.getImagePath());

            try {
                Image image = new Image(sellerItemApiService.toAbsoluteImageUrl(item.getImagePath()), true);
                productImagePreview.setImage(image);
            } catch (Exception e) {
                productImagePreview.setImage(null);
            }
        } else {
            selectedImageLabel.setText("No image selected");
            productImagePreview.setImage(null);
        }
    }

    private void loadSellerItems() {
        try {
            List<ItemResponse> responseList = sellerItemApiService.getMyItems();

            sellerListing.clear();

            for (ItemResponse item : responseList) {
                sellerListing.add(mapToSellerListing(item));
            }

        } catch (ApiException e) {
            showError("Cannot load seller items: " + e.getMessage());
        } catch (Exception e) {
            showError("Cannot load seller items: " + e.getMessage());
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose product image");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png", "*.jpg", "*.jpeg", "*.webp"
                )
        );

        List<File> files = fileChooser.showOpenMultipleDialog(productNameField.getScene().getWindow());

        if (files == null || files.isEmpty()) {
            return;
        }

        selectedImageFiles.clear();
        selectedImageFiles.addAll(files);
        currentImageUrls.clear();
        bindPrimaryFileChoices(files);
        renderLocalImagePreviews(files);
    }

    @FXML
    private void handleSubmitListing() {
        hideMessage();

        ValidationResult validation = validateForm();
        if (!validation.isValid()) {
            showError(validation.getMessage());
            return;
        }

        if (SessionManager.getUserId() == null) {
            showError("Seller id is missing. Please login again.");
            return;
        }

        try {
            List<String> imageUrls = uploadSelectedImages();
            String imagePath = imageUrls.isEmpty() ? "" : imageUrls.get(0);

            CreateItemRequest request = new CreateItemRequest(
                    SessionManager.getUserId(),
                    productNameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    getCategoryOrDefault(),
                    Double.parseDouble(startingPriceField.getText().trim()),
                    getReservePriceValue(),
                    startDatePicker.getValue().toString(),
                    endDatePicker.getValue().toString(),
                    imagePath,
                    imageUrls,
                    skuField.getText().trim(),
                    getQuantityValue()
            );

            sellerItemApiService.createItem(request);

            showSuccess("Listing created successfully.");
            clearForm();
            loadSellerItems();
            loadSellerStats();
            loadRecentListings();

        } catch (ApiException e) {
            showError("Create failed: " + e.getMessage());
        } catch (Exception e) {
            showError("Create failed: " + e.getMessage());
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

        if (SessionManager.getUserId() == null) {
            showError("Seller id is missing. Please login again.");
            return;
        }

        try {
            List<String> imageUrls = selectedImageFiles.isEmpty()
                    ? reorderCurrentImages()
                    : uploadSelectedImages();
            String imagePath = imageUrls.isEmpty() ? "" : imageUrls.get(0);

            UpdateItemRequest request = new UpdateItemRequest(
                    SessionManager.getUserId(),
                    productNameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    getCategoryOrDefault(),
                    Double.parseDouble(startingPriceField.getText().trim()),
                    getReservePriceValue(),
                    startDatePicker.getValue().toString(),
                    endDatePicker.getValue().toString(),
                    imagePath,
                    imageUrls,
                    skuField.getText().trim(),
                    getQuantityValue()
            );

            sellerItemApiService.updateItem(selectedListing.getId(), request);

            showSuccess("Listing updated successfully.");
            clearForm();
            loadSellerItems();
            loadSellerStats();
            loadRecentListings();

        } catch (ApiException e) {
            showError("Update failed: " + e.getMessage());
        } catch (Exception e) {
            showError("Update failed: " + e.getMessage());
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
            loadSellerStats();
            loadRecentListings();

        } catch (ApiException e) {
            showError("Delete failed: " + e.getMessage());
        } catch (Exception e) {
            showError("Delete failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleResetForm() {
        clearForm();
        hideMessage();
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleOpenWallet() {
        SceneManager.goToWallet();
    }

    private ValidationResult validateForm() {
        String productName = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String startingPriceText = startingPriceField.getText().trim();
        String reservePriceText = reservePriceField.getText().trim();
        String quantityText = quantityField.getText().trim();
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

        if (quantityText.isEmpty()) {
            return ValidationResult.invalid("Quantity is required.");
        }

        try {
            int quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                return ValidationResult.invalid("Quantity cannot be negative.");
            }
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Quantity must be a whole number.");
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

    private int getQuantityValue() {
        return Integer.parseInt(quantityField.getText().trim());
    }

    private SellerListing mapToSellerListing(ItemResponse item) {
        return new SellerListing(
                item.getId(),
                item.getSellerId(),
                item.getProductName(),
                item.getDescription(),
                item.getCategory(),
                String.valueOf(item.getStartingPrice()),
                String.valueOf(item.getReservePrice()),
                item.getStatus(),
                item.getStartDate(),
                item.getEndDate(),
                item.getImagePath(),
                item.getImageUrls(),
                item.getSku(),
                item.getQuantity()
        );
    }

    private void loadSellerStats() {
        try {
            SellerStatsResponse stats = sellerDashboardApiService.getStats();
            totalListingsLabel.setText(String.valueOf(stats.getTotalItems()));
            pendingCountLabel.setText(String.valueOf(stats.getPendingItems()));
            approvedCountLabel.setText(String.valueOf(stats.getApprovedItems()));
            rejectedCountLabel.setText(String.valueOf(stats.getRejectedItems()));
            activeCountLabel.setText(String.valueOf(stats.getActiveAuctions()));
        } catch (Exception e) {
            totalListingsLabel.setText("-");
            pendingCountLabel.setText("-");
            approvedCountLabel.setText("-");
            rejectedCountLabel.setText("-");
            activeCountLabel.setText("-");
        }
    }

    private void loadWalletBalance() {
        try {
            WalletBalanceResponse balance = walletApiService.getBalance();
            walletBalanceLabel.setText(balance.getBalance() == null ? "$0" : "$" + balance.getBalance());
        } catch (Exception e) {
            walletBalanceLabel.setText("-");
        }
    }

    private void loadRecentListings() {
        if (recentListingsGrid == null) {
            return;
        }

        try {
            recentListingsGrid.getChildren().clear();
            for (ItemResponse item : sellerItemApiService.getRecentItems(4)) {
                recentListingsGrid.getChildren().add(buildRecentListingCard(mapToSellerListing(item)));
            }
        } catch (Exception e) {
            recentListingsGrid.getChildren().clear();
            recentListingsGrid.getChildren().add(new Label("Cannot load recent listings."));
        }
    }

    private VBox buildRecentListingCard(SellerListing listing) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(170);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("seller-listing-image");
        setListingImage(imageView, listing.getImagePath());

        Label name = new Label(listing.getProductName());
        name.setWrapText(true);
        name.getStyleClass().add("seller-listing-name");

        Label details = new Label(firstNonBlank(listing.getCategory(), "General")
                + " | " + firstNonBlank(listing.getStatus(), "PENDING"));
        details.setWrapText(true);
        details.getStyleClass().add("seller-listing-meta");

        Label price = new Label("$" + firstNonBlank(listing.getStartingPrice(), "0"));
        price.getStyleClass().add("seller-listing-price");

        Button edit = new Button("Edit");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> selectListing(listing));

        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> {
            selectListing(listing);
            handleDeleteListing();
        });

        HBox actions = new HBox(8, edit, delete);
        HBox.setHgrow(edit, Priority.ALWAYS);
        HBox.setHgrow(delete, Priority.ALWAYS);
        edit.setMaxWidth(Double.MAX_VALUE);
        delete.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(8, imageView, name, details, price, actions);
        card.getStyleClass().add("seller-listing-card");
        return card;
    }

    private void selectListing(SellerListing listing) {
        listingTable.getSelectionModel().select(listing);
        if (listingTable.getSelectionModel().getSelectedItem() == null) {
            fillFormFromSelectedItem(listing);
            selectedListing = listing;
        }
    }

    private void setListingImage(ImageView imageView, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            imageView.setImage(null);
            return;
        }

        imageView.setImage(new Image(sellerItemApiService.toAbsoluteImageUrl(imagePath), true));
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void clearForm() {
        productNameField.clear();
        descriptionArea.clear();
        categoryField.clear();
        startingPriceField.clear();
        reservePriceField.clear();
        skuField.clear();
        quantityField.setText("1");

        startDatePicker.setValue(null);
        endDatePicker.setValue(null);

        selectedImageFiles.clear();
        currentImageUrls.clear();
        if (primaryImageChoice != null) {
            primaryImageChoice.getItems().clear();
        }
        if (imagePreviewStrip != null) {
            imagePreviewStrip.getChildren().clear();
        }
        productImagePreview.setImage(null);
        selectedImageLabel.setText("No image selected");

        listingTable.getSelectionModel().clearSelection();
        selectedListing = null;
    }

    private List<String> uploadSelectedImages() {
        List<String> uploaded = new ArrayList<>();
        for (File imageFile : selectedImageFiles) {
            String imageUrl = sellerItemApiService.uploadImage(imageFile);
            if (imageUrl != null && !imageUrl.isBlank()) {
                uploaded.add(imageUrl);
            }
        }

        return moveSelectedPrimaryFirst(uploaded);
    }

    private List<String> reorderCurrentImages() {
        return moveSelectedPrimaryFirst(new ArrayList<>(currentImageUrls));
    }

    private List<String> moveSelectedPrimaryFirst(List<String> images) {
        if (images.isEmpty() || primaryImageChoice == null || primaryImageChoice.getSelectionModel().getSelectedIndex() <= 0) {
            return images;
        }

        int primaryIndex = primaryImageChoice.getSelectionModel().getSelectedIndex();
        if (primaryIndex >= images.size()) {
            return images;
        }

        Collections.swap(images, 0, primaryIndex);
        return images;
    }

    private void bindPrimaryFileChoices(List<File> files) {
        List<String> labels = new ArrayList<>();
        for (File file : files) {
            labels.add(file.getName());
        }
        bindPrimaryImageChoices(labels);
        selectedImageLabel.setText(files.size() + " image(s) selected");
    }

    private void bindPrimaryImageChoices(List<String> values) {
        if (primaryImageChoice == null) {
            return;
        }

        primaryImageChoice.getItems().setAll(values);
        if (!values.isEmpty()) {
            primaryImageChoice.getSelectionModel().select(0);
        }
    }

    private void renderLocalImagePreviews(List<File> files) {
        imagePreviewStrip.getChildren().clear();
        for (File file : files) {
            imagePreviewStrip.getChildren().add(buildPreviewImage(new Image(file.toURI().toString(), true)));
        }
        productImagePreview.setImage(new Image(files.get(0).toURI().toString(), true));
    }

    private void renderRemoteImagePreviews(List<String> imageUrls) {
        imagePreviewStrip.getChildren().clear();
        for (String imageUrl : imageUrls) {
            imagePreviewStrip.getChildren().add(buildPreviewImage(new Image(sellerItemApiService.toAbsoluteImageUrl(imageUrl), true)));
        }
    }

    private ImageView buildPreviewImage(Image image) {
        ImageView preview = new ImageView(image);
        preview.setFitWidth(68);
        preview.setFitHeight(52);
        preview.setPreserveRatio(false);
        preview.getStyleClass().add("seller-image-thumb");
        return preview;
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
