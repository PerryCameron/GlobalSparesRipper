package com.l2;

import com.l2.dto.SparesDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class GSUpdater {
    private static final Logger logger = LoggerFactory.getLogger(GSUpdater.class);


    public static void main(String[] args) {
        updateDataBase();
    }

    private static void updateDataBase() {
        GlobalSparesRepositoryImpl globalSparesRepository = new GlobalSparesRepositoryImpl();
        Path path = AppFileTools.getDbPath();
        logger.info("Connecting to database...{}", path.toString());
        String url = "jdbc:sqlite:" + ApplicationPaths.globalSparesDir.resolve("global-spares.db");

        List<SparesDTO> sparesDTOS = globalSparesRepository.findSparesAdded("2025-04-07");


    }
}


//