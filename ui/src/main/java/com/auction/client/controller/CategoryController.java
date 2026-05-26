package com.auction.client.controller;

import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.dto.response.AuctionPageResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.FavoriteApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.ui.AuctionCardData;
import com.auction.client.ui.AuctionCardViewFactory;
import com.auction.client.util.MockData;
import com.auction.client.service.ItemApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class CategoryController {

    @FXML private Label usernameLabel;
    @FXML private Label categoryTitleLabel;
    @FXML private Label categoryDescriptionLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label endingSoonLabel;
    @FXML private Label activeBiddersLabel;
    @FXML private Label averagePriceLabel;
    @FXML private TilePane categoryGrid;
    @FXML private Button saveCategoryButton;
    @FXML private Button loadMoreButton;
    @FXML private TextField searchField;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final AuctionCardViewFactory cardFactory = new AuctionCardViewFactory(itemApiService);

    private final List<AuctionListResponse> auctions = new ArrayList<>();

    private int currentPage = 0;
    private static final int PAGE_SIZE = 12;
    private boolean categorySaved = false;
    private String currentQuery = null;
    private final Set<String> favoriteAuctionIds = new HashSet<>();

    @FXML
    private void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }
        if (usernameLabel != null) {
            String username = SessionManager.getUsername();
            usernameLabel.setText(username == null || username.isBlank() ? "Bidder" : username);
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
        currentPage = 0;
        auctions.clear();

        try {
            AuctionPageResponse page = auctionApiService.searchAuctions(
                    selectedCategory,
                    currentQuery,
                    null,
                    currentPage,
                    PAGE_SIZE,
                    "endTime,asc"
            );

            if (page.getItems() != null) {
                auctions.addAll(page.getItems());
            }

            if (auctions.isEmpty()) {
                loadMockCategoryAuctions();
            }

        } catch (Exception e) {
            loadMockCategoryAuctions();
        }

        renderStats(auctions);
        renderCards(auctions);
    }

    private void loadMockCategoryAuctions() {
        auctions.clear();

        int index = 0;
        for (AuctionItem item : MockData.getMockAuctionItems()) {
            AuctionListResponse response = new AuctionListResponse();
            response.setTitle(categoryTitleForMock(item.getName(), index));
            response.setImageUrl(item.getImagePath());
            response.setSellerName(mockSellerForCategory(index));
            response.setCurrentPrice(parseMoney(item.getCurrentBid()));
            response.setState(item.getStatus());
            response.setBidCount(8 + index * 3);
            response.setFavoriteCount(36 + index * 12);
            response.setViewCount(1200 + index * 250);
            response.setCategory(selectedCategory);
            response.setEndTime(item.getTimeLeft());
            response.setId(null);

            auctions.add(response);
            index++;
        }
    }

    private void renderStats(List<AuctionListResponse> items) {
        totalAuctionsLabel.setText(String.valueOf(items.size()));
        endingSoonLabel.setText(String.valueOf(items.stream().filter(item -> item.getEndTime() != null && !item.getEndTime().isBlank()).count()));
        activeBiddersLabel.setText(String.valueOf(items.stream().mapToInt(AuctionListResponse::getBidCount).sum()));
        double avgPrice = items.stream().mapToDouble(AuctionListResponse::getCurrentPrice).average().orElse(0.0);
        averagePriceLabel.setText("USD " + String.format("%,.0f", avgPrice));
    }

    private void renderCards(List<AuctionListResponse> items) {
        categoryGrid.getChildren().clear();

        if (items == null || items.isEmpty()) {
            categoryGrid.getChildren().add(createEmptyState());
            return;
        }

        for (AuctionListResponse auction : items) {
            categoryGrid.getChildren().add(createAuctionCard(auction));
        }
    }
    private VBox createEmptyState() {
        Label title = new Label("No auctions in this category yet.");
        title.getStyleClass().add("category-card-title");

        Label subtitle = new Label("Approved auctions will appear here once sellers and admins publish them.");
        subtitle.getStyleClass().add("category-ending");

        VBox empty = new VBox(8, title, subtitle);
        empty.getStyleClass().add("category-empty-state");
        return empty;
    }

    private VBox createAuctionCard(AuctionListResponse auction) {
        String title = auction.getTitle() != null && !auction.getTitle().isBlank() ? auction.getTitle() : auction.getItemName();
        String auctionId = getAuctionId(auction);
        AuctionCardData cardData = new AuctionCardData(
                auctionId,
                auction.getSellerName() == null ? selectedCategory : auction.getSellerName().toUpperCase(),
                title == null ? "Untitled auction" : title,
                formatPrice(auction.getCurrentPrice()),
                formatEnding(auction),
                selectedCategory + " category",
                auction.getImageUrl(),
                null,
                "Open Auction",
                "36"
        );

        return cardFactory.createCard(
                cardData,
                260,
                260,
                favoriteAuctionIds.contains(auctionId),
                selected -> toggleFavorite(auction, selected),
                () -> openAuction(auction)
        );
    }

    private void renderErrorCard() {
        categoryGrid.getChildren().clear();
        Label error = new Label("Cannot load auctions for this category right now.");
        error.getStyleClass().add("category-description");
        categoryGrid.getChildren().add(error);
    }

    private void toggleFavorite(AuctionListResponse auction, boolean selected) {
        String auctionId = getAuctionId(auction);
        if (selected) {
            favoriteAuctionIds.add(auctionId);
        } else {
            favoriteAuctionIds.remove(auctionId);
        }

        if (auction.getId() == null) {
            return;
        }

        try {
            if (selected) {
                favoriteApiService.addFavorite(auctionId);
            } else {
                favoriteApiService.removeFavorite(auctionId);
            }
        } catch (Exception ignored) {
        }
    }

    private void openAuction(AuctionListResponse auction) {
        String title = auction.getTitle() != null && !auction.getTitle().isBlank() ? auction.getTitle() : auction.getItemName();
        MockData.setSelectedItem(new AuctionItem(
                getAuctionId(auction),
                title == null ? "Auction" : title,
                auction.getImageUrl() == null ? "/images/item1.png" : auction.getImageUrl(),
                "USD " + String.format("%,.0f", auction.getCurrentPrice()),
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

    private String categoryTitleForMock(String fallback, int index) {
        return switch (selectedCategory) {
            case "Jewellery" -> switch (index) {
                case 0 -> "Affordable Silver & Laminated Objects Auction";
                case 1 -> "Emeralds, Rubies & Sapphires Auction";
                default -> "Exclusive White Diamonds Auction";
            };
            case "Watches" -> switch (index) {
                case 0 -> "Vintage Rolex Watch";
                case 1 -> "Rare Chronograph Collection";
                default -> "Luxury Watch Icons Auction";
            };
            case "Fashion" -> switch (index) {
                case 0 -> "Archive Designer Handbag";
                case 1 -> "Rare Couture Jacket";
                default -> "Collectible Fashion Accessories";
            };
            default -> switch (index) {
                case 0 -> "Contemporary Abstract Painting";
                case 1 -> "Impressionist Landscape Oil";
                default -> "Digital Art NFT Edition";
            };
        };
    }

    private String mockSellerForCategory(int index) {
        return switch (index) {
            case 0 -> "Sarah Mitchell";
            case 1 -> "Marco Rossi";
            default -> "Emma Chen";
        };
    }

    private double parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatPrice(double value) {
        return "USD " + String.format("%,.0f", value);
    }

    private String getAuctionId(AuctionListResponse auction) {
        if (auction.getId() != null) {
            return auction.getId().toString();
        }

        String title = auction.getTitle() != null ? auction.getTitle() : auction.getItemName();
        return "mock-" + selectedCategory + "-" + title;
    }

    @FXML
    private void handleBrowseAuctions() {
        if (categoryGrid != null) {
            categoryGrid.requestFocus();
        }
    }

    @FXML
    private void handleSaveCategory() {
        categorySaved = !categorySaved;

        if (saveCategoryButton != null) {
            saveCategoryButton.setText(categorySaved ? "Saved Category" : "Save Category");
        }
    }

    @FXML
    private void handleSearch() {
        currentQuery = searchField == null ? null : searchField.getText();
        loadCategoryAuctions();
    }

    @FXML
    private void handleLoadMore() {
        currentPage++;

        try {
            AuctionPageResponse page = auctionApiService.searchAuctions(
                    selectedCategory,
                    currentQuery,
                    null,
                    currentPage,
                    PAGE_SIZE,
                    "endTime,asc"
            );

            if (page.getItems() != null && !page.getItems().isEmpty()) {
                auctions.addAll(page.getItems());
            }
        } catch (Exception ignored) {
        }

        renderStats(auctions);
        renderCards(auctions);
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
