package com.example.pr_dis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class AuctionClientInterface extends Application {

    private AuctionClient client;

    @Override
    public void start(Stage primaryStage) {
        client = new AuctionClient();  // Crea un nuevo cliente
        client.connect();  // Conéctate al servidor
        VBox vbox = new VBox();
        List<AuctionItem> auctionItems = client.getAuctionItems();  // Recupera los objetos de subasta

        for (AuctionItem item : auctionItems) {
            Label itemName = new Label("Nombre: " + item.getName());
            Label itemPrice = new Label("Precio base: $" + item.getBasePrice());
            Button itemButton = new Button("Entrar a la subasta de " + item.getName());
            itemButton.setOnAction(event -> {
                System.out.println("Iniciando AuctionClientServer para el artículo: " + item.getName());
                AuctionClientServer clientServer = new AuctionClientServer(item);
                new Thread(clientServer).start();
            });
            Label sep = new Label("----------------");
            vbox.getChildren().addAll(itemName, itemPrice, itemButton, sep);
        }

        Scene scene = new Scene(vbox, 300, 250);
        primaryStage.setScene(scene);

        // Cierra la conexión con el servidor cuando se cierra la ventana
        primaryStage.setOnCloseRequest(event -> client.disconnect());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
