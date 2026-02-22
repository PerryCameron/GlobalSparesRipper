package com.l2.mvci.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Builder;

import java.io.File;
import java.util.function.Consumer;

public class MainView implements Builder<Region> {

    private final MainModel model;
    private final Consumer<MainMessage> action;

    public MainView(MainModel model, Consumer<MainMessage> action) {
        this.model = model;
        this.action = action;
    }

    @Override
    public Region build() {
        BorderPane root = new BorderPane();
        model.viewStatusProperty().addListener((obs, oldStatus, newStatus) -> {
            System.out.println("ViewStatus changed: " + oldStatus + " -> " + newStatus);
            root.setCenter(null);
            switch (newStatus) {
                case XFS_LOADED   -> {
                    root.setCenter(createStatusArea());
                }
                case LOADING_XFS -> {
                    root.setCenter(createLabledProcess("Parsing Global Spares Catalogue.xlsx"));
                }
                case PREP_TO_CONVERT -> {
                    root.setCenter(createLabledProcess("Calculating Conversion time"));
                    // action.accept(MainMessage.CONVERT_TO_SQL);
                }
                case CONVERT_TO_SQL -> {
                    root.setCenter(createConvertScreen());
                    action.accept(MainMessage.CONVERT_TO_SQL);
                }
                default      -> {
                    root.setCenter(dropArea()); }
            }
        });
        // initial state
        root.setCenter(dropArea());
        return root;
    }


    private Node createConvertScreen() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(50, 20, 10, 20));
        root.setFillWidth(true);

        ProgressBar pb = model.getProgressBar();
        TextArea ta = model.getTa();
        ta.setEditable(false);
        pb.setPrefHeight(40);   // make it taller
        pb.setMinHeight(40);
        pb.setMaxHeight(40);
        pb.setMaxWidth(Double.MAX_VALUE);    // the bar is all the way up against the left

        root.getChildren().add(pb);
        root.getChildren().add(ta);
        // VBox.setVgrow(pb, Priority.NEVER);

        return root;
    }


    private Node createLabledProcess(String labelText) {
        VBox root = new VBox();
        root.setAlignment(Pos.TOP_CENTER);
        Label label = new Label(labelText);
        label.setStyle("""
            -fx-font-size: 18;
            -fx-text-fill: #444;
            -fx-padding: 60;
            """);
        root.getChildren().add(label);
        return root;
    }

    private Node dropArea() {
        VBox root = new VBox();
        root.setPadding(new Insets(5));
        model.labelProperty().set(new Label("Drag and drop Global Spares Catalogue.xlsx here"));
        // ── Drop zone ───────────────────────────────────────────────
        model.labelProperty().get().setStyle("""
            -fx-font-size: 18;
            -fx-text-fill: #444;
            -fx-padding: 60;
            """);
        model.labelProperty().get().setTextAlignment(TextAlignment.CENTER);
        model.labelProperty().get().setAlignment(Pos.CENTER);

        StackPane dropArea = new StackPane(model.labelProperty().get());
        dropArea.getStyleClass().add("dropArea");

        // Drag & drop support
        setupDragAndDrop(dropArea);
        root.getChildren().add(dropArea);
        return root;
    }

    private void setupDragAndDrop(StackPane dropArea) {
        dropArea.setOnDragOver(event -> {
            if (event.getGestureSource() != dropArea &&
                    event.getDragboard().hasFiles()) {
                File file = event.getDragboard().getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".xlsx")) {
                    event.acceptTransferModes(TransferMode.COPY);
                    dropArea.setStyle(dropArea.getStyle()
                            .replace("#f8f9fa", "#e3f2fd")
                            .replace("#adb5bd", "#1976d2"));
                    model.labelProperty().get().setVisible(false);
                }

            }
            event.consume();
        });

        dropArea.setOnDragExited(event -> {
            dropArea.setStyle(dropArea.getStyle()
                    .replace("#e3f2fd", "#f8f9fa")
                    .replace("#1976d2", "#adb5bd"));
            event.consume();
        });

        dropArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".xlsx")) {
                    model.setDroppedFilePath(file.getAbsolutePath());
                    action.accept(MainMessage.FILE_DROPPED_SUCCESS);
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private VBox createStatusArea() {
        Label statusLabel = new Label("Catalogue loaded successfully");
        statusLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #2e7d32;");

        Button ripButton = new Button("Convert to SQL");
        ripButton.setStyle("-fx-font-size: 15; -fx-padding: 12 24;");
        ripButton.setOnAction(e -> action.accept(MainMessage.PREP_CONV));

        VBox box = new VBox(20, statusLabel, ripButton);
        box.setAlignment(Pos.CENTER);

        // Optional: show progress if you later add long-running import
        // progress.visibleProperty().bind(someRunningProperty);

        return box;
    }
}