package com.auction.client.controller;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.model.SellerListing;
import com.auction.client.service.SellerItemApiService;
import com.auction.client.session.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellerItemDialogController {

    @FXML private Label dialogTitleLabel;
    @FXML private Label messageLabel;
    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> categoryField;
    @FXML private TextField startingPriceField;
    @FXML private TextField reservePriceField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ImageView productImagePreview;
    @FXML private Label selectedImageLabel;
    @FXML private ComboBox<String> primaryImageChoice;
    @FXML private HBox imagePreviewStrip;
    @FXML private Button submitButton;
    @FXML private Button deleteButton;

    private final SellerItemApiService sellerItemApiService = new SellerItemApiService();
    private final List<SelectedImage> selectedImages = new ArrayList<>();

    private Stage dialogStage;
    private SellerListing listing;
    private Runnable onSaved;
    private boolean imageUploadSkipped;

    @FXML
    public void initialize() {
        categoryField.getItems().setAll("Art", "Jewellery", "Watches", "Fashion");
        primaryImageChoice.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex != null) {
                updateLargePreview(newIndex.intValue());
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setListing(SellerListing listing) {
        this.listing = listing;

        if (listing == null) {
            dialogTitleLabel.setText("Create Listing");
            submitButton.setText("Submit Listing");
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            return;
        }

        dialogTitleLabel.setText("Edit Listing");
        submitButton.setText("Update Listing");
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);

        productNameField.setText(safeText(listing.getProductName(), ""));
        descriptionArea.setText(safeText(listing.getDescription(), ""));
        categoryField.getSelectionModel().select(safeText(listing.getCategory(), "Art"));
        startingPriceField.setText(safeText(listing.getStartingPrice(), ""));
        reservePriceField.setText(safeText(listing.getReservePrice(), ""));
        startDatePicker.setValue(parseDate(listing.getStartDate()));
        endDatePicker.setValue(parseDate(listing.getEndDate()));

        selectedImages.clear();
        if (listing.getImageUrls() != null) {
            for (String imageUrl : listing.getImageUrls()) {
                if (imageUrl != null && !imageUrl.isBlank()) {
                    selectedImages.add(new SelectedImage(null, imageUrl));
                }
            }
        } else if (listing.getImagePath() != null && !listing.getImagePath().isBlank()) {
            selectedImages.add(new SelectedImage(null, listing.getImagePath()));
        }

        refreshImageUi(selectedImages.isEmpty() ? -1 : 0);
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose product image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = fileChooser.showOpenDialog(
                productNameField.getScene() == null ? null : productNameField.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        selectedImages.add(new SelectedImage(file, null));
        refreshImageUi(selectedImages.size() - 1);
    }

    @FXML
    private void handleRemoveSelectedImage() {
        int selectedIndex = primaryImageChoice.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= selectedImages.size()) {
            return;
        }

        selectedImages.remove(selectedIndex);
        int nextIndex = selectedImages.isEmpty() ? -1 : Math.min(selectedIndex, selectedImages.size() - 1);
        refreshImageUi(nextIndex);
    }

    @FXML
    private void handleSubmit() {
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
            List<String> imageUrls = resolveImageUrlsForSave();
            String imagePath = imageUrls.isEmpty() ? "" : imageUrls.get(0);

            if (listing == null) {
                CreateItemRequest request = new CreateItemRequest(
                        SessionManager.getUserId(),
                        productNameField.getText().trim(),
                        descriptionArea.getText().trim(),
                        getCategoryValue(),
                        Double.parseDouble(startingPriceField.getText().trim()),
                        getReservePriceValue(),
                        startDatePicker.getValue().toString(),
                        endDatePicker.getValue().toString(),
                        imagePath,
                        imageUrls,
                        "",
                        1
                );
                sellerItemApiService.createItem(request);
            } else {
                UpdateItemRequest request = new UpdateItemRequest(
                        SessionManager.getUserId(),
                        productNameField.getText().trim(),
                        descriptionArea.getText().trim(),
                        getCategoryValue(),
                        Double.parseDouble(startingPriceField.getText().trim()),
                        getReservePriceValue(),
                        startDatePicker.getValue().toString(),
                        endDatePicker.getValue().toString(),
                        imagePath,
                        imageUrls,
                        safeText(listing.getSku(), ""),
                        listing.getQuantity() == null ? 1 : listing.getQuantity()
                );
                sellerItemApiService.updateItem(listing.getId(), request);
            }

            if (onSaved != null) {
                onSaved.run();
            }

            handleClose();
        } catch (Exception e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (listing == null || listing.getId() == null) {
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
            sellerItemApiService.deleteItem(listing.getId());
            if (onSaved != null) {
                onSaved.run();
            }
            handleClose();
        } catch (Exception e) {
            showError("Delete failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private ValidationResult validateForm() {
        if (productNameField.getText() == null || productNameField.getText().trim().isEmpty()) {
            return ValidationResult.invalid("Product name is required.");
        }

        if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
            return ValidationResult.invalid("Description is required.");
        }

        if (categoryField.getValue() == null || categoryField.getValue().isBlank()) {
            return ValidationResult.invalid("Category is required.");
        }

        if (startingPriceField.getText() == null || startingPriceField.getText().trim().isEmpty()) {
            return ValidationResult.invalid("Starting price is required.");
        }

        try {
            double startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            if (startingPrice <= 0) {
                return ValidationResult.invalid("Starting price must be greater than 0.");
            }

            if (reservePriceField.getText() != null && !reservePriceField.getText().trim().isEmpty()) {
                double reservePrice = Double.parseDouble(reservePriceField.getText().trim());
                if (reservePrice < 0) {
                    return ValidationResult.invalid("Reserve price cannot be negative.");
                }
                if (reservePrice < startingPrice) {
                    return ValidationResult.invalid("Reserve price cannot be smaller than starting price.");
                }
            }
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Price must be a valid number.");
        }

        if (startDatePicker.getValue() == null) {
            return ValidationResult.invalid("Start date is required.");
        }

        if (endDatePicker.getValue() == null) {
            return ValidationResult.invalid("End date is required.");
        }

        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            return ValidationResult.invalid("End date cannot be earlier than start date.");
        }

        return ValidationResult.valid();
    }

    private String getCategoryValue() {
        return categoryField.getValue() == null ? "Art" : categoryField.getValue();
    }

    private double getReservePriceValue() {
        String reservePriceText = reservePriceField.getText();
        if (reservePriceText == null || reservePriceText.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(reservePriceText.trim());
    }

    private List<String> resolveImageUrlsForSave() {
        int primaryIndex = primaryImageChoice.getSelectionModel().getSelectedIndex();
        List<String> imageUrls = new ArrayList<>();

        for (SelectedImage selectedImage : selectedImages) {
            if (selectedImage.isRemote()) {
                imageUrls.add(selectedImage.remoteUrl());
                continue;
            }

            try {
                String imageUrl = sellerItemApiService.uploadImage(selectedImage.file());
                if (imageUrl != null && !imageUrl.isBlank()) {
                    imageUrls.add(imageUrl);
                }
            } catch (Exception e) {
                imageUploadSkipped = true;
            }
        }

        if (primaryIndex > 0 && primaryIndex < imageUrls.size()) {
            Collections.swap(imageUrls, 0, primaryIndex);
        }

        return imageUrls;
    }

    private void refreshImageUi(int selectedIndex) {
        primaryImageChoice.getItems().clear();
        for (SelectedImage selectedImage : selectedImages) {
            primaryImageChoice.getItems().add(selectedImage.label());
        }

        imagePreviewStrip.getChildren().clear();
        for (int i = 0; i < selectedImages.size(); i++) {
            imagePreviewStrip.getChildren().add(buildPreviewImage(selectedImages.get(i).toImage(), i));
        }

        selectedImageLabel.setText(selectedImages.size() + " image(s) selected");

        if (selectedImages.isEmpty()) {
            productImagePreview.setImage(null);
            return;
        }

        int safeIndex = selectedIndex < 0 ? selectedImages.size() - 1 : Math.min(selectedIndex, selectedImages.size() - 1);
        primaryImageChoice.getSelectionModel().select(safeIndex);
        updateLargePreview(safeIndex);
    }

    private void updateLargePreview(int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex >= selectedImages.size()) {
            productImagePreview.setImage(null);
            return;
        }

        productImagePreview.setImage(selectedImages.get(selectedIndex).toImage());
    }

    private ImageView buildPreviewImage(Image image, int index) {
        ImageView preview = new ImageView(image);
        preview.setFitWidth(62);
        preview.setFitHeight(48);
        preview.setPreserveRatio(false);
        preview.getStyleClass().add("seller-image-thumb");
        preview.setOnMouseClicked(event -> primaryImageChoice.getSelectionModel().select(index));
        return preview;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            String normalized = value.length() >= 10 ? value.substring(0, 10) : value;
            return LocalDate.parse(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #dc2626;");
        messageLabel.setManaged(true);
        messageLabel.setVisible(true);
    }

    private void hideMessage() {
        messageLabel.setText("");
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        boolean isValid() {
            return valid;
        }

        String getMessage() {
            return message;
        }
    }

    private class SelectedImage {
        private final File file;
        private final String remoteUrl;

        private SelectedImage(File file, String remoteUrl) {
            this.file = file;
            this.remoteUrl = remoteUrl;
        }

        boolean isRemote() {
            return remoteUrl != null && !remoteUrl.isBlank();
        }

        File file() {
            return file;
        }

        String remoteUrl() {
            return remoteUrl;
        }

        String label() {
            if (file != null) {
                return file.getName();
            }

            String value = safeText(remoteUrl, "Saved image");
            int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
            return slash >= 0 && slash < value.length() - 1 ? value.substring(slash + 1) : value;
        }

        Image toImage() {
            if (file != null) {
                return new Image(file.toURI().toString(), true);
            }
            return new Image(sellerItemApiService.toAbsoluteImageUrl(remoteUrl), true);
        }
    }
}
