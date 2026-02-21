package com.l2.main;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class MainInteractor {

    private final MainModel model;

    public MainInteractor(MainModel model) {
        this.model = model;
    }

    public void loadWorkbookFromDroppedFile() {
        String path = model.getDroppedFilePath();
        if (path == null || path.isBlank()) {
            System.err.println("No file path in model");
            return;
        }

        try (FileInputStream fis = new FileInputStream(path)) {
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            model.setWorkbook(workbook);
            model.setWorkbookReady(true);
            System.out.println("Workbook loaded successfully: " + path);
        } catch (IOException e) {
            System.err.println("Failed to read Excel file: " + e.getMessage());
            // TODO: show error to user (add error property to model)
            model.setWorkbookReady(false);
        }
    }
}