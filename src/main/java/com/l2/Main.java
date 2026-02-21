package com.l2;

import com.l2.mvci.main.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    public static Stage primaryStage;
    private MainController controller;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static String theme = "light";


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;



        controller = new MainController();
        logger.info("Main controller created");
        primaryStage.setWidth(500);
        primaryStage.setHeight(400);
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(500);
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