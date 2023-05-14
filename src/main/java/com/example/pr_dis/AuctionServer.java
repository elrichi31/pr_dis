package com.example.pr_dis;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer implements Runnable {

    private AuctionItem item;
    private static int serverCount = 0;  // Contador para los servidores creados
    private int serverId;
    private ServerSocket serverSocket;

    public AuctionServer(AuctionItem item) {
        this.item = item;
        serverId = serverCount++;

        int port = item.getServerPort();  // Obtén el puerto del item
        try {
            serverSocket = new ServerSocket(port);  // Inicia el servidor en el puerto especificado
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Implementa la lógica del servidor de subastas aquí
        System.out.println("Se ha iniciado el servidor de subastas #" + serverId + " para el artículo: " + item.getName() + " en el puerto: " + serverSocket.getLocalPort());

        // Crea una nueva ventana (Stage) que muestra la información del artículo
        Platform.runLater(() -> {
            Stage itemStage = new Stage();
            VBox itemVbox = new VBox();
            Label itemName = new Label("Nombre: " + item.getName());
            Label itemPrice = new Label("Precio base: $" + item.getBasePrice());
            Label itemPort = new Label("Puerto del servidor: " + serverSocket.getLocalPort());  // Muestra el puerto del servidor
            itemVbox.getChildren().addAll(itemName, itemPrice, itemPort);
            Scene itemScene = new Scene(itemVbox, 600, 300);
            itemStage.setScene(itemScene);
            itemStage.setTitle("Servidor de Subasta #" + serverId);
            itemStage.show();
        });

        // Escucha conexiones entrantes
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("Conectado al servidor de " + item.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                // Maneja la conexión entrante...
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


