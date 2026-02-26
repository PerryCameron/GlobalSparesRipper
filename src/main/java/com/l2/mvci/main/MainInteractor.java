package com.l2.mvci.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l2.*;
import com.l2.dto.ProductToSparesDTO;
import com.l2.dto.ReplacementCrDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.interfaces.GlobalSparesRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainInteractor {

    private final MainModel model;
    private static final Logger logger = LoggerFactory.getLogger(MainInteractor.class);
    private static final GlobalSparesRepository globalSparesRepository = new GlobalSparesRepositoryImpl();

    public MainInteractor(MainModel model) {
        this.model = model;
    }

    private final Executor backgroundExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Conversion-Worker");
        t.setDaemon(true);
        return t;
    });

    private final Executor fxExec = Platform::runLater;  // shorthand for UI thread

    // Helper: createPhase – centralizes UI start/complete logic
    private CompletableFuture<Void> createPhase(String phaseName, Runnable phaseLogic) {
        return CompletableFuture.runAsync(() -> {
            // This runs on background thread
            model.getTa().appendText("Processing " + phaseName + " … ");

            // Reset progress for this phase
            Platform.runLater(() -> {
                model.getProgressBar().setProgress(0);
            });

            phaseLogic.run();   // ← your long-running loop happens here

        }, backgroundExec).thenRunAsync(() -> {
            // Runs after phaseLogic finishes (success path)
            model.getTa().appendText("✅ Completed\n");
        }, fxExec).exceptionallyAsync(ex -> {
            // If phaseLogic throws → caught here
            Platform.runLater(() -> {
                model.getTa().appendText("❌ Failed: " + ex.getMessage() + "\n");
            });
            throw new CompletionException(ex);  // propagate to chain
        }, fxExec);
    }

    public void loadWorkbookFromDroppedFile() {
        String path = model.getDroppedFilePath();
        if (path == null || path.isBlank()) {
            System.err.println("No file path in model");
            return;
        }
        model.viewStatusProperty().setValue(ViewStatus.LOADING_XFS);
        model.getLoadingController().setOffset(50, 0);
        model.getLoadingController().showLoadSpinner(true); // this shows a spinner while the background task continues
        Task<XSSFWorkbook> contactListTask = new Task<>() {
            @Override
            protected XSSFWorkbook call() {
                XSSFWorkbook workbook = null;
                try (FileInputStream fis = new FileInputStream(path)) {
                    workbook = new XSSFWorkbook(fis);  // is there a way to get logs from this action?
                    logger.info("Workbook loaded successfully: {}", path);
                } catch (IOException e) {
                    logger.error("Failed to read Excel file: {}", e.getMessage());
                    // TODO: show error to user (add error property to model)
                    model.setWorkbookReady(false);
                }
                return workbook;
            }
        };
        contactListTask.setOnSucceeded(event -> {
            XSSFWorkbook workbook = contactListTask.getValue();
            if (workbook != null) {
                model.setWorkbook(workbook);
                model.viewStatusProperty().setValue(ViewStatus.XFS_LOADED);
                model.getLoadingController().showLoadSpinner(false);
            }
        });
        contactListTask.setOnFailed(event -> {
            model.setWorkbookReady(false);
            model.getLoadingController().showLoadSpinner(false);
        });
        Thread thread = new Thread(contactListTask);
        thread.start();
    }

    public void prepConvertToSql() {
        model.viewStatusProperty().setValue(ViewStatus.PREP_TO_CONVERT);
        model.getLoadingController().setOffset(50, 0);
        model.getLoadingController().showLoadSpinner(true);
        Task<XSSFWorkbook> createDataBaseTask = new Task<>() {
            @Override
            protected XSSFWorkbook call() {
                if (AppFileTools.moveExistingGlobalSparesDb())
                    logger.info("Existing Global Spares Catalogue found and moved for later comparison");
                else logger.info("There is no existing Global Spares Catalogue found");
                // if it was created then copy it to the old dir
                GlobalSparesSQLiteDatabaseCreator.createDataBase("global-spares.db");
                model.setTotalWork(ExcelRipper.estimateTotalWork(model.getWorkbook()));
                logger.info(model.totalWorkToString());
                return null;
            }
        };
        createDataBaseTask.setOnSucceeded(event -> {
            model.getLoadingController().showLoadSpinner(false);
            model.viewStatusProperty().setValue(ViewStatus.CONVERT_TO_SQL); // Changes UI to reflect state
        });
        createDataBaseTask.setOnFailed(event -> {
            model.getLoadingController().showLoadSpinner(false);
            model.setErrorMessage("Prep Conversion Failed");
            model.viewStatusProperty().setValue(ViewStatus.ERROR);
        });
        Thread thread = new Thread(createDataBaseTask);
        thread.start();
    }


    public void setLoadingController() {
        model.getLoadingController().getStage().setScene(new Scene(model.getLoadingController().getView(), Color.TRANSPARENT));
        model.getLoadingController().getStage().getScene().getStylesheets().add("css/" + Main.theme + ".css");
    }

    public void convertToSql() {
        long start = System.currentTimeMillis();
        model.getProgressBar().setProgress(0);
        model.getTa().appendText("Conversion started...\n");
        globalSparesRepository.changePRAGMASettinsForInsert();

        ProductToSparesDTO dto = new ProductToSparesDTO(false, false);  // shared, but mutated per phase
        ReplacementCrDTO replacementCrDTO = new ReplacementCrDTO();
        List<ProductToSparesDTO> editedSpares = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        // Start the chain
        CompletableFuture<Void> chain = CompletableFuture.runAsync(() -> {
            // Optional: any quick synchronous setup
        }, backgroundExec);

        // ──────────────────────────────────────────────────────
        // Phase 1: Active Product to Spares
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                "Product to Spares",
                () -> getSheet("Product to Spares").ifPresent(sheet ->
                        extractProductToSpares(sheet, dto, false, model.getProductToSparesTotal())
                )
        ), backgroundExec);

        // ──────────────────────────────────────────────────────
        // Phase 2: Archived Product to Spares
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                "Archived Product to Spares",
                () -> {
                    dto.setArchived(true);
                    getSheet("Archived Product to Spares").ifPresent(sheet ->
                            extractProductToSpares(sheet, dto, true, model.getArchivedProductToSparesTotal())
                    );
                }
        ), backgroundExec);

        // ──────────────────────────────────────────────────────
        // Phase 3 Replacement CRs
        // ──────────────────────────────────────────────────────
