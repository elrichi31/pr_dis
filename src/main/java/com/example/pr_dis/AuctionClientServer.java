package com.example.pr_dis;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

public class AuctionClientServer implements Runnable {
    private Timeline timer;
    private int auctionTimeRemaining;  // Tiempo restante de la subasta en segundos
    private Label timerLabel;  // Etiqueta para mostrar el temporizador
    private Socket socket;
    private AuctionItem item;
    private String highBidder;
    private String clientId;
    private StringProperty status = new SimpleStringProperty("Subasta no iniciada");
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private TextArea logArea; // Área para mostrar un registro de las ofertas
    TextField bidField = new TextField();
    DecimalFormat decimalFormat = new DecimalFormat("#");
    private int highestBid = 0;  // Inicializa la oferta más alta en 0


    public AuctionClientServer(AuctionItem item, String clientId) {
        this.item = item;
        this.clientId = clientId;
        this.logArea = new TextArea(); // Inicializa el área de registro
        this.logArea.setEditable(false); // El área de registro no debe ser editable
    }
    @Override
    public void run() {
        try {
            int port = item.getServerPort();
            socket = new Socket("localhost", port);  // Conecta al servidor en el puerto especificado

            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            // Lanzar un nuevo hilo para leer continuamente del ObjectInputStream
            new Thread(() -> {
                while (true) {
                    try {
                        Object serverObject = ois.readObject();  // Lee el objeto del servidor

                        NumberFormat numberFormat = NumberFormat.getInstance();

                        // Verifica si el objeto es una cadena (para el estado) o una oferta
                        if (serverObject instanceof String) {
                            String serverStatus = (String) serverObject;
                            Platform.runLater(() -> status.set("Estado: En progreso"));  // Actualiza status en el hilo
                            String serverMessage = (String) serverObject;
                            switch (serverMessage) {
                                case "start timer":
                                    startTimer();
                                    break;
                                case "end auction":
                                    Platform.runLater(() -> {
                                        // Mostrar una alerta con el estado final de la subasta y el ganador
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Subasta terminada");
                                        alert.setHeaderText(null);
                                        alert.setContentText("Subasta terminada el usuario " + highBidder + " gano la subasta con " + highestBid + " ofertados" );
                                        alert.showAndWait();
                                        status.set("Estado: Terminado");
                                        // Cierra la conexión
                                        try {
                                            if (oos != null) oos.close();
                                            if (ois != null) ois.close();
                                            if (socket != null) socket.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        // Cierra la ventana
                                        Stage stage = (Stage) logArea.getScene().getWindow();
                                        stage.close();
                                    });
                                    break;
                            }
                        } else if (serverObject instanceof Bid) {
                            Bid bid = (Bid) serverObject;
                            highestBid = Math.max(highestBid, bid.getAmount());
                            String formattedAmount = numberFormat.format(bid.getAmount());
                            String bidInfo = "El cliente " + bid.getClientId() + " ha ofrecido: " + formattedAmount;
                            highBidder = bid.getClientId();
                            System.out.println(bidInfo);
                            restartTimer();
                            Platform.runLater(() -> {
                                logArea.appendText(bidInfo + "\n");
                                bidField.setText(Integer.toString(bid.getAmount()));
                            });  // Muestra la oferta en el área de registro
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        break; // Si ocurre un error (por ejemplo, el socket se cierra), termina el hilo.
                    }
                }
            }).start();


            // Una vez que se ha recibido la respuesta, crea la ventana
            Platform.runLater(this::createWindow);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void startTimer() {
        auctionTimeRemaining = (int) Duration.seconds(45).toSeconds();  // Inicia el temporizador con 2 minutos (120 segundos)

        timer = new Timeline(
                new KeyFrame(Duration.ZERO, event -> updateTimerLabel()),
                new KeyFrame(Duration.seconds(1))
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }
    private void updateTimerLabel() {
        int minutes = auctionTimeRemaining / 60;
        int seconds = auctionTimeRemaining % 60;
        Platform.runLater(() -> timerLabel.setText(String.format("Tiempo restante: %02d:%02d", minutes, seconds)));
        auctionTimeRemaining--;
    }
    private void createWindow() {
        NumberFormat numberFormat = NumberFormat.getInstance();

        String formattedAmount = numberFormat.format(item.getBasePrice());

        Stage itemStage = new Stage();
        VBox itemVbox = new VBox();
        HBox buttonBox= new HBox();
        Label itemName = new Label("Nombre: " + item.getName());
        Label itemPrice = new Label("Precio base: $" + formattedAmount);
        Label itemPort = new Label("Puerto del servidor: " + socket.getPort());  // Muestra el puerto del servidor
        Label statusLabel = new Label();
        statusLabel.textProperty().bind(status);


        Button plus10000 = new Button("+10,000");
        Button plus100000 = new Button("+100,000");
        Button plus1000000 = new Button("+1,000,000");


        // Agrega los elementos necesarios para realizar ofertas
        bidField.setPromptText("Ingrese su oferta aquí");

        bidField.setText(String.valueOf(decimalFormat.format(item.getBasePrice())));
        Button bidButton = new Button("Hacer oferta");
        buttonBox.getChildren().addAll(bidButton, plus10000, plus100000, plus1000000);

        plus10000.setOnAction(event -> {
            int newBid = 10000;
            newBid = newBid + Integer.parseInt(bidField.getText());
            bidField.setText(Integer.toString(newBid));
        });

        plus100000.setOnAction(event -> {
            int newBid = 100000;
            newBid = newBid + Integer.parseInt(bidField.getText());
            bidField.setText(Integer.toString(newBid));
        });

        plus1000000.setOnAction(event -> {
            int newBid = 1000000;
            newBid = newBid + Integer.parseInt(bidField.getText());
            bidField.setText(Integer.toString(newBid));
        });

        bidButton.setOnAction(event -> {
            String bidText = bidField.getText();
            try {
                int bidAmount = Integer.parseInt(bidText);
                // Crear un objeto Bid y enviarlo al servidor

                if (bidAmount > highestBid && bidAmount >= item.getBasePrice()) {
                    Bid bid = new Bid(clientId, bidAmount);
                    oos.writeObject(bid);
                    oos.flush();


                } else {
                    logArea.appendText("Error: Su oferta es menor a la oferta más alta actual o menor que el precio base.\n");
                }
            } catch (NumberFormatException e) {
                // La oferta ingresada no es un número válido
                logArea.appendText("Error: La oferta debe ser un número válido.\n");
            } catch (IOException e) {
                logArea.appendText("Error al enviar la oferta al servidor.\n");
            }
        });

        timerLabel = new Label("Tiempo restante: --:--");
        itemVbox.getChildren().addAll(itemName, itemPrice, itemPort, statusLabel, bidField, buttonBox, timerLabel, logArea);
        Scene itemScene = new Scene(itemVbox, 600, 300);
        itemStage.setScene(itemScene);
        itemStage.setTitle("Cliente Subasta" + clientId);
        itemStage.show();
    }
    private void restartTimer() {
        auctionTimeRemaining = (int) Duration.seconds(45).toSeconds();
        if (timer != null) {
            timer.stop();
            timer.playFromStart();
        }
    }
    public StringProperty getStatusProperty() {
        return status;
    }
}





