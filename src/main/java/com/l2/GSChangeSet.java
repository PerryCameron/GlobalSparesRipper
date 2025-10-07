package com.l2;

import com.l2.dto.SparePictureDTO;
import com.l2.dto.SparesDTO;
import com.l2.dto.UpdatedByDTO;
import com.l2.repository.implementations.ChangeSetRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;
import com.l2.statictools.JsonParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class GSChangeSet {
    private static final Logger logger = LoggerFactory.getLogger(GSChangeSet.class);
    private static final ProductionRepositoryImpl productionRepository = new ProductionRepositoryImpl();
    private static final ChangeSetRepositoryImpl changeSetRepository = new ChangeSetRepositoryImpl();
    public static Integer firstTimeEdited = 0;
    public static Integer editedBefore = 0;
    public static Integer imagesAdded = 0;
    public static Integer imagesChanged = 0;
    public static Integer notesAdded = 0;
    public static Integer keywordsAdded = 0;
    public static Integer productFamily = 0;
    public static Integer partsAdded = 0;
    public static Integer sparesStatusChanged = 0;

    public static void main(String[] args) {
        addChangeSets();
        logger.info("{} spares were edited for the first time", firstTimeEdited.toString());
        logger.info("{} spares have been edited more than once", editedBefore.toString());
        logger.info("{} spares have had images added", imagesAdded.toString());
        logger.info("{} spares have had images changed", imagesChanged.toString());
        logger.info("{} spares have had keywords changed or edited", keywordsAdded.toString());
        logger.info("{} spares have had product families changed or added", productFamily.toString());
        logger.info("{} spares have had parts changed or added", partsAdded.toString());
        logger.info("{} spares have had spares status changed", sparesStatusChanged.toString());
    }

    private static void addChangeSets() {


        logger.info("{} spares found in newly created repo", changeSetRepository.countSpares());
        // get a list of the changed spare items
        List<SparesDTO> changeSetSpareItems = changeSetRepository.getAllSpares();

        for (SparesDTO changeSetSpareItem : changeSetSpareItems) {
            Optional<SparesDTO> chosenSpare = Optional.ofNullable(productionRepository.getBySpareItem(changeSetSpareItem.getSpareItem()));
            // make sure this spare is available in production
            if (chosenSpare.isPresent()) {
                // It has never been updated, so let's do so
                if (chosenSpare.get().getLastUpdatedBy() == null) {
                    logger.warn("production has no record of updates (is null)");
                    updateSpare(chosenSpare.get(), changeSetSpareItem, true);
                    continue;
                }
                // It has never been updated, so let's do so
                if (chosenSpare.get().getLastUpdatedBy().isEmpty()) {
                    logger.warn("production has no record of updates (is empty)");
                    updateSpare(chosenSpare.get(), changeSetSpareItem, true);
                    continue;
                }
                List<UpdatedByDTO> productionUpdates = JsonParserUtil.parseUpdatedByJson(chosenSpare.get().getLastUpdatedBy());
                for (UpdatedByDTO changeSetUpdate : productionUpdates) {
                    System.out.println("Got Here");
                    System.out.println(changeSetUpdate);
                }
                break;
            }
        }
    }


    private static void updateSpare(SparesDTO sparesDTO, SparesDTO changeSetSpareItem, boolean isFirstUpdate) {
        if(isFirstUpdate) {  // original production has not been updated
            firstTimeEdited++;
            List<UpdatedByDTO> changeSetUpdates = JsonParserUtil.parseUpdatedByJson(changeSetSpareItem.getLastUpdatedBy());
            if(changeSetUpdates.isEmpty()) {
                logger.warn("Nothing found to update");
                // we only have one recorded change in the set, and none in the original
            } else if (changeSetUpdates.size() == 1) {
                UpdatedByDTO changeSetUpdate = changeSetUpdates.getFirst();
                if(changeSetUpdate.getChangeMade().contains("IMAGE")) {
                    SparePictureDTO pictureDTO = changeSetRepository.getPictureBySpareName(changeSetSpareItem.getSpareItem());
                    if(productionRepository.existsBySpareName(changeSetSpareItem.getSpareItem())) {
                        logger.info("Editing picture for {}", changeSetSpareItem.getSpareItem());
                        SparePictureDTO existingPicture = productionRepository.getPictureBySpareName(changeSetSpareItem.getSpareItem());
                        existingPicture.setPicture(pictureDTO.getPicture());
                        imagesChanged++;
                        System.out.println("would update picture here");
                    } else {
                        logger.info("Adding picture for {}", changeSetSpareItem.getSpareItem());
                        imagesAdded++;
                        productionRepository.insertSparePicture(pictureDTO);
                        logger.info("Adding picture to production");
                    }
                }
                if(changeSetUpdate.getChangeMade().contains("KEYWORD")) {
                    keywordsAdded++;
                    sparesDTO.setKeywords(changeSetSpareItem.getKeywords());
                    System.out.println("Adding keyword");
                }
                if(changeSetUpdate.getChangeMade().contains("PRODUCT_FAM")) {
                    productFamily++;
                    sparesDTO.setPim(changeSetSpareItem.getPim());
                    System.out.println("Adding product family");
                }
                if(changeSetUpdate.getChangeMade().contains("NOTE")) {
                    notesAdded++;
                    sparesDTO.setComments(changeSetSpareItem.getComments());
                    System.out.println("Adding note");
                }
                if(changeSetUpdate.getChangeMade().contains("ADD_PART")) {
                    partsAdded++;
                    // TODO update data base to include new part
                    System.out.println("Adding spare");
                }
                if(changeSetUpdate.getChangeMade().contains("IN_SPARES")) {
                    sparesStatusChanged++;
                    sparesDTO.setArchived(changeSetSpareItem.getArchived());
                    System.out.println("Adding status change");
                }
                // Include who last updated it
                sparesDTO.setLastUpdatedBy(changeSetSpareItem.getLastUpdatedBy());
                productionRepository.updateSpare(sparesDTO);
            } else { // we have several recorded changes in the set, and none in the original
                for (UpdatedByDTO changeSetUpdate : changeSetUpdates) {
                    System.out.println(changeSetUpdate);
                }
            }

        // production has been updated before
        } else {
            editedBefore++;
            System.out.println("Record has been updated before");
        }

    }
}
