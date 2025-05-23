package com.l2.repository.interfaces;

import com.l2.dto.*;

import java.util.List;

public interface GlobalSparesRepository {

    int insertProductToSpare(ProductToSparesDTO productToSpares);

    int insertConsolidatedProductToSpare(ProductToSparesDTO productToSpares);

    int insertReplacementCr(ReplacementCrDTO replacementCrDTO);

    int insertWorkbookProperties(PropertiesDTO propertiesDTO);

    List<String> getDistinctSpareItems(boolean isArchived);

    List<SparesDTO> searchSparesByPartNumber(String searchTerm, int partOrderId);

    List<String> getRangesFromSpareItem(String spare, boolean isArchived);

    List<String> getProductsFromRange(String spare, String range, boolean isArchived);

    ProductToSparesDTO getProductToSpares(String spare, boolean isArchived);

    List<RangesDTO> findAllRanges();

    List<SparesDTO> searchSparesScoring(String[] keywords);

    List<SparesDTO> searchSparesWithRange(String[] keywords1, String[] keywords2);

    int countSparesByRanges(String[] ranges);
}
