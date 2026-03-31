package com.l2.statictools;

import com.l2.ApplicationPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.sqlite.SQLiteDataSource;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;

public class DatabaseConnector {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);

    // this is the new database we are making
    public static Optional<SQLiteDataSource> getGlobalSparesDataSource(String caller) {
        return getSqLiteDataSource(caller, ApplicationPaths.globalSparesDir + "/global-spares.db");
    }

    public static SQLiteDataSource getProductionDataSource(String caller, String absolutePath) {
        return getSqLiteDataSource(caller, absolutePath)
                .orElseThrow(() -> new IllegalStateException("Production database not found: " + absolutePath));
    }

    // Optional return kept here — callers must handle the missing-file case
    public static Optional<SQLiteDataSource> getOldSparesDataSource(String caller) {
        return getSqLiteDataSource(caller, ApplicationPaths.pastSqlDataBase + "/global-spares.db");
    }

    public static SQLiteDataSource getChangeSetDataSource(String caller) {
        return getSqLiteDataSource(caller, ApplicationPaths.globalSparesDir + "/change_sets/change_set.sqlite")
                .orElseThrow(() -> new IllegalStateException("Change set database not found"));
    }

    @NonNull
    private static Optional<SQLiteDataSource> getSqLiteDataSource(String caller, String dbPath) {
        Path path = Paths.get(dbPath);

        if (!Files.exists(path)) {
            logger.warn("Database file does not exist [{}]: {}", caller, dbPath);
            return Optional.empty();
        }

        String url = "jdbc:sqlite:" + dbPath;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try {
            dataSource.getConnection().close();
            logger.info("Connection established for caller [{}] -> {}", caller, url);
        } catch (SQLException e) {
            logger.error("Database file exists but connection failed for caller [{}]: {}", caller, url, e);
            return Optional.empty();
        }

        return Optional.of(dataSource);
    }
}
//public class DatabaseConnector {
//
//    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
//
//    // this is the newly created database
//    public static SQLiteDataSource getGlobalSparesDataSource(String caller) {
//        String DATABASE_URL = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir + "/global-spares.db";
//        return getSqLiteDataSource(caller, DATABASE_URL);
//    }
//
//    // this is the production database copied from application folder.
//    public static SQLiteDataSource getProductionDataSource(String caller, String absolutePath) {
//        String DATABASE_URL = "jdbc:sqlite:" + absolutePath;
//        return getSqLiteDataSource(caller, DATABASE_URL);
//    }
//
//    // this is the last database that was ripped
//    public static SQLiteDataSource getOldSparesDataSource(String caller) {
//        String DATABASE_URL = "jdbc:sqlite:" + ApplicationPaths.pastSqlDataBase + "/global-spares.db";
//        return getSqLiteDataSource(caller, DATABASE_URL);
//    }
//
//    public static SQLiteDataSource getChangeSetDataSource(String caller) {
//        String DATABASE_URL = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir + "/change_sets/change_set.sqlite";
//        return getSqLiteDataSource(caller, DATABASE_URL);
//    }
//
//
//    @NonNull
//    private static SQLiteDataSource getSqLiteDataSource(String caller, String DATABASE_URL) {
//        SQLiteDataSource dataSource = new SQLiteDataSource();
//        dataSource.setUrl(DATABASE_URL);
//
//        // I would like to test if the file defined in DATABASE_URL exists, perhaps return Optional<SQLiteDataSource> is the
//        // best answer because ApplicationPaths.pastSqlDataBase + "/global-spares.db in particular may not be there, may also be best to add "jdbc:sqlite:" in this method instead of the callers
//
//        try {
//            // Test the connection
//            dataSource.getConnection().close();
//            logger.info("Connection to {} has been established -> {}", caller, DATABASE_URL); // this keeps printing even if database isn't there
//        } catch (SQLException e) {
//            logger.error("Failed to establish SQLite connection to global spares database.", e);
//        }
//        return dataSource;
//    }
//}

