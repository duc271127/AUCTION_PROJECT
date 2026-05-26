package com.auction.client.ui;

import com.auction.client.service.ItemApiService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

        if (data.badgeText() != null && !data.badgeText().isBlank()) {
            Label badge = new Label(data.badgeText());
            badge.getStyleClass().add("market-card-badge");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            mediaPane.getChildren().add(badge);
        }

        Button favoriteButton = new Button(buildFavoriteText(data.favoriteCountText(), favoriteSelected));
        favoriteButton.setFocusTraversable(false);
        favoriteButton.getStyleClass().add("market-card-favorite");
        if (favoriteSelected) {
            favoriteButton.getStyleClass().add("market-card-favorite-active");
        }
        favoriteButton.setOnAction(event -> {
            boolean next = !favoriteButton.getStyleClass().contains("market-card-favorite-active");
            favoriteButton.setText(buildFavoriteText(data.favoriteCountText(), next));
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

    private String buildFavoriteText(String countText, boolean selected) {
        String icon = selected ? "\u2665" : "\u2661";
        return countText == null || countText.isBlank() ? icon : icon + " " + countText;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
