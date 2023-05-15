package com.example.pr_dis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

public class AuctionClientInterface extends Application {

    private AuctionClient client;
    private String clientId;

    @Override
    public void start(Stage primaryStage) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        // Solicita el ID del cliente antes de iniciar
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Identificación del Cliente");
        dialog.setHeaderText("Ingrese su ID de cliente:");
        dialog.setContentText("ID del cliente:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(id -> clientId = id);

        client = new AuctionClient();  // Crea un nuevo cliente
        client.connect();
        VBox vbox = new VBox();
        List<AuctionItem> auctionItems = client.getAuctionItems();  // Recupera los objetos de subasta
        vbox.setSpacing(10);
        for (AuctionItem item : auctionItems) {
            String formattedAmount = numberFormat.format(item.getBasePrice());
            Label itemName = new Label("Nombre: " + item.getName());
            Label itemPrice = new Label("Precio base: $" + formattedAmount);
            Label statusLabel = new Label("Estado: No iniciado");
            Button itemButton = new Button("Entrar a la subasta");
            itemButton.setOnAction(event -> {
                if (clientId != null && !clientId.isEmpty()) {
                    System.out.println("Iniciando AuctionClientServer para el artículo: " + item.getName());
                    AuctionClientServer clientServer = new AuctionClientServer(item, clientId);
                    statusLabel.textProperty().bind(clientServer.getStatusProperty());  // Observa la propiedad de estado
                    new Thread(clientServer).start();  // Iniciar AuctionClientServer solo cuando se hace clic en el botón
                    itemButton.setDisable(true);
                } else {
                    System.out.println("Por favor, ingrese un ID de cliente válido.");
                }
            });

            vbox.getChildren().addAll(itemName, itemPrice, statusLabel, itemButton);
        }

        Scene scene = new Scene(vbox, 700, 350);
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




