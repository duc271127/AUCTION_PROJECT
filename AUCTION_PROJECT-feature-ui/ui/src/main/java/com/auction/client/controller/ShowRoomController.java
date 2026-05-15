package com.auction.client.controller;

import com.auction.client.dto.response.AuctionListResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

public class ShowRoomController {

    @FXML private Label item1NameLabel;
    @FXML private Label item1BidLabel;
    @FXML private Label item1TimeLabel;
    @FXML private Label item1StatusLabel;
    @FXML private ImageView item1ImageView;

    @FXML private Label item2NameLabel;
    @FXML private Label item2BidLabel;
    @FXML private Label item2TimeLabel;
    @FXML private Label item2StatusLabel;
    @FXML private ImageView item2ImageView;

    @FXML private Label item3NameLabel;
    @FXML private Label item3BidLabel;
    @FXML private Label item3TimeLabel;
    @FXML private Label item3StatusLabel;
    @FXML private ImageView item3ImageView;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private List<AuctionItem> items = new ArrayList<>();

    @FXML
    public void initialize() {
        loadAuctionList();
    }

    private void loadAuctionList() {
        try {
            List<AuctionListResponse> responses = auctionApiService.getAuctions();
            items.clear();

            for (int i = 0; i < responses.size(); i++) {
                items.add(mapToAuctionItem(responses.get(i), i));
            }

            bindCards();
        } catch (Exception e) {
            showFallbackState("Cannot load auctions.");
        }
    }

    private AuctionItem mapToAuctionItem(AuctionListResponse response, int index) {
        String imagePath = getDefaultImagePath(index);

        String currentBid = "$" + String.format("%,.0f", response.getCurrentPrice());

        String timeInfo = formatEndTime(response.getEndTime());

        String status = response.getState() == null ? "UNKNOWN" : response.getState();

        String idValue = response.getId() != null ? response.getId().toString() : "";

        return new AuctionItem(
                idValue,
                response.getItemName() == null ? "Unnamed Item" : response.getItemName(),
                imagePath,
                currentBid,
                timeInfo,
                status
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

        // Backend trả ISO datetime, trước mắt rút gọn cho dễ nhìn
        if (endTime.length() >= 16) {
            return endTime.substring(0, 16).replace("T", " ");
        }

        return endTime;
    }

    private void bindCards() {
        bindSingleCard(
                0,
                item1NameLabel, item1BidLabel, item1TimeLabel, item1StatusLabel, item1ImageView
        );
        bindSingleCard(
                1,
                item2NameLabel, item2BidLabel, item2TimeLabel, item2StatusLabel, item2ImageView
        );
        bindSingleCard(
                2,
                item3NameLabel, item3BidLabel, item3TimeLabel, item3StatusLabel, item3ImageView
        );
    }

    private void bindSingleCard(int index,
                                Label nameLabel,
                                Label bidLabel,
                                Label timeLabel,
                                Label statusLabel,
                                ImageView imageView) {

        if (index >= items.size()) {
            nameLabel.setText("No auction");
            bidLabel.setText("Current Bid: -");
            timeLabel.setText("Ends: -");
            statusLabel.setText("N/A");
            imageView.setImage(null);
            return;
        }

        AuctionItem item = items.get(index);

        nameLabel.setText(item.getName());
        bidLabel.setText("Current Bid: " + item.getCurrentBid());
        timeLabel.setText("Ends: " + item.getTimeLeft());
        statusLabel.setText(item.getStatus());

        try {
            Image image = new Image(getClass().getResourceAsStream(item.getImagePath()));
            imageView.setImage(image);
        } catch (Exception e) {
            imageView.setImage(null);
            System.out.println("Image not found: " + item.getImagePath());
        }
    }

    private void showFallbackState(String message) {
        item1NameLabel.setText(message);
        item1BidLabel.setText("Current Bid: -");
        item1TimeLabel.setText("Ends: -");
        item1StatusLabel.setText("ERROR");
        item1ImageView.setImage(null);

        item2NameLabel.setText("No auction");
        item2BidLabel.setText("Current Bid: -");
        item2TimeLabel.setText("Ends: -");
        item2StatusLabel.setText("N/A");
        item2ImageView.setImage(null);

        item3NameLabel.setText("No auction");
        item3BidLabel.setText("Current Bid: -");
        item3TimeLabel.setText("Ends: -");
        item3StatusLabel.setText("N/A");
        item3ImageView.setImage(null);
    }

    @FXML
    private void handleLogout() {
        SceneManager.goToAuth();
    }

    @FXML
    private void handleViewDetails1() {
        openDetailAtIndex(0);
    }

    @FXML
    private void handleViewDetails2() {
        openDetailAtIndex(1);
    }

    @FXML
    private void handleViewDetails3() {
        openDetailAtIndex(2);
    }

    private void openDetailAtIndex(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }

        MockData.setSelectedItem(items.get(index));
        SceneManager.goToProductDetail();
    }
}