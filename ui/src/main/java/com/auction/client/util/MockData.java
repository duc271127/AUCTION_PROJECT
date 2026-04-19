package com.auction.client.util;

import com.auction.client.model.AuctionItem;
import java.util.ArrayList;
import java.util.List;
import com.auction.client.model.BidRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.auction.client.model.SellerListing;
import com.auction.client.model.AdminApprovalItem;

public class MockData {

    private static AuctionItem selectedItem;

    public static List<AuctionItem> getMockAuctionItems() {
        List<AuctionItem> items = new ArrayList<>();

        items.add(new AuctionItem(
                "A001",
                "Vintage Rolex Submariner",
                "/images/item1.png",
                "$12,500",
                "02h 14m",
                "LIVE"
        ));

        items.add(new AuctionItem(
                "A002",
                "Rare Ming Porcelain Vase",
                "/images/item2.png",
                "$48,000",
                "05h 42m",
                "ENDING SOON"
        ));

        items.add(new AuctionItem(
                "A003",
                "Signed Michael Jordan Jersey",
                "/images/item3.png",
                "$9,800",
                "1d 03h",
                "UPCOMING"
        ));

        return items;
    }

    public static void setSelectedItem(AuctionItem item) {
        selectedItem = item;
    }

    public static AuctionItem getSelectedItem() {
        if (selectedItem == null) {
            List<AuctionItem> items = getMockAuctionItems();
            if (!items.isEmpty()) {
                selectedItem = items.get(0);
            }
        }
        return selectedItem;
    }

    public static ObservableList<BidRecord> getMockBidHistory() {
        return FXCollections.observableArrayList(
                new BidRecord("Alice", "$12,500", "21:03"),
                new BidRecord("Brian", "$13,000", "21:04"),
                new BidRecord("Cindy", "$13,500", "21:05"),
                new BidRecord("David", "$14,000", "21:06")
        );
    }

    public static ObservableList<AdminApprovalItem> getMockAdminApprovalItems() {
        return FXCollections.observableArrayList(
                new AdminApprovalItem("Vintage Rolex Submariner", "Seller Alpha", "Luxury Watch", "2026-04-10", "Pending"),
                new AdminApprovalItem("Rare Pokémon Card Set", "Seller Beta", "Collectibles", "2026-04-11", "Pending"),
                new AdminApprovalItem("Signed Football Jersey", "Seller Gamma", "Sports Memorabilia", "2026-04-12", "Pending")
        );
    }
}