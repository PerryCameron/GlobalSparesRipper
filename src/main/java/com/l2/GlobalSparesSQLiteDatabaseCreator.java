package com.l2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class GlobalSparesSQLiteDatabaseCreator {


    private static final Logger logger = LoggerFactory.getLogger(GlobalSparesSQLiteDatabaseCreator.class);

    public static void createDataBase(String databaseName) {
    Path path = AppFileTools.getDbPath();
        logger.info("Creating database...{}", path.toString());
        String url = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir.resolve(databaseName);

        // SQL commands for creating tables
        String createTables = """
                                                CREATE TABLE IF NOT EXISTS product_to_spares (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                    pim_range TEXT,
                                                    pim_product_family TEXT,
                                                    spare_item TEXT,
                                                    replacement_item TEXT,
                                                    standard_exchange_item TEXT,
                                                    spare_description TEXT,
                                                    catalogue_version TEXT,
                                                    end_of_service_date TEXT,
                                                    last_update TEXT,
                                                    added_to_catalogue TEXT,
                                                    removed_from_catalogue TEXT,
                                                    comments TEXT,
                                                    keywords TEXT,
                                                    archived INTEGER NOT NULL CHECK (archived IN (0, 1)), 
                                                    custom_add INTEGER NOT NULL CHECK (custom_add IN (0, 1)),
                                                    last_updated_by TEXT                         
                                                );
                
                                                CREATE TABLE  IF NOT EXISTS spares (
                                                    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                                                    pim                    TEXT, -- JSON storing pim_range and pim_product_family
                                                    spare_item             TEXT UNIQUE, -- I want no duplicates here its a key
                                                    replacement_item       TEXT,
                                                    standard_exchange_item TEXT,
                                                    spare_description      TEXT,
                                                    catalogue_version      TEXT,
                                                    end_of_service_date    TEXT,
                                                    last_update            TEXT,
                                                    added_to_catalogue     TEXT,
                                                    removed_from_catalogue TEXT,
                                                    comments               TEXT,
                                                    keywords               TEXT,
                                                    archived               INTEGER NOT NULL,
                                                    custom_add             INTEGER NOT NULL,
                                                    last_updated_by        TEXT,
                                                    CHECK (archived IN (0, 1)),
                                                    CHECK (custom_add IN (0, 1))
                                                );
                
                                                CREATE TABLE IF NOT EXISTS replacement_cr (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                    item TEXT,
                                                    replacement TEXT,
                                                    comment TEXT,
                                                    old_qty REAL,
                                                    new_qty REAL,
                                                    last_update TEXT,
                                                    last_updated_by TEXT
                                                );
                
                                                CREATE TABLE IF NOT EXISTS properties (
                                                    rip_date INTEGER,
                                                    spares_added_to_database                 INTEGER NOT NULL DEFAULT 0,
                                                    spares_removed_from_database             INTEGER NOT NULL DEFAULT 0,
                                                    spares_archived                          INTEGER NOT NULL DEFAULT 0,
                                                    spares_unarchived                        INTEGER NOT NULL DEFAULT 0,
                                                    range_and_product_family_changes         INTEGER NOT NULL DEFAULT 0,
                                                    replacement_item_changes                 INTEGER NOT NULL DEFAULT 0,
                                                    std_exchange_item_changes                INTEGER NOT NULL DEFAULT 0,
                                                    spare_description_changes                INTEGER NOT NULL DEFAULT 0,
                                                    end_of_service_date_changes              INTEGER NOT NULL DEFAULT 0,
                                                    last_update_changes                      INTEGER NOT NULL DEFAULT 0,
                                                    added_to_catalogue_date_changes          INTEGER NOT NULL DEFAULT 0,
                                                    removed_from_catalogue_date_changes      INTEGER NOT NULL DEFAULT 0,
                                                    comments_changes                         INTEGER NOT NULL DEFAULT 0,
                                                    photos_copied                            INTEGER NOT NULL DEFAULT 0,
                                                    photos_skipped                           INTEGER NOT NULL DEFAULT 0,
                                                    custom_spares_added                      INTEGER NOT NULL DEFAULT 0,
                                                    custom_notes_added                       INTEGER NOT NULL DEFAULT 0,
                                                    updated_by_added                         INTEGER NOT NULL DEFAULT 0
                                                );
                
                                                CREATE TABLE IF NOT EXISTS ranges (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                    range TEXT,
                                                    range_additional TEXT,
                                                    range_type TEXT,
                                                    last_update TEXT DEFAULT CURRENT_TIMESTAMP,
                                                    last_updated_by TEXT
                                                );
                
                                                CREATE TABLE spare_pictures (
                                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                    spare_name TEXT NOT NULL UNIQUE,
                                                    picture BLOB NOT NULL,
                                                    FOREIGN KEY (spare_name) REFERENCES spares(spare_item) ON DELETE CASCADE
                                                );
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.executeUpdate(createTables);
            logger.info("Tables created successfully.");


        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public static void insert3phRanges(String databaseName) {
        Path path = AppFileTools.getDbPath();
        logger.info("Inserting 3ph Ranges...{}", path.toString());
        String url = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir.resolve(databaseName);

        // SQL commands for creating tables
        String createTables = """
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('All', 'all', 'all', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Easy UPS 3L', '3ph', 'Easy UPS 3L,3LUPS', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Easy UPS 3M', '3ph', 'Easy UPS 3M,3MUPS', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Easy UPS 3S', '3ph', 'Easy UPS 3S,3SUPS', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy 3500', '3ph', 'Galaxy 3500,G35T', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy RPP', '3ph', 'GRPPIP2X84,GRPPIP2X89,GRPPNF84,GRPPNF89,GRPPNQ84,GRPPNQ89', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy VL', '3ph', 'Galaxy VL', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy VM', '3ph', 'Galaxy VM,0G-GVM', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy VS', '3ph', 'Galaxy VS', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy VX', '3ph', 'Galaxy VX', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Li-Ion Batteries (Galaxy)', '3ph', 'Lithium Ion Battery Systems', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Li-Ion Batteries (Samsung)', '3ph', 'Lithium Ion Battery Systems 1st Gen,Lithium Ion Battery Systems 2nd Gen,Lithium Ion Battery Systems', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE EPS 6000', '3ph', 'EPS 6000', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE EPS 8000', '3ph', 'EPS 8000', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE EPSILON STS', '3ph', 'EPSILON STS', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy 4000', '3ph', 'Galaxy 4000', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy 5000', '3ph', 'Galaxy 5000', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy 7000', '3ph', 'Galaxy 7000', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE Galaxy PW', '3ph', 'Galaxy PW', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE PMM', '3ph', 'MGE PMM', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE Sinewave', '3ph', 'MGE Sinewave,ACCUSINE', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('MGE UPSILON', '3ph', 'UPSILON', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Power Distribution', '3ph', '0G-PD', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Symmetra LX', '1ph', 'Symmetra LX,Symmetra Power Module,Symmetra Battery Systems', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Symmetra MW', '3ph', 'Symmetra MW', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Symmetra PX 250/500', '3ph', 'Symmetra PX 500kW,Maintenance Bypass Left,SY%K250%,SY%K500%', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Symmetra PX1', '3ph', 'ISX20K,SY%0K40,SY%0K80,Symmetra PX Accessories,Symmetra PX 40kW Frame,Symmetra PX 80kW Frame', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Symmetra PX2', '3ph', 'Symmetra PX 100kW Frame,Battery Module Frame Symmetra PX 100kW,SY%K100F', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Symmetra ISX PDU', '3ph', '0G-PD150G6FX,Power Distribution Units,PDUM160H-B,PDUM160H-BX567', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Smart-UPS Modular Ultra', '3ph', 'Smart-UPS Modular Ultra', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Galaxy VXL', '3ph', 'Galaxy VXL', CURRENT_TIMESTAMP, null);
                """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.executeUpdate(createTables);
            logger.info("Ranges inserted successfully.");

        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public static void insertCoolingRanges(String databaseName) {
        Path path = AppFileTools.getDbPath();
        logger.info("Inserting Cooling Ranges...{}", path.toString());
        String url = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir.resolve(databaseName);

        // SQL commands for creating tables
        String createTables = """
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Third Party Local Offers', 'cooling', 'Third Party Local Offers', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Air Cooled Chillers', 'cooling', 'Uniflair Air Cooled Chillers', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Chilled Water InRow Cooling', 'cooling', 'Uniflair Chilled Water InRow Cooling', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Chillers', 'cooling', 'Uniflair Chillers', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Direct Expansion InRow Cooling', 'cooling', 'Uniflair Direct Expansion InRow Cooling', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Med/Large Room Cooling', 'cooling', 'Uniflair Med/Large Room Cooling', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Room Cooling', 'cooling', 'Uniflair Room Cooling', CURRENT_TIMESTAMP, null);
                INSERT INTO ranges (range, range_type, range_additional, last_update, last_updated_by) VALUES ('Uniflair Small Room Cooling', 'cooling', 'Uniflair Small Room Cooling', CURRENT_TIMESTAMP, null);
                """;
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.executeUpdate(createTables);
            logger.info("Ranges inserted successfully.");

        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }
}

