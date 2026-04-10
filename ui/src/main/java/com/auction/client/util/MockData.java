package com.auction.client.util;

import com.auction.client.model.AuctionItem;

import java.util.ArrayList;
import java.util.List;

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
}