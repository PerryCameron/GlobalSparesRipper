package com.l2;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface ApplicationPaths {

    Path homeDir = Paths.get(System.getProperty("user.home"));
    Path globalSparesDir = Paths.get(System.getProperty("user.home"), "Global Spares");
    Path filePath = Paths.get(System.getProperty("user.home"), "Downloads", "Global Spares Catalogue.xlsx");

}
