package com.l2;

import com.l2.main.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class Main extends Application {

    public static Stage primaryStage;
    private MainController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Get IP from command line OR show dialog
        String peerIp = getPeerIpFromArgsOrDialog();

        if (peerIp == null || peerIp.trim().isEmpty()) {
            System.exit(0); // user cancelled dialog
        }

        controller = new MainController(peerIp);

        primaryStage.setWidth(1028);
        primaryStage.setHeight(840);
        primaryStage.setMinHeight(600);
        primaryStage.setMinWidth(800);
        primaryStage.setResizable(true);
        primaryStage.setScene(new Scene(controller.getView()));
        primaryStage.setTitle("Parts P2P App - Peer: " + peerIp);
        primaryStage.show();
    }

    private String getPeerIpFromArgsOrDialog() {
        List<String> args = getParameters().getRaw();
        if (!args.isEmpty()) {
            return args.get(0).trim();
        }

        // No command-line arg → show nice dialog
        TextInputDialog dialog = new TextInputDialog("10.252.68.");
        dialog.setTitle("P2P Connection");
        dialog.setHeaderText("Enter the other computer's VPN IP address");
        dialog.setContentText("VPN IP:");

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.shutdown();
        }
        super.stop();
    }
}