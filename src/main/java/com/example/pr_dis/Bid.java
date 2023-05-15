package com.example.pr_dis;

import java.io.Serializable;

public class Bid implements Serializable {
    private String clientId;
    private int amount;

    public Bid(String clientId, int amount) {
        this.clientId = clientId;
        this.amount = amount;
    }

    public String getClientId() {
        return clientId;
    }

    public int getAmount() {
        return amount;
    }
}
