package com.l2;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class GSRipper {
    public static Path dataBaseLocation;
    private static final Logger logger = LoggerFactory.getLogger(GSRipper.class);

    // nothing fancy, makes db from excel file
    public static void main(String[] args) {
        buildDatabase();
    }

    public static boolean buildDatabase() {

        // create a workbook of data
        try (FileInputStream fis = new FileInputStream(ApplicationPaths.sourceExcel.toString());
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            // create the folder to hold database if it does not exist
            AppFileTools.getOrCreateGlobalSparesFolder();
            // creates the database and puts it in database folder
            GlobalSparesSQLiteDatabaseCreator.createDataBase("global-spares.db");
            // extracts information from xlsx file and updates database with extracted information
            // ExcelRipper.extractWorkbookToSql(workbook);
            // clean up unused database components
            ExcelRipper.cleanUpDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}

