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
import com.auction.client.util.DateTimeDisplayHelper;
import com.auction.client.util.MockData;
import com.auction.client.util.SearchNavigationContext;
import com.auction.client.util.WishlistStateStore;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class WonAuctionsController {

    @FXML private Label usernameLabel;
    @FXML private Label totalWinsLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label latestWinLabel;
    @FXML private Label wonMessageLabel;
    @FXML private Button wishlistButton;
    @FXML private VBox notificationsBox;
    @FXML private TilePane wonAuctionGrid;
    @FXML private TextField searchField;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final FavoriteApiService favoriteApiService = new FavoriteApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private final AuctionCardViewFactory cardFactory = new AuctionCardViewFactory(itemApiService);

    private final List<AuctionListResponse> wonAuctions = new ArrayList<>();
    private final Set<String> favoriteAuctionIds = new LinkedHashSet<>();

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole("BIDDER")) {
            SceneManager.goToAuth();
            return;
        }

        usernameLabel.setText(firstNonBlank(SessionManager.getUsername(), "Bidder"));
        updateWishlistButton();
        loadFavorites();
        loadWonAuctions();
    }

    private void loadFavorites() {
        try {
            favoriteAuctionIds.clear();
            for (AuctionDetailResponse favorite : favoriteApiService.getFavorites()) {
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

    private void loadWonAuctions() {
        UUID currentUserId = SessionManager.getUserId();
        boolean loaded = false;

        try {
            wonAuctions.clear();
            wonAuctions.addAll(filterConfirmedWins(auctionApiService.listMyWonAuctions(), currentUserId));
            loaded = true;
        } catch (Exception e) {
        }

        if (!loaded) {
            try {
                wonAuctions.clear();
                wonAuctions.addAll(loadWonAuctionsFallback());
                loaded = true;
            } catch (Exception e) {
                wonAuctions.clear();
            }
        }

        wonAuctions.sort(Comparator.comparing(this::resolveEndInstant, Comparator.nullsLast(Comparator.reverseOrder())));
        renderSummary();
        renderNotifications();
        renderWonGrid();
        showMessage(loaded
                ? (wonAuctions.isEmpty() ? "No successful auction yet." : "")
                : "Cannot load won auctions right now.");
    }

    private List<AuctionListResponse> loadWonAuctionsFallback() {
        UUID currentUserId = SessionManager.getUserId();
        if (currentUserId == null) {
            return List.of();
        }

        List<AuctionListResponse> auctions = auctionApiService
                .searchAuctions(null, null, null, 0, 100, "endTime,desc")
                .getItems();

        return filterConfirmedWins(auctions, currentUserId);
    }

    private List<AuctionListResponse> filterConfirmedWins(List<AuctionListResponse> auctions, UUID currentUserId) {
        List<AuctionListResponse> wins = new ArrayList<>();
        if (auctions == null || currentUserId == null) {
            return wins;
        }

        for (AuctionListResponse auction : auctions) {
            if (!isConfirmedWin(auction, currentUserId)) {
                continue;
            }

            if (auction.getWinnerName() == null || auction.getWinnerName().isBlank()) {
                auction.setWinnerName(firstNonBlank(SessionManager.getUsername(), "You"));
            }

            wins.add(auction);
        }

        return wins;
    }

    private boolean isConfirmedWin(AuctionListResponse auction, UUID currentUserId) {
        if (auction == null || currentUserId == null) {
            return false;
        }

        return isClosedAuction(auction) && currentUserId.equals(auction.getWinnerId());
    }

    private boolean isClosedAuction(AuctionListResponse auction) {
        if (auction == null) {
            return false;
        }
        return AuctionStateViewHelper.isClosed(
                auction.getState(),
                auction.getStartTime(),
                auction.getEndTime()
        );
    }

    private void renderSummary() {
        totalWinsLabel.setText(String.valueOf(wonAuctions.size()));

        double totalSpent = wonAuctions.stream()
                .mapToDouble(AuctionListResponse::getCurrentPrice)
                .sum();
        totalSpentLabel.setText(formatMoney(totalSpent));

        AuctionListResponse latest = wonAuctions.isEmpty() ? null : wonAuctions.get(0);
        latestWinLabel.setText(latest == null
                ? "No win yet"
                : firstNonBlank(latest.getTitle(), latest.getItemName(), "Latest win"));
    }

    private void renderNotifications() {
        notificationsBox.getChildren().clear();

        if (wonAuctions.isEmpty()) {
            notificationsBox.getChildren().add(createNotificationCard(
                    "No winner notice yet",
                    "Your successful auctions will appear here as soon as you win one.",
                    "won-notification-empty"
            ));
            return;
        }

        int limit = Math.min(3, wonAuctions.size());
        for (int i = 0; i < limit; i++) {
            AuctionListResponse auction = wonAuctions.get(i);
            String title = "You won " + firstNonBlank(auction.getTitle(), auction.getItemName(), "an auction");
            String body = formatMoney(auction.getCurrentPrice())
                    + " final price | Closed "
                    + formatDateTime(auction.getEndTime())
                    + " | Seller "
                    + firstNonBlank(auction.getSellerName(), "Seller");
            notificationsBox.getChildren().add(createNotificationCard(title, body, "won-notification-success"));
        }
    }

    private VBox createNotificationCard(String title, String body, String accentClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("won-notification-title");
        titleLabel.setWrapText(true);

        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("won-notification-body");
        bodyLabel.setWrapText(true);

        Region pulse = new Region();
        pulse.getStyleClass().add("won-notification-pulse");

        VBox copy = new VBox(6, titleLabel, bodyLabel);
        HBox row = new HBox(12, pulse, copy);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.getStyleClass().addAll("won-notification-card", accentClass);

        VBox wrapper = new VBox(row);
        return wrapper;
    }

    private void renderWonGrid() {
        wonAuctionGrid.getChildren().clear();

        if (wonAuctions.isEmpty()) {
            wonAuctionGrid.getChildren().add(createEmptyState());
            return;
        }

        for (AuctionListResponse auction : wonAuctions) {
            wonAuctionGrid.getChildren().add(createWonCard(auction));
        }
    }

    private VBox createWonCard(AuctionListResponse auction) {
        String auctionId = auction.getId() == null ? "" : auction.getId().toString();
        AuctionCardData cardData = new AuctionCardData(
                auctionId,
                firstNonBlank(auction.getCategory(), "Won Auction").toUpperCase(Locale.ROOT),
                firstNonBlank(auction.getTitle(), auction.getItemName(), "Won Auction"),
                formatMoney(auction.getCurrentPrice()),
                "Won on " + formatDateTime(auction.getEndTime()),
                "Seller " + firstNonBlank(auction.getSellerName(), "Seller") + " | Winner " + firstNonBlank(auction.getWinnerName(), "You"),
                firstNonBlank(auction.getImageUrl(), "/images/item1.png"),
                "CLOSED",
                "View Win Detail",
                String.valueOf(Math.max(auction.getFavoriteCount(), 0))
        );

        return cardFactory.createCard(
                cardData,
                320,
                230,
                favoriteAuctionIds.contains(auctionId),
                selected -> toggleFavorite(auction, selected),
                () -> openAuction(auction)
        );
    }

    private VBox createEmptyState() {
        Label title = new Label("No successful auctions yet.");
        title.getStyleClass().add("won-empty-title");

        Label subtitle = new Label("Win an auction and it will appear here with the final result.");
        subtitle.getStyleClass().add("won-empty-subtitle");
        subtitle.setWrapText(true);

        VBox box = new VBox(8, title, subtitle);
        box.getStyleClass().add("won-empty-state");
        box.setPadding(new Insets(24, 24, 24, 24));
        return box;
    }

    private void toggleFavorite(AuctionListResponse auction, boolean selected) {
        String auctionId = auction.getId() == null ? "" : auction.getId().toString();
        if (selected) {
            favoriteAuctionIds.add(auctionId);
            WishlistStateStore.add(auctionId);
        } else {
            favoriteAuctionIds.remove(auctionId);
            WishlistStateStore.remove(auctionId);
        }
        updateWishlistButton();

        if (auction.getId() == null) {
            return;
        }

        try {
            if (selected) {
                favoriteApiService.addFavorite(auctionId);
            } else {
                favoriteApiService.removeFavorite(auctionId);
            }
            loadFavorites();
            loadWonAuctions();
            return;
        } catch (Exception ignored) {
            if (selected) {
                favoriteAuctionIds.remove(auctionId);
                WishlistStateStore.remove(auctionId);
            } else {
                favoriteAuctionIds.add(auctionId);
                WishlistStateStore.add(auctionId);
            }
            updateWishlistButton();
        }
    }

    private void openAuction(AuctionListResponse auction) {
        String title = firstNonBlank(auction.getTitle(), auction.getItemName(), "Won Auction");
        MockData.setSelectedItem(new AuctionItem(
                auction.getId() == null ? "" : auction.getId().toString(),
                title,
                firstNonBlank(auction.getImageUrl(), "/images/item1.png"),
                formatMoney(auction.getCurrentPrice()),
                formatDateTime(auction.getEndTime()),
                "CLOSED"
        ));
        SceneManager.goToProductDetail();
    }

    @FXML
    private void handleRefresh() {
        loadFavorites();
        loadWonAuctions();
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

    private void showMessage(String message) {
        wonMessageLabel.setText(message == null ? "" : message);
        boolean visible = message != null && !message.isBlank();
        wonMessageLabel.setManaged(visible);
        wonMessageLabel.setVisible(visible);
    }

    private String formatMoney(double value) {
        return "USD " + String.format("%,.0f", value);
    }

    private String formatDateTime(String value) {
        return DateTimeDisplayHelper.formatDateTime(value, "N/A");
    }

    private void updateWishlistButton() {
        if (wishlistButton != null) {
            wishlistButton.setText("\u2661 " + WishlistStateStore.count());
        }
    }

    private Instant resolveEndInstant(AuctionListResponse auction) {
        if (auction == null || auction.getEndTime() == null || auction.getEndTime().isBlank()) {
            return null;
        }

        try {
            return Instant.parse(auction.getEndTime());
        } catch (Exception ignored) {
        }

        try {
            String normalized = auction.getEndTime().trim().replace(" ", "T");
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }
            return LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception ignored) {
            return null;
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
