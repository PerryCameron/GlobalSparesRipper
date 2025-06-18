package com.l2;

import com.l2.dto.SparePictureDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CopyImagesToNewDataBase {
    private static final Logger logger = LoggerFactory.getLogger(CopyImagesToNewDataBase.class);


    public static void main(String[] args) {
        copyToNewDataBase();
    }

    private static void copyToNewDataBase() {
        GlobalSparesRepositoryImpl globalSparesRepository = new GlobalSparesRepositoryImpl();
        ProductionRepositoryImpl productionRepository = new ProductionRepositoryImpl();
        List<SparePictureDTO> pictures = productionRepository.findAllSparePictures();
        logger.info("{} pictures retrieved", pictures);
//        Set<String> uniqueNames = pictures.stream()
//                .map(SparePictureDTO::getSpareName)
//                .collect(Collectors.toSet());
//        if (uniqueNames.size() < pictures.size()) {
//            logger.error("Input list contains duplicate spare_name values");
//            throw new IllegalArgumentException("Duplicate spare_name values detected");
//        }
        globalSparesRepository.insertSparePictures(pictures);
    }
}
