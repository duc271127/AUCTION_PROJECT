package com.auction.client.controller;

import com.auction.client.navigation.SceneManager;
import com.auction.client.session.SessionManager;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.FavoriteApiService;
import com.auction.client.service.ItemApiService;
import com.auction.client.util.MockData;
import com.auction.client.ui.AuctionCardData;
import com.auction.client.ui.AuctionCardViewFactory;
import com.auction.client.util.AuctionStateViewHelper;
import com.auction.client.util.DateTimeDisplayHelper;
import com.auction.client.util.SearchNavigationContext;
import com.auction.client.util.WishlistStateStore;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TrendingController {

    @FXML private Label usernameLabel;
    @FXML private TilePane trendingGrid;
    @FXML private Button hotFilterButton;
    @FXML private Button viewedFilterButton;
    @FXML private Button savedFilterButton;
    @FXML private Button loadMoreButton;
    @FXML private Button wishlistButton;
    @FXML private TextField searchField;

    @FXML private Label activeBidsLabel;
    @FXML private Label newAuctionsLabel;
    @FXML private Label totalBidsPlacedLabel;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final AuctionCardViewFactory cardFactory = new AuctionCardViewFactory(itemApiService);

    private final List<TrendingCardItem> trendingItems = new ArrayList<>();
    private final Set<String> favoriteAuctionIds = new LinkedHashSet<>();

    private TrendingFilter selectedFilter = TrendingFilter.HOT;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 12;
    private String currentQuery;

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }

        usernameLabel.setText(
                SessionManager.getUsername() == null || SessionManager.getUsername().isBlank()
                        ? "Bidder"
                        : SessionManager.getUsername()
        );
        currentQuery = SearchNavigationContext.consumePendingQuery();
        if (searchField != null && currentQuery != null) {
            searchField.setText(currentQuery);
        }
        updateWishlistButton();
        loadFavorites();
        loadTrendingAuctions(true);
    }

    private void loadFavorites() {
        try {
            favoriteAuctionIds.clear();

            for (var favorite : favoriteApiService.getFavorites()) {
                if (favorite.getId() != null) {
                    favoriteAuctionIds.add(favorite.getId().toString());
                }
            }
            WishlistStateStore.replaceAll(favoriteAuctionIds);
        } catch (Exception ignored) {
            favoriteAuctionIds.clear();
        }
        updateWishlistButton();
    }

    private void loadTrendingAuctions(boolean reset) {
        if (reset) {
            currentPage = 0;
            trendingItems.clear();
        }

        try {
            List<AuctionListResponse> responses =
                    auctionApiService.getTrendingAuctions(null, currentQuery, null, currentPage, PAGE_SIZE).getItems();

            if (responses != null) {
                for (AuctionListResponse response : responses) {
                    if (!isVisibleToBidder(response)) {
                        continue;
                    }
                    trendingItems.add(mapToTrendingItem(response, trendingItems.size() + 1));
                }
            }
        } catch (Exception e) {
            if (hasSearchQuery() || reset) {
                trendingItems.clear();
            }
        }

        updateStats();
        renderTrendingGrid();
    }

    private TrendingCardItem mapToTrendingItem(AuctionListResponse response, int rank) {
        String id = response.getId() == null ? "" : response.getId().toString();

        String title = firstNonBlank(
                response.getTitle(),
                response.getItemName(),
                "Unnamed Auction"
        );

        String imageUrl = firstNonBlank(
                response.getImageUrl(),
                getDefaultImagePath(rank)
        );

        String seller = firstNonBlank(response.getSellerName(), "Verified Seller");

        String currentBid = "USD " + String.format("%,.0f", response.getCurrentPrice());

        String ending = formatEndTime(response.getEndTime());

        long views = Math.max(response.getViewCount(), 0);
        long saves = Math.max(response.getFavoriteCount(), 0);

        double score = response.getTrendingScore() > 0
                ? response.getTrendingScore()
                : response.getCurrentPrice();

        return new TrendingCardItem(
                id,
                title,
                imageUrl,
                seller,
                currentBid,
                ending,
                AuctionStateViewHelper.resolveDisplayState(
                        response.getState(),
                        response.getStartTime(),
                        response.getEndTime()
                ),
                response.getCreatedAt(),
                views,
                saves,
                response.getBidCount(),
                rank,
                score
        );
    }

    private void renderTrendingGrid() {
        if (trendingGrid == null) {
            return;
        }

        trendingGrid.getChildren().clear();

        List<TrendingCardItem> visibleItems = getVisibleItems();

        if (visibleItems.isEmpty()) {
            if (loadMoreButton != null) {
                loadMoreButton.setManaged(false);
                loadMoreButton.setVisible(false);
            }
            trendingGrid.getChildren().add(createEmptyState());
            return;
        }

        if (loadMoreButton != null) {
            loadMoreButton.setManaged(true);
            loadMoreButton.setVisible(true);
        }

        int rank = 1;
        for (TrendingCardItem item : visibleItems) {
            trendingGrid.getChildren().add(createTrendingCard(item, rank));
            rank++;
        }
    }

    private VBox createTrendingCard(TrendingCardItem item, int rank) {
        AuctionCardData cardData = new AuctionCardData(
                item.id(),
                item.sellerName().toUpperCase(Locale.ROOT),
                item.title(),
                item.currentBid(),
                "Ending: " + item.ending(),
                formatCompact(item.viewCount()) + " views / " + formatCompact(item.saveCount()) + " saves",
                item.imageUrl(),
                item.state(),
                "View Details",
                String.valueOf(Math.max(item.saveCount(), 0))
        );

        return cardFactory.createCard(
                cardData,
                320,
                250,
                favoriteAuctionIds.contains(item.id()),
                selected -> toggleFavorite(item, selected),
                () -> openDetail(item)
        );
    }

    private void openDetail(TrendingCardItem item) {
        MockData.setSelectedItem(new AuctionItem(
                item.id(),
                item.title(),
                item.imageUrl(),
                item.currentBid(),
                item.ending(),
                item.state(),
                null,
                null,
                responseFavoriteCount(item)
        ));

        SceneManager.goToProductDetail();
    }

    private void toggleFavorite(TrendingCardItem item, boolean selected) {
        if (selected) {
            favoriteAuctionIds.add(item.id());
            WishlistStateStore.add(item.id());
        } else {
            favoriteAuctionIds.remove(item.id());
            WishlistStateStore.remove(item.id());
        }
        updateWishlistButton();

        try {
            if (selected) {
                favoriteApiService.addFavorite(item.id());
            } else {
                favoriteApiService.removeFavorite(item.id());
            }
            loadFavorites();
            loadTrendingAuctions(true);
            return;
        } catch (Exception ignored) {
            if (selected) {
                favoriteAuctionIds.remove(item.id());
                WishlistStateStore.remove(item.id());
            } else {
                favoriteAuctionIds.add(item.id());
                WishlistStateStore.add(item.id());
            }
            updateWishlistButton();
        }
    }

    @FXML
    private void handleFilterHot() {
        applyFilter(TrendingFilter.HOT);
    }

    @FXML
    private void handleFilterMostViewed() {
        applyFilter(TrendingFilter.MOST_VIEWED);
    }

    @FXML
    private void handleFilterMostSaved() {
        applyFilter(TrendingFilter.MOST_SAVED);
    }

    @FXML
    private void handleLoadMore() {
        currentPage++;
        loadTrendingAuctions(false);
    }

    @FXML
    private void handleSearch() {
        currentQuery = searchField == null ? null : normalizeQuery(searchField.getText());
        loadTrendingAuctions(true);
    }

    private void applyFilter(TrendingFilter filter) {
        selectedFilter = filter;
        updateFilterButtons();
        renderTrendingGrid();
    }

    private List<TrendingCardItem> getVisibleItems() {
        List<TrendingCardItem> visibleItems = new ArrayList<>(trendingItems);
        visibleItems.removeIf(this::isDeletedAuction);

        switch (selectedFilter) {
            case HOT -> visibleItems.sort(
                    Comparator.comparingDouble(TrendingCardItem::trendingScore).reversed()
            );
            case MOST_VIEWED -> visibleItems.sort(
                    Comparator.comparingLong(TrendingCardItem::viewCount).reversed()
            );
            case MOST_SAVED -> visibleItems.sort(
                    Comparator.comparingLong(TrendingCardItem::saveCount).reversed()
            );
        }

        return visibleItems;
    }

    private boolean isDeletedAuction(TrendingCardItem item) {
        return item != null && !isVisibleBidderState(item.state());
    }

    private void updateFilterButtons() {
        setFilterButtonState(hotFilterButton, selectedFilter == TrendingFilter.HOT);
        setFilterButtonState(viewedFilterButton, selectedFilter == TrendingFilter.MOST_VIEWED);
        setFilterButtonState(savedFilterButton, selectedFilter == TrendingFilter.MOST_SAVED);
    }

    private void setFilterButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().removeAll("trending-filter-button", "trending-filter-active");
        button.getStyleClass().add(active ? "trending-filter-active" : "trending-filter-button");
    }

    @FXML
    private void handleGoToForYou() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleGoToTrending() {
    }

    @FXML
    private void handleOpenCategories() {
        SceneManager.goToCategory();
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
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
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

    private void updateStats() {
        List<TrendingCardItem> visibleItems = getVisibleItems();
        long activeBids = visibleItems.stream()
                .filter(item -> {
                    String normalized = firstNonBlank(item.state(), "").trim().toUpperCase(Locale.ROOT);
                    return "ACTIVE".equals(normalized) || "OPEN".equals(normalized) || "LIVE".equals(normalized);
                })
                .count();
        long newAuctions = visibleItems.stream()
                .filter(this::isNewAuction)
                .count();
        int totalBidsPlaced = visibleItems.stream().mapToInt(TrendingCardItem::bidCount).sum();

        if (hasSearchQuery() && visibleItems.isEmpty()) {
            activeBidsLabel.setText("0");
            newAuctionsLabel.setText("0");
            totalBidsPlacedLabel.setText("0");
            return;
        }

        activeBidsLabel.setText(formatCompact(activeBids));
        newAuctionsLabel.setText(formatCompact(newAuctions));
        totalBidsPlacedLabel.setText(formatCompact(totalBidsPlaced));
    }

    private String formatEndTime(String endTime) {
        return DateTimeDisplayHelper.formatDateTime(endTime, "Ending soon");
    }

    private String formatCompact(long value) {
        if (value >= 1000) {
            return String.format(Locale.US, "%.1fK", value / 1000.0);
        }

        return String.valueOf(value);
    }

    private long responseFavoriteCount(TrendingCardItem item) {
        return item == null ? 0 : Math.max(item.saveCount(), 0);
    }

    private void updateWishlistButton() {
        if (wishlistButton != null) {
            wishlistButton.setText("\u2661 " + WishlistStateStore.count());
        }
    }

    private boolean isNewAuction(TrendingCardItem item) {
        if (item == null || item.createdAt() == null || item.createdAt().isBlank()) {
            return false;
        }
        try {
            return Duration.between(Instant.parse(item.createdAt()), Instant.now()).toHours() <= 24;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getDefaultImagePath(int index) {
        return switch (index % 3) {
            case 1 -> "/images/item1.png";
            case 2 -> "/images/item2.png";
            default -> "/images/item3.png";
        };
    }

    private boolean isVisibleToBidder(AuctionListResponse response) {
        if (response == null) {
            return false;
        }

        return isVisibleBidderState(AuctionStateViewHelper.resolveDisplayState(
                response.getState(),
                response.getStartTime(),
                response.getEndTime()
        ));
    }

    private boolean isVisibleBidderState(String state) {
        String normalized = firstNonBlank(state, "").trim().toUpperCase(Locale.ROOT);
        return "ACTIVE".equals(normalized)
                || "OPEN".equals(normalized)
                || "LIVE".equals(normalized)
                || "SCHEDULED".equals(normalized)
                || "INCOMING".equals(normalized)
                || "PENDING".equals(normalized)
                || "DRAFT".equals(normalized);
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

    private String normalizeQuery(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private VBox createEmptyState() {
        if (hasSearchQuery()) {
            return createSearchEmptyState();
        }

        Label title = new Label("No trending auctions yet.");
        title.getStyleClass().add("trending-card-title");

        Label subtitle = new Label("Trending auctions will appear here once bidding activity starts.");
        subtitle.getStyleClass().add("trending-card-meta");

        VBox emptyState = new VBox(8, title, subtitle);
        emptyState.getStyleClass().add("trending-empty-state");
        return emptyState;
    }

    private VBox createSearchEmptyState() {
        ImageView illustration = buildSearchIllustration();

        Label title = new Label("No results found");
        title.getStyleClass().add("search-empty-title");

        Label subtitle = new Label("We couldn't find any trending auctions matching \"" + currentQuery + "\".");
        subtitle.getStyleClass().add("search-empty-copy");
        subtitle.setWrapText(true);

        Label hint = new Label("Try a broader keyword or return to the hottest live auctions.");
        hint.getStyleClass().add("search-empty-hint");
        hint.setWrapText(true);

        VBox emptyState = new VBox(18, illustration, title, subtitle, hint);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPrefWidth(1040);
        emptyState.setMinHeight(420);
        emptyState.getStyleClass().add("search-empty-state");
        return emptyState;
    }

    private ImageView buildSearchIllustration() {
        ImageView imageView = new ImageView();
        var resource = getClass().getResource("/images/item2.png");
        if (resource != null) {
            imageView.setImage(new Image(resource.toExternalForm(), true));
        }
        imageView.setFitWidth(220);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("search-empty-illustration");
        return imageView;
    }

    private boolean hasSearchQuery() {
        return currentQuery != null && !currentQuery.isBlank();
    }

    private enum TrendingFilter {
        HOT,
        MOST_VIEWED,
        MOST_SAVED
    }

    private record TrendingCardItem(
            String id,
            String title,
            String imageUrl,
            String sellerName,
            String currentBid,
            String ending,
            String state,
            String createdAt,
            long viewCount,
            long saveCount,
            int bidCount,
            int rank,
            double trendingScore
    ) {
    }
}
