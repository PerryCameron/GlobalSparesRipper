package com.l2.mvci.main;

import javafx.beans.property.*;
import javafx.scene.control.Label;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainModel {
    private final StringProperty droppedFilePath = new SimpleStringProperty("");
    private final BooleanProperty workbookReady = new SimpleBooleanProperty(false);
    private final ObjectProperty<ViewStatus> viewStatus = new SimpleObjectProperty<>();
    private final ObjectProperty<Label> label = new SimpleObjectProperty<>();

    private XSSFWorkbook workbook;   // not a property → we don't want FX bindings on heavy object

    public String getDroppedFilePath() {
        return droppedFilePath.get();
    }

    public void setDroppedFilePath(String path) {
        droppedFilePath.set(path);
    }

    public StringProperty droppedFilePathProperty() {
        return droppedFilePath;
    }

    public boolean isWorkbookReady() {
        return workbookReady.get();
    }

    public void setWorkbookReady(boolean ready) {
        workbookReady.set(ready);
    }

    public BooleanProperty workbookReadyProperty() {
        return workbookReady;
    }

    public XSSFWorkbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(XSSFWorkbook workbook) {
        this.workbook = workbook;
    }

    public Label getLabel() {
        return label.get();
    }

    public ObjectProperty<Label> labelProperty() {
        return label;
    }

    public ViewStatus getViewStatus() {
        return viewStatus.get();
    }

    public ObjectProperty<ViewStatus> viewStatusProperty() {
        return viewStatus;
    }
}