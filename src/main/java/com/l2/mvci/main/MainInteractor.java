package com.l2.mvci.main;

import com.l2.*;
import com.l2.dto.ProductToSparesDTO;
import com.l2.dto.ReplacementCrDTO;
import com.l2.repository.implementations.GlobalSparesRepositoryImpl;
import com.l2.repository.interfaces.GlobalSparesRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class MainInteractor {

    private final MainModel model;
    private static final Logger logger = LoggerFactory.getLogger(MainInteractor.class);
    private static final GlobalSparesRepository globalSparesRepository = new GlobalSparesRepositoryImpl();

    public MainInteractor(MainModel model) {
        this.model = model;
    }

    public void loadWorkbookFromDroppedFile() {
        String path = model.getDroppedFilePath();
        if (path == null || path.isBlank()) {
            System.err.println("No file path in model");
            return;
        }
        model.viewStatusProperty().setValue(ViewStatus.LOADING_XFS);
        model.getLoadingController().setOffset(50, 0);
        model.getLoadingController().showLoadSpinner(true);
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
            model.viewStatusProperty().setValue(ViewStatus.CONVERT_TO_SQL);
        });
        createDataBaseTask.setOnFailed(event -> {model.getLoadingController().showLoadSpinner(false);});
        Thread thread = new Thread(createDataBaseTask);
        thread.start();
    }


    public void setLoadingController() {
        model.getLoadingController().getStage().setScene(new Scene(model.getLoadingController().getView(), Color.TRANSPARENT));
        model.getLoadingController().getStage().getScene().getStylesheets().add("css/" + Main.theme + ".css");
    }

    public void convertToSql() {
        extractWorkbookToSql();
    }


    public boolean extractWorkbookToSql() {
        Task<Boolean> extractToSql = new Task<>() {
            @Override
            protected Boolean call() {
                Sheet sheet = model.getWorkbook().getSheet("Product to Spares");
                if (sheet == null) {
                    System.out.println("Sheet 'Product to Spares' not found.");
                    return false;
                }
                // extracts metadata from workbook
                logger.info("Saving Meta data properties");

                ExcelRipper.extractWorkbookProperties(model.getWorkbook(), globalSparesRepository);
                Platform.runLater(() -> {
                    model.getTa().appendText("Extracted Workbook Properties");
                    model.getProgressBar().setProgress(0.01);
                });
                // here is where we fill the product to spares table with items in the catelogue
                ProductToSparesDTO productToSpares = new ProductToSparesDTO(false, false);  // archived, customadd
                logger.info("Ripping Product to Spares");  // succeeds
                extractProductToSpares(sheet, productToSpares, false);
                Platform.runLater(() -> {
                    model.getTa().appendText("Ripped Product to Spares");
                });

                // here is where we fill the product to spares table with items that are archived
                productToSpares.setArchived(true);
                sheet = model.getWorkbook().getSheet("Archived Product to Spares");
                logger.info("Ripping Archived Product to Spares");  // succeeds
                ExcelRipper.extractProductToSpares(sheet, productToSpares, globalSparesRepository, true);
                ReplacementCrDTO replacementCrDTO = new ReplacementCrDTO();
                Platform.runLater(() -> {
                    model.getTa().appendText("Ripped Archived Product to Spares");
                });

                // here is where we fill our replament_cr table with 3-phase
                sheet = model.getWorkbook().getSheet("Replacement CRs");
                logger.info("Ripping Replacement CRs (3-ph)");
                ExcelRipper.extractReplacementCr(sheet, replacementCrDTO, globalSparesRepository);
                Platform.runLater(() -> {
                    model.getTa().appendText("Ripped Replacement CRs (3-ph)");
                });

                // here is where we fill our replacement_cr with uniflair
                sheet = model.getWorkbook().getSheet("Uniflair Cross Reference");
                logger.info("Ripping Replacement CRs (Uniflair Cross Reference)");
                ExcelRipper.extractReplacementCr(sheet, replacementCrDTO, globalSparesRepository);
                Platform.runLater(() -> {
                    model.getTa().appendText("Ripped Replacement CRs (Uniflair Cross Reference)");
                });

                // this is normalizing
                logger.info("Consolidating Product to Spares ");
                ExcelRipper.consolidateWithJSON(false, globalSparesRepository);
                Platform.runLater(() -> {
                    model.getTa().appendText("Consolidated Product to Spares ");
                });
                // this is more normalizing
                logger.info("Consolidating Archived Product to Spares");
                ExcelRipper.consolidateWithJSON(true, globalSparesRepository);
                Platform.runLater(() -> {
                    model.getTa().appendText("Consolidated Archived Product to Spares");
                });
                return true;
            }
        };
        extractToSql.setOnSucceeded(event -> {

        });
        extractToSql.setOnFailed(event -> {});
        Thread thread = new Thread(extractToSql);
        thread.start();
        return false;
    }

    public void extractProductToSpares(
            Sheet sheet,
            ProductToSparesDTO productToSpares,
            boolean isArchived) {

        DataFormatter formatter = new DataFormatter(); // formats numbers/dates like Excel shows them

        // Start from row 3 (0-based indexing), stop at 1300
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            if (r >= 1300) break;
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


            Platform.runLater(() -> {
                double progress = model.getProgressBar().getProgress();
                double total = model.getProductToSparesTotal(); // ensure this is double or cast below

                // Use floating-point division
                double step = 1.0 / total;          // NOT 1 / total
                double newProgress = Math.min(progress + step, 1.0);

                System.out.println(newProgress);
                model.getProgressBar().setProgress(newProgress);
            });

        }
    }
}