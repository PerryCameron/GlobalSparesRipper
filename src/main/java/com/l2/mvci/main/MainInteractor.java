package com.l2.mvci.main;

import com.l2.*;
import com.l2.dto.*;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.implementations.OldRepositoryImpl;
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
    private CompletableFuture<Void> createPhase(boolean selected, String phaseName, Runnable phaseLogic) {
        TaskItem newTask = new TaskItem(phaseName);
        fxExec.execute(() -> {
            // Reset progress for this phase
            model.getProgressBar().setProgress(0);
            model.getTaskList().add(newTask);
        }); // JavaFX thread
        // Conversion-Worker thread
        if (selected) return CompletableFuture.runAsync(() -> {
            // This runs on background thread
            model.getTa().appendText("Processing " + phaseName + " … ");

            if(!Main.testMode)
                phaseLogic.run();   // ← your long-running loop happens here
            else logger.info("Phase {} is running in test mode", phaseName);
        }, backgroundExec).thenRunAsync(() -> {
            // JavaFX thread
            // Runs after phaseLogic finishes (success path)
            newTask.setCompleted(true);
        }, fxExec).exceptionallyAsync(ex -> {
            logger.error(ex.getMessage(), ex);
            Platform.runLater(() -> {
                model.setErrorMessage(ex.getMessage());
                model.viewStatusProperty().setValue(ViewStatus.ERROR);
            });
            throw new CompletionException(ex);  // propagate to chain
        }, fxExec);

        // default not selected
        return CompletableFuture.runAsync(() -> {
            logger.info("Phase {} is not selected", phaseName);
            newTask.includeProperty().set(false);
            newTask.completedProperty().set(true);
        }, backgroundExec).thenRunAsync(() -> newTask.setCompleted(true), fxExec);
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
                // model.viewStatusProperty().setValue(ViewStatus.XFS_LOADED);
                model.viewStatusProperty().setValue(ViewStatus.UPDATE_OPTIONS);
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

                }
                globalSparesRepository = new GlobalSparesRepositoryImpl();
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
        String absolutePath = model.fileNameProperty().get().getText();
        productionRepository = new ProductionRepositoryImpl(absolutePath);
        model.spareComparisonResultProperty().setValue(new SpareComparisonResult());

        long start = System.currentTimeMillis();
        model.getProgressBar().setProgress(0);

        List<ProductToSparesDTO> editedSpares = new ArrayList<>();
        // Start the chain
        CompletableFuture<Void> chain = CompletableFuture.runAsync(() -> {
            // Optional: any quick synchronous setup
        }, backgroundExec);

        // changes sqlite PRAGMA settings for speed
        globalSparesRepository.changePRAGMASettinsForInsert();
        // ──────────────────────────────────────────────────────
        // Phase 1: Active Product to Spares
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                true,
                "Product to Spares",
                () -> getSheet("Product to Spares").ifPresent(sheet ->
                        extractProductToSpares(sheet, false, model.getProductToSparesTotal())
                )
        ), backgroundExec);

        // ──────────────────────────────────────────────────────
        // Phase 2: Archived Product to Spares
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(true,
                "Archived Product to Spares",
                () -> getSheet("Archived Product to Spares").ifPresent(sheet ->
                        extractProductToSpares(sheet, true, model.getArchivedProductToSparesTotal())
                )
        ), backgroundExec);
        //──────────────────────────────────────────────────────
        // Phase 3 Replacement CRs
        //──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(true,
                "Replacement CRs",
                () -> getSheet("Replacement CRs").ifPresent(sheet ->
                        extractReplacementCr(sheet, model.getReplacementCRs())
                )
        ), backgroundExec);
        //──────────────────────────────────────────────────────
        // Phase 4 Uniflair Cross Reference
        //──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(true,
                "Uniflair Cross Reference",
                () -> getSheet("Uniflair Cross Reference").ifPresent(sheet ->
                        extractReplacementCr(sheet, model.getUniflairCrossReference())
                )
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 5 Consolidating Product to Spares
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(true,
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
        chain = chain.thenComposeAsync(v -> createPhase(true,
                "Consolidating Archived Product to Spares",
                () -> consolidateWithJSON(true, editedSpares)
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 7 Vacuum Database
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(true,
                "Vacuuming database",
                MainInteractor::cleanUpDatabase
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 8 Adding Custom Spares
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                model.dataBaseOptionsObjectProperty().get().includesCustomParts(),
                "Adding Custom Spares",
                () -> {
                    List<SparesDTO> customSpares = productionRepository.getCustomAddedSpares();
                    int[] updates = globalSparesRepository.batchInsertSpares(customSpares);
                    logBatchInsertResult(updates, "custom spares");
                }
        ), backgroundExec);

        // ──────────────────────────────────────────────────────
        // Phase 9 Adding 3ph Ranges
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                model.dataBaseOptionsObjectProperty().get().includes3PhaseRanges(),
                "Adding 3ph Ranges",
                () -> GlobalSparesSQLiteDatabaseCreator.insert3phRanges("global-spares.db")
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 10 Adding 3ph Ranges
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                model.dataBaseOptionsObjectProperty().get().includeaCoolingRanges(),
                "Adding Cooling Ranges",
                () -> GlobalSparesSQLiteDatabaseCreator.insertCoolingRanges("global-spares.db")
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 11 Adding Tech Support Custom Notes
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                model.dataBaseOptionsObjectProperty().get().includesCustomNotes(),
                "Adding Tech Support Custom Notes",
                () -> {
                    List<SparesDTO> customNotes = productionRepository.getAllSparesWithKeywords();
                    int[] updates = globalSparesRepository.syncKeywordsFromProduction(customNotes);
                    Platform.runLater(() -> model.spareComparisonResultProperty().get().setCustomNotesAdded(countOnes(updates)));
                    logBatchInsertResult(updates, "custom notes");
                }
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 12 Include Photos from original database
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                model.dataBaseOptionsObjectProperty().get().includesPhotos(),
                "Adding Photos from original database",
                this::migratePhotos
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Phase 13 Include last_updated_by
        // ──────────────────────────────────────────────────────
        chain = chain.thenComposeAsync(v -> createPhase(
                true,
                "Adding updated by information",
                this::migrateLastUpdatedBy
        ), backgroundExec);
        // ──────────────────────────────────────────────────────
        // Final completion / error handling
        // ──────────────────────────────────────────────────────


        chain.whenCompleteAsync((result, ex) -> {
            long end = System.currentTimeMillis();
            String timeTaken = "Rip time: " + millisecondsToMinutesSeconds(end - start);
            logger.info("Time taken: {} ms", timeTaken);

            if(compareSparesTables()) {
                globalSparesRepository.insertComparisonResult(model.spareComparisonResultProperty().get());
                model.viewStatusProperty().set(ViewStatus.VIEW_CHANGES);
                logger.info("There are changes made to the database");
                logger.info("Spares added to database: {}", model.spareComparisonResultProperty().get().getAdded());
                logger.info("Spares removed from database: {}", model.spareComparisonResultProperty().get().getRemoved());
                logger.info("Spares archived: {}", model.spareComparisonResultProperty().get().getArchived());
                logger.info("Spares unarchived: {}", model.spareComparisonResultProperty().get().getUnarchived());
                logger.info("Range and Product Family Changes: {}", model.spareComparisonResultProperty().get().getPimChanges());
                logger.info("Replacement Item Changes: {}", model.spareComparisonResultProperty().get().getReplacementItemChanges());
                logger.info("Std Exchange Item Changes: {}", model.spareComparisonResultProperty().get().getStandardExchangeItemChanges());
                logger.info("Spare Description Changes: {}", model.spareComparisonResultProperty().get().getSpareDescriptionChanges());
                logger.info("End of Service Date Changes: {}", model.spareComparisonResultProperty().get().getEndOfServiceDateChanges());
                logger.info("Last Update Changes: {}", model.spareComparisonResultProperty().get().getLastUpdateChanges());
                logger.info("Added to Catalogue Date Changes: {}", model.spareComparisonResultProperty().get().getAddedToCatalogueChanges());
                logger.info("Removed from Catalogue Date Changes: {}", model.spareComparisonResultProperty().get().getRemovedFromCatalogueChanges());
                logger.info("Comments Changes: {}", model.spareComparisonResultProperty().get().getCommentsChanges());
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
                logger.error(ex.getMessage()); // or better logger
                model.viewStatusProperty().set(ViewStatus.ERROR);
                model.getProgressBar().setProgress(0);
            }
        }, fxExec);
    }


    public static int countOnes(int[] updates) {
        int count = 0;
        for (int value : updates) {
            if (value == 1) {
                count++;
            }
        }
        return count;
    }

    public boolean compareSparesTables() {
        OldRepository oldRepository = new OldRepositoryImpl();

        if (oldRepository == null) {
            logger.error("Old repository is null!");
            return false;
        }
        Map<String, SparesDTO> oldSpares = oldRepository.getAllBySpareItem();

        if (oldSpares.isEmpty()) {
            logger.error("Old repository is empty!");
            return false;
        }

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
            if (!Objects.equals(oldDto.getSpareDescription(), newDto.getSpareDescription()))
                spareDescriptionChanges++;
            if (!Objects.equals(oldDto.getProductEndOfServiceDate(), newDto.getProductEndOfServiceDate()))
                endOfServiceDateChanges++;
            if (!Objects.equals(oldDto.getLastUpdate(), newDto.getLastUpdate())) lastUpdateChanges++;
            if (!Objects.equals(oldDto.getAddedToCatalogue(), newDto.getAddedToCatalogue()))
                addedToCatalogueChanges++;
            if (!Objects.equals(oldDto.getRemovedFromCatalogue(), newDto.getRemovedFromCatalogue()))
                removedFromCatalogueChanges++;
            if (!Objects.equals(oldDto.getComments(), newDto.getComments())) commentsChanges++;
        }

        model.spareComparisonResultProperty().get().setValues(
                added, removed, archived, unarchived,
                pimChanges, replacementItemChanges, standardExchangeItemChanges,
                spareDescriptionChanges, endOfServiceDateChanges, lastUpdateChanges,
                addedToCatalogueChanges, removedFromCatalogueChanges, commentsChanges);
        return true;
    }

    public void migrateLastUpdatedBy() {
        List<SparesDTO> sparesWithUpdates = productionRepository.getSparesWithLastUpdatedBy();

        if (sparesWithUpdates.isEmpty()) {
            logger.info("No spares with last_updated_by found in production DB, nothing to migrate");
            return;
        }

        int[] updateCounts = globalSparesRepository.migrateLastUpdatedBy(sparesWithUpdates);

        int totalUpdated = 0;
        for (int count : updateCounts) {
            if (count > 0) totalUpdated++;
        }
        int notFound = sparesWithUpdates.size() - totalUpdated;
        model.spareComparisonResultProperty().get().setUpdateByAdded(totalUpdated);  // I don't think we need to use long, maybe change longs to int so I don't have to cast
        logger.info("Migrated last_updated_by for {}/{} spares ({} not found in new DB)",
                totalUpdated, sparesWithUpdates.size(), notFound);
    }

    public void migratePhotos() {
        int total = productionRepository.countSparePictures();
        double step = 1.0 / total;

        List<SparePictureDTO> pictures = productionRepository.getAllSparePictures();

        int copied = 0;
        int skipped = 0;

        for (SparePictureDTO picture : pictures) {
            if (globalSparesRepository.spareExists(picture.getSpareName())) {
                globalSparesRepository.insertSparePicture(picture);
                copied++;
            } else {
                logger.info("Spare '{}' not found in new database, skipping photo.", picture.getSpareName());
                skipped++;
            }
            moveProgressIndicator(step);
        }
        model.spareComparisonResultProperty().get().setPhotosCopied(copied);
        model.spareComparisonResultProperty().get().setPhotosSkipped(skipped);

        logger.info("Photo migration complete. Copied: {}, Skipped: {}", copied, skipped);
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

                    case 3 -> // OldQty
                            dto.setOldQty(parseDoubleCell(cell, text, 0.0));
                    case 4 -> // NewQty
                            dto.setNewQty(parseDoubleCell(cell, text, 0.0));
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
        System.out.println("Cleaning up database...");
        globalSparesRepository.dropProductToSparesAndVacuum();
        return true;
    }

    public void closeApplication() {
        System.exit(0);
    }

    public void updateOptions() {
        model.viewStatusProperty().set(ViewStatus.UPDATE_OPTIONS);
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