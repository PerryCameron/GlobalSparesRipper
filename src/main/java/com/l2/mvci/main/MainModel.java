package com.l2.mvci.main;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainModel {
    private final StringProperty droppedFilePath = new SimpleStringProperty("");
    private final BooleanProperty workbookReady = new SimpleBooleanProperty(false);

    // will be set once file is successfully read
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
}