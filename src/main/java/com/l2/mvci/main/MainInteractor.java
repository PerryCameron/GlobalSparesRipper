package com.l2.mvci.main;

import com.l2.*;
import com.l2.dto.*;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.ProductionRepositoryImpl;
import com.l2.repository.interfaces.GlobalSparesRepository;
import com.l2.repository.interfaces.OldRepository;
import com.l2.repository.interfaces.ProductionRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

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
    private static GlobalSparesRepository globalSparesRepository = null;
    private static final OldRepository oldRepository = null;
    private static ProductionRepository productionRepository = null;

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
            // for testing
            if (Main.testMode) {
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
                model.getTaskList().get(model.incrementElement()).setCompleted(true);
            }
            // normal
            model.getTaskList().get(model.incrementElement()).setCompleted(true);
        }, fxExec).exceptionallyAsync(ex -> {
            logger.error(ex.getMessage(), ex);
            Platform.runLater(() -> {
                model.setErrorMessage(ex.getMessage());
                model.viewStatusProperty().setValue(ViewStatus.ERROR);
            });
            throw new CompletionException(ex);  // propagate to chain
        }, fxExec);
    }

    public void loadWorkbookFromDroppedFile() {
        String path = model.getDroppedFilePath();
        if (path == null || path.isBlank()) {
            logger.error("No file path in model");
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
                // TODO for testing remove later
                if (!Main.testMode) {
                    if (AppFileTools.moveExistingGlobalSparesDb()) {
                        logger.info("Existing Global Spares Catalogue found and moved for later comparison");
                        model.sparesDataBaseProperty().setValue(true);
                    } else {
                        logger.info("There is no existing Global Spares Catalogue found");
                    }
                    // Create new db if in normal mode
                    GlobalSparesSQLiteDatabaseCreator.createDataBase("global-spares.db");
                    globalSparesRepository =  new GlobalSparesRepositoryImpl();
                }

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
        model.getTaskList().addAll(
                new TaskItem("Adding Product to Spares"),
                new TaskItem("Adding Archived Product to Spares"),
                new TaskItem("Adding Replacement CRs"),
                new TaskItem("Adding Uniflair Cross Reference"),
                new TaskItem("Consolidating Product to Spares"),
                new TaskItem("Consolidating Archived Product to Spares"),
                new TaskItem("Vacuuming database"),
                new TaskItem("Calculating Changes")
        );
        // changes sqlite PRAGMA settings for speed
        globalSparesRepository.changePRAGMASettinsForInsert();
        List<ProductToSparesDTO> editedSpares = new ArrayList<>();

        // Start the chain
        CompletableFuture<Void> chain = CompletableFuture.runAsync(() -> {
            // Optional: any quick synchronous setup
        }, backgroundExec);

        if (!Main.testMode) {
            // ──────────────────────────────────────────────────────
            // Phase 1: Active Product to Spares
            // ──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Product to Spares",
                    () -> getSheet("Product to Spares").ifPresent(sheet ->
                            extractProductToSpares(sheet, false, model.getProductToSparesTotal())
                    )
            ), backgroundExec);

            // ──────────────────────────────────────────────────────
            // Phase 2: Archived Product to Spares
            // ──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Archived Product to Spares",
                    () -> {
                        getSheet("Archived Product to Spares").ifPresent(sheet ->
                                extractProductToSpares(sheet, true, model.getArchivedProductToSparesTotal())
                        );
                    }
            ), backgroundExec);
            //──────────────────────────────────────────────────────
            // Phase 3 Replacement CRs
            //──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Replacement CRs",
                    () -> {
                        getSheet("Replacement CRs").ifPresent(sheet ->
                                extractReplacementCr(sheet, model.getReplacementCRs())
                        );
                    }
            ), backgroundExec);
            //──────────────────────────────────────────────────────
            // Phase 4 Uniflair Cross Reference
            //──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Uniflair Cross Reference",
                    () -> {
                        getSheet("Uniflair Cross Reference").ifPresent(sheet ->
                                extractReplacementCr(sheet, model.getUniflairCrossReference())
                        );
                    }
            ), backgroundExec);
            // ──────────────────────────────────────────────────────
            // Phase 5 Consolidating Product to Spares
            // ──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Consolidating Product to Spares",
                    () -> {
                        // increases speed by 3 seconds but increases size
                        //globalSparesRepository.indexProductToSpares();
                        consolidateWithJSON(false, editedSpares);
                    }
            ), backgroundExec);
            // ──────────────────────────────────────────────────────
            // Phase 6 Consolidating Archived Product to Spares
            // ──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Consolidating Archived Product to Spares",
                    () -> {
                        consolidateWithJSON(true, editedSpares);
                    }
            ), backgroundExec);
            // ──────────────────────────────────────────────────────
            // Phase 7 Vacuum Database
            // ──────────────────────────────────────────────────────
            chain = chain.thenComposeAsync(v -> createPhase(
                    "Vacuuming database",
                    () -> {
                        cleanUpDatabase();
                    }
            ), backgroundExec);
            // ──────────────────────────────────────────────────────
            // Final completion / error handling
            // ──────────────────────────────────────────────────────
        }

        chain.whenCompleteAsync((result, ex) -> {
            long end = System.currentTimeMillis();
            String timeTaken = "Rip time: " + millisecondsToMinutesSeconds(end - start);
            logger.info("Time taken: {} ms", timeTaken);

            Optional<SpareComparisonResult> compareSparesTables = compareSparesTables();
            if (compareSparesTables.isPresent()) {
                model.spareComparisonResultProperty().setValue(compareSparesTables.get()); // do we need this?
                model.viewStatusProperty().set(ViewStatus.VIEW_CHANGES);
                model.spareComparisonResultProperty().get();
                logger.info("There are changes made to the database");
                logger.info("Spares added to database: {}", compareSparesTables.get().getAdded());
                logger.info("Spares removed from database: {}", compareSparesTables.get().getRemoved());
                logger.info("Spares archived: {}", compareSparesTables.get().getArchived());
                logger.info("Spares unarchived: {}", compareSparesTables.get().getUnarchived());
                logger.info("Range and Product Family Changes: {}", compareSparesTables.get().getPimChanges());
                logger.info("Replacement Item Changes: {}", compareSparesTables.get().getReplacementItemChanges());
                logger.info("Std Exchange Item Changes: {}", compareSparesTables.get().getStandardExchangeItemChanges());
                logger.info("Spare Description Changes: {}", compareSparesTables.get().getSpareDescriptionChanges());
                logger.info("End of Service Date Changes: {}", compareSparesTables.get().getEndOfServiceDateChanges());
                logger.info("Last Update Changes: {}", compareSparesTables.get().getLastUpdateChanges());
                logger.info("Added to Catalogue Date Changes: {}", compareSparesTables.get().getAddedToCatalogueChanges());
                logger.info("Removed from Catalogue Date Changes: {}", compareSparesTables.get().getRemovedFromCatalogueChanges());
                logger.info("Comments Changes: {}", compareSparesTables.get().getCommentsChanges());
            } else {
                model.viewStatusProperty().set(ViewStatus.UPDATE_OPTIONS);
                logger.warn("There is no previous db to calculate changes, moving to options screen");
            }
            model.statusMessageProperty().set(timeTaken);
            if (ex == null) {
                model.getTa().appendText("All phases completed successfully ✓\n");
                //model.viewStatusProperty().set(ViewStatus.CONVERSION_DONE);
                model.getProgressBar().setProgress(1.0);
            } else {
                model.getTa().appendText("❌ Conversion failed: " + ex.getMessage() + "\n");
                ex.printStackTrace(); // or better logger
                model.viewStatusProperty().set(ViewStatus.ERROR);
                model.getProgressBar().setProgress(0);
            }
        }, fxExec);
    }

    public static String millisecondsToMinutesSeconds(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    // helper to return specified sheet
    private Optional<Sheet> getSheet(String sheetName) {
        Sheet sheet = model.getWorkbook().getSheet(sheetName);
        if (sheet == null) {
            return Optional.empty();
        }
        return Optional.of(sheet);
    }

    //Claud's version
    public void consolidateWithJSON(
            boolean isArchived,
            List<ProductToSparesDTO> editedSpares) {

        List<ProductToSparesDTO> consolidated = globalSparesRepository.getConsolidatedSpares(isArchived);

        // Batch-fetch which spare_items already exist in `spares`
        // SELECT spare_item FROM spares WHERE spare_item IN (...)
        List<String> allSpareItems = consolidated.stream()
                .map(ProductToSparesDTO::getSpareItem)
                .toList();
        // Non-static method 'getExistingSpareItems(java.util.List<java.lang.String>)' cannot be referenced from a static context <- I don't understand how this is a static context
        Set<String> existingSpares = globalSparesRepository.getExistingSpareItems(allSpareItems);

        double step = 1.0 / consolidated.size();

        for (ProductToSparesDTO dto : consolidated) {
            if (dto.getPimRange() == null) continue; // was skipped due to empty pim

            if (!existingSpares.contains(dto.getSpareItem())) {
                globalSparesRepository.insertConsolidatedProductToSpare(dto);
            } else {
                logger.warn("Spare {} exists: setting aside", dto.getSpareItem());
                editedSpares.add(dto);
            }
            moveProgressIndicator(step);
        }
    }

    public void extractReplacementCr(
            Sheet sheet,
            double total) {

        DataFormatter formatter = new DataFormatter();
        double step = 1.0 / total;

        // We'll create fresh DTOs — reusing one object can be error-prone with batching
        List<ReplacementCrDTO> batch = new ArrayList<>(1000);

        // Adjust starting row if your header is on row 1 or 2 (0-based)
        for (int r = Math.max(3, sheet.getFirstRowNum()); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            ReplacementCrDTO dto = new ReplacementCrDTO();

            for (int col = 0; col < 5; col++) {   // columns A–E (0–4)
                Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String text = formatter.formatCellValue(cell).trim();

                switch (col) {
                    case 0 -> dto.setItem(text);
                    case 1 -> dto.setReplacement(text);
                    case 2 -> dto.setComment(text.isEmpty() ? null : text);

                    case 3 -> { // OldQty
                        dto.setOldQty(parseDoubleCell(cell, text, 0.0));
                    }
                    case 4 -> { // NewQty
                        dto.setNewQty(parseDoubleCell(cell, text, 0.0));
                    }
                }
            }

            // Skip invalid rows (core business rule)
            if (dto.getItem() == null || dto.getItem().isBlank()) {
                continue;
            }

            batch.add(dto);

            // Flush batch when large enough
            if (batch.size() >= 1000) {
                globalSparesRepository.insertReplacementCrInBatch(batch);
                batch.clear();
                // Optional: update UI progress bar
                // moveProgressIndicator(1000 * step);
            }

            moveProgressIndicator(step);
        }

        // Final batch (remainder)
        if (!batch.isEmpty()) {
            globalSparesRepository.insertReplacementCrInBatch(batch);
        }
    }

    public void extractProductToSpares(
            Sheet sheet,
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
                    case 0 -> dto.setPimRange(val);
                    case 1 -> dto.setPimProductFamily(val);
                    case 2 -> dto.setSpareItem(val);
                    case 3 -> dto.setReplacementItem(val);
                    case 4 -> dto.setStandardExchangeItem(val);
                    case 5 -> dto.setSpareDescription(val);
                    case 6 -> dto.setCatalogueVersion(val);
                    case 7 -> dto.setProductEndOfServiceDate(val);
                    case 8 -> {
                        if (isArchived) dto.setRemovedFromCatalogue(val);
                        else dto.setLastUpdate(val);
                    }
                    case 9 -> {
                        if (isArchived) dto.setComments(val);
                        else dto.setAddedToCatalogue(val);
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
            logger.error("Qty parse failed at row {}, col {}: {}", cell.getRowIndex(), cell.getColumnIndex(), e.getMessage());
            return 99999.0; // or fallback
        }
    }

    public static boolean cleanUpDatabase() {
        globalSparesRepository.dropProductToSparesAndVacuum();
        return true;
    }

    private static String ts() {
        return java.time.LocalDateTime.now() + " [" + Thread.currentThread().getName() + "]";
    }

    public Optional<SpareComparisonResult> compareSparesTables() {

        if(oldRepository == null) return Optional.empty();
        Map<String, SparesDTO> oldSpares = oldRepository.getAllBySpareItem();

        if (oldSpares.size() == 0) return Optional.empty();
        Map<String, SparesDTO> newSpares = globalSparesRepository.getAllBySpareItem();

        int added = (int) newSpares.keySet().stream()
                .filter(item -> !oldSpares.containsKey(item))
                .count();

        int removed = (int) oldSpares.keySet().stream()
                .filter(item -> !newSpares.containsKey(item))
                .count();

        int archived = 0, unarchived = 0;
        int pimChanges = 0, replacementItemChanges = 0, standardExchangeItemChanges = 0;
        int spareDescriptionChanges = 0, endOfServiceDateChanges = 0, lastUpdateChanges = 0;
        int addedToCatalogueChanges = 0, removedFromCatalogueChanges = 0, commentsChanges = 0;

        for (Map.Entry<String, SparesDTO> entry : newSpares.entrySet()) {
            String key = entry.getKey();
            if (!oldSpares.containsKey(key)) continue;

            SparesDTO newDto = entry.getValue();
            SparesDTO oldDto = oldSpares.get(key);

            if (!oldDto.getArchived() && newDto.getArchived()) archived++;
            if (oldDto.getArchived() && !newDto.getArchived()) unarchived++;

            if (!Objects.equals(oldDto.getPim(), newDto.getPim())) pimChanges++;
            if (!Objects.equals(oldDto.getReplacementItem(), newDto.getReplacementItem())) replacementItemChanges++;
            if (!Objects.equals(oldDto.getStandardExchangeItem(), newDto.getStandardExchangeItem()))
                standardExchangeItemChanges++;
            if (!Objects.equals(oldDto.getSpareDescription(), newDto.getSpareDescription())) spareDescriptionChanges++;
            if (!Objects.equals(oldDto.getProductEndOfServiceDate(), newDto.getProductEndOfServiceDate()))
                endOfServiceDateChanges++;
            if (!Objects.equals(oldDto.getLastUpdate(), newDto.getLastUpdate())) lastUpdateChanges++;
            if (!Objects.equals(oldDto.getAddedToCatalogue(), newDto.getAddedToCatalogue())) addedToCatalogueChanges++;
            if (!Objects.equals(oldDto.getRemovedFromCatalogue(), newDto.getRemovedFromCatalogue()))
                removedFromCatalogueChanges++;
            if (!Objects.equals(oldDto.getComments(), newDto.getComments())) commentsChanges++;
        }

        return Optional.of(new SpareComparisonResult(
                added, removed, archived, unarchived,
                pimChanges, replacementItemChanges, standardExchangeItemChanges,
                spareDescriptionChanges, endOfServiceDateChanges, lastUpdateChanges,
                addedToCatalogueChanges, removedFromCatalogueChanges, commentsChanges
        ));
    }

    public void closeApplication() {
        System.exit(0);
    }

    public void updateOptions() {
        model.viewStatusProperty().set(ViewStatus.UPDATE_OPTIONS);
    }

    public void buildFinalDatabase() {
        String absolutePath = model.fileNameProperty().get().getText();
        productionRepository = new ProductionRepositoryImpl(absolutePath);

        if (model.dataBaseOptionsObjectProperty().get().includesCustomParts()) {
            List<SparesDTO> customSpares = productionRepository.getCustomAddedSpares();
            int[] updates = globalSparesRepository.batchInsertSpares(customSpares);
            logBatchInsertResult(updates, "custom spares");
        }

        // next lets add ranges
        if (model.dataBaseOptionsObjectProperty().get().includes3PhaseRanges())
            GlobalSparesSQLiteDatabaseCreator.insert3phRanges("global-spares.db");

        if (model.dataBaseOptionsObjectProperty().get().includeaCoolingRanges())
            GlobalSparesSQLiteDatabaseCreator.insertCoolingRanges("global-spares.db");

        if (model.dataBaseOptionsObjectProperty().get().includesCustomNotes()) {

        }

        if (model.dataBaseOptionsObjectProperty().get().includesPhotos()) {

        }
    }

    private void logBatchInsertResult(int[] results, String label) {
        int inserted = 0;
        for (int r : results) {
            if (r > 0) {
                inserted += r;
            }
        }
        logger.info("Inserted {} of {} {}", inserted, results.length, label);
    }
}