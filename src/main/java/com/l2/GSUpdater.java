package com.l2;

import com.l2.dto.SparesDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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

    // Get spare_item lists from both databases
    List<SparesDTO> newSpareItems = removeDuplicateComments(globalSparesRepository.getAllSpares());
    List<SparesDTO> prodSpareItems = productionRepository.getAllSpares();
    logger.info("Removed duplicate comments from {} spares in new database", newSpareItems.size());

    // These are new Spares added by the Global Spares team
    List<SparesDTO> newAdds = getNewAdds(newSpareItems, prodSpareItems);
    // Add spares to production (uncomment when ready to apply)
    productionRepository.addSpares(newAdds);

    // below should be implemented, but I had 0 this way so I stopped
//    List<SparesDTO> removedFromSpares = getRemovedFromSpares(newSpareItems, prodSpareItems);
//    List<SparesDTO> addedToSpares = getNewlyAddedToSpares(newSpareItems, prodSpareItems);
    List<SparesDTO> editedSpares = getSparesWithDifferentComments(newSpareItems, prodSpareItems);

    // Test comments for selected spares
    testComments(newSpareItems, prodSpareItems, editedSpares);

    // Update production database with edited spares
    int rowsUpdated = productionRepository.updateSpares(editedSpares);
    logger.info("Updated {} spares with different comments in production database", rowsUpdated);
}

    private static List<SparesDTO> removeDuplicateComments(List<SparesDTO> spareItems) {
        return spareItems.stream()
                .map(spare -> {
                    String cleanedComments = cleanComments(spare.getComments());
                    return new SparesDTO(
                            spare.getId(),
                            spare.getPim(),
                            spare.getSpareItem(),
                            spare.getReplacementItem(),
                            spare.getStandardExchangeItem(),
                            spare.getSpareDescription(),
                            spare.getCatalogueVersion(),
                            spare.getProductEndOfServiceDate(),
                            spare.getLastUpdate(),
                            spare.getAddedToCatalogue(),
                            spare.getRemovedFromCatalogue(),
                            cleanedComments,
                            spare.getKeywords(),
                            spare.getArchived(),
                            spare.getCustomAdd(),
                            spare.getLastUpdatedBy()
                    );
                })
                .toList();
    }

    // Helper method to remove duplicate lines in comments
    private static String cleanComments(String comments) {
        if (comments == null) {
            return null;
        }
        // Split comments by newlines and remove duplicates while preserving order
        return Arrays.stream(comments.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    // after everything has been done I want to test it with this method.  please finish
    public static void testComments(List<SparesDTO> newSpareItems, List<SparesDTO> prodSpareItems, List<SparesDTO> editedSpareItems) {
        // Map spares by spare_item for efficient lookup
        Map<String, SparesDTO> newSpareItemsMap = newSpareItems.stream()
                .collect(Collectors.toMap(SparesDTO::getSpareItem, spare -> spare));
        Map<String, SparesDTO> prodSpareItemsMap = prodSpareItems.stream()
                .collect(Collectors.toMap(SparesDTO::getSpareItem, spare -> spare));

        for (int i = 0; i < 400; i += 50) {
            // Check if index is valid
            if (i >= editedSpareItems.size()) {
                System.out.println("Index " + i + " exceeds editedSpareItems size (" + editedSpareItems.size() + "). Stopping.");
                break;
            }

            SparesDTO editedSpare = editedSpareItems.get(i);
            String spareItem = editedSpare.getSpareItem();
            System.out.println("Testing spare item: " + spareItem + "\n");

            // Pull matching spare from newSpareItems and print its comment
            SparesDTO newSpare = newSpareItemsMap.get(spareItem);
            System.out.println("New database comments: \n" + (newSpare != null ? newSpare.getComments() : "Not found"));

            // Pull matching spare from prodSpareItems and print its comment
            SparesDTO prodSpare = prodSpareItemsMap.get(spareItem);
            System.out.println("Production database comments: \n" + (prodSpare != null ? prodSpare.getComments() : "Not found"));

            // Print final edited comments
            System.out.println("\nFinal Edited comments\n" + editedSpare.getComments() + "\n");
        }
    }

    private static List<SparesDTO> getSparesWithDifferentComments(List<SparesDTO> newSpareItems, List<SparesDTO> prodSpareItems) {
        // Map production spares by spare_item for efficient lookup
        Map<String, SparesDTO> prodSparesMap = prodSpareItems.stream()
                .collect(Collectors.toMap(SparesDTO::getSpareItem, spare -> spare));

        // Find spares where comments differ
        List<SparesDTO> differentCommentsSpares = newSpareItems.stream()
                .filter(newSpare -> {
                    SparesDTO prodSpare = prodSparesMap.get(newSpare.getSpareItem());
                    boolean differs = prodSpare != null && !Objects.equals(newSpare.getComments(), prodSpare.getComments());
                    return differs;
                })
                .map(newSpare -> {
                    SparesDTO prodSpare = prodSparesMap.get(newSpare.getSpareItem());
                    String uniqueComments = extractUniqueComments(newSpare.getComments(), prodSpare.getComments());
                    String mergedComments = mergeComments(newSpare.getComments(), uniqueComments);
                    return new SparesDTO(
                            newSpare.getId(),
                            newSpare.getPim(),
                            newSpare.getSpareItem(),
                            newSpare.getReplacementItem(),
                            newSpare.getStandardExchangeItem(),
                            newSpare.getSpareDescription(),
                            newSpare.getCatalogueVersion(),
                            newSpare.getProductEndOfServiceDate(),
                            newSpare.getLastUpdate(),
                            newSpare.getAddedToCatalogue(),
                            newSpare.getRemovedFromCatalogue(),
                            mergedComments,
                            newSpare.getKeywords(),
                            newSpare.getArchived(),
                            newSpare.getCustomAdd(),
                            newSpare.getLastUpdatedBy()
                    );
                })
                .toList();

        logger.info("Found {} spares with different comments",
                differentCommentsSpares.size());

        return differentCommentsSpares;
    }

    // Helper method to extract the unique part of production comments
    private static String extractUniqueComments(String newComments, String prodComments) {
        if (prodComments == null && newComments == null) {
            return null;
        }
        if (prodComments == null) {
            return null;
        }
        if (newComments == null) {
            return prodComments;
        }
        // Remove new comments from production comments if present
        if (newComments.contains(prodComments)) {
            return "";
        }
        // Remove any part of new comments present in production comments
        String uniquePart = prodComments;
        for (String newCommentPart : newComments.split("\\s+")) {
            uniquePart = uniquePart.replace(newCommentPart, "").trim();
        }
        // Clean up multiple spaces or punctuation
        uniquePart = uniquePart.replaceAll("\\s+", " ").trim();
        return uniquePart.isEmpty() ? "" : uniquePart;
    }

    // Helper method to merge comments
    private static String mergeComments(String newComments, String uniqueProdComments) {
        if (newComments == null && uniqueProdComments == null) {
            return null;
        }
        if (newComments == null) {
            return uniqueProdComments;
        }
        if (uniqueProdComments == null || uniqueProdComments.isEmpty()) {
            return newComments;
        }
        return newComments + " " + uniqueProdComments;
    }

    private static List<SparesDTO> getRemovedFromSpares(List<SparesDTO> newSpareItems, List<SparesDTO> prodSpareItems) {
        // Map production spares by spare_item for efficient lookup
        Map<String, SparesDTO> prodSparesMap = prodSpareItems.stream()
                .collect(Collectors.toMap(SparesDTO::getSpareItem, spare -> spare));

        // Find spares where archived is 0 in new database but was 1 in production
        List<SparesDTO> removedSpares = newSpareItems.stream()
                .filter(newSpare -> newSpare.getArchived() != null && !newSpare.getArchived())
                .filter(newSpare -> {
                    SparesDTO prodSpare = prodSparesMap.get(newSpare.getSpareItem());
                    return prodSpare != null && prodSpare.getArchived() != null && prodSpare.getArchived();
                })
                .toList();

        logger.info("Found {} spares with archived set to 0",
                removedSpares.size());

        return removedSpares;
    }

    private static List<SparesDTO> getNewlyAddedToSpares(List<SparesDTO> newSpareItems, List<SparesDTO> prodSpareItems) {
        // Map production spares by spare_item for efficient lookup
        Map<String, SparesDTO> prodSparesMap = prodSpareItems.stream()
                .collect(Collectors.toMap(SparesDTO::getSpareItem, spare -> spare));

        // Find spares where archived is 1 in new database but was 0 in production
        List<SparesDTO> newlyArchivedSpares = newSpareItems.stream()
                .filter(newSpare -> newSpare.getArchived() != null && newSpare.getArchived())
                .filter(newSpare -> {
                    SparesDTO prodSpare = prodSparesMap.get(newSpare.getSpareItem());
                    return prodSpare != null && prodSpare.getArchived() != null && !prodSpare.getArchived();
                })
                .toList();

        logger.info("Found {} spares with archived set to 1",
                newlyArchivedSpares.size());

        return newlyArchivedSpares;
    }

    private static List<SparesDTO> getNewAdds(List<SparesDTO> newSpareItems, List<SparesDTO> prodSpareItems) {
        // Extract spare_item values from production database for efficient lookup
        Set<String> prodSpareItemValues = prodSpareItems.stream()
                .map(SparesDTO::getSpareItem)
                .collect(Collectors.toSet());

        // Find SparesDTO where spare_item is in new database but not in production
        List<SparesDTO> missingSpareItems = newSpareItems.stream()
                .filter(spareItem -> !prodSpareItemValues.contains(spareItem.getSpareItem()))
                .toList();

        logger.info("Found {} spare_item values in new database but not in production: {}",
                missingSpareItems.size(),
                missingSpareItems.stream().map(SparesDTO::getSpareItem).toList());

        return missingSpareItems;
    }
}


