package com.auction.client.controller;

import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.PublicItemDetailResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.service.ItemApiService;
import com.auction.client.util.MockData;
import com.auction.client.dto.response.WalletBalanceResponse;
import com.auction.client.service.WalletApiService;
import com.auction.client.dto.response.BidPlacementResponse;
import com.auction.client.dto.response.AutoBidResponse;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProductDetailController {

    @FXML private ImageView mainImageView;
    @FXML private ImageView thumb1ImageView;
    @FXML private ImageView thumb2ImageView;

    @FXML private Label productNameLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label countdownLabel;
    @FXML private Label countdownDayLabel;
    @FXML private Label countdownHourLabel;
    @FXML private Label countdownMinuteLabel;
    @FXML private Label countdownSecondLabel;
    @FXML private Label statusLabel;
    @FXML private Label specsLabel;
    @FXML private Label bidMessageLabel;
    @FXML private Label detailUsernameLabel;
    @FXML private Label topUsernameLabel;
    @FXML private Label balanceValueLabel;
    @FXML private Label aboutProductLabel;
    @FXML private Label lotIdValueLabel;
    @FXML private Label categoryValueLabel;
    @FXML private Label conditionValueLabel;
    @FXML private Label estimatedValueLabel;

    @FXML private TextField bidAmountField;
    @FXML private Button detailFavoriteButton;
    @FXML private Button placeBidButton;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final WalletApiService walletApiService = new WalletApiService();

    private AuctionItem selectedItem;
    private AuctionListResponse currentAuction;
    private PublicItemDetailResponse currentItemDetail;
    private boolean favoriteSelected = false;
    private int favoriteCount = 36;
    private Timeline countdownTimeline;
    private Timeline auctionRefreshTimeline;
    private Instant countdownEndInstant;
    private boolean detailLoading;
    private Double activeAutoBidStep;
    private AutoBidResponse currentUserAutoBid;
    private String mainImageKey;
    private String thumb1ImageKey;
    private String thumb2ImageKey;

    @FXML
    public void initialize() {
        bindSessionUsername();
        loadWalletBalance();
        selectedItem = MockData.getSelectedItem();

        if (selectedItem == null) {
            showEmptyState();
            hideBidMessage();
            return;
        }

        loadAuctionDetail();
        hideBidMessage();
        startAuctionRefreshTimer();
    }
    private void loadAuctionDetail() {
        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            return;
        }

        try {
            AuctionListResponse response = auctionApiService.getAuctionById(selectedItem.getId());
            currentAuction = response;

            try {
                auctionApiService.trackView(selectedItem.getId());
            } catch (Exception ignored) {
            }

            bindDetailFromApi(response);
            refreshCurrentUserAutoBidState();

        } catch (Exception e) {
            bindFallbackFromSelectedItem();
            showBidMessage("Cannot load full detail from server. Showing fallback data.");
        }
    }

    private void refreshAuctionDetailSilently() {
        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank() || detailLoading) {
            return;
        }

        detailLoading = true;

        CompletableFuture.supplyAsync(() -> auctionApiService.getAuctionById(selectedItem.getId()))
                .thenAccept(response -> Platform.runLater(() -> {
                    try {
                        currentAuction = response;
                        bindDetailFromApi(response);
                    } finally {
                        detailLoading = false;
                    }
                }))
                .exceptionally(error -> {
                    detailLoading = false;
                    return null;
                });
    }

    private void bindDetailFromApi(AuctionListResponse response) {
        String title = firstNonBlank(
                response.getTitle(),
                response.getItemName(),
                "Unnamed Auction"
        );

        productNameLabel.setText(title);
        currentBidLabel.setText(formatMoney(response.getCurrentPrice()));
        updateCountdown(response.getEndTime());

        String sellerName = firstNonBlank(response.getSellerName(), SessionManager.getUsername(), "Seller");
        detailUsernameLabel.setText(sellerName);

        long count = response.getFavoriteCount() > 0 ? response.getFavoriteCount() : favoriteCount;
        favoriteCount = (int) count;
        detailFavoriteButton.setText((favoriteSelected ? "\u2665 " : "\u2661 ") + favoriteCount);

        statusLabel.setText(firstNonBlank(response.getState(), "No reserve price"));

        double low = response.getCurrentPrice();
        double high = resolveDisplayedHighEstimate(response.getMinNextBid(), low);
        estimatedValueLabel.setText(formatMoney(low) + " - " + formatMoney(high));

        lotIdValueLabel.setText(shortId(response.getId() == null ? null : response.getId().toString()));
        categoryValueLabel.setText(firstNonBlank(response.getCategory(), "Collectibles"));
        conditionValueLabel.setText("Good");

        aboutProductLabel.setText(firstNonBlank(
                response.getDescription(),
                "A beautiful collection item presented for live auction."
        ));

        bindPublicItemDetail(response);
    }

    private void bindFallbackFromSelectedItem() {
        productNameLabel.setText(selectedItem.getName());
        currentBidLabel.setText(selectedItem.getCurrentBid());
        countdownLabel.setText("Ends: " + selectedItem.getTimeLeft());
        setCountdownParts(0);
        statusLabel.setText("No reserve price");

        specsLabel.setText(
                "Auction Detail:\\n" +
                        "- Backend detail is not available right now\\n" +
                        "- Showing selected item data from showroom\\n" +
                        "- Full specs will be added when backend returns more fields"
        );

        setDefaultImages(selectedItem.getImagePath());
    }

    private void bindPublicItemDetail(AuctionListResponse auction) {
        if (auction.getItemId() == null) {
            bindAuctionSpecsOnly(auction);
            setDefaultImages(selectedItem.getImagePath());
            return;
        }

        try {
            PublicItemDetailResponse item = itemApiService.getPublicItemDetail(auction.getItemId().toString());
            currentItemDetail = item;

            if (item.getProductName() != null && !item.getProductName().isBlank()) {
                productNameLabel.setText(item.getProductName());
            }

            aboutProductLabel.setText(firstNonBlank(
                    item.getDescription(),
                    auction.getDescription(),
                    "A beautiful collection item presented for live auction."
            ));

            categoryValueLabel.setText(firstNonBlank(
                    item.getCategory(),
                    auction.getCategory(),
                    "Collectibles"
            ));

            lotIdValueLabel.setText(shortId(
                    item.getId() == null ? auction.getId().toString() : item.getId().toString()
            ));

            conditionValueLabel.setText(firstNonBlank(item.getStatus(), "Good"));

            specsLabel.setText(
                    "Item Detail:\n" +
                            "- Category: " + safeText(item.getCategory(), "General") + "\n" +
                            "- Description: " + safeText(item.getDescription(), "No description") + "\n" +
                            "- SKU: " + safeText(item.getSku(), "N/A") + "\n" +
                            "- Quantity: " + (item.getQuantity() == null ? "N/A" : item.getQuantity()) + "\n" +
                            "- Seller ID: " + safeText(item.getSellerId() == null ? null : item.getSellerId().toString(), "N/A") + "\n\n" +
                            auctionSpecs(auction)
            );

            setUploadedImages(item);

        } catch (Exception e) {
            bindAuctionSpecsOnly(auction);
            setDefaultImages(selectedItem.getImagePath());
        }
    }

    private void bindAuctionSpecsOnly(AuctionListResponse auction) {
        specsLabel.setText(auctionSpecs(auction));
    }

    private String auctionSpecs(AuctionListResponse auction) {
        return "Auction Detail:\n" +
                "- Auction ID: " + safeText(auction.getId() == null ? null : auction.getId().toString(), "N/A") + "\n" +
                "- Item ID: " + safeText(auction.getItemId() == null ? null : auction.getItemId().toString(), "N/A") + "\n" +
                "- Start Time: " + formatDateTime(auction.getStartTime()) + "\n" +
                "- End Time: " + formatDateTime(auction.getEndTime()) + "\n" +
                "- Leader ID: " + safeText(auction.getLeaderId() == null ? null : auction.getLeaderId().toString(), "No leader yet");
    }

    private void setUploadedImages(PublicItemDetailResponse item) {
        List<String> images = new ArrayList<>();
        if (item.getImageUrls() != null) {
            images.addAll(item.getImageUrls());
        }
        if (images.isEmpty() && item.getImagePath() != null && !item.getImagePath().isBlank()) {
            images.add(item.getImagePath());
        }
        if (images.isEmpty()) {
            setDefaultImages(selectedItem.getImagePath());
            return;
        }

        setRemoteImage(mainImageView, images.get(0), "main");
        setRemoteImage(thumb1ImageView, images.get(0), "thumb1");
        setRemoteImage(thumb2ImageView, images.size() > 1 ? images.get(1) : images.get(0), "thumb2");
    }

    private void setRemoteImage(ImageView imageView, String imagePath, String slot) {
        if (!shouldUpdateImage(slot, imagePath)) {
            return;
        }

        imageView.setImage(itemApiService.loadImage(imagePath));
        rememberImageKey(slot, imagePath);
    }

    private void setDefaultImages(String imagePath) {
        try {
            if (itemApiService.isRemoteImagePath(imagePath)) {
                setRemoteImage(mainImageView, imagePath, "main");
                setRemoteImage(thumb1ImageView, imagePath, "thumb1");
                setRemoteImage(thumb2ImageView, imagePath, "thumb2");
                return;
            }

            if (!shouldUpdateImage("main", imagePath)
                    && !shouldUpdateImage("thumb1", imagePath)
                    && !shouldUpdateImage("thumb2", imagePath)) {
                return;
            }

            Image image = itemApiService.loadImage(imagePath);
            if (shouldUpdateImage("main", imagePath)) {
                mainImageView.setImage(image);
                rememberImageKey("main", imagePath);
            }
            if (shouldUpdateImage("thumb1", imagePath)) {
                thumb1ImageView.setImage(image);
                rememberImageKey("thumb1", imagePath);
            }
            if (shouldUpdateImage("thumb2", imagePath)) {
                thumb2ImageView.setImage(image);
                rememberImageKey("thumb2", imagePath);
            }

        } catch (Exception e) {
            clearImage(mainImageView, "main");
            clearImage(thumb1ImageView, "thumb1");
            clearImage(thumb2ImageView, "thumb2");
            System.out.println("Image not found: " + imagePath);
        }
    }

    private boolean shouldUpdateImage(String slot, String nextKey) {
        String currentKey = switch (slot) {
            case "main" -> mainImageKey;
            case "thumb1" -> thumb1ImageKey;
            case "thumb2" -> thumb2ImageKey;
            default -> null;
        };

        if (currentKey == null) {
            return true;
        }

        return !currentKey.equals(nextKey);
    }

    private void rememberImageKey(String slot, String key) {
        switch (slot) {
            case "main" -> mainImageKey = key;
            case "thumb1" -> thumb1ImageKey = key;
            case "thumb2" -> thumb2ImageKey = key;
            default -> {
            }
        }
    }

    private void clearImage(ImageView imageView, String slot) {
        imageView.setImage(null);
        rememberImageKey(slot, null);
    }

    private String formatDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }

        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(value));
        } catch (Exception ignored) {
        }

        if (value.length() >= 16) {
            return value.substring(0, 16).replace("T", " ");
        }

        return value;
    }

    private void bindSessionUsername() {
        String username = safeText(SessionManager.getUsername(), "Bidder");
        detailUsernameLabel.setText(username);
        topUsernameLabel.setText(username);
    }

    private void updateCountdown(String endTime) {
        countdownLabel.setText("Ends: " + formatDateTime(endTime));
        countdownEndInstant = parseEndInstant(endTime);
        refreshCountdown();
        startCountdownTimer();
    }

    private long resolveRemainingSeconds(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return 0;
        }

        try {
            Instant auctionEnd = Instant.parse(endTime);
            return Math.max(0, java.time.Duration.between(Instant.now(), auctionEnd).getSeconds());
        } catch (Exception ignored) {
        }

        try {
            String normalizedEndTime = endTime.trim().replace(" ", "T");
            if (normalizedEndTime.length() > 19) {
                normalizedEndTime = normalizedEndTime.substring(0, 19);
            }

            LocalDateTime auctionEnd = LocalDateTime.parse(normalizedEndTime);
            return Math.max(0, java.time.Duration.between(LocalDateTime.now(), auctionEnd).getSeconds());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void setCountdownParts(long remainingSeconds) {
        long days = remainingSeconds / 86400;
        long hours = remainingSeconds % 86400 / 3600;
        long minutes = remainingSeconds % 3600 / 60;
        long seconds = remainingSeconds % 60;

        countdownDayLabel.setText(formatCountdownPart(days));
        countdownHourLabel.setText(formatCountdownPart(hours));
        countdownMinuteLabel.setText(formatCountdownPart(minutes));
        countdownSecondLabel.setText(formatCountdownPart(seconds));
    }

    private String formatCountdownPart(long value) {
        return String.format("%02d", value);
    }

    private void showEmptyState() {
        productNameLabel.setText("No selected item");
        currentBidLabel.setText("-");
        countdownLabel.setText("Ends: -");
        setCountdownParts(0);
        statusLabel.setText("No reserve price");
        specsLabel.setText("Please go back to the showroom and choose an auction item.");
        clearImage(mainImageView, "main");
        clearImage(thumb1ImageView, "thumb1");
        clearImage(thumb2ImageView, "thumb2");
    }

    @FXML
    private void handleBack() {
        stopAuctionRefreshTimer();
        stopCountdownTimer();
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleJoinLiveBidding() {
        System.out.println("JOIN LIVE BIDDING CLICKED");

        if (selectedItem == null) {
            System.out.println("selectedItem is null");
            showBidMessage("No selected auction.");
            return;
        }

        System.out.println("selectedItem id = " + selectedItem.getId());
        stopAuctionRefreshTimer();
        stopCountdownTimer();
        SceneManager.goToLiveBidding();
    }

    @FXML
    private void handleOpenBidDialog() {
        if (selectedItem == null || currentAuction == null) {
            showBidMessage("Auction data is unavailable.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bid_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Place bid");
            stage.initModality(Modality.WINDOW_MODAL);

            if (productNameLabel != null && productNameLabel.getScene() != null) {
                stage.initOwner(productNameLabel.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            addDialogStyles(scene);

            BidDialogController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setAuction(currentAuction, this::applyBidPlacementToDetail);

            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            showBidMessage("Cannot open bid dialog: " + e.getMessage());
        }
    }

    private AuctionListResponse resolveAuctionForDialog() {
        if (currentAuction != null) {
            return currentAuction;
        }

        AuctionListResponse demoAuction = new AuctionListResponse();
        demoAuction.setTitle(selectedItem == null ? "Demo Auction" : selectedItem.getName());

        double currentPrice = parseMoneyForDialog(
                currentBidLabel == null ? "0" : currentBidLabel.getText()
        );

        demoAuction.setCurrentPrice(currentPrice);
        demoAuction.setMinNextBid(currentPrice + 50);

        return demoAuction;
    }

    private void applyAutoBidToDetail(AutoBidResponse response) {
        if (response == null) {
            return;
        }

        currentUserAutoBid = response;
        if (response.getBidStep() > 0) {
            activeAutoBidStep = response.getBidStep();
        }
        updateAutoBidActionButton();

        if (currentAuction != null) {
            double low = currentAuction.getCurrentPrice();
            double high = resolveDisplayedHighEstimate(currentAuction.getMinNextBid(), low);
            estimatedValueLabel.setText(formatMoney(low) + " - " + formatMoney(high));
        }

        String stepText = response.getBidStep() > 0
                ? " with step " + formatMoney(response.getBidStep())
                : "";
        showBidSuccess("Auto-Bid is running up to " + formatMoney(response.getMaxAmount()) + stepText + ".");
    }

    private void applyBidPlacementToDetail(BidPlacementResponse response) {
        if (response == null) {
            return;
        }

        if (currentAuction != null) {
            currentAuction.setCurrentPrice(response.getCurrentPrice());
            currentAuction.setMinNextBid(response.getMinNextBid());
            currentAuction.setState(response.getState());
            currentAuction.setEndTime(response.getEndTime());
        }

        currentBidLabel.setText(formatMoney(response.getCurrentPrice()));
        statusLabel.setText(firstNonBlank(response.getState(), "Bid placed"));

        if (response.getEndTime() != null && !response.getEndTime().isBlank()) {
            updateCountdown(response.getEndTime());
        }

        double low = response.getCurrentPrice();
        double high = resolveDisplayedHighEstimate(response.getMinNextBid(), low);
        estimatedValueLabel.setText(formatMoney(low) + " - " + formatMoney(high));

        showBidSuccess("Bid placed successfully.");
    }

    @FXML
    private void handleGoToForYou() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleGoToTrending() {
        SceneManager.goToTrending();
    }

    @FXML
    private void handleOpenWallet() {
        SceneManager.goToWallet();
    }

    @FXML
    private void handleGoToArt() {
        SceneManager.goToCategory("Art");
    }

    @FXML
    private void handleGoToJewellery() {
        SceneManager.goToCategory("Jewellery");
    }

    @FXML
    private void handleGoToWatches() {
        SceneManager.goToCategory("Watches");
    }

    @FXML
    private void handleGoToFashion() {
        SceneManager.goToCategory("Fashion");
    }

    @FXML
    private void handleLogout() {
        stopAuctionRefreshTimer();
        stopCountdownTimer();
        clearCurrentUserAutoBidState(false);
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleOpenAutoBid() {
        if (selectedItem == null) {
            showBidMessage("No selected auction.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auto_bid_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Auto-Bid");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Window ownerWindow = resolveDialogOwnerWindow();
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }

            Scene scene = createOverlayDialogScene(root, ownerWindow);

            AutoBidDialogController controller = loader.getController();

            AuctionListResponse auctionForDialog = resolveAuctionForDialog();
            boolean demoMode = currentAuction == null || currentAuction.getId() == null;

            controller.setDialogStage(stage);
            controller.setAuction(
                    auctionForDialog,
                    demoMode,
                    currentUserAutoBid,
                    this::applyAutoBidToDetail,
                    this::applyAutoBidDisabledToDetail
            );

            stage.setScene(scene);
            positionOverlayDialogStage(stage, ownerWindow);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            showBidMessage("Cannot open auto-bid dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewChart() {
        if (selectedItem == null) {
            showBidMessage("No selected auction.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bid_history_dialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Bid History");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            Window ownerWindow = resolveDialogOwnerWindow();
            if (ownerWindow != null) {
                stage.initOwner(ownerWindow);
            }

            Scene scene = createOverlayDialogScene(root, ownerWindow);

            BidHistoryDialogController controller = loader.getController();

            AuctionListResponse auctionForDialog = resolveAuctionForDialog();
            boolean demoMode = currentAuction == null || currentAuction.getId() == null;

            controller.setDialogStage(stage);
            controller.setAuction(auctionForDialog, demoMode);

            stage.setScene(scene);
            positionOverlayDialogStage(stage, ownerWindow);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            showBidMessage("Cannot open bid history: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleFavorite() {
        favoriteSelected = !favoriteSelected;

        if (favoriteSelected) {
            detailFavoriteButton.setText("\u2665 " + favoriteCount);
            detailFavoriteButton.getStyleClass().add("detail-favorite-active");
        } else {
            detailFavoriteButton.setText("\u2661 " + favoriteCount);
            detailFavoriteButton.getStyleClass().remove("detail-favorite-active");
        }
    }
    @FXML
    private void handlePlaceBid() {
        hideBidMessage();

        String bidText = bidAmountField.getText().trim();

        if (bidText.isEmpty()) {
            showBidMessage("Please enter a bid amount.");
            return;
        }

        String numericText = bidText.replaceAll("[^0-9]", "");

        if (numericText.isEmpty()) {
            showBidMessage("Bid amount must be numeric.");
            return;
        }

        int enteredBid = Integer.parseInt(numericText);

        String currentBidText = currentBidLabel.getText().replaceAll("[^0-9]", "");
        if (currentBidText.isEmpty()) {
            showBidMessage("Current bid is unavailable.");
            return;
        }

        int currentBid = Integer.parseInt(currentBidText);

        if (enteredBid <= currentBid) {
            showBidMessage("Your bid must be higher than the current highest bid.");
            return;
        }

        showBidSuccess("Quick bid validation passed (detail screen only). Real bid will be in Block 5.");
    }

    private void loadWalletBalance() {
        if (balanceValueLabel == null) {
            return;
        }

        if (!SessionManager.isAuthenticated()) {
            balanceValueLabel.setText(formatMoney(BigDecimal.ZERO));
            return;
        }

        try {
            WalletBalanceResponse balance = walletApiService.getBalance();
            balanceValueLabel.setText(formatMoney(balance.getBalance()));
        } catch (Exception e) {
            balanceValueLabel.setText(formatMoney(BigDecimal.ZERO));
        }
    }

    private String formatMoney(double value) {
        return "USD " + String.format("%,.0f", value);
    }

    private String formatMoney(BigDecimal value) {
        return "USD " + (value == null ? "0" : String.format("%,.0f", value.doubleValue()));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "N/A";
        }

        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private void addDialogStyles(Scene scene) {
        addStylesheet(scene, "/css/app.css");
        addStylesheet(scene, "/css/components.css");
        addStylesheet(scene, "/css/product_detail.css");
    }

    private Window resolveDialogOwnerWindow() {
        return productNameLabel != null && productNameLabel.getScene() != null
                ? productNameLabel.getScene().getWindow()
                : null;
    }

    private Scene createOverlayDialogScene(Parent dialogCard, Window ownerWindow) {
        StackPane overlay = new StackPane(dialogCard);
        overlay.getStyleClass().add("modal-overlay");

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

    private void addStylesheet(Scene scene, String path) {
        try {
            String css = getClass().getResource(path).toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {
        }
    }

    private double parseMoneyForDialog(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        String normalized = text.replaceAll("[^0-9.]", "");

        if (normalized.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            return 0;
        }
    }

    private void showBidMessage(String message) {
        bidMessageLabel.setText(message);
        bidMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        bidMessageLabel.setManaged(true);
        bidMessageLabel.setVisible(true);
    }

    private void showBidSuccess(String message) {
        bidMessageLabel.setText(message);
        bidMessageLabel.setStyle("-fx-text-fill: #16a34a;");
        bidMessageLabel.setManaged(true);
        bidMessageLabel.setVisible(true);
    }

    private void hideBidMessage() {
        bidMessageLabel.setText("");
        bidMessageLabel.setManaged(false);
        bidMessageLabel.setVisible(false);
    }

    private void refreshCurrentUserAutoBidState() {
        if (!SessionManager.isAuthenticated() || selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            clearCurrentUserAutoBidState(false);
            return;
        }

        CompletableFuture.supplyAsync(() -> auctionApiService.listMyAutoBids())
                .thenAccept(autoBids -> Platform.runLater(() -> applyCurrentUserAutoBidState(autoBids)))
                .exceptionally(error -> null);
    }

    private void applyCurrentUserAutoBidState(List<AutoBidResponse> autoBids) {
        if (selectedItem == null || selectedItem.getId() == null) {
            clearCurrentUserAutoBidState(false);
            return;
        }

        AutoBidResponse matching = null;
        if (autoBids != null) {
            for (AutoBidResponse autoBid : autoBids) {
                if (autoBid == null || !autoBid.isActive()) {
                    continue;
                }
                if (selectedItem.getId().equals(autoBid.getAuctionId())) {
                    matching = autoBid;
                    break;
                }
            }
        }

        currentUserAutoBid = matching;
        activeAutoBidStep = matching == null || matching.getBidStep() <= 0 ? null : matching.getBidStep();
        updateAutoBidActionButton();

        if (currentAuction != null) {
            double low = currentAuction.getCurrentPrice();
            double high = resolveDisplayedHighEstimate(currentAuction.getMinNextBid(), low);
            estimatedValueLabel.setText(formatMoney(low) + " - " + formatMoney(high));
        }

        if (matching != null) {
            String stepText = matching.getBidStep() > 0
                    ? " with step " + formatMoney(matching.getBidStep())
                    : "";
            showBidSuccess("Auto-Bid is running up to " + formatMoney(matching.getMaxAmount()) + stepText + ".");
        }
    }

    private void applyAutoBidDisabledToDetail() {
        clearCurrentUserAutoBidState(true);
        showBidSuccess("Auto-Bid turned off.");
    }

    private void clearCurrentUserAutoBidState(boolean refreshEstimate) {
        currentUserAutoBid = null;
        activeAutoBidStep = null;
        updateAutoBidActionButton();

        if (refreshEstimate && currentAuction != null) {
            double low = currentAuction.getCurrentPrice();
            double high = resolveDisplayedHighEstimate(currentAuction.getMinNextBid(), low);
            estimatedValueLabel.setText(formatMoney(low) + " - " + formatMoney(high));
        }
    }

    private boolean hasActiveCurrentUserAutoBid() {
        return currentUserAutoBid != null && currentUserAutoBid.isActive();
    }

    private void updateAutoBidActionButton() {
        if (placeBidButton == null) {
            return;
        }

        placeBidButton.setDisable(false);
        placeBidButton.setText("Place Bid");
    }

    private String extractFriendlyMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "Unexpected error.";
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

    private void startCountdownTimer() {
        if (countdownTimeline == null) {
            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshCountdown()));
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        countdownTimeline.playFromStart();
    }

    private void stopCountdownTimer() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private void startAuctionRefreshTimer() {
        if (auctionRefreshTimeline == null) {
            auctionRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> refreshAuctionDetailSilently()));
            auctionRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        auctionRefreshTimeline.playFromStart();
    }

    private void stopAuctionRefreshTimer() {
        if (auctionRefreshTimeline != null) {
            auctionRefreshTimeline.stop();
        }
    }

    private void refreshCountdown() {
        if (countdownEndInstant == null) {
            setCountdownParts(0);
            return;
        }

        long remainingSeconds = Math.max(0, java.time.Duration.between(Instant.now(), countdownEndInstant).getSeconds());
        setCountdownParts(remainingSeconds);

        if (remainingSeconds <= 0) {
            stopCountdownTimer();
            if (statusLabel != null && (statusLabel.getText() == null || statusLabel.getText().isBlank())) {
                statusLabel.setText("FINISHED");
            }
        }
    }

    private Instant parseEndInstant(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(endTime);
        } catch (Exception ignored) {
        }

        try {
            String normalizedEndTime = endTime.trim().replace(" ", "T");
            if (normalizedEndTime.length() > 19) {
                normalizedEndTime = normalizedEndTime.substring(0, 19);
            }
            return LocalDateTime.parse(normalizedEndTime).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    private double resolveDisplayedHighEstimate(double minNextBid, double currentPrice) {
        if (activeAutoBidStep != null && activeAutoBidStep > 0) {
            return currentPrice + activeAutoBidStep;
        }

        if (minNextBid > currentPrice) {
            return minNextBid;
        }

        return currentPrice + 100;
    }
}
