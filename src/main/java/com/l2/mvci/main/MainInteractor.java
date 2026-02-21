package com.l2.mvci.main;

import javafx.concurrent.Task;
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
        Task<XSSFWorkbook> contactListTask = new Task<>() {
            @Override
            protected XSSFWorkbook call() {
                XSSFWorkbook workbook = null;
                try (FileInputStream fis = new FileInputStream(path)) {
                    workbook = new XSSFWorkbook(fis);
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
                model.setWorkbookReady(true);
            }
        });
        contactListTask.setOnFailed(event -> {
            model.setWorkbookReady(false);
        });
        Thread thread = new Thread(contactListTask);
        thread.start();
    }
}