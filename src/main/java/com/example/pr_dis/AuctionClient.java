package com.example.pr_dis;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class AuctionClient {

    private List<AuctionItem> auctionItems;
    private Socket socket;
    private ObjectInputStream ois;

    public void connect() {
        try {
            socket = new Socket("localhost", 1234);  // Conéctate al servidor principal
            ois = new ObjectInputStream(socket.getInputStream());
            auctionItems = (List<AuctionItem>) ois.readObject();  // Recibe los objetos de subasta del servidor
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (ois != null) ois.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<AuctionItem> getAuctionItems() {
        return auctionItems;
    }
}
