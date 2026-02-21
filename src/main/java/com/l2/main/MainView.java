package com.l2.main;


import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

        // ── Drop zone ───────────────────────────────────────────────
        Label dropLabel = new Label("Drag and drop Global Spares Catalogue.xlsx here");
        dropLabel.setStyle("""
            -fx-font-size: 18;
            -fx-text-fill: #444;
            -fx-padding: 60;
            """);
        dropLabel.setTextAlignment(TextAlignment.CENTER);
        dropLabel.setAlignment(Pos.CENTER);

        StackPane dropArea = new StackPane(dropLabel);
        dropArea.setStyle("""
            -fx-background-color: #f8f9fa;
            -fx-border-color: #adb5bd;
            -fx-border-width: 3;
            -fx-border-style: dashed;
            -fx-background-radius: 8;
            -fx-border-radius: 8;
            """);

        // Drag & drop support
        setupDragAndDrop(dropArea);

        // ── Status / next step area ─────────────────────────────────
        VBox statusBox = createStatusArea();

        // Switch between drop zone and status view
        model.workbookReadyProperty().addListener((obs, wasReady, isReady) -> {
            if (isReady) {
                root.setCenter(statusBox);
            } else {
                root.setCenter(dropArea);
            }
        });

        // initial state
        root.setCenter(dropArea);

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

        ProgressBar progress = new ProgressBar(0);
        progress.setVisible(false);

        Button ripButton = new Button("Rip into database");
        ripButton.setStyle("-fx-font-size: 15; -fx-padding: 12 24;");
        ripButton.setOnAction(e -> action.accept(MainMessage.LOAD_WORKBOOK_REQUEST));

        VBox box = new VBox(20, statusLabel, ripButton);
        box.setAlignment(Pos.CENTER);

        // Optional: show progress if you later add long-running import
        // progress.visibleProperty().bind(someRunningProperty);

        return box;
    }
}