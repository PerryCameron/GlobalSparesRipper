package com.l2;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class BaseApplication {
    public static Path dataBaseLocation;
    private static final Logger logger = LoggerFactory.getLogger(BaseApplication.class);

    // nothing fancy, makes db from excel file
    public static void main(String[] args) {
        buildDatabase();
    }

    public static boolean buildDatabase() {

        logMemory("Before workbook load");
        try (FileInputStream fis = new FileInputStream(ApplicationPaths.filePath.toString());
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            logMemory("After workbook load");
            // create the folder to hold database if it does not exist
            AppFileTools.getOrCreateGlobalSparesFolder();
            // creates the database and puts it in database folder
            GlobalSparesSQLiteDatabaseCreator.createDataBase("global-spares.db");
            // extracts information from xlsx file and updates database with extracted information
            ExcelRipper.extractWorkbookToSql(workbook);
            logMemory("Before workbook close");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logMemory("After workbook close");
        return true;
    }

    private static void logMemory(String point) {
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println(point + ": " + usedMB + " MB");
    }
}

