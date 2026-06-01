package com.auction.client.ui;

import com.auction.client.service.ItemApiService;
import com.auction.client.util.FavoriteUiStateStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.function.Consumer;

public class AuctionCardViewFactory {

    private final ItemApiService itemApiService;

    public AuctionCardViewFactory(ItemApiService itemApiService) {
        this.itemApiService = itemApiService;
    }

    public VBox createCard(AuctionCardData data,
                           double cardWidth,
                           double imageHeight,
                           boolean favoriteSelected,
                           Consumer<Boolean> onFavoriteChanged,
                           Runnable onOpen) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(cardWidth - 2);
        imageView.setFitHeight(imageHeight);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("market-card-image");
        bindCardImage(imageView, data.imagePath());

        StackPane mediaPane = new StackPane(imageView);
        mediaPane.getStyleClass().add("market-card-media");

        String badgeText = resolveBadgeText(data.badgeText());
        String badgeStateClass = resolveBadgeStateClass(data.badgeText());
        if (badgeText != null && !badgeText.isBlank() && badgeStateClass != null) {
            Label badge = new Label(badgeText);
            badge.getStyleClass().add("market-card-badge");
            badge.getStyleClass().add(badgeStateClass);
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            mediaPane.getChildren().add(badge);
        }

        FavoriteUiStateStore.FavoriteState favoriteState = FavoriteUiStateStore.get(data.id());
        boolean initialSelected = favoriteState != null ? favoriteState.selected() : favoriteSelected;
        int[] favoriteCount = {favoriteState != null ? favoriteState.count() : parseFavoriteCount(data.favoriteCountText())};
        Button favoriteButton = new Button(buildFavoriteText(favoriteCount[0], initialSelected));
        favoriteButton.setFocusTraversable(false);
        favoriteButton.getStyleClass().add("market-card-favorite");
        if (initialSelected) {
            favoriteButton.getStyleClass().add("market-card-favorite-active");
        }
        favoriteButton.setOnAction(event -> {
            boolean next = !favoriteButton.getStyleClass().contains("market-card-favorite-active");
            favoriteCount[0] = next
                    ? favoriteCount[0] + 1
                    : Math.max(0, favoriteCount[0] - 1);
            FavoriteUiStateStore.put(data.id(), next, favoriteCount[0]);
            favoriteButton.setText(buildFavoriteText(favoriteCount[0], next));
            favoriteButton.getStyleClass().remove("market-card-favorite-active");
            if (next) {
                favoriteButton.getStyleClass().add("market-card-favorite-active");
            }
            if (onFavoriteChanged != null) {
                onFavoriteChanged.accept(next);
            }
        });
        StackPane.setAlignment(favoriteButton, Pos.TOP_RIGHT);
        StackPane.setMargin(favoriteButton, new Insets(10, 10, 0, 0));
        mediaPane.getChildren().add(favoriteButton);

        Label eyebrowLabel = new Label(firstNonBlank(data.eyebrow(), ""));
        eyebrowLabel.getStyleClass().add("market-card-eyebrow");

        Label titleLabel = new Label(firstNonBlank(data.title(), "Untitled auction"));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("market-card-title");

        Label priceLabel = new Label(firstNonBlank(data.priceText(), ""));
        priceLabel.getStyleClass().add("market-card-price");
        priceLabel.setVisible(data.priceText() != null && !data.priceText().isBlank());
        priceLabel.setManaged(priceLabel.isVisible());

        Label statusLabel = new Label(firstNonBlank(data.statusText(), ""));
        statusLabel.getStyleClass().add("market-card-status");
        statusLabel.setVisible(data.statusText() != null && !data.statusText().isBlank());
        statusLabel.setManaged(statusLabel.isVisible());

        Label metaLabel = new Label(firstNonBlank(data.metaText(), ""));
        metaLabel.getStyleClass().add("market-card-meta");
        metaLabel.setWrapText(true);
        metaLabel.setVisible(data.metaText() != null && !data.metaText().isBlank());
        metaLabel.setManaged(metaLabel.isVisible());

        Button actionButton = new Button(firstNonBlank(data.actionText(), "View Details"));
        actionButton.getStyleClass().add("market-card-action");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setOnAction(event -> {
            if (onOpen != null) {
                onOpen.run();
            }
        });

        VBox body = new VBox(6, eyebrowLabel, titleLabel, priceLabel, statusLabel, metaLabel);
        body.getStyleClass().add("market-card-body");
        VBox.setVgrow(titleLabel, Priority.NEVER);

        HBox footer = new HBox(actionButton);
        footer.getStyleClass().add("market-card-footer");
        HBox.setHgrow(actionButton, Priority.ALWAYS);

        VBox card = new VBox(12, mediaPane, body, footer);
        card.getStyleClass().add("market-card");
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);

        return card;
    }

    private void bindCardImage(ImageView imageView, String imagePath) {
        try {
            imageView.setImage(itemApiService.loadImage(imagePath));
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    private String buildFavoriteText(int count, boolean selected) {
        String icon = selected ? "\u2665" : "\u2661";
        return icon + " " + count;
    }

    private int parseFavoriteCount(String countText) {
        if (countText == null || countText.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(countText.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveBadgeText(String badgeText) {
        if (badgeText == null || badgeText.isBlank()) {
            return badgeText;
        }

        return switch (badgeText.trim().toUpperCase(Locale.ROOT)) {
            case "FINISHED", "CANCELLED", "ENDED" -> "CLOSED";
            default -> badgeText.trim();
        };
    }

    private String resolveBadgeStateClass(String badgeText) {
        if (badgeText == null || badgeText.isBlank()) {
            return null;
        }

        String normalized = badgeText.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVE", "OPEN", "LIVE" -> "market-card-badge-active";
            case "SCHEDULED", "DRAFT", "INCOMING", "PENDING" -> "market-card-badge-pending";
            case "FINISHED", "CANCELLED", "CLOSED", "ENDED" -> "market-card-badge-finished";
            case "DELETED" -> "market-card-badge-deleted";
            case "REJECTED" -> "market-card-badge-rejected";
            default -> normalized.contains("TRENDING") ? "market-card-badge-trending" : null;
        };
    }
}
