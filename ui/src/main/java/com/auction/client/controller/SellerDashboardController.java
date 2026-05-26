package com.auction.client.controller;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.dto.response.ItemResponse;
import com.auction.client.dto.response.SellerStatsResponse;
import com.auction.client.exception.ApiException;
import com.auction.client.model.SellerListing;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.SellerItemApiService;
import com.auction.client.service.SellerDashboardApiService;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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
    @FXML private Label successRateLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgPriceLabel;
    @FXML private Label sellerMessageLabel;
    @FXML private VBox recentListingsGrid;
    @FXML private TilePane myListingsGrid;
    @FXML private TextField listingSearchField;

    @FXML private Button overviewTabButton;
    @FXML private Button myListingsTabButton;
    @FXML private Button inventoryTabButton;
    @FXML private Button createListingButton;
    @FXML private VBox overviewPanel;
    @FXML private VBox myListingsPanel;
    @FXML private VBox inventoryPanel;
    @FXML private VBox listingFormPanel;

    @FXML private TableView<SellerListing> listingTable;
    @FXML private TableColumn<SellerListing, String> productNameColumn;
    @FXML private TableColumn<SellerListing, String> categoryColumn;
    @FXML private TableColumn<SellerListing, String> startingPriceColumn;
    @FXML private TableColumn<SellerListing, String> statusColumn;
    @FXML private TableColumn<SellerListing, String> startDateColumn;
    @FXML private TableColumn<SellerListing, String> endDateColumn;

    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> categoryField;
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
    private final ObservableList<SellerListing> sellerListing = FXCollections.observableArrayList();

    private SellerListing selectedListing;
    private final List<File> selectedImageFiles = new ArrayList<>();
    private final List<String> currentImageUrls = new ArrayList<>();
    private boolean imageUploadSkipped;

    @FXML
    public void initialize() {
        setupTable();
        setupSelectionListener();
        if (listingTable != null) {
            listingTable.setItems(sellerListing);
        }
        setupCategoryChoices();
        loadSellerItems();
        loadSellerStats();
        loadRecentListings();
        hideMessage();
        showSellerTab("overview");
    }

    private void setupTable() {
        if (listingTable == null) {
            return;
        }
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        startingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
    }

    private void setupSelectionListener() {
        if (listingTable == null) {
            return;
        }
        listingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            selectedListing = newItem;
        });
    }

    private void setupCategoryChoices() {
        if (categoryField == null) {
            return;
        }

        categoryField.getItems().setAll("Art", "Jewellery", "Watches", "Fashion");
    }

    private void fillFormFromSelectedItem(SellerListing item) {
        productNameField.setText(item.getProductName());
        descriptionArea.setText(item.getDescription());
        categoryField.getSelectionModel().select(firstNonBlank(item.getCategory(), "Art"));
        startingPriceField.setText(item.getStartingPrice());
        reservePriceField.setText(item.getReservePrice());
        if (skuField != null) {
            skuField.setText(item.getSku());
        }
        if (quantityField != null) {
            quantityField.setText(item.getQuantity() == null ? "1" : String.valueOf(item.getQuantity()));
        }

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

            renderMyListingsGrid();
            updateDerivedSellerStats();

        } catch (ApiException e) {
            showError("Cannot load seller items: " + e.getMessage());
        } catch (Exception e) {
            showError("Cannot load seller items: " + e.getMessage());
        }
    }

    private void showSellerTab(String tab) {
        boolean overview = "overview".equals(tab);
        boolean listings = "listings".equals(tab);
        boolean inventory = "inventory".equals(tab);

        setVisibleManaged(overviewPanel, overview);
        setVisibleManaged(myListingsPanel, listings);
        setVisibleManaged(inventoryPanel, inventory);

        setActiveTab(overviewTabButton, overview);
        setActiveTab(myListingsTabButton, listings);
        setActiveTab(inventoryTabButton, inventory);
    }

    private void setActiveTab(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().remove("seller-tab-active");
        if (active) {
            button.getStyleClass().add("seller-tab-active");
        }
    }

    private void setVisibleManaged(Control control, boolean visible) {
        if (control == null) {
            return;
        }

        control.setVisible(visible);
        control.setManaged(visible);
    }

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node == null) {
            return;
        }

        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void showListingForm() {
        if (listingFormPanel == null) {
            return;
        }

        listingFormPanel.setVisible(true);
        listingFormPanel.setManaged(true);
    }

    private void hideListingForm() {
        if (listingFormPanel == null) {
            return;
        }

        listingFormPanel.setVisible(false);
        listingFormPanel.setManaged(false);
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
            imageUploadSkipped = false;
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
                    getSkuValue(),
                    getQuantityValue()
            );

            sellerItemApiService.createItem(request);

            showSuccess(imageUploadSkipped
                    ? "Listing created successfully. Image upload endpoint is not available yet, so images were skipped."
                    : "Listing created successfully.");
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
            imageUploadSkipped = false;
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
                    getSkuValue(),
                    getQuantityValue()
            );

            sellerItemApiService.updateItem(selectedListing.getId(), request);

            showSuccess(imageUploadSkipped
                    ? "Listing updated successfully. Image upload endpoint is not available yet, so images were skipped."
                    : "Listing updated successfully.");
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

        if (!selectedListing.isDeletable()) {
            showError(resolveDeleteBlockedReason(selectedListing));
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
        hideListingForm();
    }

    @FXML
    private void handleCreateListing() {
        openItemDialog(null);
    }

    @FXML
    private void handleShowOverview() {
        showSellerTab("overview");
    }

    @FXML
    private void handleShowMyListings() {
        showSellerTab("listings");
    }

    @FXML
    private void handleShowInventory() {
        showSellerTab("inventory");
    }

    @FXML
    private void handleSearchListings() {
        renderMyListingsGrid();
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
        String quantityText = quantityField == null ? "1" : quantityField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (productName.isEmpty()) {
            return ValidationResult.invalid("Product name is required.");
        }

        if (description.isEmpty()) {
            return ValidationResult.invalid("Description is required.");
        }

        if (categoryField.getValue() == null || categoryField.getValue().isBlank()) {
            return ValidationResult.invalid("Category is required.");
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
        String category = categoryField.getValue();
        return category == null || category.isBlank() ? "Art" : category;
    }

    private double getReservePriceValue() {
        String reservePriceText = reservePriceField.getText().trim();

        if (reservePriceText.isEmpty()) {
            return 0;
        }

        return Double.parseDouble(reservePriceText);
    }

    private int getQuantityValue() {
        if (quantityField == null) {
            return 1;
        }
        return Integer.parseInt(quantityField.getText().trim());
    }

    private String getSkuValue() {
        return skuField == null || skuField.getText() == null ? "" : skuField.getText().trim();
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
                item.getQuantity(),
                item.isDeletable(),
                item.getDeleteBlockedReason()
        );
    }

    private void loadSellerStats() {
        try {
            SellerStatsResponse stats = sellerDashboardApiService.getStats();
            setLabel(totalListingsLabel, String.valueOf(stats.getTotalItems()));
            setLabel(pendingCountLabel, String.valueOf(stats.getPendingItems()));
            setLabel(approvedCountLabel, String.valueOf(stats.getCompletedAuctions()));
            setLabel(rejectedCountLabel, String.valueOf(stats.getRejectedItems()));
            setLabel(activeCountLabel, String.valueOf(stats.getActiveAuctions()));
            setLabel(successRateLabel, String.format("%.0f%%", stats.getSuccessRate()));
            setLabel(totalRevenueLabel, formatMoney(stats.getTotalRevenue()));
            setLabel(avgPriceLabel, formatMoney(stats.getAverageSalePrice()));
        } catch (Exception e) {
            setLabel(totalListingsLabel, "-");
            setLabel(pendingCountLabel, "-");
            setLabel(approvedCountLabel, "-");
            setLabel(rejectedCountLabel, "-");
            setLabel(activeCountLabel, "-");
            setLabel(successRateLabel, "-");
            setLabel(totalRevenueLabel, "-");
            setLabel(avgPriceLabel, "-");
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

    private void renderMyListingsGrid() {
        if (myListingsGrid == null) {
            return;
        }

        String keyword = listingSearchField == null || listingSearchField.getText() == null
                ? ""
                : listingSearchField.getText().trim().toLowerCase();

        myListingsGrid.getChildren().clear();
        for (SellerListing listing : sellerListing) {
            if (!matchesListingSearch(listing, keyword)) {
                continue;
            }
            myListingsGrid.getChildren().add(buildMyListingCard(listing));
        }

        if (myListingsGrid.getChildren().isEmpty()) {
            Label empty = new Label("No listings found.");
            empty.getStyleClass().add("seller-empty-label");
            myListingsGrid.getChildren().add(empty);
        }
    }

    private boolean matchesListingSearch(SellerListing listing, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        return containsIgnoreCase(listing.getProductName(), keyword)
                || containsIgnoreCase(listing.getCategory(), keyword)
                || containsIgnoreCase(listing.getStatus(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void updateDerivedSellerStats() {
        if (totalListingsLabel != null) {
            totalListingsLabel.setText(String.valueOf(sellerListing.size()));
        }

        if (activeCountLabel != null && "-".equals(activeCountLabel.getText())) {
            long active = sellerListing.stream()
                    .filter(item -> "APPROVED".equalsIgnoreCase(firstNonBlank(item.getStatus(), "")))
                    .count();
            activeCountLabel.setText(String.valueOf(active));
        }
    }

    private VBox buildMyListingCard(SellerListing listing) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(215);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("seller-card-image");
        setListingImage(imageView, listing.getImagePath());

        Label status = new Label(firstNonBlank(listing.getStatus(), "pending").toLowerCase());
        status.getStyleClass().addAll("seller-status-pill", sellerStatusClass(listing.getStatus()));

        StackPane imageLayer = new StackPane(imageView, status);
        imageLayer.getStyleClass().add("seller-card-media");
        StackPane.setAlignment(status, Pos.TOP_RIGHT);

        Label title = new Label(firstNonBlank(listing.getProductName(), "Untitled product"));
        title.setWrapText(true);
        title.getStyleClass().add("seller-card-title");

        Label category = new Label(firstNonBlank(listing.getCategory(), "General"));
        category.getStyleClass().add("seller-card-meta");

        Label price = new Label(formatMoneyValue(listing.getStartingPrice()));
        price.getStyleClass().add("seller-card-price");

        Label quantity = new Label("Qty " + (listing.getQuantity() == null ? "0" : listing.getQuantity()));
        quantity.getStyleClass().add("seller-card-meta");

        Button view = new Button("View");
        view.getStyleClass().add("seller-mini-button");
        view.setOnAction(event -> {
            selectListing(listing);
            showSellerTab("inventory");
        });

        Button edit = new Button("Edit");
        edit.getStyleClass().add("seller-mini-button");
        edit.setOnAction(event -> {
            selectListing(listing);
            openItemDialog(listing);
        });

        Button delete = new Button("Delete");
        delete.getStyleClass().add("seller-mini-danger-button");
        delete.setDisable(!listing.isDeletable());
        if (!listing.isDeletable()) {
            delete.setTooltip(new Tooltip(resolveDeleteBlockedReason(listing)));
        }
        delete.setOnAction(event -> {
            selectListing(listing);
            handleDeleteListing();
        });

        HBox actions = new HBox(8, view, edit, delete);
        view.setMaxWidth(Double.MAX_VALUE);
        edit.setMaxWidth(Double.MAX_VALUE);
        delete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(view, Priority.ALWAYS);
        HBox.setHgrow(edit, Priority.ALWAYS);
        HBox.setHgrow(delete, Priority.ALWAYS);

        VBox details = new VBox(6, title, category, price, quantity, actions);
        details.getStyleClass().add("seller-card-body");

        VBox card = new VBox(imageLayer, details);
        card.getStyleClass().add("seller-product-card");
        return card;
    }

    private String sellerStatusClass(String status) {
        String normalized = firstNonBlank(status, "").toLowerCase();
        if (normalized.contains("approved") || normalized.contains("active")) {
            return "seller-status-active";
        }
        if (normalized.contains("reject") || normalized.contains("ended")) {
            return "seller-status-ended";
        }
        return "seller-status-pending";
    }

    private void setLabel(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String formatMoney(double value) {
        return String.format("USD %,.0f", value);
    }

    private String formatMoneyValue(String value) {
        if (value == null || value.isBlank()) {
            return "USD 0";
        }

        try {
            return formatMoney(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return "USD " + value;
        }
    }

    private HBox buildRecentListingCard(SellerListing listing) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(58);
        imageView.setFitHeight(58);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("seller-listing-image");
        setListingImage(imageView, listing.getImagePath());

        Label name = new Label(firstNonBlank(listing.getProductName(), "Untitled product"));
        name.setWrapText(true);
        name.getStyleClass().add("seller-listing-name");

        Label details = new Label(firstNonBlank(listing.getCategory(), "General")
                + " • " + firstNonBlank(listing.getStatus(), "pending").toLowerCase());
        details.setWrapText(true);
        details.getStyleClass().add("seller-listing-meta");

        Label price = new Label(formatMoneyValue(listing.getStartingPrice()));
        price.getStyleClass().add("seller-listing-price");

        Label status = new Label(firstNonBlank(listing.getStatus(), "pending").toLowerCase());
        status.getStyleClass().addAll("seller-row-status", sellerStatusClass(listing.getStatus()));

        Button edit = new Button("Edit");
        edit.getStyleClass().add("seller-row-action");
        edit.setOnAction(event -> {
            selectListing(listing);
            openItemDialog(listing);
        });

        Button delete = new Button("Delete");
        delete.getStyleClass().add("seller-row-danger");
        delete.setDisable(!listing.isDeletable());
        if (!listing.isDeletable()) {
            delete.setTooltip(new Tooltip(resolveDeleteBlockedReason(listing)));
        }
        delete.setOnAction(event -> {
            selectListing(listing);
            handleDeleteListing();
        });

        VBox info = new VBox(4, name, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox value = new VBox(5, price, status);
        value.getStyleClass().add("seller-row-value");

        HBox actions = new HBox(8, edit, delete);
        actions.getStyleClass().add("seller-row-actions");

        HBox row = new HBox(14, imageView, info, value, actions);
        row.getStyleClass().add("seller-listing-row");
        return row;
    }

    private void selectListing(SellerListing listing) {
        if (listingTable != null) {
            listingTable.getSelectionModel().select(listing);
        }
        if (listingTable == null || listingTable.getSelectionModel().getSelectedItem() == null) {
            selectedListing = listing;
        }
    }

    private String resolveDeleteBlockedReason(SellerListing listing) {
        if (listing == null) {
            return "This listing cannot be deleted.";
        }

        String reason = listing.getDeleteBlockedReason();
        if (reason != null && !reason.isBlank()) {
            return reason;
        }

        return "This listing already has an auction, so the seller cannot delete it.";
    }

    private void openItemDialog(SellerListing listing) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/seller_item_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(listing == null ? "Create Listing" : "Edit Listing");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Window ownerWindow = resolveDialogOwnerWindow();
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }

            SellerItemDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setListing(listing);
            controller.setOnSaved(() -> {
                loadSellerItems();
                loadSellerStats();
                loadRecentListings();
                showSellerTab("listings");
            });

            Scene scene = createOverlayDialogScene(root, ownerWindow);
            stage.setScene(scene);
            positionOverlayDialogStage(stage, ownerWindow);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Cannot open listing dialog: " + e.getMessage());
        }
    }

    private Window resolveDialogOwnerWindow() {
        return createListingButton != null && createListingButton.getScene() != null
                ? createListingButton.getScene().getWindow()
                : null;
    }

    private Scene createOverlayDialogScene(Parent dialogCard, Window ownerWindow) {
        StackPane overlay = new StackPane(dialogCard);
        overlay.getStyleClass().add("modal-overlay");
        dialogCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #dfe3ea; -fx-border-radius: 10;");
        if (dialogCard instanceof Region region) {
            region.setPrefWidth(560);
            region.setMinWidth(560);
            region.setMaxWidth(560);
            region.setPrefHeight(700);
            region.setMaxHeight(700);
        }

        Scene scene = ownerWindow == null
                ? new Scene(overlay, Color.TRANSPARENT)
                : new Scene(overlay, ownerWindow.getWidth(), ownerWindow.getHeight(), Color.TRANSPARENT);

        addDialogStyles(scene);
        return scene;
    }

    private void positionOverlayDialogStage(Stage stage, Window ownerWindow) {
        if (ownerWindow == null) {
            return;
        }

        stage.setX(ownerWindow.getX());
        stage.setY(ownerWindow.getY());
    }

    private void addDialogStyles(Scene scene) {
        addStylesheet(scene, "/css/app.css");
        addStylesheet(scene, "/css/components.css");
        addStylesheet(scene, "/css/seller.css");
    }

    private void addStylesheet(Scene scene, String path) {
        try {
            String css = getClass().getResource(path).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {
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
        categoryField.getSelectionModel().clearSelection();
        startingPriceField.clear();
        reservePriceField.clear();
        if (skuField != null) {
            skuField.clear();
        }
        if (quantityField != null) {
            quantityField.setText("1");
        }

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

        if (listingTable != null) {
            listingTable.getSelectionModel().clearSelection();
        }
        selectedListing = null;
    }

    private List<String> uploadSelectedImages() {
        List<String> uploaded = new ArrayList<>();
        for (File imageFile : selectedImageFiles) {
            try {
                String imageUrl = sellerItemApiService.uploadImage(imageFile);
                if (imageUrl != null && !imageUrl.isBlank()) {
                    uploaded.add(imageUrl);
                }
            } catch (Exception e) {
                imageUploadSkipped = true;
                return uploaded;
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
