package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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

    private List<AuctionItem> items;

    @FXML
    public void initialize() {
        items = MockData.getMockAuctionItems();

        if (items.size() >= 3) {
            bindItemToCard(items.get(0), item1NameLabel, item1BidLabel, item1TimeLabel, item1StatusLabel, item1ImageView);
            bindItemToCard(items.get(1), item2NameLabel, item2BidLabel, item2TimeLabel, item2StatusLabel, item2ImageView);
            bindItemToCard(items.get(2), item3NameLabel, item3BidLabel, item3TimeLabel, item3StatusLabel, item3ImageView);
        }
    }

    private void bindItemToCard(AuctionItem item,
                                Label nameLabel,
                                Label bidLabel,
                                Label timeLabel,
                                Label statusLabel,
                                ImageView imageView) {

        nameLabel.setText(item.getName());
        bidLabel.setText("Current Bid: " + item.getCurrentBid());
        timeLabel.setText("Time Left: " + item.getTimeLeft());
        statusLabel.setText(item.getStatus());

        try {
            Image image = new Image(getClass().getResourceAsStream(item.getImagePath()));
            imageView.setImage(image);
        } catch (Exception e) {
            System.out.println("Image not found: " + item.getImagePath());
        }
    }

    @FXML
    private void handleLogout() {
        SceneManager.goToAuth();
    }

    @FXML
    private void handleViewDetails1() {
        MockData.setSelectedItem(items.get(0));
        SceneManager.goToProductDetail();
    }

    @FXML
    private void handleViewDetails2() {
        MockData.setSelectedItem(items.get(1));
        SceneManager.goToProductDetail();
    }

    @FXML
    private void handleViewDetails3() {
        MockData.setSelectedItem(items.get(2));
        SceneManager.goToProductDetail();
    }
}