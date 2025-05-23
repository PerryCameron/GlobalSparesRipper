package com.l2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;


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
        }
    }


    public static Path getDbPath() {
        if (Files.exists(ApplicationPaths.globalSparesDir)) {
            logger.info("One-drive found: {}", ApplicationPaths.globalSparesDir);
            if (!Files.exists(ApplicationPaths.globalSparesDir)) {
                createFileIfNotExists(ApplicationPaths.globalSparesDir);
            }
            return ApplicationPaths.globalSparesDir;
        } else {
            logger.info("Path not found: {}", ApplicationPaths.globalSparesDir);
            return null;
        }
    }

    public static Path getOrCreateGlobalSparesFolder() {
        if (Files.exists(ApplicationPaths.globalSparesDir)) {
            logger.info("Global Spares directory found: {}", ApplicationPaths.globalSparesDir);
            return ApplicationPaths.globalSparesDir;
        } else {
            try {
                Files.createDirectories(ApplicationPaths.globalSparesDir);
                logger.info("Global spares directory created: {}", ApplicationPaths.globalSparesDir);
            } catch (IOException e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return ApplicationPaths.globalSparesDir;
    }
}
