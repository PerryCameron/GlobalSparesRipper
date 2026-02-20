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
        BorderPane borderPane = new BorderPane();
        //borderPane.setCenter(root);
        return borderPane;
    }
}