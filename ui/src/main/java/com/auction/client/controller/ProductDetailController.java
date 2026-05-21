package com.auction.client.controller;

import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.PublicItemDetailResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.service.ItemApiService;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;

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

    @FXML private TextField bidAmountField;
    @FXML private Button detailFavoriteButton;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private AuctionItem selectedItem;
    private boolean favoriteSelected = false;
    private int favoriteCount = 36;

    @FXML
    public void initialize() {
        bindSessionUsername();
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
        productNameLabel.setText(
                response.getItemName() == null || response.getItemName().isBlank()
                        ? "Unnamed Item"
                        : response.getItemName()
        );

        currentBidLabel.setText("\u20ac " + String.format("%,.0f", response.getCurrentPrice()));
        updateCountdown(response.getEndTime());
        statusLabel.setText("No reserve price");

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

            if (item.getProductName() != null && !item.getProductName().isBlank()) {
                productNameLabel.setText(item.getProductName());
            }

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

        if (value.length() >= 16) {
            return value.substring(0, 16).replace("T", " ");
        }

        return value;
    }

    private String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
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
