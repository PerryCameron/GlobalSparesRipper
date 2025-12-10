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

import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelRipper {
    private static final Logger logger = LoggerFactory.getLogger(ExcelRipper.class);
    private static final GlobalSparesRepository globalSparesRepository = new GlobalSparesRepositoryImpl();
    private static List<ProductToSparesDTO> editedSpares = new ArrayList<>();

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

//        addReplacmentCRstoNotes();
        // print all spares we caste aside
//        editedSpares.forEach(System.out::println);
        return true;
    }

    private static void consolidateWithJSON(boolean isArchived, GlobalSparesRepository globalSparesRepository) {
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

    private static void addReplacmentCRstoNotes() {
        GlobalSparesRepositoryImpl globalSparesRepository = new GlobalSparesRepositoryImpl();
        List<ReplacementCrDTO> replacements = globalSparesRepository.findAllReplacementCr();
        int count = 0;
        for (ReplacementCrDTO replacementCrDTO : replacements) {
            count++;
            String replacement = replacementCrDTO.getReplacement();
            if (replacement == null || replacement.trim().isEmpty()) {
                continue;
            }
            boolean exists = globalSparesRepository.existsBySpareItem(replacement);
            if (exists) {
                String note = "Old P/N: " + replacementCrDTO.getItem() + ", " + replacementCrDTO.getComment();
                System.out.println("Updating " + replacement + " note as: " + note);
                globalSparesRepository.appendCommentBySpareItem(replacementCrDTO.getReplacement(), note);
            } else {
                System.out.println("No match found for spare_item: " + replacement);
            }
        }
        System.out.println("Number of replacements: " + count);
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


    private static void extractReplacementCr(
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


//    private static void extractReplacementCr(Sheet sheet, ReplacementCrDTO replacementCrDTO, GlobalSparesRepository globalSparesRepository) {
//        // Iterate through the first 10 rows
//        for (Row row : sheet) {
//            // this is temp for testing
////            if (row.getRowNum() >= 1000) {
////                break; // Stop after 300 rows
////            }
//            // we will not start writing until we get to row three
//            if (row.getRowNum() < 3) {
//                continue;
//            }
//            // start rowCount when we begin
//
//            int colCount = 0;
//            for (Cell cell : row) {
//                String cellValue = getCellValueAsString(cell);
//                switch (colCount) {
//                    case 0 -> replacementCrDTO.setItem(cellValue);
//                    case 1 -> replacementCrDTO.setReplacement(cellValue);
//                    case 2 -> replacementCrDTO.setComment(cellValue);
//                    case 3 -> {
//                        try {
//                            if (!cellValue.isEmpty())
//                                replacementCrDTO.setOldQty(Double.parseDouble(cellValue));
//                            else
//                                replacementCrDTO.setOldQty(0.0);
//                        } catch (Exception e) {
//                            logger.error(e.getMessage());
//                            e.printStackTrace();
//                            replacementCrDTO.setOldQty(99999.0);
//                        }
//                    }
//                    case 4 -> {
//                        try {
//                            if (!cellValue.isEmpty())
//                                replacementCrDTO.setNewQty(Double.parseDouble(cellValue));
//                            else
//                                replacementCrDTO.setNewQty(0.0);
//                        } catch (Exception e) {
//                            logger.error(e.getMessage());
//                            e.printStackTrace();
//                            replacementCrDTO.setNewQty(99999.0);
//                        }
//                    }
//                }
//                colCount++;
//                if (row.getRowNum() % 100 == 0 && colCount == 1) {
//                    logger.info("{} ", row.getRowNum());
//                }
//            }
//            // Print the row
//            if (!replacementCrDTO.getItem().isEmpty())
//                globalSparesRepository.insertReplacementCr(replacementCrDTO);
//            replacementCrDTO.clear();
//        }
//    }

//    private static void extractProductToSpares(Sheet sheet, ProductToSparesDTO productToSpares, GlobalSparesRepository globalSparesRepository, boolean isArchived) {
//        // Iterate through the first 10 rows
//        for (Row row : sheet) {
//            // this is temp for testing
//            if (row.getRowNum() >= 1300) {
//                break; // Stop after 1300 rows
//            }
//            // we will not start writing until we get to row three
//            if (row.getRowNum() < 3) {
//                continue;
//            }
//            // start rowCount when we begin
//
//            int colCount = 0;
//            for (Cell cell : row) {
//                String cellValue = getCellValueAsString(cell);
//
//                if(row.getRowNum() > 800) {
//                    if(colCount == 0) {
//                        System.out.println();
//                        System.out.print("Row: " + row.getRowNum());
//                    }
//                    System.out.print(" " + colCount + ") " + cellValue + " ");
//                }
//
//                switch (colCount) {
//                    case 0 -> {
//                        productToSpares.setPimRange(cellValue);
//                        productToSpares.setArchived(isArchived);
//                        productToSpares.setCustomAdd(false);
//                    }
//                    case 1 -> productToSpares.setPimProductFamily(cellValue);
//                    case 2 -> productToSpares.setSpareItem(cellValue);
//                    case 3 -> productToSpares.setReplacementItem(cellValue); // here is where it goes wrong
//                    case 4 -> {
//                        if(cellValue.equals("Standard Offer")) {  // standard offer should be 6?
//                            System.out.println();
//                            logger.error("Failure! column count= {} row number = {}", colCount, row.getRowNum());
//                            exit(1);
//                        }
//                        productToSpares.setStandardExchangeItem(cellValue);
//                    }
//                    case 5 -> productToSpares.setSpareDescription(cellValue);
//                    case 6 -> productToSpares.setCatalogueVersion(cellValue);
//                    case 7 -> productToSpares.setProductEndOfServiceDate(cellValue);
//                    case 8 -> {
//                        if (isArchived)
//                            productToSpares.setRemovedFromCatalogue(cellValue);
//                        else
//                            productToSpares.setLastUpdate(cellValue);
//                    }
//                    case 9 -> {
//                        if (isArchived)
//                            productToSpares.setComments(cellValue);
//                        else
//                            productToSpares.setAddedToCatalogue(cellValue);
//                    }
//                    case 10 -> {
//                        if (!isArchived)
//                            if(cellValue.isEmpty())
//                                productToSpares.setComments(null);
//                            else
//                                productToSpares.setComments(cellValue);
//                    }
//                }
////                if (row.getRowNum() < 5)
////                    System.out.println(colCount + ") " + cellValue);
//                colCount++;
////                if (row.getRowNum() % 100 == 0 && colCount == 1) {
////                    logger.info("{} ", row.getRowNum());
////                }
//            }
//            // Print the row
//            globalSparesRepository.insertProductToSpare(productToSpares);
//            productToSpares.clear();
//        }
//    }


    private static void extractProductToSpares(
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

//                if (r > 800) {
//                    if (col == 0) {
//                        System.out.println();
//                        System.out.print("Row: " + r);
//                    }
//                    System.out.print(" " + col + ") " + cellValue + " ");
//                }

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


    public static String getCellValueAsString(Cell cell) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Format the date to "yyyy-MM-dd HH:mm:ss"
                    Date date = cell.getDateCellValue();
                    return dateFormat.format(date);
                }
                // Format numbers to avoid scientific notation
                return String.format("%.8f", cell.getNumericCellValue()).replaceAll("\\.0+$", "");
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
