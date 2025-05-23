package com.l2;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface ApplicationPaths {

    Path homeDir = Paths.get(System.getProperty("user.home"));
    Path oneDrive = homeDir.resolve("OneDrive - Schneider Electric");
    Path secondaryDbDirectory = homeDir.resolve("TSENotes");
    Path preferredDbDirectory = homeDir.resolve("OneDrive - Schneider Electric\\TSENotes");
    Path backupDir = Paths.get(secondaryDbDirectory.toString(), "backup");
    Path globalSparesDir = Paths.get(secondaryDbDirectory.toString(), "global_spares");
    // this will be where we store attachments
    Path fileDirectory = homeDir.resolve("OneDrive - Schneider Electric\\TSENotes\\files");
}
