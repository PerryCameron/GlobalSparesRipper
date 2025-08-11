package com.l2.statictools;

import com.l2.ApplicationPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.sql.SQLException;

public class DatabaseConnector {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);

    // this is the newly created database
    public static SQLiteDataSource getGlobalSparesDataSource(String caller) {
        String DATABASE_URL = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir + "/global-spares.db";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(DATABASE_URL);

        try {
            // Test the connection
            dataSource.getConnection().close();
            logger.info("Connection to {} has been established -> {}", caller, DATABASE_URL);
        } catch (SQLException e) {
            logger.error("Failed to establish SQLite connection to global spares database.", e);
        }
        return dataSource;
    }

    // this is the production database copied from application folder.
    public static SQLiteDataSource getProductionDataSource(String caller) {
        String DATABASE_URL = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir + "/Production/global-spares.db";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(DATABASE_URL);

        try {
            // Test the connection
            dataSource.getConnection().close();
            logger.info("Connection to {} has been established -> {}", caller, DATABASE_URL);
        } catch (SQLException e) {
            logger.error("Failed to establish SQLite connection to global spares database.", e);
        }
        return dataSource;
    }
}

