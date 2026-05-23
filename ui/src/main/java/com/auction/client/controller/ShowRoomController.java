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
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.geometry.Pos;


public class ShowRoomController {

    @FXML private Label usernameLabel;
    @FXML private Button wishlistButton;
    @FXML private Label savedAuctionsCountLabel;
    @FXML private TilePane auctionGrid;
    @FXML private Button allFilterButton;
    @FXML private Button endingSoonFilterButton;
    @FXML private Button newListingsFilterButton;
    @FXML private Button highDemandFilterButton;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final List<AuctionItem> items = new ArrayList<>();
    private final Set<String> favoriteAuctionIds = new LinkedHashSet<>();
    private ForYouFilter selectedFilter = ForYouFilter.ALL;

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
            List<AuctionListResponse> responses =
                    auctionApiService.getForYouAuctions(null, null, null, 0, 12).getItems();

            items.clear();

            for (int i = 0; i < responses.size(); i++) {
                items.add(mapToAuctionItem(responses.get(i), i));
            }

            if (items.isEmpty()) {
                loadMockAuctionsForDemo();
            }

            renderAuctionGrid();

        } catch (Exception e) {
            loadMockAuctionsForDemo();
            renderAuctionGrid();
        }
    }

    private void loadMockAuctionsForDemo() {
        items.clear();
        items.addAll(MockData.getMockAuctionItems());
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
        applyFilter(ForYouFilter.HIGH_DEMAND);
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
        ImageView imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(230);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("auction-card-image");
        bindCardImage(imageView, item.getImagePath());

        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("favorite-floating-button");
        favoriteButton.setFocusTraversable(false);
        updateFavoriteButton(favoriteButton, item);
        favoriteButton.setOnAction(event -> toggleFavorite(item, favoriteButton));

        StackPane media = new StackPane(imageView, favoriteButton);
        StackPane.setAlignment(favoriteButton, Pos.TOP_RIGHT);
        StackPane.setMargin(favoriteButton, new Insets(8, 8, 0, 0));

        Label statusLabel = new Label(item.getStatus());
        statusLabel.getStyleClass().add("auction-seller-label");

        Label titleLabel = new Label(item.getName());
        titleLabel.getStyleClass().add("auction-card-title");

        Label bidLabel = new Label("Current Bid: " + item.getCurrentBid());
        bidLabel.getStyleClass().add("auction-price-label");

        Label endLabel = new Label("Ends: " + item.getTimeLeft());
        endLabel.getStyleClass().add("auction-ending-label");

        Button viewButton = new Button("View Details");
        viewButton.setMaxWidth(Double.MAX_VALUE);
        viewButton.getStyleClass().add("ghost-button");
        viewButton.setOnAction(event -> openDetail(item));

        VBox card = new VBox(8, media, statusLabel, titleLabel, bidLabel, endLabel, viewButton);
        card.getStyleClass().add("for-you-auction-card");
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        return card;
    }

    private VBox createEmptyState() {
        Label title = new Label("No auctions match this filter.");
        title.getStyleClass().add("auction-card-title");

        Label subtitle = new Label("Try All to see the current For You demo auctions.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox emptyState = new VBox(8, title, subtitle);
        emptyState.getStyleClass().add("for-you-empty-state");
        return emptyState;
    }

    private void openDetail(AuctionItem item) {
        MockData.setSelectedItem(item);
        SceneManager.goToProductDetail();
    }

    private void toggleFavorite(AuctionItem item, Button button) {
        boolean wasFavorite = favoriteAuctionIds.contains(item.getId());

        if (wasFavorite) {
            favoriteAuctionIds.remove(item.getId());
        } else {
            favoriteAuctionIds.add(item.getId());
        }

        try {
            if (wasFavorite) {
                favoriteApiService.removeFavorite(item.getId());
            } else {
                favoriteApiService.addFavorite(item.getId());
            }
        } catch (Exception ignored) {
        }

        updateFavoriteButton(button, item);
        updateWishlistUi();
    }

    private void applyFilter(ForYouFilter filter) {
        selectedFilter = filter;
        updateFilterButtons();
        renderAuctionGrid();
    }

    private List<AuctionItem> getVisibleItems() {
        List<AuctionItem> visibleItems = new ArrayList<>(items);

        switch (selectedFilter) {
            case ENDING_SOON -> visibleItems.removeIf(item -> !hasEndingSoonStatus(item));
            case NEW_LISTINGS -> visibleItems.removeIf(item -> !hasNewListingStatus(item));
            case HIGH_DEMAND -> visibleItems.sort(Comparator.comparingDouble(this::parseBidAmount).reversed());
            case ALL -> {
            }
        }

        return visibleItems;
    }

    private boolean hasEndingSoonStatus(AuctionItem item) {
        String status = normalize(item.getStatus());
        String timeLeft = normalize(item.getTimeLeft());
        return status.contains("ending") || timeLeft.contains("h") && !timeLeft.contains("d");
    }

    private boolean hasNewListingStatus(AuctionItem item) {
        String status = normalize(item.getStatus());
        return status.contains("upcoming") || status.contains("new") || status.contains("scheduled");
    }

    private double parseBidAmount(AuctionItem item) {
        String bid = item.getCurrentBid();
        if (bid == null || bid.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(bid.replaceAll("[^0-9.]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void updateFilterButtons() {
        setFilterButtonState(allFilterButton, selectedFilter == ForYouFilter.ALL);
        setFilterButtonState(endingSoonFilterButton, selectedFilter == ForYouFilter.ENDING_SOON);
        setFilterButtonState(newListingsFilterButton, selectedFilter == ForYouFilter.NEW_LISTINGS);
        setFilterButtonState(highDemandFilterButton, selectedFilter == ForYouFilter.HIGH_DEMAND);
    }

    private void setFilterButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
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

    private enum ForYouFilter {
        ALL,
        ENDING_SOON,
        NEW_LISTINGS,
        HIGH_DEMAND
    }
}
