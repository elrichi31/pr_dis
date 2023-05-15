package com.example.pr_dis;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import javafx.application.Platform;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionInterface extends Application {

    private static List<AuctionItem> auctionItems;
    private static ServerSocket serverSocket;
    private static AtomicInteger clientCount = new AtomicInteger(0);  // Contador de clientes
    private static Label clientCountLabel;  // Etiqueta para mostrar el número de clientes conectados

    @Override
    public void start(Stage primaryStage) {
        auctionItems = AuctionItemSingleton.getInstance().getAuctionItems();

        clientCountLabel = new Label("Clientes conectados: 0");

        // Crea el ServerSocket en un nuevo hilo para evitar bloquear la interfaz de usuario
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(1234);  // Inicia un ServerSocket en el puerto 1234
                while (true) {
                    Socket socket = serverSocket.accept();  // Acepta una conexión entrante

                    // Incrementa el contador de clientes
                    clientCount.incrementAndGet();

                    // Actualiza la etiqueta de la interfaz de usuario
                    Platform.runLater(() -> {
                        clientCountLabel.setText("Clientes conectados: " + clientCount.get());
                    });

                    // Inicia un nuevo hilo para manejar la conexión de cliente
                    new Thread(() -> {
                        try {
                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                            oos.writeObject(auctionItems);  // Envia los objetos de subasta al cliente
                        } catch (IOException e) {
                            e.printStackTrace();

                            // Decrementa el contador de clientes en caso de error
                            clientCount.decrementAndGet();

                            // Actualiza la etiqueta de la interfaz de usuario
                            Platform.runLater(() -> {
                                clientCountLabel.setText("Clientes conectados: " + clientCount.get());
                            });
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        GridPane gridPane = new GridPane();

        Label labelName = new Label("Subasta");
        Label labelServer = new Label("Servidor");
        Label labelState = new Label("Estado");

        labelName.setAlignment(Pos.CENTER);
        labelServer.setAlignment(Pos.CENTER);
        labelState.setAlignment(Pos.CENTER);

        gridPane.add(labelName, 0,0);
        gridPane.add(labelServer, 1,0);
        gridPane.add(labelState,2,0);


        List<AuctionItem> auctionItems = AuctionItemSingleton.getInstance().getAuctionItems();
        gridPane.setVgap(10);
        gridPane.setHgap(10);

        for (int i = 0; i < auctionItems.size(); i++) {
            AuctionItem item = auctionItems.get(i);
            Label itemNameLabel = new Label();
            itemNameLabel.setText(item.getName());
            Button btn = new Button();
            btn.setText("Iniciar servidor");
            Label statusLabel = new Label();  // Etiqueta para mostrar el estado
            statusLabel.setText("Servidor no iniciado");
            btn.setAlignment(Pos.CENTER);
            btn.setOnAction(event -> {
                AuctionServer auctionServer = new AuctionServer(item);
                new Thread(auctionServer).start();
                statusLabel.textProperty().bind(auctionServer.getStatusProperty());  // Observa la propiedad de estado
                btn.setDisable(true);
            });


            // Agrega los elementos a la celda correspondiente en el GridPane
            gridPane.add(itemNameLabel, 0, i+1);
            gridPane.add(btn, 1, i+1);
            gridPane.add(statusLabel, 2, i+1);
        }

        gridPane.add(clientCountLabel, 0, auctionItems.size()+1);  // Añade la etiqueta de conteo de clientes al final

        Scene scene = new Scene(gridPane, 700, 250);

        primaryStage.setTitle("Servidor principal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    // Método para obtener el número de clientes conectados
    public static int getClientCount() {
        return clientCount.get();
    }
    public static void main(String[] args) {
        AuctionItemSingleton auctionItems = AuctionItemSingleton.getInstance();

        AuctionItem item1 = new AuctionItem("Una pintura de Vincent Van Gogh: La Noche Estrellada", 70000000, 1444);
        AuctionItem item2 = new AuctionItem("Un carro Ferrari clásico de los años 70", 1000000, 1555);
        AuctionItem item3 = new AuctionItem("Isaac Newton “Principios matemáticos de la filosofía natural", 40000000, 1666);

        auctionItems.addAuctionItem(item1);
        auctionItems.addAuctionItem(item2);
        auctionItems.addAuctionItem(item3);
        Application.launch(AuctionInterface.class, args);
    }

}
