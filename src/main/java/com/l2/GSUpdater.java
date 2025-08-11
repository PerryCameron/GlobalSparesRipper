package com.l2;

import com.l2.dto.SparesDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GSUpdater {
    private static final Logger logger = LoggerFactory.getLogger(GSUpdater.class);
    private static final GlobalSparesRepositoryImpl globalSparesRepository = new GlobalSparesRepositoryImpl();
    private static final ProductionRepositoryImpl productionRepository = new ProductionRepositoryImpl();

    public static void main(String[] args) {
        updateDataBase();
    }

    private static void updateDataBase() {
        // Get spare_item lists from both databases
        List<String> newSpareItems = globalSparesRepository.getSpareItems();
        List<String> prodSpareItems = productionRepository.getSpareItems();

        getNewAdds(newSpareItems, prodSpareItems, globalSparesRepository);
    }

    private static List<String> getNewAdds(List<String> newSpareItems, List<String> prodSpareItems, GlobalSparesRepositoryImpl globalSparesRepository) {
        // Find spare_item values in new database but not in production
        List<String> missingSpareItems = newSpareItems.stream()
                .filter(spareItem -> !prodSpareItems.contains(spareItem))
                .toList();
        logger.info("Found {} spare_item values in new database but not in production: {}",
                missingSpareItems.size(), missingSpareItems);

        // Fetch SparesDTO for missing spare_item values
        List<SparesDTO> missingSpares = globalSparesRepository.findSparesByItems(missingSpareItems);

        // Log details of missing spares
        if (missingSpares.isEmpty()) {
            logger.info("No spares found in new database that are not in production.");
        } else {
            logger.info("There are {} new spares", missingSpareItems.size());
            productionRepository.addSpares(missingSpares);
        }
        return missingSpareItems;
    }
}


