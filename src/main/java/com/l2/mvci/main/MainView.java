package com.l2.mvci.main;

import com.l2.dto.TaskItemDTO;
import com.l2.statictools.ImageResources;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Builder;
import javafx.beans.binding.Bindings;

import java.io.File;
import java.util.Arrays;
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
        model.rootProperty().set(new BorderPane());
        model.viewStatusProperty().addListener((obs, oldStatus, newStatus) -> {
            model.rootProperty().get().setCenter(null);
            switch (newStatus) {
                case XFS_LOADED -> {
                    model.rootProperty().get().setCenter(createStatusArea());
                }
                case LOADING_XFS -> {
                    model.rootProperty().get().setCenter(createLabledProcess("Parsing Global Spares Catalogue.xlsx"));
                }
                case PREP_TO_CONVERT -> {
                    model.rootProperty().get().setCenter(createLabledProcess("Calculating Conversion time"));
                }
                case CONVERT_TO_SQL -> {
                    model.rootProperty().get().setCenter(createConvertScreen());
                    action.accept(MainMessage.CONVERT_TO_SQL);
                }
                case ERROR -> {
                    model.rootProperty().get().setCenter(createErrorMessage());
                }
                case CONVERSION_DONE -> {
                    model.buttonProperty().get().setVisible(true);
                }
                default -> {
                    model.rootProperty().get().setCenter(dropArea());
                }
            }
        });
        // initial state
        model.rootProperty().get().setCenter(dropArea());
        return model.rootProperty().get();
    }

    private Node createConvertScreen() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(50, 20, 10, 20));
        root.setFillWidth(true);
        model.buttonProperty().get().setVisible(false);
        model.buttonProperty().get().setOnMouseClicked(event -> System.exit(0));

        ProgressBar pb = model.getProgressBar();
        pb.setPrefHeight(40);
        pb.setMinHeight(40);
        pb.setMaxHeight(40);
        pb.setMaxWidth(Double.MAX_VALUE);

        // --- Task Table ---
        TableView<TaskItemDTO> taskTable = new TableView<>(model.getTaskList());
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        taskTable.setSelectionModel(null); // no row selection needed
        taskTable.setFixedCellSize(36);
        taskTable.setMaxWidth(Double.MAX_VALUE);

        // Left column: task name
        TableColumn<TaskItemDTO, String> nameCol = new TableColumn<>("Task");
        nameCol.setCellValueFactory(data -> data.getValue().taskNameProperty());
        nameCol.setStyle("-fx-font-size: 13px;");

        // Right column: checkmark image (only shown when complete)
        TableColumn<TaskItemDTO, Boolean> doneCol = new TableColumn<>("Done");
        doneCol.setCellValueFactory(data -> data.getValue().completedProperty());
        doneCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView checkmark = new ImageView(ImageResources.YES);

            {
                checkmark.setFitHeight(22);
                checkmark.setFitWidth(22);
                checkmark.setPreserveRatio(true);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Boolean done, boolean empty) {
                super.updateItem(done, empty);
                setGraphic((done != null && done && !empty) ? checkmark : null);
            }
        });
        doneCol.setMaxWidth(80);
        doneCol.setMinWidth(80);

        taskTable.getColumns().addAll(Arrays.asList(nameCol, doneCol));

        // Bind table height to row count so there's no empty rows / scrollbar
        taskTable.prefHeightProperty().bind(
                Bindings.size(model.getTaskList())
                        .multiply(taskTable.getFixedCellSize())
                        .add(30) // header height
        ); // Cannot resolve symbol 'Bindings'

        root.getChildren().addAll(pb, taskTable, model.buttonProperty().get());
        VBox.setVgrow(taskTable, Priority.ALWAYS);
        return root;
    }

    private Node createErrorMessage() {
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.TOP_CENTER);
        Label errorLabel = new Label(model.getErrorMessage());
        Button okButton = new Button("OK");
        errorLabel.setStyle("""
                -fx-font-size: 18;
                -fx-text-fill: #020202;
                -fx-padding: 60;
                """);
        Label label = new Label("Error Occurred");
        label.setStyle("""
                -fx-font-size: 28;
                -fx-text-fill: #f40606;
                -fx-padding: 60;
                """);
        okButton.setOnAction(e -> {
            model.rootProperty().get().setCenter(dropArea());
        });
        vBox.getChildren().addAll(label, errorLabel, okButton);
        return vBox;
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