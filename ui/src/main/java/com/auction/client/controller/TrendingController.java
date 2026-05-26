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
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
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
        } catch (Exception ignored) {
            favoriteAuctionIds.clear();
        }
    }

    private void loadTrendingAuctions(boolean reset) {
        if (reset) {
            currentPage = 0;
            trendingItems.clear();
        }

        try {
            List<AuctionListResponse> responses =
                    auctionApiService.getTrendingAuctions(null, null, null, currentPage, PAGE_SIZE).getItems();

            if (responses != null) {
                for (int i = 0; i < responses.size(); i++) {
                    trendingItems.add(mapToTrendingItem(responses.get(i), trendingItems.size() + 1));
                }
            }

            if (trendingItems.isEmpty()) {
                loadMockTrendingItems();
            }

        } catch (Exception e) {
            if (trendingItems.isEmpty()) {
                loadMockTrendingItems();
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

        long views = response.getViewCount() > 0 ? response.getViewCount() : 1200;
        long saves = response.getFavoriteCount() > 0 ? response.getFavoriteCount() : 234;

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
                views,
                saves,
                response.getBidCount(),
                rank,
                score
        );
    }

    private void loadMockTrendingItems() {
        trendingItems.clear();

        List<AuctionItem> mockItems = MockData.getMockAuctionItems();

        int rank = 1;
        for (AuctionItem item : mockItems) {
            trendingItems.add(new TrendingCardItem(
                    item.getId(),
                    item.getName(),
                    item.getImagePath(),
                    "Verified Seller",
                    item.getCurrentBid().replace("$", "USD "),
                    item.getTimeLeft(),
                    1200 + rank * 150,
                    234 + rank * 12,
                    8 + rank,
                    rank,
                    1000 - rank
            ));
            rank++;
        }
    }

    private void renderTrendingGrid() {
        if (trendingGrid == null) {
            return;
        }

        trendingGrid.getChildren().clear();

        List<TrendingCardItem> visibleItems = getVisibleItems();

        if (visibleItems.isEmpty()) {
            trendingGrid.getChildren().add(createEmptyState());
            return;
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
                "#" + rank + " Trending",
                "View Details",
                String.valueOf(item.saveCount())
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
                "TRENDING"
        ));

        SceneManager.goToProductDetail();
    }

    private void toggleFavorite(TrendingCardItem item, boolean selected) {
        if (selected) {
            favoriteAuctionIds.add(item.id());
        } else {
            favoriteAuctionIds.remove(item.id());
        }

        try {
            if (selected) {
                favoriteApiService.addFavorite(item.id());
            } else {
                favoriteApiService.removeFavorite(item.id());
            }
        } catch (Exception ignored) {
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

    private void applyFilter(TrendingFilter filter) {
        selectedFilter = filter;
        updateFilterButtons();
        renderTrendingGrid();
    }

    private List<TrendingCardItem> getVisibleItems() {
        List<TrendingCardItem> visibleItems = new ArrayList<>(trendingItems);

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
        int totalBids = trendingItems.stream().mapToInt(TrendingCardItem::bidCount).sum();

        activeBidsLabel.setText(totalBids > 0 ? formatCompact(totalBids) : "1.2K");
        newAuctionsLabel.setText(String.valueOf(Math.max(trendingItems.size(), 0)));
        totalBidsPlacedLabel.setText(totalBids > 0 ? formatCompact(totalBids) : "5.3K");
    }

    private String formatEndTime(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return "Ending soon";
        }

        return endTime.length() >= 16
                ? endTime.substring(0, 16).replace("T", " ")
                : endTime;
    }

    private String formatCompact(long value) {
        if (value >= 1000) {
            return String.format("%.1fK", value / 1000.0);
        }

        return String.valueOf(value);
    }

    private String getDefaultImagePath(int index) {
        return switch (index % 3) {
            case 1 -> "/images/item1.png";
            case 2 -> "/images/item2.png";
            default -> "/images/item3.png";
        };
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

    private VBox createEmptyState() {
        Label title = new Label("No trending auctions yet.");
        title.getStyleClass().add("trending-card-title");

        Label subtitle = new Label("Trending auctions will appear here once bidding activity starts.");
        subtitle.getStyleClass().add("trending-card-meta");

        VBox emptyState = new VBox(8, title, subtitle);
        emptyState.getStyleClass().add("trending-empty-state");
        return emptyState;
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
            long viewCount,
            long saveCount,
            int bidCount,
            int rank,
            double trendingScore
    ) {
    }
}
