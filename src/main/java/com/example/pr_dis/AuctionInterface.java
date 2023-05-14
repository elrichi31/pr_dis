package com.example.pr_dis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
                            // No cierra el socket ni el ObjectOutputStream aquí
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
        VBox vbox = new VBox();
        List<AuctionItem> auctionItems = AuctionItemSingleton.getInstance().getAuctionItems();

        for (AuctionItem item : auctionItems) {
            Button btn = new Button();
            btn.setText(item.getName());
            btn.setOnAction(event -> {
                AuctionServer auctionServer = new AuctionServer(item);
                new Thread(auctionServer).start();
            });
            vbox.getChildren().add(btn);
        }
        vbox.getChildren().add(clientCountLabel);
        Scene scene = new Scene(vbox, 900, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    // Método para obtener el número de clientes conectados
    public static int getClientCount() {
        return clientCount.get();
    }
    public static void main(String[] args) {
        AuctionItemSingleton auctionItems = AuctionItemSingleton.getInstance();

        AuctionItem item1 = new AuctionItem("Una pintura de Vincent Van Gogh: La Noche Estrellada", 70e6, 1444);
        AuctionItem item2 = new AuctionItem("Un carro Ferrari clásico de los años 70", 1e6, 1555);
        AuctionItem item3 = new AuctionItem("El manuscrito original de la obra de Isaac Newton “Principios matemáticos de la filosofía natural", 40e6, 1666);

        auctionItems.addAuctionItem(item1);
        auctionItems.addAuctionItem(item2);
        auctionItems.addAuctionItem(item3);

        // Ahora puedes lanzar la interfaz de usuario de JavaFX
        Application.launch(AuctionInterface.class, args);
    }

}
