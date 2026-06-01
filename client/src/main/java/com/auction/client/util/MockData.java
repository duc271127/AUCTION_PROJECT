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
                "Affordable Silver & Laminated Objects Auction",
                "/images/item1.png",
                "USD 13,000",
                "Ends today",
                "Jewellery"
        ));

        items.add(new AuctionItem(
                "A002",
                "Emeralds, Rubies & Sapphires Auction",
                "/images/item2.png",
                "USD 24,500",
                "Ending now",
                "Jewellery"
        ));

        items.add(new AuctionItem(
                "A003",
                "Exclusive White Diamonds Auction",
                "/images/item3.png",
                "USD 32,000",
                "Ends today 16:00",
                "Jewellery"
        ));

        items.add(new AuctionItem(
                "A004",
                "Coloured Gemstones Jewellery Auction",
                "/images/item1.png",
                "USD 18,750",
                "Ends today 17:00",
                "Jewellery"
        ));

        items.add(new AuctionItem(
                "A005",
                "Murano Glass Auction",
                "/images/item2.png",
                "USD 9,250",
                "Tomorrow 11:00",
                "Decor"
        ));

        items.add(new AuctionItem(
                "A006",
                "Unused Watches Auction",
                "/images/item3.png",
                "USD 14,300",
                "2 days left",
                "Watches"
        ));

        items.add(new AuctionItem(
                "A007",
                "Ceramic Figurines Auction",
                "/images/item1.png",
                "USD 6,850",
                "3 days left",
                "Collectibles"
        ));

        items.add(new AuctionItem(
                "A008",
                "Vintage Decorative Plates",
                "/images/item2.png",
                "USD 4,900",
                "New listing",
                "Art"
        ));

        return items;
    }

    public static void setSelectedItem(AuctionItem item) {
        selectedItem = item;
    }

    public static AuctionItem getSelectedItem() {
        return selectedItem;
    }
}
