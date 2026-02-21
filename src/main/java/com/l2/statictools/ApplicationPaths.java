package com.l2.statictools;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface ApplicationPaths {

    Path homeDir = Paths.get(System.getProperty("user.home"));
    Path globalSparesDir = homeDir.resolve("Global Spares");
}
