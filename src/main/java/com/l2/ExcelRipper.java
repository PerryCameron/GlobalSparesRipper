package com.l2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l2.dto.ProductToSparesDTO;
import com.l2.dto.PropertiesDTO;

import com.l2.dto.ReplacementCrDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.interfaces.GlobalSparesRepository;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ExcelRipper {
    private static final Logger logger = LoggerFactory.getLogger(ExcelRipper.class);
    private static final GlobalSparesRepository globalSparesRepository = new GlobalSparesRepositoryImpl();
    private static List<ProductToSparesDTO> editedSpares = new ArrayList<>();

    // the old school
    public static boolean extractWorkbookToSql(XSSFWorkbook workbook) {
        Sheet sheet = workbook.getSheet("Product to Spares");
        if (sheet == null) {
            System.out.println("Sheet 'Product to Spares' not found.");
            return false;
        }
        // extracts metadata from workbook
        logger.info("Saving Meta data properties");
        extractWorkbookProperties(workbook, globalSparesRepository);

        // here is where we fill the product to spares table with items in the catelogue
        ProductToSparesDTO productToSpares = new ProductToSparesDTO(false, false);  // archived, customadd
        logger.info("Ripping Product to Spares");  // succeeds
        extractProductToSpares(sheet, productToSpares, globalSparesRepository, false);

        // here is where we fill the product to spares table with items that are archived
        productToSpares.setArchived(true);
        sheet = workbook.getSheet("Archived Product to Spares");
        logger.info("Ripping Archived Product to Spares");  // succeeds
        extractProductToSpares(sheet, productToSpares, globalSparesRepository, true);

        ReplacementCrDTO replacementCrDTO = new ReplacementCrDTO();
        // here is where we fill our replament_cr table with 3-phase
        sheet = workbook.getSheet("Replacement CRs");
        logger.info("Ripping Replacement CRs (3-ph)");
        extractReplacementCr(sheet, replacementCrDTO, globalSparesRepository);

        // here is where we fill our replacement_cr with uniflair
        sheet = workbook.getSheet("Uniflair Cross Reference");
        logger.info("Ripping Replacement CRs (Uniflair Cross Reference)");
        extractReplacementCr(sheet, replacementCrDTO, globalSparesRepository);

        logger.info("Consolidating Product to Spares ");  // this fails
        consolidateWithJSON(false, globalSparesRepository);

        logger.info("Consolidating Archived Product to Spares");
        consolidateWithJSON(true, globalSparesRepository);

        return true;
    }

    public static void consolidateWithJSON(boolean isArchived, GlobalSparesRepository globalSparesRepository) {
        ObjectMapper objectMapper = new ObjectMapper();

        // get a list of just the spares (part numbers) as strings distinct with no duplicates from product_to_spares
        List<String> compactedSpares = globalSparesRepository.getDistinctSpareItems(isArchived);

        compactedSpares.forEach(spare -> {
            logger.info("Processing for: {}------------------------------", spare);

            // Get pim_range values for this spare_item from product_to_spares
            List<String> ranges = globalSparesRepository.getRangesFromSpareItem(spare, isArchived);
            // Build JSON for pim column
            List<Map<String, Object>> pimData = new ArrayList<>();
            for (String range : ranges) {
                // gets products from the range of the spare
                List<String> products = globalSparesRepository.getProductsFromRange(spare, range, isArchived);
                if (!products.isEmpty()) {
                    Map<String, Object> rangeEntry = new HashMap<>();
                    rangeEntry.put("range", range);
                    rangeEntry.put("product_families", products);
                    pimData.add(rangeEntry);
                }
            }
            // Skip if no pim data
            if (pimData.isEmpty()) {
                logger.warn("No pim data for spare_item: {}", spare);
                return;
            }
            // Convert pimData to JSON
            String pimJson;
            try {
                pimJson = objectMapper.writeValueAsString(pimData);
            } catch (Exception e) {
                logger.error("Error serializing JSON for spare_item: {}", spare, e);
                return;
            }
            // The spare does not yet exist so it is ok to insert it
            ProductToSparesDTO productToSpares = globalSparesRepository.getProductToSpares(spare, isArchived);
            if(globalSparesRepository.getSpareItemId(spare) == 0) {
                logger.info("Spare: {}, Replacement Item: {}", spare, productToSpares.getReplacementItem());
                productToSpares.setPimProductFamily("");
                productToSpares.setPimRange(pimJson);
                System.out.println(productToSpares);
                // inserts into spares using data from productToSpares
                globalSparesRepository.insertConsolidatedProductToSpare(productToSpares);
            } else { // the spare exists, we need to update it
                logger.warn("Spare exists: setting aside");
                editedSpares.add(productToSpares);
            }
        });
    }

    public static boolean cleanUpDatabase() {
        globalSparesRepository.dropProductToSparesAndVacuum();
        return true;
    }

    public static boolean extractWorkbookProperties(XSSFWorkbook workbook, GlobalSparesRepository globalSparesRepository) {
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        try {
            POIXMLProperties properties = workbook.getProperties();
            POIXMLProperties.CoreProperties coreProperties = properties.getCoreProperties();
            propertiesDTO.setLastModifiedDate(coreProperties.getModified().toString());
            propertiesDTO.setLastModifiedBy(coreProperties.getLastModifiedByUser());
            propertiesDTO.setCreatedBy(coreProperties.getCreator());
            propertiesDTO.setCreationDate(coreProperties.getCreated().toString());
            globalSparesRepository.insertWorkbookProperties(propertiesDTO);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void extractReplacementCr(
            Sheet sheet,
            ReplacementCrDTO replacementCrDTO,
            GlobalSparesRepository globalSparesRepository) {

        DataFormatter formatter = new DataFormatter(); // formats like Excel

        // Define how many columns you expect in this sheet
        final int expectedCols = 5; // columns 0..4 (Item, Replacement, Comment, OldQty, NewQty)

        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            // if (r >= 1000) break; // temp throttle if needed
            if (r < 3) continue;

            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Reset DTO for this row
            replacementCrDTO.clear();

            for (int col = 0; col < expectedCols; col++) {
                Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                // Prefer numeric parsing for qty columns, but keep a string for others
                String cellText = formatter.formatCellValue(cell).trim();

                switch (col) {
                    case 0 -> replacementCrDTO.setItem(cellText);
                    case 1 -> replacementCrDTO.setReplacement(cellText);
                    case 2 -> replacementCrDTO.setComment(cellText);

                    case 3 -> { // OldQty
                        // If the cell type is numeric, read it directly; else try parsing the text
                        Double val = parseDoubleCell(cell, cellText, 0.0);
                        replacementCrDTO.setOldQty(val);
                    }

                    case 4 -> { // NewQty
                        Double val = parseDoubleCell(cell, cellText, 0.0);
                        replacementCrDTO.setNewQty(val);
                    }

                    default -> { /* ignore extras */ }
                }
            }

            // Write only if item is present
            if (replacementCrDTO.getItem() != null && !replacementCrDTO.getItem().isEmpty()) {
                globalSparesRepository.insertReplacementCr(replacementCrDTO);
            }
        }
    }

    // Helper: try numeric cell first, else parse string; on error return fallback
    private static Double parseDoubleCell(Cell cell, String cellText, double fallback) {
        try {
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            if (!cellText.isEmpty()) {
                return Double.parseDouble(cellText);
            }
            return fallback;
        } catch (Exception e) {
            // log and return a sentinel if you prefer, or fallback to 0.0
            // logger.error("Qty parse failed at row {}, col {}: {}", cell.getRowIndex(), cell.getColumnIndex(), e.getMessage());
            return 99999.0; // or fallback
        }
    }

    public static void extractProductToSpares(
            Sheet sheet,
            ProductToSparesDTO productToSpares,
            GlobalSparesRepository globalSparesRepository,
            boolean isArchived) {

        DataFormatter formatter = new DataFormatter(); // formats numbers/dates like Excel shows them

        // Start from row 3 (0-based indexing), stop at 1300
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
//            if (r >= 1300) break;
            if (r < 3) continue;

            Row row = sheet.getRow(r);
            if (row == null) continue;

            // If you know exactly how many columns you expect, hard-code it for stability.
            // Based on your printout you have at least 13 columns (0..12).
            int expectedCols = 13;

            // Reset DTO for this row
            productToSpares.clear();
            productToSpares.setArchived(isArchived);
            productToSpares.setCustomAdd(false);

            for (int col = 0; col < expectedCols; col++) {
                // CREATE_NULL_AS_BLANK ensures we get a blank cell for missing entries
                Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String cellValue = formatter.formatCellValue(cell).trim();

                switch (col) {
                    case 0 -> productToSpares.setPimRange(cellValue);
                    case 1 -> productToSpares.setPimProductFamily(cellValue);
                    case 2 -> productToSpares.setSpareItem(cellValue);
                    case 3 -> productToSpares.setReplacementItem(cellValue);
                    case 4 -> {
                        // In your sample "Standard Offer" belongs to column 6, not 4.
                        // This mapping will remain correct now because blanks are preserved.
                        productToSpares.setStandardExchangeItem(cellValue);
                    }
                    case 5 -> productToSpares.setSpareDescription(cellValue);
                    case 6 -> productToSpares.setCatalogueVersion(cellValue); // if "Standard Offer" is catalogue version
                    case 7 -> productToSpares.setProductEndOfServiceDate(cellValue);
                    case 8 -> {
                        if (isArchived) productToSpares.setRemovedFromCatalogue(cellValue);
                        else productToSpares.setLastUpdate(cellValue);
                    }
                    case 9 -> {
                        if (isArchived) productToSpares.setComments(cellValue);
                        else productToSpares.setAddedToCatalogue(cellValue);
                    }
                    case 10 -> {
                        if (!isArchived) {
                            productToSpares.setComments(cellValue.isEmpty() ? null : cellValue);
                        }
                    }
                    // If you need 11/12 (you printed them), add mappings here:
                    // case 11 -> productToSpares.setKeywords(cellValue);
                    // case 12 -> productToSpares.setLastUpdatedBy(cellValue);
                    default -> {
                        // ignore extra columns or log as needed
                    }
                }
            }

            globalSparesRepository.insertProductToSpare(productToSpares);
        }
    }

    public static int[] estimateTotalWork(XSSFWorkbook workbook) {
        int[] totals = new int[6]; // {Product to Spares, Archived, Replacement CRs, Uniflair}
        // 0 = Product to Spares
        totals[0] = countNonEmptyDataRows(workbook.getSheet("Product to Spares"), 3, 2);
        // 1 = Archived Product to Spares
        totals[1] = countNonEmptyDataRows(workbook.getSheet("Archived Product to Spares"), 3, 2);
        // 2 = Replacement CRs
        totals[2] = countNonEmptyDataRows(workbook.getSheet("Replacement CRs"), 3, 2);
        // 3 = Uniflair Cross-Reference
        totals[3] = countNonEmptyDataRows(workbook.getSheet("Uniflair Cross Reference"), 3, 2);
        totals[4] = 0;
        totals[5] = 0;
        // If literally nothing was found → return minimal non-zero value to prevent ÷0
        boolean completelyEmpty = totals[0] == 0 && totals[1] == 0 && totals[2] == 0 && totals[3] == 0;
        if (completelyEmpty) {
            return new int[]{1, 0, 0, 0, 0, 0}; // or {1,1,1,1} depending on downstream logic
        }
        return totals;
    }

    /**
     * Counts rows that look like real data records.
     *
     * @param sheet        the sheet (may be null)
     * @param skipFirstN   number of header/title rows to skip
     * @param keyColumnIdx 0-based column index that must be non-blank to count the row
     * @return number of data-like rows
     */
    private static int countNonEmptyDataRows(Sheet sheet, int skipFirstN, int keyColumnIdx) {
        if (sheet == null) {
            return 0;
        }

        int count = 0;
        int firstPossibleDataRow = Math.max(skipFirstN, sheet.getFirstRowNum());

        // Use getPhysicalNumberOfRows() as a fast upper bound when possible
        int maxRowToCheck = Math.min(
                sheet.getLastRowNum(),
                sheet.getPhysicalNumberOfRows() + 50  // small safety margin
        );

        DataFormatter formatter = new DataFormatter();

        for (int r = firstPossibleDataRow; r <= maxRowToCheck; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            // Skip clearly empty rows early
            if (row.getLastCellNum() <= keyColumnIdx) {
                continue;
            }

            Cell keyCell = row.getCell(keyColumnIdx, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
            if (keyCell == null) {
                continue;
            }

            String value = formatter.formatCellValue(keyCell).trim();
            if (!value.isEmpty()) {
                count++;
                // Optional early exit heuristic if you know max realistic rows
                // if (count > 150_000) break;
            }
        }

        return count;
    }
}
