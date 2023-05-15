package com.example.pr_dis;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

public class AuctionServer implements Runnable {
    private Label timerLabel;
    private Timeline timer;
    private String highestBidder = null;
    private volatile int auctionTimeRemaining;// Tiempo restante de la subasta en segundos
    private AuctionItem item;
    private static int serverCount = 0;  // Contador para los servidores creados
    private int serverId;
    private ServerSocket serverSocket;
    private StringProperty status = new SimpleStringProperty("Servidor no iniciado");
    private List<ObjectOutputStream> clientOutputStreams = new ArrayList<>(); // Lista para almacenar los streams de cada cliente
    private Map<String, Integer> clientBids = new ConcurrentHashMap<>();  // Mapa para almacenar las ofertas de los clientes
    private Label bidInfo; // Label para mostrar la información de las ofertas en la interfaz de usuario
    private Boolean auctionListener = true;

    public AuctionServer(AuctionItem item) {
        timerLabel = new Label("2:00");

        this.item = item;
        serverId = serverCount++;

        int port = item.getServerPort();  // Obtén el puerto del item
        try {
            serverSocket = new ServerSocket(port);  // Inicia el servidor en el puerto especificado
        } catch (IOException e) {
            e.printStackTrace();
        }

        bidInfo = new Label(); // Inicializa el label de la información de las ofertas
    }


    private class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;

        public ClientHandler(Socket socket, ObjectOutputStream oos) {
            this.socket = socket;
            this.oos = oos;
            try {
                this.ois = new ObjectInputStream(this.socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // Espera a recibir una oferta
                    Object object = ois.readObject();
                    System.out.println(object);
                    if (object instanceof Bid) {
                        Bid bid = (Bid) object;
                        handleClientBid(bid.getClientId(), bid.getAmount());
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        status.set("Servidor iniciado");
        // Implementa la lógica del servidor de subastas aquí
        System.out.println("Se ha iniciado el servidor de subastas #" + serverId + " para el artículo: " + item.getName() + " en el puerto: " + serverSocket.getLocalPort());

        // Crea una nueva ventana (Stage) que muestra la información del artículo
        Platform.runLater(() -> {
            Stage itemStage = new Stage();
            VBox itemVbox = new VBox();
            Label itemName = new Label("Nombre: " + item.getName());
            Label itemPrice = new Label("Precio base: $" + item.getBasePrice());
            Label itemPort = new Label("Puerto del servidor: " + serverSocket.getLocalPort());
            Button startAuctionButton = new Button("Iniciar Subasta");
            timerLabel = new Label("Temporizador: Aun no iniciado");  // Inicializa timerLabel

            startAuctionButton.setOnAction(event -> {
                changeStatus("Subasta iniciada");
                startAuction();
            });

            itemVbox.getChildren().addAll(itemName, itemPrice, itemPort, startAuctionButton, timerLabel, bidInfo);
            Scene itemScene = new Scene(itemVbox, 600, 300);
            itemStage.setScene(itemScene);
            itemStage.setTitle("Servidor de Subasta #" + serverId);
            itemStage.show();
        });

        // Escucha conexiones entrantes
        while (auctionListener) {
            try {
                Socket socket = serverSocket.accept();
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                clientOutputStreams.add(oos); // Almacena el ObjectOutputStream para este cliente
                new Thread(new ClientHandler(socket, oos)).start();

                // Envia el estado al cliente usando ObjectOutputStream en lugar de DataOutputStream
                oos.writeObject("Conectado al servidor de " + item.getName());
                oos.writeObject(status.get());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public StringProperty getStatusProperty() {
        return status;
    }

    // Método para cambiar el estado y notificar a todos los clientes
    public void changeStatus(String newStatus) {
        status.set(newStatus);
        for (ObjectOutputStream oos : clientOutputStreams) {
            try {
                oos.writeObject(newStatus);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private int highestBid = 0;

    // Método para manejar las ofertas de los clientes
    public void handleClientBid(String clientId, int bid) {
        if(bid > highestBid && bid > item.getBasePrice()) {
            clientBids.put(clientId, bid);
            highestBid = bid;
            highestBidder = clientId;
            NumberFormat numberFormat = NumberFormat.getInstance();
            String formattedBid = numberFormat.format(bid);

            // Actualiza la interfaz de usuario para reflejar las nuevas ofertas
            Platform.runLater(() -> {
                bidInfo.setText("La oferta más reciente fue de: " + formattedBid + " por el cliente " + clientId);
            });

            restartTimer();

            // Envía notificaciones a los demás clientes
            Bid bidObject = new Bid(clientId, bid);
            for (ObjectOutputStream oos : clientOutputStreams) {
                try {
                    oos.writeObject(bidObject);
                    oos.flush();
                    System.out.println("enviando....");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            timer.stop();
            timer.playFromStart();
        } else {
            System.out.println("Oferta rechazada: la oferta de " + clientId + " es menor a la oferta más alta actual.");
        }
    }

    public void startAuction() {
        // Inicia el temporizador
        auctionTimeRemaining = 45;  // Inicia el temporizador con 10 segundos

        // Actualiza la etiqueta del temporizador inmediatamente
        updateTimerLabel();

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            auctionTimeRemaining--;
            Platform.runLater(this::updateTimerLabel);
            if (auctionTimeRemaining <= 0) {
                timer.stop();
                endAuction();
            }
        }));

        timer.setCycleCount(auctionTimeRemaining);
        timer.play();

        // Envía un mensaje a todos los clientes para que inicien sus temporizadores
        for (ObjectOutputStream oos : clientOutputStreams) {
            try {
                oos.writeObject("start timer");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateTimerLabel() {
        int minutes = auctionTimeRemaining / 60;
        int seconds = auctionTimeRemaining % 60;
        Platform.runLater(() -> timerLabel.setText(String.format("Tiempo restante: %02d:%02d", minutes, seconds)));
    }
    private void restartTimer() {
        auctionTimeRemaining = (int) Duration.seconds(45).toSeconds(); // reset the timer to 2 minutes
        if (timer != null) {
            timer.stop();
            timer.playFromStart();
        }
    }
    private void endAuction() {
        auctionListener = false;
        // Detén el temporizador
        timer.stop();

        // Envía un mensaje a todos los clientes para que sepan que la subasta ha terminado
        for (ObjectOutputStream oos : clientOutputStreams) {
            try {
                oos.writeObject("end auction");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Muestra una alerta con el ganador
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Subasta terminada");
            alert.setHeaderText(null);
            if (highestBidder != null) {
                alert.setContentText("El ganador es el cliente " + highestBidder + " con una oferta de " + NumberFormat.getNumberInstance(Locale.US).format(highestBid) + ".");
            } else {
                alert.setContentText("No se han realizado ofertas. No hay ganador.");
            }
            alert.showAndWait();

            // Cierra el servidor
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }





}