//        chain = chain.thenComposeAsync(v -> createPhase(
//                "Replacement CRs",
//                () -> {
//                    getSheet("Replacement CRs").ifPresent(sheet ->
//                            extractReplacementCr(sheet, replacementCrDTO, model.getReplacementCRs())
//                    );
//                }
//        ), backgroundExec);

        // ──────────────────────────────────────────────────────
        // Phase 4 Uniflair Cross Reference
        // ──────────────────────────────────────────────────────
//        chain = chain.thenComposeAsync(v -> createPhase(
//                "Uniflair Cross Reference",
//                () -> {
//                    getSheet("Uniflair Cross Reference").ifPresent(sheet ->
//                            extractReplacementCr(sheet, replacementCrDTO, model.getUniflairCrossReference())
//                    );
//                }
//        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 5 Consolidating Product to Spares
        // ──────────────────────────────────────────────────────
//        chain = chain.thenComposeAsync(v -> createPhase(
//                "Consolidating Product to Spares",
//                () -> {
//                    consolidateWithJSON(false, editedSpares, objectMapper);
//                }
//        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 6 Consolidating Archived Product to Spares
        // ──────────────────────────────────────────────────────
//        chain = chain.thenComposeAsync(v -> createPhase(
//                "Consolidating Archived Product to Spares",
//                () -> {
//                    consolidateWithJSON(true, editedSpares, objectMapper);
//                }
//        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 7 Vacuum Database
        // ──────────────────────────────────────────────────────
