package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.ItemApiService;
import com.auction.client.ui.AuctionCardData;
import com.auction.client.ui.AuctionCardViewFactory;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

import java.util.ArrayList;
import java.util.List;

public class HomeController {

    @FXML private ImageView heroImageView;
    @FXML private TilePane auctionGrid;
    @FXML private TilePane forYouGrid;
    @FXML private TilePane trendingGrid;
    @FXML private TextField searchField;

    private final ItemApiService itemApiService = new ItemApiService();
    private final AuctionCardViewFactory cardFactory = new AuctionCardViewFactory(itemApiService);

    @FXML
    public void initialize() {
        bindHeroImage();
        renderSections();
    }

    @FXML
    private void handleGoToAuth() {
        SceneManager.goToAuth();
    }

    @FXML
    private void handleGoToShowroom() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleGoToTrending() {
        SceneManager.goToTrending();
    }

    @FXML
    private void handleGoToCategory() {
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

    private void bindHeroImage() {
        if (heroImageView == null) {
            return;
        }

        try {
            heroImageView.setImage(itemApiService.loadImage("/images/item1.png"));
        } catch (Exception e) {
            heroImageView.setImage(null);
        }
    }

    private void renderSections() {
        List<AuctionItem> items = MockData.getMockAuctionItems();

        renderGrid(auctionGrid, buildCards(items.subList(0, Math.min(4, items.size())), "View Item", null));
        renderGrid(forYouGrid, buildCards(items.subList(2, Math.min(6, items.size())), "Start Bidding", null));
        renderGrid(trendingGrid, buildCards(items.subList(4, Math.min(8, items.size())), "View Details", "#"));
    }

    private List<AuctionCardData> buildCards(List<AuctionItem> items, String actionText, String badgePrefix) {
        List<AuctionCardData> cards = new ArrayList<>();
        int rank = 1;
        for (AuctionItem item : items) {
            String badge = badgePrefix == null ? null : badgePrefix + rank + " Trending";
            cards.add(new AuctionCardData(
                    item.getId(),
                    item.getStatus(),
                    item.getName(),
                    item.getCurrentBid(),
                    item.getTimeLeft(),
                    "Curated by experts",
                    item.getImagePath(),
                    badge,
                    actionText,
                    "36"
            ));
            rank++;
        }
        return cards;
    }

    private void renderGrid(TilePane grid, List<AuctionCardData> cards) {
        if (grid == null) {
            return;
        }

        grid.getChildren().clear();
        for (AuctionCardData card : cards) {
            grid.getChildren().add(cardFactory.createCard(
                    card,
                    256,
                    254,
                    false,
                    next -> { },
                    this::handleGoToShowroom
            ));
        }
    }
}
