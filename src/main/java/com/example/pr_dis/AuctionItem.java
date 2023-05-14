package com.example.pr_dis;

import java.io.Serializable;

public class AuctionItem implements Serializable {

    private String name;
    private double basePrice;
    private int serverPort;

    public AuctionItem(String name, double basePrice, int serverPort) {
        this.name = name;
        this.basePrice = basePrice;
        this.serverPort = serverPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getName() {
        return name;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
