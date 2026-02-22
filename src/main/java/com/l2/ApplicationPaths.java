package com.l2;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface ApplicationPaths {

    Path homeDir = Paths.get(System.getProperty("user.home"));
    Path globalSparesDir = homeDir.resolve("Global Spares");
    Path pastSqlDataBase = globalSparesDir.resolve("Past Catalogue");
    Path catalogueDir = globalSparesDir.resolve("Catalogue");

    // what are we do do with this nonsense
    Path sourceExcel = Paths.get(globalSparesDir.toString(), "Global Spares Catalogue.xlsx");
    // The path to the newly created global Spares database created from the latest Excel file
    Path createdGlobalSpares = Paths.get(System.getProperty("user.home"), "Global Spares", "global-spares.db");
}
