package com.l2;

import com.l2.dto.SparesDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GSUpdater {
    private static final Logger logger = LoggerFactory.getLogger(GSUpdater.class);


    public static void main(String[] args) {
        updateDataBase();
    }

    private static void updateDataBase() {
        GlobalSparesRepositoryImpl globalSparesRepository = new GlobalSparesRepositoryImpl();
        ProductionRepositoryImpl productionRepository = new ProductionRepositoryImpl();


        logger.info("{} spares found in newly created repo", globalSparesRepository.countSpares());
        logger.info("{} spares found in production repo", productionRepository.countSpares());


        // This will add all the new ones
//        List<SparesDTO> sparesDTOS = globalSparesRepository.findSparesAdded("2025-04-07");
//        logger.info("Global Spares added are {}", sparesDTOS.size());
//        sparesDTOS.forEach(sparesDTO -> {
////            productionRepository.insertSpare(sparesDTO);
//            System.out.println("insert ID: " + sparesDTO);
//        });
//
//        // This will archive the archived ones
//        List<SparesDTO> deletedFromSpares = globalSparesRepository.findSparesRemovedFromCatalogue("2025-04-07");
//        logger.info("Global Spares removed are {}", deletedFromSpares.size());
//        deletedFromSpares.forEach(sparesDTO -> {
//            System.out.println("delete: " + sparesDTO);
////            productionRepository.updateSpareAsArchived(sparesDTO);
//        });
    }
}


