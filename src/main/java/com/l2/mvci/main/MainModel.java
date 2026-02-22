package com.l2.mvci.main;

import com.l2.mvci.load.LoadingController;
import javafx.beans.property.*;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainModel {
    private final StringProperty droppedFilePath = new SimpleStringProperty("");
    private final BooleanProperty workbookReady = new SimpleBooleanProperty(false);
    private final LongProperty totalWork = new SimpleLongProperty(0);
    private final ObjectProperty<ViewStatus> viewStatus = new SimpleObjectProperty<>();
    private final ObjectProperty<Label> label = new SimpleObjectProperty<>();
    private final LoadingController loadingController = new LoadingController();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final TextArea ta = new TextArea();

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

    public LoadingController getLoadingController() {
        return loadingController;
    }

    public long getTotalWork() {
        return totalWork.get();
    }

    public LongProperty totalWorkProperty() {
        return totalWork;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public TextArea getTa() {
        return ta;
    }
}