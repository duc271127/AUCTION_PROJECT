package com.auction.client.controller;

import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.FavoriteApiService;
import com.auction.client.service.ItemApiService;
import com.auction.client.session.SessionManager;
import com.auction.client.ui.AuctionCardData;
import com.auction.client.ui.AuctionCardViewFactory;
import com.auction.client.util.AuctionStateViewHelper;
import com.auction.client.util.MockData;
import com.auction.client.util.SearchNavigationContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ShowRoomController {

    @FXML private Label usernameLabel;
    @FXML private Button wishlistButton;
    @FXML private Label savedAuctionsCountLabel;
    @FXML private TilePane auctionGrid;
    @FXML private Button allFilterButton;
    @FXML private Button endingSoonFilterButton;
    @FXML private Button newListingsFilterButton;
    @FXML private Button highDemandFilterButton;
    @FXML private TextField searchField;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final AuctionCardViewFactory cardFactory = new AuctionCardViewFactory(itemApiService);
    private final List<AuctionItem> items = new ArrayList<>();
    private final Set<String> favoriteAuctionIds = new LinkedHashSet<>();
    private ForYouFilter selectedFilter = ForYouFilter.ALL;
    private String currentQuery;

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }

        usernameLabel.setText(firstNonBlank(SessionManager.getUsername(), "Bidder"));
        currentQuery = SearchNavigationContext.consumePendingQuery();
        if (searchField != null && currentQuery != null) {
            searchField.setText(currentQuery);
        }
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
            List<AuctionListResponse> responses =
                    auctionApiService.searchAuctions(null, currentQuery, null, 0, 24, "startTime,asc").getItems();

            items.clear();

            if (responses != null) {
                List<AuctionListResponse> sortedResponses = responses.stream()
                        .filter(this::isVisibleToBidder)
                        .sorted(Comparator
                                .comparingInt(this::auctionDisplayPriority)
                                .thenComparing(AuctionListResponse::getStartTime, Comparator.nullsLast(String::compareTo))
                                .thenComparing(AuctionListResponse::getEndTime, Comparator.nullsLast(String::compareTo)))
                        .toList();

                for (int i = 0; i < sortedResponses.size(); i++) {
                    items.add(mapToAuctionItem(sortedResponses.get(i), i));
                }
            }

            renderAuctionGrid();

        } catch (Exception e) {
            items.clear();
            renderAuctionGrid();
        }
    }

    private AuctionItem mapToAuctionItem(AuctionListResponse response, int index) {
        String imagePath = response.getImageUrl() == null || response.getImageUrl().isBlank()
                ? getDefaultImagePath(index)
                : response.getImageUrl();
        String currentBid = "USD " + String.format("%,.0f", response.getCurrentPrice());
        String timeInfo = formatEndTime(response.getEndTime());
        String status = AuctionStateViewHelper.resolveDisplayState(
                response.getState(),
                response.getStartTime(),
                response.getEndTime()
        );
        String idValue = response.getId() != null ? response.getId().toString() : "";
        String title = response.getTitle() != null && !response.getTitle().isBlank()
                ? response.getTitle()
                : response.getItemName();

        return new AuctionItem(
                idValue,
                title == null ? "Unnamed Item" : title,
                imagePath,
                currentBid,
                timeInfo,
                status,
                response.getCreatedAt(),
                response.getEndTime()
        );
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

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        SceneManager.goToAuth();
    }

    @FXML
    private void handleFilterAll() {
        applyFilter(ForYouFilter.ALL);
    }

    @FXML
    private void handleFilterEndingSoon() {
        applyFilter(ForYouFilter.ENDING_SOON);
    }

    @FXML
    private void handleFilterNewListings() {
        applyFilter(ForYouFilter.NEW_LISTINGS);
    }

    @FXML
    private void handleFilterHighDemand() {
        applyFilter(ForYouFilter.MY_WISHLIST);
    }

    @FXML
    private void handleGoToForYou() {
        currentQuery = null;
        if (searchField != null) {
            searchField.clear();
        }
        loadFavorites();
        loadAuctionList();
        updateWishlistUi();
    }

    @FXML
    private void handleSearch() {
        currentQuery = searchField == null ? null : normalizeQuery(searchField.getText());
        loadAuctionList();
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

    private void renderAuctionGrid() {
        if (auctionGrid == null) {
            return;
        }

        auctionGrid.getChildren().clear();

        List<AuctionItem> visibleItems = getVisibleItems();

        if (visibleItems.isEmpty()) {
            auctionGrid.getChildren().add(createEmptyState());
            return;
        }

        for (AuctionItem item : visibleItems) {
            auctionGrid.getChildren().add(createAuctionCard(item));
        }
    }

    private VBox createAuctionCard(AuctionItem item) {
        AuctionCardData cardData = new AuctionCardData(
                item.getId(),
                "",
                item.getName(),
                item.getCurrentBid(),
                item.getTimeLeft(),
                "Personalized for you",
                item.getImagePath(),
                item.getStatus(),
                "View Details",
                "0"
        );

        return cardFactory.createCard(
                cardData,
                320,
                230,
                favoriteAuctionIds.contains(item.getId()),
                selected -> toggleFavorite(item, selected),
                () -> openDetail(item)
        );
    }

    private VBox createEmptyState() {
        if (hasSearchQuery()) {
            return createSearchEmptyState();
        }

        Label title = new Label("No active auctions match this filter.");
        title.getStyleClass().add("auction-card-title");

        Label subtitle = new Label("Only scheduled or live auctions are shown to bidders.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox emptyState = new VBox(8, title, subtitle);
        emptyState.getStyleClass().add("for-you-empty-state");
        return emptyState;
    }

    private VBox createSearchEmptyState() {
        ImageView illustration = buildSearchIllustration();

        Label title = new Label("No results found");
        title.getStyleClass().add("search-empty-title");

        Label subtitle = new Label("We couldn't find any auctions matching \"" + currentQuery + "\".");
        subtitle.getStyleClass().add("search-empty-copy");
        subtitle.setWrapText(true);

        Label hint = new Label("Try a different keyword or browse categories for more live auctions.");
        hint.getStyleClass().add("search-empty-hint");
        hint.setWrapText(true);

        VBox emptyState = new VBox(18, illustration, title, subtitle, hint);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPrefWidth(1040);
        emptyState.setMinHeight(460);
        emptyState.getStyleClass().add("search-empty-state");
        return emptyState;
    }

    private ImageView buildSearchIllustration() {
        ImageView imageView = new ImageView();
        var resource = getClass().getResource("/images/item3.png");
        if (resource != null) {
            imageView.setImage(new Image(resource.toExternalForm(), true));
        }
        imageView.setFitWidth(220);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("search-empty-illustration");
        return imageView;
    }

    private void openDetail(AuctionItem item) {
        MockData.setSelectedItem(item);
        SceneManager.goToProductDetail();
    }

    private void toggleFavorite(AuctionItem item, boolean selected) {
        if (selected) {
            favoriteAuctionIds.add(item.getId());
        } else {
            favoriteAuctionIds.remove(item.getId());
        }

        try {
            if (selected) {
                favoriteApiService.addFavorite(item.getId());
            } else {
                favoriteApiService.removeFavorite(item.getId());
            }
        } catch (Exception ignored) {
        }

        updateWishlistUi();
        renderAuctionGrid();
    }

    private void applyFilter(ForYouFilter filter) {
        selectedFilter = filter;
        updateFilterButtons();
        renderAuctionGrid();
    }

    private List<AuctionItem> getVisibleItems() {
        List<AuctionItem> visibleItems = new ArrayList<>(items);
        visibleItems.removeIf(this::isDeletedAuction);

        switch (selectedFilter) {
            case ENDING_SOON -> {
                visibleItems.removeIf(item -> !hasEndingSoonStatus(item));
                visibleItems.sort(Comparator.comparing(this::parseAuctionEndInstant, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            case NEW_LISTINGS -> {
                visibleItems.removeIf(item -> !hasNewListingStatus(item));
                visibleItems.sort(Comparator.comparing(this::parseAuctionCreatedInstant, Comparator.nullsLast(Comparator.reverseOrder())));
            }
            case MY_WISHLIST -> visibleItems.removeIf(item -> !favoriteAuctionIds.contains(item.getId()));
            case ALL -> {
            }
        }

        return visibleItems;
    }

    private boolean isDeletedAuction(AuctionItem item) {
        return item != null && !isVisibleBidderState(item.getStatus());
    }

    private int auctionDisplayPriority(AuctionListResponse response) {
        String state = response == null ? "" : AuctionStateViewHelper.resolveDisplayState(
                response.getState(),
                response.getStartTime(),
                response.getEndTime()
        );

        return switch (state) {
            case "SCHEDULED" -> 0;
            case "ACTIVE" -> 1;
            case "CLOSED" -> 2;
            case "REJECTED" -> 3;
            case "DELETED" -> 4;
            default -> 5;
        };
    }

    private boolean isVisibleToBidder(AuctionListResponse response) {
        if (response == null) {
            return false;
        }

        String state = AuctionStateViewHelper.resolveDisplayState(
                response.getState(),
                response.getStartTime(),
                response.getEndTime()
        );
        return isVisibleBidderState(state);
    }

    private boolean isVisibleBidderState(String state) {
        String normalized = normalize(state).toUpperCase(Locale.ROOT);
        return "ACTIVE".equals(normalized)
                || "OPEN".equals(normalized)
                || "LIVE".equals(normalized)
                || "SCHEDULED".equals(normalized)
                || "INCOMING".equals(normalized)
                || "PENDING".equals(normalized)
                || "DRAFT".equals(normalized);
    }

    private boolean hasEndingSoonStatus(AuctionItem item) {
        if (item == null) {
            return false;
        }

        String normalizedState = firstNonBlank(item.getStatus(), "").trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalizedState) && !"OPEN".equals(normalizedState) && !"LIVE".equals(normalizedState)) {
            return false;
        }

        Instant endInstant = parseInstant(item.getEndTime());
        if (endInstant == null) {
            return false;
        }

        long secondsRemaining = Duration.between(Instant.now(), endInstant).getSeconds();
        return secondsRemaining > 0 && secondsRemaining <= 600;
    }

    private boolean hasNewListingStatus(AuctionItem item) {
        if (item == null) {
            return false;
        }

        Instant createdAt = parseInstant(item.getCreatedAt());
        if (createdAt == null) {
            return false;
        }

        String normalizedState = firstNonBlank(item.getStatus(), "").trim().toUpperCase(Locale.ROOT);
        boolean eligibleState = "ACTIVE".equals(normalizedState)
                || "OPEN".equals(normalizedState)
                || "LIVE".equals(normalizedState)
                || "SCHEDULED".equals(normalizedState)
                || "INCOMING".equals(normalizedState)
                || "PENDING".equals(normalizedState)
                || "DRAFT".equals(normalizedState);
        if (!eligibleState) {
            return false;
        }

        return Duration.between(createdAt, Instant.now()).toHours() <= 24;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void updateFilterButtons() {
        setFilterButtonState(allFilterButton, selectedFilter == ForYouFilter.ALL);
        setFilterButtonState(endingSoonFilterButton, selectedFilter == ForYouFilter.ENDING_SOON);
        setFilterButtonState(newListingsFilterButton, selectedFilter == ForYouFilter.NEW_LISTINGS);
        setFilterButtonState(highDemandFilterButton, selectedFilter == ForYouFilter.MY_WISHLIST);
    }

    private void setFilterButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
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

    private String normalizeQuery(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasSearchQuery() {
        return currentQuery != null && !currentQuery.isBlank();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Instant parseAuctionCreatedInstant(AuctionItem item) {
        return item == null ? null : parseInstant(item.getCreatedAt());
    }

    private Instant parseAuctionEndInstant(AuctionItem item) {
        return item == null ? null : parseInstant(item.getEndTime());
    }

    private enum ForYouFilter {
        ALL,
        ENDING_SOON,
        NEW_LISTINGS,
        MY_WISHLIST
    }
}
