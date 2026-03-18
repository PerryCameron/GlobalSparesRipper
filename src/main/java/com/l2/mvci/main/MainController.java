package com.l2.mvci.main;


import com.l2.interfaces.Controller;
import javafx.scene.layout.Region;


public class MainController extends Controller<MainMessage> {

    private final MainModel model;
    private final MainInteractor interactor;
    private final MainView view;

    public MainController() {
        this.model = new MainModel();
        this.interactor = new MainInteractor(model);
        this.view = new MainView(model, this::action);
        this.interactor.setLoadingController();
    }

    @Override
    public Region getView() {
        return view.build();
    }

    @Override
    public void action(MainMessage msg) {
        switch (msg) {
            case FILE_DROPPED_SUCCESS -> interactor.loadWorkbookFromDroppedFile();

            case LOAD_WORKBOOK_REQUEST -> {

            }
            case PREP_CONV -> interactor.prepConvertToSql();
            case CONVERT_TO_SQL -> interactor.convertToSql();
            case NONE, SEND_MESSAGE -> {
                // no-op / future use
            }
            case CLOSE_APPLICATION -> interactor.closeApplication();
            case UPDATE_OPTIONS -> interactor.updateOptions();
            case BUILD_FINAL_DATABASE -> interactor.buildFinalDatabase();
        }
    }
}
