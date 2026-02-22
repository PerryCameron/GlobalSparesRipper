package com.l2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.l2.ApplicationPaths.globalSparesDir;
import static com.l2.ApplicationPaths.pastSqlDataBase;

public class AppFileTools {

    private static final Logger logger = LoggerFactory.getLogger(AppFileTools.class);
    public static File outputFile;

    public static void createFileIfNotExists(Path path) {
        if (!Files.exists(path)) {
            logger.info("Database directory does not exist: {}", path);
            try {
                Files.createDirectories(path);
                logger.info("Directory created: {}", path);
            } catch (IOException e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            logger.info("GSRipper directory already exists: {}", path);
        }
    }


    public static Path getDbPath() {
        if (Files.exists(globalSparesDir)) {
            logger.info("One-drive found: {}", globalSparesDir);
            if (!Files.exists(globalSparesDir)) {
                createFileIfNotExists(globalSparesDir);
            }
            return globalSparesDir;
        } else {
            logger.info("Path not found: {}", globalSparesDir);
            return null;
        }
    }

    public static Path getOrCreateGlobalSparesFolder() {
        if (Files.exists(globalSparesDir)) {
            logger.info("Global Spares directory found: {}", globalSparesDir);
            return globalSparesDir;
        } else {
            try {
                Files.createDirectories(globalSparesDir);
                logger.info("Global spares directory created: {}", globalSparesDir);
            } catch (IOException e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return globalSparesDir;
    }


    /**
     * Looks for "global-spares.db" in globalSparesDir and, if present,
     * moves it into pastSqlDataBase overwriting any existing file with the same name.
     *
     * @return true if a file was successfully moved; false if not found or on error
     */
    public static boolean moveExistingGlobalSparesDb() {
        Path currentDb = globalSparesDir.resolve("global-spares.db");

        if (!Files.exists(currentDb)) {
            return false; // Nothing to move
        }

        try {
            // Ensure the destination directory exists
            Files.createDirectories(pastSqlDataBase);

            // Destination: Past Catalogue/global-spares.db
            Path target = pastSqlDataBase.resolve("global-spares.db");

            // Move and overwrite if destination already exists
            Files.move(currentDb, target, StandardCopyOption.REPLACE_EXISTING);
            return true;

        } catch (IOException e) {
            // Swap for your app's logger if available
            System.err.println("Failed to move global-spares.db: " + e.getMessage());
            return false;
        }
    }
}
