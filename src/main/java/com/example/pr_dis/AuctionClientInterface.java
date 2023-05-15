package com.example.pr_dis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class AuctionClientInterface extends Application {

    private AuctionClient client;
    private String clientId;

    @Override
    public void start(Stage primaryStage) {
        // Solicita el ID del cliente antes de iniciar
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Identificación del Cliente");
        dialog.setHeaderText("Ingrese su ID de cliente:");
        dialog.setContentText("ID del cliente:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(id -> clientId = id);

        client = new AuctionClient();  // Crea un nuevo cliente
        client.connect();  // Conéctate al servidor
        VBox vbox = new VBox();
        List<AuctionItem> auctionItems = client.getAuctionItems();  // Recupera los objetos de subasta
        vbox.setSpacing(10);
        for (AuctionItem item : auctionItems) {
            Label itemName = new Label("Nombre: " + item.getName());
            Label itemPrice = new Label("Precio base: $" + item.getBasePrice());
            Label statusLabel = new Label();
            Button itemButton = new Button("Entrar a la subasta");
            itemButton.setOnAction(event -> {
                if (clientId != null && !clientId.isEmpty()) {
                    System.out.println("Iniciando AuctionClientServer para el artículo: " + item.getName());
                    AuctionClientServer clientServer = new AuctionClientServer(item, clientId);
                    statusLabel.textProperty().bind(clientServer.getStatusProperty());  // Observa la propiedad de estado
                    new Thread(clientServer).start();  // Iniciar AuctionClientServer solo cuando se hace clic en el botón
                } else {
                    System.out.println("Por favor, ingrese un ID de cliente válido.");
                }
            });

            vbox.getChildren().addAll(itemName, itemPrice, itemButton, statusLabel);
        }

        Scene scene = new Scene(vbox, 300, 250);
        primaryStage.setTitle("Cliente " + clientId);
        primaryStage.setScene(scene);

        // Cierra la conexión con el servidor cuando se cierra la ventana
        primaryStage.setOnCloseRequest(event -> client.disconnect());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}




