package com.l2.mvci.main;

import com.l2.*;
import com.l2.mvci.load.LoadingController;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class MainInteractor {

    private final MainModel model;
    private static final Logger logger = LoggerFactory.getLogger(MainInteractor.class);

    public MainInteractor(MainModel model) {
        this.model = model;
    }

    public void loadWorkbookFromDroppedFile() {
        String path = model.getDroppedFilePath();
        if (path == null || path.isBlank()) {
            System.err.println("No file path in model");
            return;
        }
        model.viewStatusProperty().setValue(ViewStatus.LOADING_XFS);
        model.getLoadingController().setOffset(50, 0);
        model.getLoadingController().showLoadSpinner(true);
        Task<XSSFWorkbook> contactListTask = new Task<>() {
            @Override
            protected XSSFWorkbook call() {
                XSSFWorkbook workbook = null;
                try (FileInputStream fis = new FileInputStream(path)) {
                    workbook = new XSSFWorkbook(fis);  // is there a way to get logs from this action?
                    logger.info("Workbook loaded successfully: {}", path);
                } catch (IOException e) {
                    logger.error("Failed to read Excel file: {}", e.getMessage());
                    // TODO: show error to user (add error property to model)
                    model.setWorkbookReady(false);
                }
                return workbook;
            }
        };
        contactListTask.setOnSucceeded(event -> {
            XSSFWorkbook workbook = contactListTask.getValue();
            if (workbook != null) {
                model.setWorkbook(workbook);
                model.viewStatusProperty().setValue(ViewStatus.XFS_LOADED);
                model.getLoadingController().showLoadSpinner(false);
            }
        });
        contactListTask.setOnFailed(event -> {
            model.setWorkbookReady(false);
            model.getLoadingController().showLoadSpinner(false);
        });
        Thread thread = new Thread(contactListTask);
        thread.start();
    }

    public void convertToSql() {
        model.viewStatusProperty().setValue(ViewStatus.CONVERT_TO_SQL);
        model.getLoadingController().setOffset(50, 0);
        model.getLoadingController().showLoadSpinner(true);
        Task<XSSFWorkbook> createDataBaseTask = new Task<>() {
            @Override
            protected XSSFWorkbook call() {
                if (AppFileTools.moveExistingGlobalSparesDb())
                    logger.info("Existing Global Spares Catalogue found and moved for later comparison");
                else logger.info("There is no existing Global Spares Catalogue found");
                // if it was created then copy it to the old dir
                // now create the sql
                Long estimateNumber = ExcelRipper.estimateTotalWork(model.getWorkbook());
                logger.info("Total Work Estimate: {}", estimateNumber);
                return null;
            }
        };
        createDataBaseTask.setOnSucceeded(event -> {model.getLoadingController().showLoadSpinner(false);});
        createDataBaseTask.setOnFailed(event -> {model.getLoadingController().showLoadSpinner(false);});
        Thread thread = new Thread(createDataBaseTask);
        thread.start();
    }

    public static boolean buildDatabase() {

        // create a workbook of data
        try (FileInputStream fis = new FileInputStream(ApplicationPaths.sourceExcel.toString());
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            // creates the database and puts it in database folder
            GlobalSparesSQLiteDatabaseCreator.createDataBase("global-spares.db");


            // extracts information from xlsx file and updates database with extracted information
//            ExcelRipper.extractWorkbookToSql(workbook);
//            // clean up unused database components
//            ExcelRipper.cleanUpDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setLoadingController() {
        model.getLoadingController().getStage().setScene(new Scene(model.getLoadingController().getView(), Color.TRANSPARENT));
        model.getLoadingController().getStage().getScene().getStylesheets().add("css/" + Main.theme + ".css");
    }
}