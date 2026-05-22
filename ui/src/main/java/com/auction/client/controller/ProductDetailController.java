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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

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

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final WalletApiService walletApiService = new WalletApiService();

    private AuctionItem selectedItem;
    private AuctionListResponse currentAuction;
    private PublicItemDetailResponse currentItemDetail;
    private boolean favoriteSelected = false;
    private int favoriteCount = 36;

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
    }
    private void loadAuctionDetail() {
        try {
            AuctionListResponse response = auctionApiService.getAuctionById(selectedItem.getId());
            currentAuction = response;

            try {
                auctionApiService.trackView(selectedItem.getId());
            } catch (Exception ignored) {
            }

            bindDetailFromApi(response);

        } catch (Exception e) {
            bindFallbackFromSelectedItem();
            showBidMessage("Cannot load full detail from server. Showing fallback data.");
        }
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
        double high = response.getMinNextBid() > low ? response.getMinNextBid() : low + 100;
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

        setRemoteImage(mainImageView, images.get(0));
        setRemoteImage(thumb1ImageView, images.get(0));
        setRemoteImage(thumb2ImageView, images.size() > 1 ? images.get(1) : images.get(0));
    }

    private void setRemoteImage(ImageView imageView, String imagePath) {
        imageView.setImage(new Image(itemApiService.toAbsoluteImageUrl(imagePath), true));
    }

    private void setDefaultImages(String imagePath) {
        try {
            if (imagePath != null && (
                    imagePath.startsWith("http://")
                            || imagePath.startsWith("https://")
                            || imagePath.startsWith("/uploads")
                            || imagePath.startsWith("uploads/")
            )) {
                setRemoteImage(mainImageView, imagePath);
                setRemoteImage(thumb1ImageView, imagePath);
                setRemoteImage(thumb2ImageView, imagePath);
                return;
            }

            Image image = new Image(getClass().getResourceAsStream(imagePath));
            mainImageView.setImage(image);
            thumb1ImageView.setImage(image);
            thumb2ImageView.setImage(image);

        } catch (Exception e) {
            mainImageView.setImage(null);
            thumb1ImageView.setImage(null);
            thumb2ImageView.setImage(null);
            System.out.println("Image not found: " + imagePath);
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
        countdownLabel.setText("Ends: " + formatDateTime(endTime));
        setCountdownParts(resolveRemainingSeconds(endTime));
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
            return Math.max(0, Duration.between(LocalDateTime.now(), auctionEnd).getSeconds());
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
        mainImageView.setImage(null);
        thumb1ImageView.setImage(null);
        thumb2ImageView.setImage(null);
    }

    @FXML
    private void handleBack() {
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

        showBidSuccess("Auto-Bid is running up to " + formatMoney(response.getMaxAmount()) + ".");
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
        double high = response.getMinNextBid() > low ? response.getMinNextBid() : low + 100;
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

            if (productNameLabel != null && productNameLabel.getScene() != null) {
                stage.initOwner(productNameLabel.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            addDialogStyles(scene);

            AutoBidDialogController controller = loader.getController();

            AuctionListResponse auctionForDialog = resolveAuctionForDialog();
            boolean demoMode = currentAuction == null || currentAuction.getId() == null;

            controller.setDialogStage(stage);
            controller.setAuction(auctionForDialog, demoMode, this::applyAutoBidToDetail);

            stage.setScene(scene);
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

            if (productNameLabel != null && productNameLabel.getScene() != null) {
                stage.initOwner(productNameLabel.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            addDialogStyles(scene);

            BidHistoryDialogController controller = loader.getController();

            AuctionListResponse auctionForDialog = resolveAuctionForDialog();
            boolean demoMode = currentAuction == null || currentAuction.getId() == null;

            controller.setDialogStage(stage);
            controller.setAuction(auctionForDialog, demoMode);

            stage.setScene(scene);
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
        return "\u20ac " + String.format("%,.0f", value);
    }

    private String formatMoney(BigDecimal value) {
        return "\u20ac " + (value == null ? "0" : String.format("%,.0f", value.doubleValue()));
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
}
