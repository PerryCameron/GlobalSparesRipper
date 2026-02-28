package com.l2.mvci.main;

import com.l2.dto.TaskItemDTO;
import com.l2.mvci.load.LoadingController;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainModel {
    private final StringProperty droppedFilePath = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final IntegerProperty element = new SimpleIntegerProperty(0);
    private final BooleanProperty workbookReady = new SimpleBooleanProperty(false);
    private final int[] totalWork = new int[] { 0,0,0,0,0,0 };
    private final ObjectProperty<ViewStatus> viewStatus = new SimpleObjectProperty<>();
    private final ObjectProperty<Label> label = new SimpleObjectProperty<>();
    private final ObjectProperty<BorderPane> root = new SimpleObjectProperty<>();
    private final ObjectProperty<Button> button = new SimpleObjectProperty<>(new Button("Close"));
    private final LoadingController loadingController = new LoadingController();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final TextArea ta = new TextArea();
    private XSSFWorkbook workbook;   // not a property → we don't want FX bindings on heavy object
    private final ObservableList<TaskItemDTO> taskList = FXCollections.observableArrayList();
    public ObservableList<TaskItemDTO> getTaskList() { return taskList; }

    public String getDroppedFilePath() {
        return droppedFilePath.get();
    }

    public void setDroppedFilePath(String path) {
        droppedFilePath.set(path);
    }

    public StringProperty droppedFilePathProperty() {
        return droppedFilePath;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String message) {
        errorMessage.set(message);
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

    public int getProductToSparesTotal() {
        return totalWork[0];
    }

    public int getArchivedProductToSparesTotal() {
        return totalWork[1];
    }

    public int getReplacementCRs() {
        return totalWork[2];
    }

    public int getUniflairCrossReference() {
        return totalWork[3];
    }

    public int getConsolidatedProductToSpares() {
        return totalWork[4];
    }

    public int getConsolidatedArchivedProductToSpares() {
        return totalWork[5];
    }

    public void setConsolidatedProductToSpares(int work) {
        totalWork[4] = work;
    }

    public void setConsolidatedArchivedProductToSpares(int work) {
        totalWork[5] = work;
    }

    public void setTotalWork(int[] work) {
        System.arraycopy(work, 0, totalWork, 0, totalWork.length);
    }

    public String totalWorkToString() {
        return
                "Product to Spares total: " +totalWork[0] +
                " Archived Product to Spares total: " +totalWork[1] +
                " Replacement Cr's: " +totalWork[2] +
                " Uniflair Cross Reference: " +totalWork[3];
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public TextArea getTa() {
        return ta;
    }

    public BorderPane getRoot() {
        return root.get();
    }

    public ObjectProperty<BorderPane> rootProperty() {
        return root;
    }

    public Button getButton() {
        return button.get();
    }

    public ObjectProperty<Button> buttonProperty() {
        return button;
    }

    public int getElement() {
        return element.get();
    }

    public IntegerProperty elementProperty() {
        return element;
    }

    public int incrementElement() {
        element.set(element.get() + 1);
        return element.get() - 1;
    }
}