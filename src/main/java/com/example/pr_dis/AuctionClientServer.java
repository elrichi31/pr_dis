package com.example.pr_dis;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class AuctionClientServer implements Runnable {
    private Socket socket;
    private AuctionItem item;
    private DataInputStream dis;
    private String res;

    public AuctionClientServer(AuctionItem item) {
        this.item = item;
    }

    @Override
    public void run() {
        try {
            int port = item.getServerPort();
            socket = new Socket("localhost", port);  // Conecta al servidor en el puerto especificado
            dis = new DataInputStream(socket.getInputStream());
            res = dis.readUTF();  // Lee el mensaje del servidor

            // Una vez que se ha recibido la respuesta, crea la ventana
            Platform.runLater(this::createWindow);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createWindow() {
        Stage itemStage = new Stage();
        VBox itemVbox = new VBox();
        Label itemName = new Label("Nombre: " + item.getName());
        Label itemPrice = new Label("Precio base: $" + item.getBasePrice());
        Label itemPort = new Label("Puerto del servidor: " + socket.getPort());  // Muestra el puerto del servidor
        Label labelRes = new Label("Respuesta del servidor: " + res);
        itemVbox.getChildren().addAll(itemName, itemPrice, itemPort, labelRes);
        Scene itemScene = new Scene(itemVbox, 600, 300);
        itemStage.setScene(itemScene);
        itemStage.setTitle("Servidor de Subasta #" + item.getName());
        itemStage.show();
    }
}



