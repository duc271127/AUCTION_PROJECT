package com.auction.client.controller;

import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.PublicItemDetailResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.service.ItemApiService;
import com.auction.client.util.MockData;
import com.auction.client.util.AuctionStateViewHelper;
import com.auction.client.util.FavoriteUiStateStore;
import com.auction.client.util.SearchNavigationContext;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ProductDetailController {

    @FXML private ImageView mainImageView;
    @FXML private javafx.scene.layout.HBox thumbnailStrip;

    @FXML private Label productNameLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label reservePriceLabel;
    @FXML private Label countdownLabel;
    @FXML private Label timeCardTitleLabel;
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
    @FXML private Label winnerNoticeTitleLabel;
    @FXML private Label winnerNoticeSubtitleLabel;

    @FXML private TextField bidAmountField;
    @FXML private TextField searchField;
    @FXML private Button detailFavoriteButton;
    @FXML private Button placeBidButton;
    @FXML private Button autoBidButton;
    @FXML private Button winnerNoticeButton;
    @FXML private javafx.scene.layout.VBox winnerNoticeCard;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final WalletApiService walletApiService = new WalletApiService();

    private AuctionItem selectedItem;
    private AuctionListResponse currentAuction;
    private PublicItemDetailResponse currentItemDetail;
    private String loadedItemDetailId;
    private boolean favoriteSelected = false;
    private boolean favoriteDirty = false;
    private int favoriteCount = 0;
    private Timeline countdownTimeline;
    private Timeline auctionRefreshTimeline;
    private Instant countdownEndInstant;
    private boolean detailLoading;
    private Double activeAutoBidStep;
    private AutoBidResponse currentUserAutoBid;
    private String mainImageKey;
    private final List<String> galleryPaths = new ArrayList<>();
    private boolean walletBalanceLoaded;
    private boolean auctionCloseRefreshTriggered;
    private BigDecimal lastKnownWalletBalance = BigDecimal.ZERO;
    private String pendingWinnerBalanceAuctionId;
    private BigDecimal pendingWinnerBalanceValue;
    private Timeline winnerBalanceRefreshTimeline;
    private int winnerBalanceRefreshAttemptsRemaining;

    @FXML
    public void initialize() {
        bindSessionUsername();
        loadWalletBalance();
        renderFavoriteButton();
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

    // Initial load runs off the JavaFX thread so opening a product never freezes
    // the UI while the auction + item detail are fetched.
    private void loadAuctionDetail() {
        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            return;
        }

        final String auctionId = selectedItem.getId();

        CompletableFuture.runAsync(() -> {
            try {
                auctionApiService.trackView(auctionId);
            } catch (Exception ignored) {
            }
        });

        CompletableFuture.supplyAsync(() -> auctionApiService.getAuctionById(auctionId))
                .thenAccept(response -> Platform.runLater(() -> {
                    currentAuction = response;
                    bindDetailFromApi(response);
                    refreshCurrentUserAutoBidState();
                }))
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        bindFallbackFromSelectedItem();
                        showBidMessage("Cannot load full detail from server. Showing fallback data.");
                    });
                    return null;
                });
    }

    private void refreshAuctionDetailSilently() {
        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank() || detailLoading) {
            return;
        }

        detailLoading = true;

        CompletableFuture.supplyAsync(() -> auctionApiService.getAuctionById(selectedItem.getId()))
                .thenAccept(response -> Platform.runLater(() -> {
                    try {
                        boolean wasClosed = isAuctionClosed(currentAuction);
                        currentAuction = response;
                        bindDetailFromApi(response);
                        if (!wasClosed && isAuctionClosed(response) && SessionManager.isAuthenticated()) {
                            handleAuctionClosureWalletUpdate(response);
                        }
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
        String auctionId = response.getId() == null ? null : response.getId().toString();
        FavoriteUiStateStore.FavoriteState storedFavorite = FavoriteUiStateStore.get(auctionId);
        String title = firstNonBlank(
                response.getTitle(),
                response.getItemName(),
                "Unnamed Auction"
        );

        productNameLabel.setText(title);
        currentBidLabel.setText(formatMoney(response.getCurrentPrice()));
        updateReserveDisplay(response);
        updateAuctionTiming(response);
        updateWinnerNotice(response);

        if (SessionManager.isAuthenticated() && isAuctionClosed(response)) {
            loadWalletBalance();
        }

        String sellerName = firstNonBlank(response.getSellerName(), SessionManager.getUsername(), "Seller");
        detailUsernameLabel.setText(sellerName);

        if (storedFavorite != null) {
            favoriteSelected = storedFavorite.selected();
            favoriteCount = storedFavorite.count();
            favoriteDirty = true;
        } else if (!favoriteDirty) {
            favoriteCount = (int) Math.max(response.getFavoriteCount(), 0);
        }
        renderFavoriteButton();

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
        countdownLabel.setText("Ends at: " + selectedItem.getTimeLeft());
        if (timeCardTitleLabel != null) {
            timeCardTitleLabel.setText("Time remaining");
        }
        setCountdownParts(0);
        statusLabel.setText("No reserve price");
        hideWinnerNotice();

        specsLabel.setText(
                "Auction Detail:\\n" +
                        "- Backend detail is not available right now\\n" +
                        "- Showing selected item data from showroom\\n" +
                        "- Full specs will be added when the server returns more fields"
        );

        setDefaultImages(selectedItem.getImagePath());
    }

    private void bindPublicItemDetail(AuctionListResponse auction) {
        if (auction.getItemId() == null) {
            bindAuctionSpecsOnly(auction);
            setDefaultImages(selectedItem.getImagePath());
            return;
        }

        String itemIdStr = auction.getItemId().toString();

        // The item detail (name, description, images, category) is immutable during
        // an auction, so fetch it once and reuse it on every 2s refresh instead of
        // hitting the network on the JavaFX thread each tick.
        PublicItemDetailResponse cached = currentItemDetail != null && itemIdStr.equals(loadedItemDetailId)
                ? currentItemDetail
                : null;

        try {
            PublicItemDetailResponse item = cached != null
                    ? cached
                    : itemApiService.getPublicItemDetail(itemIdStr);
            currentItemDetail = item;
            loadedItemDetailId = itemIdStr;

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
            for (String url : item.getImageUrls()) {
                if (url != null && !url.isBlank()) {
                    images.add(url);
                }
            }
        }
        if (images.isEmpty() && item.getImagePath() != null && !item.getImagePath().isBlank()) {
            images.add(item.getImagePath());
        }
        if (images.isEmpty()) {
            setDefaultImages(selectedItem.getImagePath());
            return;
        }

        applyImageGallery(images);
    }

    private void setDefaultImages(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            clearGallery();
            return;
        }
        applyImageGallery(List.of(imagePath));
    }

    private void applyImageGallery(List<String> images) {
        List<String> distinct = new ArrayList<>();
        for (String path : images) {
            if (path != null && !path.isBlank() && !distinct.contains(path)) {
                distinct.add(path);
            }
        }

        if (distinct.isEmpty()) {
            clearGallery();
            return;
        }

        // Skip rebuilding when the gallery is unchanged so the 2s auto-refresh
        // does not reload images or reset the user's selected thumbnail.
        if (distinct.equals(galleryPaths)) {
            return;
        }

        galleryPaths.clear();
        galleryPaths.addAll(distinct);

        setMainImage(distinct.get(0));
        rebuildThumbnailStrip();
    }

    private void setMainImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        if (!imagePath.equals(mainImageKey)) {
            mainImageView.setImage(itemApiService.loadImage(imagePath));
            mainImageKey = imagePath;
        }
        highlightActiveThumbnail();
    }

    private void rebuildThumbnailStrip() {
        if (thumbnailStrip == null) {
            return;
        }

        thumbnailStrip.getChildren().clear();
        for (String path : galleryPaths) {
            ImageView thumb = new ImageView(itemApiService.loadImage(path));
            thumb.setFitWidth(100);
            thumb.setFitHeight(78);
            thumb.setPreserveRatio(true);
            thumb.getStyleClass().add("detail-thumbnail");
            thumb.setOnMouseClicked(event -> setMainImage(path));
            thumbnailStrip.getChildren().add(thumb);
        }
        highlightActiveThumbnail();
    }

    private void highlightActiveThumbnail() {
        if (thumbnailStrip == null) {
            return;
        }
        for (int i = 0; i < thumbnailStrip.getChildren().size(); i++) {
            var node = thumbnailStrip.getChildren().get(i);
            boolean active = i < galleryPaths.size() && galleryPaths.get(i).equals(mainImageKey);
            node.getStyleClass().remove("detail-thumbnail-active");
            if (active) {
                node.getStyleClass().add("detail-thumbnail-active");
            }
        }
    }

    private void clearGallery() {
        galleryPaths.clear();
        mainImageKey = null;
        if (mainImageView != null) {
            mainImageView.setImage(null);
        }
        if (thumbnailStrip != null) {
            thumbnailStrip.getChildren().clear();
        }
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
        countdownLabel.setText("Ends at " + formatDateTime(endTime));
        countdownEndInstant = parseEndInstant(endTime);
        auctionCloseRefreshTriggered = false;
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
        countdownLabel.setText("Ends at -");
        if (timeCardTitleLabel != null) {
            timeCardTitleLabel.setText("Time remaining");
        }
        setCountdownParts(0);
        statusLabel.setText("No reserve price");
        favoriteDirty = false;
        favoriteCount = 0;
        renderFavoriteButton();
        setBidControlsDisabled(true, "Place Bid", "Auto-Bid");
        hideWinnerNotice();
        specsLabel.setText("Please go back to the showroom and choose an auction item.");
        clearGallery();
    }

    @FXML
    private void handleBack() {
        stopAuctionRefreshTimer();
        stopCountdownTimer();
        stopWinnerBalanceRefresh();
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleJoinLiveBidding() {
        if (selectedItem == null) {
            showBidMessage("No selected auction.");
            return;
        }

        stopAuctionRefreshTimer();
        stopCountdownTimer();
        stopWinnerBalanceRefresh();
        SceneManager.goToLiveBidding();
    }

    @FXML
    private void handleOpenBidDialog() {
        if (selectedItem == null || currentAuction == null) {
            showBidMessage("Auction data is unavailable.");
            return;
        }

        if (isAuctionScheduled(currentAuction)) {
            showBidMessage("Auction has not started yet.");
            return;
        }

        if (isAuctionClosed(currentAuction)) {
            showBidMessage("Auction is closed.");
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
            currentAuction.setLeaderId(parseUuid(response.getLeaderId()));
            currentAuction.setLeaderName(response.getLeaderName());
        }

        currentBidLabel.setText(formatMoney(response.getCurrentPrice()));

        if (currentAuction != null) {
            updateAuctionTiming(currentAuction);
        } else if (response.getEndTime() != null && !response.getEndTime().isBlank()) {
            updateCountdown(response.getEndTime());
        }

        double low = response.getCurrentPrice();
        double high = resolveDisplayedHighEstimate(response.getMinNextBid(), low);
        estimatedValueLabel.setText(formatMoney(low) + " - " + formatMoney(high));
        loadWalletBalance();

        showBidSuccess("Bid placed successfully.");
    }

    @FXML
    private void handleGoToForYou() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleSearch() {
        SearchNavigationContext.setPendingQuery(searchField == null ? null : searchField.getText());
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
    private void handleOpenWonAuctions() {
        SceneManager.goToWonAuctions();
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
        stopWinnerBalanceRefresh();
        clearCurrentUserAutoBidState(false);
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleOpenWinnerHub() {
        SceneManager.goToWonAuctions();
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
        favoriteDirty = true;
        favoriteCount = favoriteSelected
                ? favoriteCount + 1
                : Math.max(0, favoriteCount - 1);
        FavoriteUiStateStore.put(resolveCurrentAuctionId(), favoriteSelected, favoriteCount);
        renderFavoriteButton();
    }
    @FXML
    private void handlePlaceBid() {
        hideBidMessage();

        if (selectedItem == null || selectedItem.getId() == null || selectedItem.getId().isBlank()) {
            showBidMessage("No selected auction.");
            return;
        }

        if (!SessionManager.isAuthenticated()) {
            showBidMessage("Please login before bidding.");
            return;
        }

        if (currentAuction == null) {
            showBidMessage("Auction data is unavailable.");
            return;
        }

        if (isAuctionScheduled(currentAuction)) {
            showBidMessage("Auction has not started yet.");
            return;
        }

        if (isAuctionClosed(currentAuction)) {
            showBidMessage("Auction is closed.");
            return;
        }

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

        double minNextBid = currentAuction.getMinNextBid() > 0
                ? currentAuction.getMinNextBid()
                : currentAuction.getCurrentPrice() + 1.0d;
        if (enteredBid < minNextBid) {
            showBidMessage("Your bid must be at least " + formatMoney(minNextBid) + ".");
            return;
        }

        setBidControlsDisabled(true, "Placing...", "Auto-Bid");

        CompletableFuture.supplyAsync(() -> auctionApiService.placeBid(
                        selectedItem.getId(),
                        new com.auction.client.dto.request.BidRequest(enteredBid)
                ))
                .thenAccept(response -> Platform.runLater(() -> {
                    applyBidPlacementToDetail(response);
                    bidAmountField.clear();
                    refreshAuctionDetailSilently();
                    refreshCurrentUserAutoBidState();
                    updateAutoBidActionButton();
                }))
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        showBidMessage(extractFriendlyMessage(error.getMessage()));
                        refreshAuctionDetailSilently();
                        updateAutoBidActionButton();
                    });
                    return null;
                })
                .whenComplete((ignored, error) -> Platform.runLater(() -> updateAutoBidActionButton()));
    }

    private void loadWalletBalance() {
        if (balanceValueLabel == null) {
            return;
        }

        if (!SessionManager.isAuthenticated()) {
            walletBalanceLoaded = false;
            lastKnownWalletBalance = BigDecimal.ZERO;
            clearPendingWinnerBalancePreview();
            balanceValueLabel.setText(formatMoney(BigDecimal.ZERO));
            return;
        }

        CompletableFuture.supplyAsync(walletApiService::getBalance)
                .thenAccept(balance -> Platform.runLater(() -> applyWalletBalance(balance)))
                .exceptionally(error -> {
                    Platform.runLater(this::handleWalletBalanceLoadFailure);
                    return null;
                });
    }

    private void applyWalletBalance(WalletBalanceResponse balance) {
        BigDecimal actualBalance = balance == null || balance.getBalance() == null
                ? BigDecimal.ZERO
                : balance.getBalance();

        lastKnownWalletBalance = actualBalance;
        walletBalanceLoaded = true;

        if (shouldKeepWinnerBalancePreview(actualBalance)) {
            balanceValueLabel.setText(formatMoney(pendingWinnerBalanceValue));
            return;
        }

        clearPendingWinnerBalancePreview();
        balanceValueLabel.setText(formatMoney(actualBalance));
    }

    private void handleWalletBalanceLoadFailure() {
        if (!walletBalanceLoaded) {
            balanceValueLabel.setText(formatUnavailableMoney());
        }
    }

    private void handleAuctionClosureWalletUpdate(AuctionListResponse auction) {
        if (auction == null || !SessionManager.isAuthenticated()) {
            return;
        }

        if (isCurrentUserWinner(auction)) {
            previewWinnerBalanceDeduction(auction);
            scheduleWinnerBalanceRefresh();
        } else {
            clearPendingWinnerBalancePreview();
        }

        loadWalletBalance();
    }

    private void previewWinnerBalanceDeduction(AuctionListResponse auction) {
        if (!walletBalanceLoaded || balanceValueLabel == null || auction == null || auction.getId() == null) {
            return;
        }

        String auctionId = auction.getId().toString();
        if (auctionId.equals(pendingWinnerBalanceAuctionId)) {
            return;
        }

        BigDecimal deductedBalance = lastKnownWalletBalance.subtract(BigDecimal.valueOf(Math.max(0, auction.getCurrentPrice())));
        if (deductedBalance.compareTo(BigDecimal.ZERO) < 0) {
            deductedBalance = BigDecimal.ZERO;
        }

        pendingWinnerBalanceAuctionId = auctionId;
        pendingWinnerBalanceValue = deductedBalance;
        balanceValueLabel.setText(formatMoney(deductedBalance));
    }

    private boolean shouldKeepWinnerBalancePreview(BigDecimal actualBalance) {
        if (pendingWinnerBalanceAuctionId == null
                || pendingWinnerBalanceValue == null
                || currentAuction == null
                || currentAuction.getId() == null) {
            return false;
        }

        if (!pendingWinnerBalanceAuctionId.equals(currentAuction.getId().toString())) {
            return false;
        }

        if (!isAuctionClosed(currentAuction) || !isCurrentUserWinner(currentAuction)) {
            return false;
        }

        return actualBalance.compareTo(pendingWinnerBalanceValue) > 0;
    }

    private void scheduleWinnerBalanceRefresh() {
        winnerBalanceRefreshAttemptsRemaining = 6;

        if (winnerBalanceRefreshTimeline == null) {
            winnerBalanceRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                if (pendingWinnerBalanceAuctionId == null || winnerBalanceRefreshAttemptsRemaining <= 0) {
                    stopWinnerBalanceRefresh();
                    return;
                }

                winnerBalanceRefreshAttemptsRemaining--;
                loadWalletBalance();
            }));
            winnerBalanceRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }

        winnerBalanceRefreshTimeline.playFromStart();
    }

    private void stopWinnerBalanceRefresh() {
        if (winnerBalanceRefreshTimeline != null) {
            winnerBalanceRefreshTimeline.stop();
        }
    }

    private void clearPendingWinnerBalancePreview() {
        pendingWinnerBalanceAuctionId = null;
        pendingWinnerBalanceValue = null;
        stopWinnerBalanceRefresh();
    }

    private boolean isCurrentUserWinner(AuctionListResponse auction) {
        if (auction == null || SessionManager.getUserId() == null) {
            return false;
        }

        java.util.UUID resolvedWinnerId = resolveEffectiveWinnerId(auction);
        return resolvedWinnerId != null && resolvedWinnerId.equals(SessionManager.getUserId());
    }

    private String formatMoney(double value) {
        return "USD " + String.format("%,.0f", value);
    }

    private String formatMoney(BigDecimal value) {
        return value == null ? formatUnavailableMoney() : "USD " + String.format("%,.0f", value.doubleValue());
    }

    private String formatUnavailableMoney() {
        return "USD --";
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

    private void renderFavoriteButton() {
        if (detailFavoriteButton == null) {
            return;
        }

        detailFavoriteButton.setText((favoriteSelected ? "\u2665 " : "\u2661 ") + favoriteCount);
        detailFavoriteButton.getStyleClass().remove("detail-favorite-active");
        if (favoriteSelected) {
            detailFavoriteButton.getStyleClass().add("detail-favorite-active");
        }
    }

    private String resolveCurrentAuctionId() {
        if (currentAuction != null && currentAuction.getId() != null) {
            return currentAuction.getId().toString();
        }
        return selectedItem == null ? null : selectedItem.getId();
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) {
            return "N/A";
        }

        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private java.util.UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return java.util.UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
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
        if (placeBidButton == null && autoBidButton == null) {
            return;
        }

        if (currentAuction != null) {
            if (isAuctionScheduled(currentAuction)) {
                setScheduledBidControls();
                return;
            }

            if (isAuctionClosed(currentAuction)) {
                setBidControlsDisabled(true, "Auction Closed", "Auction Closed");
                return;
            }
        }

        setBidControlsDisabled(false, "Place Bid", "Auto-Bid");
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
            if (currentAuction != null) {
                updateAuctionTiming(currentAuction);
                if (countdownEndInstant != null
                        && java.time.Duration.between(Instant.now(), countdownEndInstant).getSeconds() > 0) {
                    auctionCloseRefreshTriggered = false;
                    return;
                }
                if (!auctionCloseRefreshTriggered) {
                    auctionCloseRefreshTriggered = true;
                    refreshAuctionDetailSilently();
                }
            }

            stopCountdownTimer();
            if (statusLabel != null && (statusLabel.getText() == null || statusLabel.getText().isBlank())) {
                statusLabel.setText("CLOSED");
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

    private void updateAuctionTiming(AuctionListResponse response) {
        String displayState = AuctionStateViewHelper.resolveDisplayState(
                response.getState(),
                response.getStartTime(),
                response.getEndTime()
        );
        response.setState(displayState);
        Instant now = Instant.now();
        Instant startInstant = parseEndInstant(response.getStartTime());
        Instant endInstant = parseEndInstant(response.getEndTime());

        boolean terminalState = isTerminalAuctionState(displayState);
        boolean finished = terminalState || (endInstant != null && !now.isBefore(endInstant));
        boolean scheduledByState = isScheduledAuctionState(displayState);
        boolean activeByState = isLiveAuctionState(displayState);
        boolean scheduledByTime = startInstant != null && now.isBefore(startInstant);
        boolean activeByTime = startInstant != null && !now.isBefore(startInstant);
        boolean scheduled = !finished && (scheduledByTime || (startInstant == null && scheduledByState && !activeByState));
        boolean active = !finished && !scheduled && (activeByState || activeByTime || (scheduledByState && startInstant != null && !now.isBefore(startInstant)));

        if (timeCardTitleLabel != null) {
            if (scheduled) {
                timeCardTitleLabel.setText("Auction starts in");
            } else if (finished) {
                timeCardTitleLabel.setText("Auction ended");
            } else {
                timeCardTitleLabel.setText("Time remaining");
            }
        }

        if (scheduled) {
            auctionCloseRefreshTriggered = false;
            countdownLabel.setText("Starts at " + formatDateTime(response.getStartTime()));
            if (startInstant != null && now.isBefore(startInstant)) {
                countdownEndInstant = startInstant;
                refreshCountdown();
                startCountdownTimer();
            } else {
                countdownEndInstant = null;
                setCountdownParts(0);
                stopCountdownTimer();
            }
            setScheduledBidControls();
            statusLabel.setText("SCHEDULED | Auction opens soon");
            return;
        }

        if (active) {
            auctionCloseRefreshTriggered = false;
            updateCountdown(response.getEndTime());
            setBidControlsDisabled(false, "Place Bid", "Auto-Bid");
            statusLabel.setText("ACTIVE | Live bidding");
            return;
        }

        countdownLabel.setText(finished
                ? "Ended at " + formatDateTime(response.getEndTime())
                : "Starts at " + formatDateTime(response.getStartTime()));
        countdownEndInstant = null;
        setCountdownParts(0);
        stopCountdownTimer();
        setBidControlsDisabled(true, finished ? "Auction Closed" : "Unavailable", finished ? "Auction Closed" : "Unavailable");
        if (finished) {
            statusLabel.setText("CLOSED | Auction closed");
        }
    }

    private boolean isAuctionLive(AuctionListResponse auction) {
        if (auction == null) {
            return false;
        }

        return AuctionStateViewHelper.isActive(
                auction.getState(),
                auction.getStartTime(),
                auction.getEndTime()
        );
    }

    private boolean isAuctionScheduled(AuctionListResponse auction) {
        if (auction == null) {
            return false;
        }

        return AuctionStateViewHelper.isScheduled(
                auction.getState(),
                auction.getStartTime(),
                auction.getEndTime()
        );
    }

    private boolean isAuctionClosed(AuctionListResponse auction) {
        if (auction == null) {
            return false;
        }

        return AuctionStateViewHelper.isClosed(
                auction.getState(),
                auction.getStartTime(),
                auction.getEndTime()
        );
    }

    private boolean isTerminalAuctionState(String state) {
        String normalized = normalizeAuctionState(state);
        return "FINISHED".equalsIgnoreCase(normalized)
                || "CANCELLED".equalsIgnoreCase(normalized)
                || "CLOSED".equalsIgnoreCase(normalized)
                || "ENDED".equalsIgnoreCase(normalized)
                || "DELETED".equalsIgnoreCase(normalized)
                || "REJECTED".equalsIgnoreCase(normalized);
    }

    private boolean isLiveAuctionState(String state) {
        String normalized = normalizeAuctionState(state);
        return "ACTIVE".equalsIgnoreCase(normalized)
                || "OPEN".equalsIgnoreCase(normalized)
                || "LIVE".equalsIgnoreCase(normalized);
    }

    private boolean isScheduledAuctionState(String state) {
        String normalized = normalizeAuctionState(state);
        return "SCHEDULED".equalsIgnoreCase(normalized)
                || "INCOMING".equalsIgnoreCase(normalized)
                || "PENDING".equalsIgnoreCase(normalized)
                || "DRAFT".equalsIgnoreCase(normalized);
    }

    private String normalizeAuctionState(String state) {
        return firstNonBlank(state, "").trim().toUpperCase(Locale.ROOT);
    }

    private void setBidControlsDisabled(boolean disabled, String placeBidText, String autoBidText) {
        if (placeBidButton != null) {
            placeBidButton.setDisable(disabled);
            placeBidButton.setText(placeBidText);
        }
        if (autoBidButton != null) {
            autoBidButton.setDisable(disabled);
            autoBidButton.setText(autoBidText);
        }
    }

    private void setScheduledBidControls() {
        if (placeBidButton != null) {
            placeBidButton.setDisable(true);
            placeBidButton.setText("Available When Live");
        }
        if (autoBidButton != null) {
            autoBidButton.setDisable(false);
            autoBidButton.setText("Auto-Bid");
        }
    }

    private void updateReserveDisplay(AuctionListResponse auction) {
        if (reservePriceLabel == null || auction == null) {
            return;
        }

        boolean hasReserve = auction.getReservePrice() > 0;
        if (!hasReserve) {
            reservePriceLabel.setText("Reserve price: None");
            return;
        }

        String reserveText = "Reserve price: " + formatMoney(auction.getReservePrice());
        if (isReserveMet(auction)) {
            reserveText += isAuctionClosed(auction) ? " (met)" : " (reached)";
        } else {
            reserveText += isAuctionClosed(auction) ? " (not met)" : " (not reached yet)";
        }
        reservePriceLabel.setText(reserveText);
    }

    private boolean isReserveMet(AuctionListResponse auction) {
        if (auction == null) {
            return false;
        }
        return auction.getReservePrice() <= 0 || auction.getCurrentPrice() >= auction.getReservePrice();
    }

    // The effective winner is empty when the auction closed below its reserve price,
    // matching the server's "no winner" decision.
    private java.util.UUID resolveEffectiveWinnerId(AuctionListResponse auction) {
        if (auction == null || !isReserveMet(auction)) {
            return null;
        }
        return auction.getWinnerId() != null ? auction.getWinnerId() : auction.getLeaderId();
    }

    private void updateWinnerNotice(AuctionListResponse auction) {
        if (auction == null) {
            hideWinnerNotice();
            return;
        }

        if (!isAuctionClosed(auction)) {
            hideWinnerNotice();
            return;
        }

        if (winnerNoticeCard == null || winnerNoticeTitleLabel == null || winnerNoticeSubtitleLabel == null) {
            return;
        }

        boolean reserveMet = isReserveMet(auction);
        java.util.UUID resolvedWinnerId = resolveEffectiveWinnerId(auction);
        String winnerName = firstNonBlank(auction.getWinnerName(), auction.getLeaderName(), "No winner");
        boolean currentUserWon = SessionManager.getUserId() != null
                && resolvedWinnerId != null
                && SessionManager.getUserId().equals(resolvedWinnerId);

        if (!reserveMet) {
            winnerNoticeCard.getStyleClass().removeAll(
                    "winner-notice-card",
                    "winner-notice-card-success",
                    "winner-notice-card-muted"
            );
            winnerNoticeCard.getStyleClass().addAll("winner-notice-card", "winner-notice-card-muted");
            winnerNoticeTitleLabel.setText("Auction finished with no winner");
            winnerNoticeSubtitleLabel.setText("The final bid " + formatMoney(auction.getCurrentPrice())
                    + " did not reach the reserve price of " + formatMoney(auction.getReservePrice())
                    + ", so no winner was selected.");
            if (winnerNoticeButton != null) {
                winnerNoticeButton.setManaged(false);
                winnerNoticeButton.setVisible(false);
            }
            winnerNoticeCard.setManaged(true);
            winnerNoticeCard.setVisible(true);
            return;
        }

        winnerNoticeCard.getStyleClass().removeAll(
                "winner-notice-card",
                "winner-notice-card-success",
                "winner-notice-card-muted"
        );
        winnerNoticeCard.getStyleClass().add("winner-notice-card");

        if (currentUserWon) {
            winnerNoticeCard.getStyleClass().add("winner-notice-card-success");
            winnerNoticeTitleLabel.setText("You won this auction");
            winnerNoticeSubtitleLabel.setText(
                    formatMoney(auction.getCurrentPrice()) + " final price deducted from your balance. Open Wins to review your successful auction."
            );
            if (winnerNoticeButton != null) {
                winnerNoticeButton.setManaged(true);
                winnerNoticeButton.setVisible(true);
            }
        } else if (resolvedWinnerId != null) {
            winnerNoticeCard.getStyleClass().add("winner-notice-card-muted");
            winnerNoticeTitleLabel.setText("Winning bidder confirmed");
            winnerNoticeSubtitleLabel.setText(winnerName + " won this auction at " + formatMoney(auction.getCurrentPrice()) + ". Your balance stays unchanged.");
            if (winnerNoticeButton != null) {
                winnerNoticeButton.setManaged(false);
                winnerNoticeButton.setVisible(false);
            }
        } else {
            winnerNoticeCard.getStyleClass().add("winner-notice-card-muted");
            winnerNoticeTitleLabel.setText("Auction finished with no winner");
            winnerNoticeSubtitleLabel.setText("This auction closed without a successful bidder.");
            if (winnerNoticeButton != null) {
                winnerNoticeButton.setManaged(false);
                winnerNoticeButton.setVisible(false);
            }
        }

        winnerNoticeCard.setManaged(true);
        winnerNoticeCard.setVisible(true);
    }

    private void hideWinnerNotice() {
        if (winnerNoticeCard != null) {
            winnerNoticeCard.setManaged(false);
            winnerNoticeCard.setVisible(false);
        }
        if (winnerNoticeButton != null) {
            winnerNoticeButton.setManaged(false);
            winnerNoticeButton.setVisible(false);
        }
    }
}
