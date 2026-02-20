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



        controller = new MainController();

        primaryStage.setWidth(1028);
        primaryStage.setHeight(840);
        primaryStage.setMinHeight(600);
        primaryStage.setMinWidth(800);
        primaryStage.setResizable(true);
        primaryStage.setScene(new Scene(controller.getView()));
        primaryStage.setTitle("Global Spares Ripper");
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}