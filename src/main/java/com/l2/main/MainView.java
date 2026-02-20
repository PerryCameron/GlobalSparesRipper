package com.l2.main;


import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Builder;

import java.util.function.Consumer;

public class MainView implements Builder<Region> {
    private final MainModel mainModel;
    private final Consumer<MainMessage> action;

    public MainView(MainModel mainModel, Consumer<MainMessage> action) {
        this.mainModel = mainModel;
        this.action = action;
    }

    @Override
    public Region build() {
        TextArea messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.textProperty().bind(mainModel.chatLogProperty());

        TextField inputField = new TextField();
        inputField.textProperty().bindBidirectional(mainModel.inputMessageProperty());
        inputField.setPromptText("Type message...");

        Button sendButton = new Button("Send");
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(e -> action.accept(MainMessage.SEND_MESSAGE));

        HBox inputBox = new HBox(10, inputField, sendButton);
        inputBox.setPadding(new Insets(10));

        Label title = new Label("P2P Chat (VPN)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox root = new VBox(10, title, messageArea, inputBox);
        root.setPadding(new Insets(15));

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(root);
        return borderPane;
    }
}