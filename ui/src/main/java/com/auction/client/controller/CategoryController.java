package com.auction.client.controller;

import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AuctionPageResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.FavoriteApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryController {

    @FXML private Label usernameLabel;
    @FXML private Label categoryTitleLabel;
    @FXML private Label categoryDescriptionLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label endingSoonLabel;
    @FXML private Label activeBiddersLabel;
    @FXML private Label averagePriceLabel;
    @FXML private FlowPane categoryGrid;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final Set<String> favoriteAuctionIds = new HashSet<>();

    @FXML
    private void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }
        if (usernameLabel != null) {
            usernameLabel.setText(SessionManager.getUsername());
        }
        categoryTitleLabel.setText(selectedCategory);
        categoryDescriptionLabel.setText(descriptionForCategory(selectedCategory));
        loadFavorites();
        loadCategoryAuctions();
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

    private void loadCategoryAuctions() {
        try {
            AuctionPageResponse page = auctionApiService.searchAuctions(selectedCategory, null, null, 0, 6, "endTime,asc");
            List<AuctionListResponse> items = page.getItems();
            renderStats(items);
            renderCards(items);
        } catch (Exception e) {
            renderStats(List.of());
            renderErrorCard();
        }
    }

    private void renderStats(List<AuctionListResponse> items) {
        totalAuctionsLabel.setText(String.valueOf(items.size()));
        endingSoonLabel.setText(String.valueOf(items.stream().filter(item -> item.getEndTime() != null && !item.getEndTime().isBlank()).count()));
        activeBiddersLabel.setText(String.valueOf(items.stream().mapToInt(AuctionListResponse::getBidCount).sum()));
        double avgPrice = items.stream().mapToDouble(AuctionListResponse::getCurrentPrice).average().orElse(0.0);
        averagePriceLabel.setText("$" + String.format("%,.0f", avgPrice));
    }

    private void renderCards(List<AuctionListResponse> items) {
        categoryGrid.getChildren().clear();
        for (AuctionListResponse auction : items) {
            categoryGrid.getChildren().add(createAuctionCard(auction));
        }
    }

    private VBox createAuctionCard(AuctionListResponse auction) {
        VBox card = new VBox(8);
        card.setPrefWidth(260.0);
        card.getStyleClass().add("category-card");

        Region media = new Region();
        media.setPrefHeight(260.0);
        media.getStyleClass().add("category-card-media");

        Button favoriteButton = new Button(favoriteAuctionIds.contains(auction.getId().toString()) ? "\u2665" : "\u2661");
        favoriteButton.getStyleClass().add("category-heart-button");
        favoriteButton.setFocusTraversable(false);
        favoriteButton.setOnAction(event -> toggleFavorite(auction, favoriteButton));

        StackPane mediaStack = new StackPane(media, favoriteButton);
        StackPane.setAlignment(favoriteButton, Pos.TOP_RIGHT);
        StackPane.setMargin(favoriteButton, new Insets(12, 12, 0, 0));

        Label sellerLabel = new Label(auction.getSellerName() == null ? "SELLER" : auction.getSellerName().toUpperCase());
        sellerLabel.getStyleClass().add("category-seller");

        String title = auction.getTitle() != null && !auction.getTitle().isBlank() ? auction.getTitle() : auction.getItemName();
        Label titleLabel = new Label(title == null ? "Untitled auction" : title);
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("category-card-title");

        Label endingLabel = new Label(formatEnding(auction));
        endingLabel.getStyleClass().add("category-ending");

        Button openButton = new Button("Open Auction");
        openButton.getStyleClass().add("secondary-button");
        openButton.setFocusTraversable(false);
        openButton.setOnAction(event -> openAuction(auction));

        card.getChildren().addAll(mediaStack, sellerLabel, titleLabel, endingLabel, openButton);
        return card;
    }

    private void renderErrorCard() {
        categoryGrid.getChildren().clear();
        Label error = new Label("Cannot load auctions for this category right now.");
        error.getStyleClass().add("category-description");
        categoryGrid.getChildren().add(error);
    }

    private void toggleFavorite(AuctionListResponse auction, Button favoriteButton) {
        try {
            String auctionId = auction.getId().toString();
            if (favoriteAuctionIds.contains(auctionId)) {
                favoriteApiService.removeFavorite(auctionId);
                favoriteAuctionIds.remove(auctionId);
                favoriteButton.setText("\u2661");
            } else {
                favoriteApiService.addFavorite(auctionId);
                favoriteAuctionIds.add(auctionId);
                favoriteButton.setText("\u2665");
            }
        } catch (Exception ignored) {
        }
    }

    private void openAuction(AuctionListResponse auction) {
        String title = auction.getTitle() != null && !auction.getTitle().isBlank() ? auction.getTitle() : auction.getItemName();
        MockData.setSelectedItem(new AuctionItem(
                auction.getId().toString(),
                title == null ? "Auction" : title,
                auction.getImageUrl() == null ? "/images/item1.png" : auction.getImageUrl(),
                "$" + String.format("%,.0f", auction.getCurrentPrice()),
                formatEnding(auction),
                auction.getState() == null ? "UNKNOWN" : auction.getState()
        ));
        SceneManager.goToProductDetail();
    }

    private String formatEnding(AuctionListResponse auction) {
        if (auction.getEndTime() == null || auction.getEndTime().isBlank()) {
            return "Ending date unavailable";
        }
        return auction.getEndTime().length() >= 16
                ? "Ends " + auction.getEndTime().substring(0, 16).replace("T", " ")
                : "Ends " + auction.getEndTime();
    }

    private String descriptionForCategory(String category) {
        return switch (category == null ? "" : category) {
            case "Jewellery" -> "Browse rings, necklaces and rare high-jewelry pieces from active auctions.";
            case "Watches" -> "Explore mechanical icons, limited editions and vintage timepieces with live bidding.";
            case "Fashion" -> "Discover luxury fashion, archive garments and collectible accessories.";
            default -> "Explore exceptional artworks and collectibles from live auctions in this category.";
        };
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
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    private static String selectedCategory = "Art";

    public static void setSelectedCategory(String category) {
        selectedCategory = category;
    }
}
