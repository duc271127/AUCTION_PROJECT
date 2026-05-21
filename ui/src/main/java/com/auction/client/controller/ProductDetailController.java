package com.auction.client.controller;

import com.auction.client.dto.response.AuctionDetailResponse;
import com.auction.client.dto.response.PublicItemDetailResponse;
import com.auction.client.model.AuctionItem;
import com.auction.client.navigation.SceneManager;
import com.auction.client.service.AuctionApiService;
import com.auction.client.service.ItemApiService;
import com.auction.client.util.MockData;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailController {

    @FXML private ImageView mainImageView;
    @FXML private ImageView thumb1ImageView;
    @FXML private ImageView thumb2ImageView;
    @FXML private Label productNameLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label countdownLabel;
    @FXML private Label statusLabel;
    @FXML private Label specsLabel;
    @FXML private Label bidMessageLabel;
    @FXML private TextField bidAmountField;

    private final AuctionApiService auctionApiService = new AuctionApiService();
    private final ItemApiService itemApiService = new ItemApiService();
    private AuctionItem selectedItem;

    @FXML
    public void initialize() {
        selectedItem = MockData.getSelectedItem();

        if (selectedItem == null) {
            showEmptyState();
            hideBidMessage();
            return;
        }

        loadAuctionDetail();
        hideBidMessage();
    }

    private void loadAuctionDetail() {
        try {
            AuctionDetailResponse response = auctionApiService.getAuctionDetail(selectedItem.getId());
            bindDetailFromApi(response);
        } catch (Exception e) {
            bindFallbackFromSelectedItem();
            showBidMessage("Cannot load full detail from server. Showing fallback data.");
        }
    }

    private void bindDetailFromApi(AuctionDetailResponse response) {
        productNameLabel.setText(safeText(response.getTitle(), "Unnamed Item"));
        currentBidLabel.setText("Current Highest Bid: $" + String.format("%,.0f", response.getCurrentPrice()));
        countdownLabel.setText("Ends: " + formatDateTime(response.getEndTime()));
        statusLabel.setText("Status: " + safeText(response.getState(), "UNKNOWN"));
        bindPublicItemDetail(response);
    }

    private void bindFallbackFromSelectedItem() {
        productNameLabel.setText(selectedItem.getName());
        currentBidLabel.setText("Current Highest Bid: " + selectedItem.getCurrentBid());
        countdownLabel.setText("Ends: " + selectedItem.getTimeLeft());
        statusLabel.setText("Status: " + selectedItem.getStatus());
        specsLabel.setText(
                "Auction Detail:\n" +
                        "- Backend detail is not available right now\n" +
                        "- Showing selected item data from showroom\n" +
                        "- Full specs will be added when backend returns more fields"
        );
        setDefaultImages(selectedItem.getImagePath());
    }

    private void bindPublicItemDetail(AuctionDetailResponse auction) {
        if (auction.getItemId() == null) {
            bindAuctionSpecsOnly(auction);
            setAuctionImages(auction);
            return;
        }

        try {
            PublicItemDetailResponse item = itemApiService.getPublicItemDetail(auction.getItemId().toString());
            if (item.getProductName() != null && !item.getProductName().isBlank()) {
                productNameLabel.setText(item.getProductName());
            }

            specsLabel.setText(
                    "Item Detail:\n" +
                            "- Category: " + safeText(item.getCategory(), safeText(auction.getCategory(), "General")) + "\n" +
                            "- Description: " + safeText(item.getDescription(), safeText(auction.getDescription(), "No description")) + "\n" +
                            "- SKU: " + safeText(item.getSku(), "N/A") + "\n" +
                            "- Quantity: " + (item.getQuantity() == null ? "N/A" : item.getQuantity()) + "\n" +
                            "- Seller: " + safeText(auction.getSellerName(), auction.getSellerId() == null ? "N/A" : auction.getSellerId().toString()) + "\n\n" +
                            auctionSpecs(auction)
            );
            setUploadedImages(item, auction);
        } catch (Exception e) {
            bindAuctionSpecsOnly(auction);
            setAuctionImages(auction);
        }
    }

    private void bindAuctionSpecsOnly(AuctionDetailResponse auction) {
        specsLabel.setText(auctionSpecs(auction));
    }

    private String auctionSpecs(AuctionDetailResponse auction) {
        return "Auction Detail:\n" +
                "- Auction ID: " + safeText(auction.getId() == null ? null : auction.getId().toString(), "N/A") + "\n" +
                "- Category: " + safeText(auction.getCategory(), "N/A") + "\n" +
                "- Bid Count: " + auction.getBidCount() + "\n" +
                "- Current Price: $" + String.format("%,.0f", auction.getCurrentPrice()) + "\n" +
                "- Min Next Bid: $" + String.format("%,.0f", auction.getMinNextBid()) + "\n" +
                "- Leader: " + safeText(auction.getLeaderName(), "No leader yet") + "\n" +
                "- Start Time: " + formatDateTime(auction.getStartTime()) + "\n" +
                "- End Time: " + formatDateTime(auction.getEndTime());
    }

    private void setUploadedImages(PublicItemDetailResponse item, AuctionDetailResponse auction) {
        List<String> images = new ArrayList<>();
        if (item.getImageUrls() != null) {
            images.addAll(item.getImageUrls());
        }
        if (images.isEmpty() && item.getImagePath() != null && !item.getImagePath().isBlank()) {
            images.add(item.getImagePath());
        }

        if (images.isEmpty()) {
            setAuctionImages(auction);
            return;
        }

        setRemoteImage(mainImageView, images.get(0));
        setRemoteImage(thumb1ImageView, images.get(0));
        setRemoteImage(thumb2ImageView, images.size() > 1 ? images.get(1) : images.get(0));
    }

    private void setAuctionImages(AuctionDetailResponse auction) {
        if (auction.getImageUrl() != null && !auction.getImageUrl().isBlank()) {
            setRemoteImage(mainImageView, auction.getImageUrl());
            setRemoteImage(thumb1ImageView, auction.getImageUrl());
            setRemoteImage(thumb2ImageView, auction.getImageUrl());
            return;
        }
        setDefaultImages(selectedItem.getImagePath());
    }

    private void setRemoteImage(ImageView imageView, String imagePath) {
        imageView.setImage(new Image(itemApiService.toAbsoluteImageUrl(imagePath), true));
    }

    private void setDefaultImages(String imagePath) {
        try {
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            mainImageView.setImage(image);
            thumb1ImageView.setImage(image);
            thumb2ImageView.setImage(image);
        } catch (Exception e) {
            mainImageView.setImage(null);
            thumb1ImageView.setImage(null);
            thumb2ImageView.setImage(null);
        }
    }

    private String formatDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.length() >= 16 ? value.substring(0, 16).replace("T", " ") : value;
    }

    private String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void showEmptyState() {
        productNameLabel.setText("No selected item");
        currentBidLabel.setText("Current Highest Bid: -");
        countdownLabel.setText("Ends: -");
        statusLabel.setText("Status: N/A");
        specsLabel.setText("Please go back to the showroom and choose an auction item.");
        mainImageView.setImage(null);
        thumb1ImageView.setImage(null);
        thumb2ImageView.setImage(null);
    }

    @FXML
    private void handleBack() {
        SceneManager.goToShowroom();
    }

    @FXML
    private void handleJoinLiveBidding() {
        if (selectedItem == null) {
            showBidMessage("No selected auction.");
            return;
        }
        SceneManager.goToLiveBidding();
    }

    @FXML
    private void handlePlaceBid() {
        hideBidMessage();
        if (bidAmountField.getText() == null || bidAmountField.getText().isBlank()) {
            showBidMessage("Enter a bid amount first.");
            return;
        }
        showBidMessage("Use Live Bidding to place bids with the new contract.");
    }

    private void showBidMessage(String message) {
        bidMessageLabel.setText(message);
    }

    private void hideBidMessage() {
        bidMessageLabel.setText("");
    }
}
