package com.auction.client.controller;

import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.FavoriteApiService;
import com.auction.client.service.ItemApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ShowRoomController {

    @FXML private Label item1NameLabel;
    @FXML private Label item1BidLabel;
    @FXML private Label item1TimeLabel;
    @FXML private Label item1StatusLabel;
    @FXML private ImageView item1ImageView;
    @FXML private Button item1FavoriteButton;

    @FXML private Label item2NameLabel;
    @FXML private Label item2BidLabel;
    @FXML private Label item2TimeLabel;
    @FXML private Label item2StatusLabel;
    @FXML private ImageView item2ImageView;
    @FXML private Button item2FavoriteButton;

    @FXML private Label item3NameLabel;
    @FXML private Label item3BidLabel;
    @FXML private Label item3TimeLabel;
    @FXML private Label item3StatusLabel;
    @FXML private ImageView item3ImageView;
    @FXML private Button item3FavoriteButton;

    @FXML private Label usernameLabel;
    @FXML private Button wishlistButton;
    @FXML private Label savedAuctionsCountLabel;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final List<AuctionItem> items = new ArrayList<>();
    private final Set<String> favoriteAuctionIds = new LinkedHashSet<>();

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }

        usernameLabel.setText(firstNonBlank(SessionManager.getUsername(), "Bidder"));
        loadFavorites();
        loadAuctionList();
        updateWishlistUi();
    }

    private void loadFavorites() {
        try {
            favoriteAuctionIds.clear();
            for (AuctionDetailResponse favorite : favoriteApiService.getFavorites()) {
                if (favorite.getId() != null) {
                    favoriteAuctionIds.add(favorite.getId().toString());
                }
            }
        } catch (Exception ignored) {
            favoriteAuctionIds.clear();
        }
    }

    private void loadAuctionList() {
        try {
            List<AuctionListResponse> responses = auctionApiService.getAuctions().getItems();
            items.clear();
            for (int i = 0; i < Math.min(3, responses.size()); i++) {
                items.add(mapToAuctionItem(responses.get(i), i));
            }
            bindCards();
        } catch (Exception e) {
            showFallbackState("Cannot load auctions.");
        }
    }

    private AuctionItem mapToAuctionItem(AuctionListResponse response, int index) {
        String imagePath = response.getImageUrl() == null || response.getImageUrl().isBlank()
                ? getDefaultImagePath(index)
                : response.getImageUrl();
        String currentBid = "$" + String.format("%,.0f", response.getCurrentPrice());
        String timeInfo = formatEndTime(response.getEndTime());
        String status = response.getState() == null ? "UNKNOWN" : response.getState();
        String idValue = response.getId() != null ? response.getId().toString() : "";
        String title = response.getTitle() != null && !response.getTitle().isBlank()
                ? response.getTitle()
                : response.getItemName();

        return new AuctionItem(idValue, title == null ? "Unnamed Item" : title, imagePath, currentBid, timeInfo, status);
    }

    private String getDefaultImagePath(int index) {
        return switch (index % 3) {
            case 0 -> "/images/item1.png";
            case 1 -> "/images/item2.png";
            default -> "/images/item3.png";
        };
    }

    private String formatEndTime(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return "No end time";
        }
        return endTime.length() >= 16 ? endTime.substring(0, 16).replace("T", " ") : endTime;
    }

    private void bindCards() {
        bindSingleCard(0, item1NameLabel, item1BidLabel, item1TimeLabel, item1StatusLabel, item1ImageView, item1FavoriteButton);
        bindSingleCard(1, item2NameLabel, item2BidLabel, item2TimeLabel, item2StatusLabel, item2ImageView, item2FavoriteButton);
        bindSingleCard(2, item3NameLabel, item3BidLabel, item3TimeLabel, item3StatusLabel, item3ImageView, item3FavoriteButton);
    }

    private void bindSingleCard(int index,
                                Label nameLabel,
                                Label bidLabel,
                                Label timeLabel,
                                Label statusLabel,
                                ImageView imageView,
                                Button favoriteButton) {
        if (index >= items.size()) {
            nameLabel.setText("No auction");
            bidLabel.setText("Current Bid: -");
            timeLabel.setText("Ends: -");
            statusLabel.setText("N/A");
            imageView.setImage(null);
            favoriteButton.setDisable(true);
            favoriteButton.setText("\u2661");
            favoriteButton.getStyleClass().remove("favorite-button-active");
            return;
        }

        AuctionItem item = items.get(index);
        nameLabel.setText(item.getName());
        bidLabel.setText("Current Bid: " + item.getCurrentBid());
        timeLabel.setText("Ends: " + item.getTimeLeft());
        statusLabel.setText(item.getStatus());
        bindCardImage(imageView, item.getImagePath());
        favoriteButton.setDisable(false);
        updateFavoriteButton(favoriteButton, item);
    }

    private void bindCardImage(ImageView imageView, String imagePath) {
        try {
            if (imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("/uploads") || imagePath.startsWith("uploads/"))) {
                imageView.setImage(new Image(itemApiService.toAbsoluteImageUrl(imagePath), true));
            } else {
                imageView.setImage(new Image(getClass().getResourceAsStream(imagePath)));
            }
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    private void showFallbackState(String message) {
        item1NameLabel.setText(message);
        item1BidLabel.setText("Current Bid: -");
        item1TimeLabel.setText("Ends: -");
        item1StatusLabel.setText("ERROR");
        item1ImageView.setImage(null);
        item1FavoriteButton.setDisable(true);

        item2NameLabel.setText("No auction");
        item2BidLabel.setText("Current Bid: -");
        item2TimeLabel.setText("Ends: -");
        item2StatusLabel.setText("N/A");
        item2ImageView.setImage(null);
        item2FavoriteButton.setDisable(true);

        item3NameLabel.setText("No auction");
        item3BidLabel.setText("Current Bid: -");
        item3TimeLabel.setText("Ends: -");
        item3StatusLabel.setText("N/A");
        item3ImageView.setImage(null);
        item3FavoriteButton.setDisable(true);

        updateWishlistUi();
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleToggleFavorite1() {
        toggleFavoriteAtIndex(0);
    }

    @FXML
    private void handleToggleFavorite2() {
        toggleFavoriteAtIndex(1);
    }

    @FXML
    private void handleToggleFavorite3() {
        toggleFavoriteAtIndex(2);
    }

    @FXML
    private void handleViewDetails1() {
        openDetailAtIndex(0);
    }

    @FXML
    private void handleViewDetails2() {
        openDetailAtIndex(1);
    }

    @FXML
    private void handleViewDetails3() {
        openDetailAtIndex(2);
    }

    @FXML
    private void handleGoToForYou() {
        loadFavorites();
        loadAuctionList();
        updateWishlistUi();
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
    private void handleOpenCategories() {
        SceneManager.goToCategory();
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

    private void openDetailAtIndex(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        MockData.setSelectedItem(items.get(index));
        SceneManager.goToProductDetail();
    }

    private void toggleFavoriteAtIndex(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }

        AuctionItem item = items.get(index);
        try {
            if (favoriteAuctionIds.contains(item.getId())) {
                favoriteApiService.removeFavorite(item.getId());
                favoriteAuctionIds.remove(item.getId());
            } else {
                favoriteApiService.addFavorite(item.getId());
                favoriteAuctionIds.add(item.getId());
            }
        } catch (Exception ignored) {
        }

        bindCards();
        updateWishlistUi();
    }

    private void updateFavoriteButton(Button button, AuctionItem item) {
        boolean selected = favoriteAuctionIds.contains(item.getId());
        button.setText(selected ? "\u2665" : "\u2661");
        button.getStyleClass().remove("favorite-button-active");
        if (selected) {
            button.getStyleClass().add("favorite-button-active");
        }
    }

    private void updateWishlistUi() {
        int count = favoriteAuctionIds.size();

        if (wishlistButton != null) {
            wishlistButton.setText("\u2661 " + count);
        }

        if (savedAuctionsCountLabel != null) {
            savedAuctionsCountLabel.setText(String.valueOf(count));
        }
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
}
