package com.l2.mvci.main;


import com.l2.Main;
import com.l2.interfaces.Controller;
import com.l2.mvci.load.LoadingController;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;


public class MainController extends Controller<MainMessage> {

    private final MainModel model;
    private final MainInteractor interactor;
    private final MainView view;
    private LoadingController loadingController;

    public MainController() {
        this.model = new MainModel();
        this.interactor = new MainInteractor(model);
        this.view = new MainView(model, this::action);
        createLoadingController();
    }

    @Override
    public Region getView() {
        return view.build();
    }

    @Override
    public void action(MainMessage msg) {
        switch (msg) {
            case FILE_DROPPED_SUCCESS -> interactor.loadWorkbookFromDroppedFile(loadingController); // how can I send action back to do case LOAD_WORKBOOK_REQUEST from this method in interactor
            case CONVERT_TO_SQL -> {
                interactor.convertToSql();
            }
            case LOAD_WORKBOOK_REQUEST -> {

            }

            case NONE, SEND_MESSAGE -> {
                // no-op / future use
            }
        }
    }

    public void createLoadingController() {
        loadingController = new LoadingController();
        loadingController.getStage().setScene(new Scene(loadingController.getView(), Color.TRANSPARENT));
        loadingController.getStage().getScene().getStylesheets().add("css/" + Main.theme + ".css");
    }

    public void showLoadingSpinner(boolean isVisible) {
        loadingController.showLoadSpinner(isVisible);
    }

    public void setSpinnerOffset(double x, double y) {
        loadingController.setOffset(x, y);
    }
}
