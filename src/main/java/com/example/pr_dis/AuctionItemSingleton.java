package com.example.pr_dis;

import java.util.ArrayList;
import java.util.List;

public class AuctionItemSingleton {

    private static AuctionItemSingleton instance = null;
    private List<AuctionItem> auctionItems;

    private AuctionItemSingleton() {
        this.auctionItems = new ArrayList<>();
    }

    public static AuctionItemSingleton getInstance() {
        if (instance == null) {
            instance = new AuctionItemSingleton();
        }
        return instance;
    }

    public List<AuctionItem> getAuctionItems() {
        return auctionItems;
    }

    public void addAuctionItem(AuctionItem item) {
        this.auctionItems.add(item);
    }
}
