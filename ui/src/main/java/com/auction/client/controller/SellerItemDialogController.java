package com.auction.client.controller;

import com.auction.client.dto.request.CreateItemRequest;
import com.auction.client.dto.request.UpdateItemRequest;
import com.auction.client.model.SellerListing;
import com.auction.client.service.SellerItemApiService;
import com.auction.client.session.SessionManager;
import javafx.application.Platform;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
    @FXML private ComboBox<String> startHourComboBox;
    @FXML private ComboBox<String> startMinuteComboBox;
    @FXML private ComboBox<String> endHourComboBox;
    @FXML private ComboBox<String> endMinuteComboBox;
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
    private static final ZoneId APP_ZONE = ZoneId.systemDefault();

    @FXML
    public void initialize() {
        categoryField.getItems().setAll("Art", "Jewellery", "Watches", "Fashion");
        populateTimeChoices(startHourComboBox, 24);
        populateTimeChoices(endHourComboBox, 24);
        populateTimeChoices(startMinuteComboBox, 60);
        populateTimeChoices(endMinuteComboBox, 60);
        selectDefaultTimes();
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
        deleteButton.setDisable(!listing.isDeletable());
        if (!listing.isDeletable()) {
            deleteButton.setTooltip(new javafx.scene.control.Tooltip(resolveDeleteBlockedReason()));
        } else {
            deleteButton.setTooltip(null);
        }

        productNameField.setText(safeText(listing.getProductName(), ""));
        descriptionArea.setText(safeText(listing.getDescription(), ""));
        categoryField.getSelectionModel().select(safeText(listing.getCategory(), "Art"));
        startingPriceField.setText(safeText(listing.getStartingPrice(), ""));
        reservePriceField.setText(safeText(listing.getReservePrice(), ""));
        startDatePicker.setValue(parseDate(listing.getStartDate()));
        endDatePicker.setValue(parseDate(listing.getEndDate()));
        applySavedTime(listing.getStartDate(), true);
        applySavedTime(listing.getEndDate(), false);

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

        // Snapshot every value read from JavaFX controls on the UI thread,
        // then do all network work in the background so the dialog never freezes.
        final UUID sellerId = SessionManager.getUserId();
        final String productName = productNameField.getText().trim();
        final String description = descriptionArea.getText().trim();
        final String category = getCategoryValue();
        final double startingPrice = Double.parseDouble(startingPriceField.getText().trim());
        final double reservePrice = getReservePriceValue();
        final String startIso = buildIsoDateTime(startDatePicker.getValue(), true);
        final String endIso = buildIsoDateTime(endDatePicker.getValue(), false);
        final int primaryIndex = primaryImageChoice.getSelectionModel().getSelectedIndex();
        final List<SelectedImage> imagesSnapshot = new ArrayList<>(selectedImages);
        final SellerListing editing = listing;

        setBusy(true);

        CompletableFuture.runAsync(() -> {
            imageUploadSkipped = false;
            List<String> imageUrls = resolveImageUrlsForSave(imagesSnapshot, primaryIndex);
            String imagePath = imageUrls.isEmpty() ? "" : imageUrls.get(0);

            if (editing == null) {
                CreateItemRequest request = new CreateItemRequest(
                        sellerId, productName, description, category,
                        startingPrice, reservePrice, startIso, endIso,
                        imagePath, imageUrls, "", 1
                );
                sellerItemApiService.createItem(request);
            } else {
                UpdateItemRequest request = new UpdateItemRequest(
                        sellerId, productName, description, category,
                        startingPrice, reservePrice, startIso, endIso,
                        imagePath, imageUrls,
                        safeText(editing.getSku(), ""),
                        editing.getQuantity() == null ? 1 : editing.getQuantity()
                );
                sellerItemApiService.updateItem(editing.getId(), request);
            }
        }).whenComplete((ignored, error) -> Platform.runLater(() -> {
            setBusy(false);
            if (error != null) {
                showError("Save failed: " + rootMessage(error));
                return;
            }
            if (onSaved != null) {
                onSaved.run();
            }
            handleClose();
        }));
    }

    @FXML
    private void handleDelete() {
        if (listing == null || listing.getId() == null) {
            return;
        }

        if (!listing.isDeletable()) {
            showError(resolveDeleteBlockedReason());
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete listing");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this listing?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        final UUID itemId = listing.getId();
        setBusy(true);

        CompletableFuture.runAsync(() -> sellerItemApiService.deleteItem(itemId))
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    setBusy(false);
                    if (error != null) {
                        showError("Delete failed: " + rootMessage(error));
                        return;
                    }
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    handleClose();
                }));
    }

    private void setBusy(boolean busy) {
        if (submitButton != null) {
            submitButton.setDisable(busy);
        }
        if (deleteButton != null && (listing == null || listing.isDeletable())) {
            deleteButton.setDisable(busy);
        }
        if (busy) {
            messageLabel.setText("Saving, please wait...");
            messageLabel.setStyle("-fx-text-fill: #2563eb;");
            messageLabel.setManaged(true);
            messageLabel.setVisible(true);
        }
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
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

        if (endDatePicker.getValue().isEqual(startDatePicker.getValue())) {
            LocalDateTime startDateTime = buildLocalDateTime(startDatePicker.getValue(), true);
            LocalDateTime endDateTime = buildLocalDateTime(endDatePicker.getValue(), false);
            if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
                return ValidationResult.invalid("End time must be later than start time.");
            }
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

    private List<String> resolveImageUrlsForSave(List<SelectedImage> images, int primaryIndex) {
        List<String> imageUrls = new ArrayList<>();

        for (SelectedImage selectedImage : images) {
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

    private void populateTimeChoices(ComboBox<String> comboBox, int limit) {
        if (comboBox == null) {
            return;
        }

        List<String> values = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            values.add(String.format("%02d", i));
        }
        comboBox.getItems().setAll(values);
    }

    private void selectDefaultTimes() {
        selectTime(startHourComboBox, "00");
        selectTime(startMinuteComboBox, "00");
        selectTime(endHourComboBox, "23");
        selectTime(endMinuteComboBox, "59");
    }

    private void applySavedTime(String value, boolean startField) {
        String defaultHour = startField ? "00" : "23";
        String defaultMinute = startField ? "00" : "59";

        ComboBox<String> hourBox = startField ? startHourComboBox : endHourComboBox;
        ComboBox<String> minuteBox = startField ? startMinuteComboBox : endMinuteComboBox;

        if (value == null || value.isBlank()) {
            selectTime(hourBox, defaultHour);
            selectTime(minuteBox, defaultMinute);
            return;
        }

        try {
            Instant instant = Instant.parse(value);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, APP_ZONE);
            selectTime(hourBox, String.format("%02d", dateTime.getHour()));
            selectTime(minuteBox, String.format("%02d", dateTime.getMinute()));
            return;
        } catch (Exception ignored) {
        }

        selectTime(hourBox, defaultHour);
        selectTime(minuteBox, defaultMinute);
    }

    private void selectTime(ComboBox<String> comboBox, String value) {
        if (comboBox == null) {
            return;
        }

        comboBox.getSelectionModel().select(value);
    }

    private String buildIsoDateTime(LocalDate date, boolean startField) {
        return buildLocalDateTime(date, startField).atZone(APP_ZONE).toInstant().toString();
    }

    private LocalDateTime buildLocalDateTime(LocalDate date, boolean startField) {
        int hour = parseTimeValue(startField ? startHourComboBox : endHourComboBox, startField ? "00" : "23");
        int minute = parseTimeValue(startField ? startMinuteComboBox : endMinuteComboBox, startField ? "00" : "59");
        return date.atTime(hour, minute, startField ? 0 : 59);
    }

    private int parseTimeValue(ComboBox<String> comboBox, String fallback) {
        String value = comboBox == null || comboBox.getValue() == null || comboBox.getValue().isBlank()
                ? fallback
                : comboBox.getValue();
        return Integer.parseInt(value);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveDeleteBlockedReason() {
        if (listing == null) {
            return "This listing cannot be deleted.";
        }

        String reason = listing.getDeleteBlockedReason();
        if (reason != null && !reason.isBlank()) {
            return reason;
        }

        return "This listing already has an auction, so the seller cannot delete it.";
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