//        chain = chain.thenComposeAsync(v -> createPhase(
//                "Vacuuming database",
//                () -> {
//                    cleanUpDatabase();
//                }
//        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Final completion / error handling
        // ──────────────────────────────────────────────────────
        chain.whenCompleteAsync((result, ex) -> {
            long end = System.currentTimeMillis();
            System.out.println("Time taken: " + (end - start) + " ms");
            if (ex == null) {
                model.getTa().appendText("All phases completed successfully ✓\n");
                model.viewStatusProperty().set(ViewStatus.CONVERSION_DONE);
                model.getProgressBar().setProgress(1.0);
            } else {
                model.getTa().appendText("❌ Conversion failed: " + ex.getMessage() + "\n");
                ex.printStackTrace(); // or better logger
                model.viewStatusProperty().set(ViewStatus.ERROR);
                model.getProgressBar().setProgress(0);
            }
        }, fxExec);


    }


    // helper to return specified sheet
    private Optional<Sheet> getSheet(String sheetName) {
        Sheet sheet = model.getWorkbook().getSheet(sheetName);
        if (sheet == null) {
            return Optional.empty();
        }
        return Optional.of(sheet);
    }

//     This is the slow, but it does correctly work
//    public void consolidateWithJSON(
//            boolean isArchived,
//            List<ProductToSparesDTO> editedSpares,
//            ObjectMapper objectMapper) {
//
//        // get a list of just the spares (part numbers) as strings distinct with no duplicates from product_to_spares
//        List<String> compactedSpares = globalSparesRepository.getDistinctSpareItems(isArchived);
//        double step = 1.0 / compactedSpares.size();
//
//        compactedSpares.forEach(spare -> {
//            // Get pim_range values for this spare_item from product_to_spares
//            List<String> ranges = globalSparesRepository.getRangesFromSpareItem(spare, isArchived);
//            // Build JSON for pim column
//            List<Map<String, Object>> pimData = new ArrayList<>();
//            for (String range : ranges) {
//                // gets products from the range of the spare
//                List<String> products = globalSparesRepository.getProductsFromRange(spare, range, isArchived);
//                if (!products.isEmpty()) {
//                    Map<String, Object> rangeEntry = new HashMap<>();
//                    rangeEntry.put("range", range);
//                    rangeEntry.put("product_families", products);
//                    pimData.add(rangeEntry);
//                }
//            }
//            // Skip if no pim data
//            if (pimData.isEmpty()) {
//                logger.warn("No pim data for spare_item: {}", spare);
//                return;
//            }
//            // Convert pimData to JSON
//            String pimJson;
//            try {
//                pimJson = objectMapper.writeValueAsString(pimData);
//            } catch (Exception e) {
//                logger.error("Error serializing JSON for spare_item: {}", spare, e);
//                return;
//            }
//            // The spare does not yet exist so it is ok to insert it
//            ProductToSparesDTO productToSpares = globalSparesRepository.getProductToSpares(spare, isArchived);
//            if(globalSparesRepository.getSpareItemId(spare) == 0) {
//                // logger.info("Spare: {}, Replacement Item: {}", spare, productToSpares.getReplacementItem());
//                productToSpares.setPimProductFamily("");
//                productToSpares.setPimRange(pimJson);
//                // inserts into spares using data from productToSpares
//                globalSparesRepository.insertConsolidatedProductToSpare(productToSpares);
//            } else { // the spare exists, we need to update it
//                logger.warn("Spare exists: setting aside");
//                editedSpares.add(productToSpares);
//            }
//            moveProgressIndicator(step);
//        });
//    }

    public void consolidateWithJSON(
            boolean isArchived,
            List<ProductToSparesDTO> editedSpares,
            ObjectMapper objectMapper) {

        var allData = globalSparesRepository.getAllSpareRangeProductsBulk(isArchived);
        if (allData.isEmpty()) return;

        // We only process spares that actually have data
        List<String> spares = new ArrayList<>(allData.keySet());

        Set<String> alreadyExist = globalSparesRepository
                .getExistingSparesInSparesTable(spares, isArchived);

        Map<String, ProductToSparesDTO> toInsertMap = new LinkedHashMap<>();  // preserves order if needed

        double step = 1.0 / spares.size();

        for (String spare : spares) {
            var rangeToProducts = allData.get(spare);
            if (rangeToProducts == null || rangeToProducts.isEmpty()) continue;

            List<Map<String, Object>> pimData = new ArrayList<>();
            for (var entry : rangeToProducts.entrySet()) {
                String range = entry.getKey();
                Set<String> products = (Set<String>) entry.getValue();
                if (!products.isEmpty()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("range", range);
                    m.put("product_families", new ArrayList<>(products));  // ← convert to list here
                    pimData.add(m);
                }
            }

            if (pimData.isEmpty()) continue;

            String pimJson;
            try {
                pimJson = objectMapper.writeValueAsString(pimData);
            } catch (Exception e) {
                logger.error("JSON serialization failed for spare {}", spare, e);
                continue;
            }

            // We still need one DTO per spare (for other fields)
            ProductToSparesDTO dto = globalSparesRepository.getProductToSpares(spare, isArchived);
            if (dto == null) {
                logger.warn("No product_to_spares row found for spare: {}", spare);
                continue;
            }

            dto.setPimRange(pimJson);
            dto.setPimProductFamily("");   // as in your original logic

            if (!alreadyExist.contains(spare)) {
                toInsertMap.putIfAbsent(spare, dto);   // only keep first occurrence
            }

            moveProgressIndicator(step);
        }

        if (!toInsertMap.isEmpty()) {
            Set<ProductToSparesDTO> toInsert = new LinkedHashSet<>(toInsertMap.values());
            globalSparesRepository.batchInsertConsolidated(toInsert);
        }
    }

    public void extractReplacementCr(
            Sheet sheet,
            ReplacementCrDTO replacementCrDTO,
            double total) {

        DataFormatter formatter = new DataFormatter(); // formats like Excel
        double step = 1.0 / total;
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
            moveProgressIndicator(step);
        }

    }

    // I am not using Spring Boot, just the jdbcTemplate library, is this correct?
    public void extractProductToSpares(
            Sheet sheet,
            ProductToSparesDTO prototype,   // ← you can reuse one instance and clear it
            boolean isArchived,
            double total) {

        DataFormatter formatter = new DataFormatter();
        double step = 1.0 / total;

        // Collect all valid rows first
        List<ProductToSparesDTO> batch = new ArrayList<>(2000);

        for (int r = Math.max(3, sheet.getFirstRowNum()); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            ProductToSparesDTO dto = new ProductToSparesDTO(); // or prototype.clear() + reuse
            dto.setArchived(isArchived);
            dto.setCustomAdd(false);

            for (int col = 0; col < 13; col++) {   // adjust if more columns appear
                Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String val = formatter.formatCellValue(cell).trim();

                switch (col) {
                    case 0  -> dto.setPimRange(val);
                    case 1  -> dto.setPimProductFamily(val);
                    case 2  -> dto.setSpareItem(val);
                    case 3  -> dto.setReplacementItem(val);
                    case 4  -> dto.setStandardExchangeItem(val);
                    case 5  -> dto.setSpareDescription(val);
                    case 6  -> dto.setCatalogueVersion(val);
                    case 7  -> dto.setProductEndOfServiceDate(val);
                    case 8  -> {
                        if (isArchived) dto.setRemovedFromCatalogue(val);
                        else            dto.setLastUpdate(val);
                    }
                    case 9  -> {
                        if (isArchived) dto.setComments(val);
                        else            dto.setAddedToCatalogue(val);
                    }
                    case 10 -> {
                        if (!isArchived) {
                            dto.setComments(val.isEmpty() ? null : val);
                        }
                    }
                    // add more cases if needed
                }
            }

            // Optional: skip completely empty / invalid rows
            if (dto.getSpareItem() == null || dto.getSpareItem().isBlank()) {
                continue;
            }

            batch.add(dto);

            // Flush every 500–2000 rows (tune depending on your DB & RAM)
            if (batch.size() >= 1000) {
                globalSparesRepository.insertProductToSparesInBatch(batch);
                batch.clear();
                // Optional: moveProgressIndicator(batchWasSize * step);
            }
            moveProgressIndicator(step);
        }

        // Don't forget the remainder
        if (!batch.isEmpty()) {
            globalSparesRepository.insertProductToSparesInBatch(batch);
        }
    }

    private void moveProgressIndicator(double step) {
        Platform.runLater(() -> {
            double progress = model.getProgressBar().getProgress();
            double newProgress = Math.min(progress + step, 1.0);
            model.getProgressBar().setProgress(newProgress);
        });
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

    public static boolean cleanUpDatabase() {
        globalSparesRepository.dropProductToSparesAndVacuum();
        return true;
    }
}