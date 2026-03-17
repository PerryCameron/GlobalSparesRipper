package com.l2;

import com.l2.mvci.main.MainController;
import com.l2.statictools.ImageResources;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    public static Stage primaryStage;
    private MainController controller;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static String theme = "light";
    public static boolean testMode = false;
    public static boolean mvDatabase = true;

    public static void main(String[] args) {
        // if one of the args is "test" I want to set the static variable  public static boolean testMode to true
        for (String arg : args) {
            if ("test".equalsIgnoreCase(arg)) {
                testMode = true;
                logger.info("test mode");
                break;
            }
            if ("moveDatabase".equalsIgnoreCase(arg)) {
                mvDatabase = false;
                logger.info("move database mode");
                break;
            }
        }
        launch(args);
    }

    @Override
    public void init() {
        AppFileTools.createFileIfNotExists(ApplicationPaths.globalSparesDir);
        AppFileTools.createFileIfNotExists(ApplicationPaths.catalogueDir);
        AppFileTools.createFileIfNotExists(ApplicationPaths.pastSqlDataBase);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        controller = new MainController();
        logger.info("Main controller created");
        primaryStage.setWidth(500);
        primaryStage.setMinHeight(550);
        primaryStage.setMinWidth(500);
        primaryStage.setHeight(550);
        primaryStage.setResizable(true);
        primaryStage.setScene(new Scene(controller.getView()));
        primaryStage.setTitle("Global Spares Ripper");
        primaryStage.getIcons().addAll(
                ImageResources.GSLOGO64,
                ImageResources.GSLOGO16,
                ImageResources.GSLOGO20,
                ImageResources.GSLOGO24,
                ImageResources.GSLOGO30,
                ImageResources.GSLOGO32,
                ImageResources.GSLOGO36,
                ImageResources.GSLOGO48,
                ImageResources.GSLOGO64,
                ImageResources.GSLOGO80,
                ImageResources.GSLOGO96,
                ImageResources.GSLOGO128,
                ImageResources.GSLOGO256
        );
        primaryStage.getScene().getStylesheets().add(
                getClass().getResource("/css/light.css").toExternalForm()
        );
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}