package com.l2.repository.interfaces;

import com.l2.dto.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GlobalSparesRepository {

    List<SparesDTO> findSparesByItems(List<String> spareItems);

    List<String> getSpareItems();

    List<SparesDTO> getAllSpares();

    long countSpares();

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

    List<SparesDTO> findSparesAdded(String date);

    List<ReplacementCrDTO> findAllReplacementCr();

    int getSpareItemId(String spareItem);

    int updateSpare(SparesDTO sparesDTO);

    SparesDTO findBySpareItem(String spareItem);

    void dropProductToSparesAndVacuum();

    boolean existsBySpareItem(String spareItem);

    void appendCommentBySpareItem(String spareItem, String newComment);

    void insertSparePictures(List<SparePictureDTO> sparePictures);

    int updateSpares(List<SparesDTO> spares);

    Map<String, Map<String, Set<String>>> getAllSpareRangeProductsBulk(boolean isArchived);

    Set<String> getExistingSparesInSparesTable(Collection<String> spares, boolean isArchived);

    void batchInsertConsolidated(Collection<ProductToSparesDTO> dtos);

    void batchUpdateConsolidated(List<ProductToSparesDTO> dtos);

    void changePRAGMASettinsForInsert();

    void insertProductToSparesInBatch(List<ProductToSparesDTO> dtos);
}
