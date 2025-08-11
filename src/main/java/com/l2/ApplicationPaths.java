package com.l2;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface ApplicationPaths {

    // the main directory we update spares in
    Path globalSparesDir = Paths.get(System.getProperty("user.home"), "Global Spares");
    // the path to the latest production database (usually copied from the application directory)
    Path productionGlobalSpares = Paths.get(System.getProperty("user.home"), "Global Spares\\Production", "global-spares.db");
    // The path the Excel catalogue
    Path sourceExcel = Paths.get(globalSparesDir.toString(), "Global Spares Catalogue.xlsx");
    // The path to the newly created global Spares database created from the latest Excel file
    Path createdGlobalSpares = Paths.get(System.getProperty("user.home"), "Global Spares", "global-spares.db");

}
