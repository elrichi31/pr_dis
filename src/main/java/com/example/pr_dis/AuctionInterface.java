package com.example.pr_dis;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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
        vbox.setSpacing(10);
        for (AuctionItem item : auctionItems) {
            HBox hbox = new HBox();
            hbox.setSpacing(10);
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

            });
            hbox.getChildren().addAll(itemNameLabel,btn,statusLabel);
            vbox.getChildren().add(hbox);

        }
        vbox.getChildren().add(clientCountLabel);
        Scene scene = new Scene(vbox, 900, 450);
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

        // Ahora puedes lanzar la interfaz de usuario de JavaFX
        Application.launch(AuctionInterface.class, args);
    }

}
