package com.l2.main;


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
    }

    @Override
    public Region getView() {
        return view.build();
    }

    @Override
    public void action(MainMessage msg) {
        switch (msg) {
            case FILE_DROPPED_SUCCESS -> {
                // File path is already in model → we can load workbook now
                // or wait for explicit user confirmation
                // Here we load automatically for simplicity
                interactor.loadWorkbookFromDroppedFile();
            }

            case LOAD_WORKBOOK_REQUEST -> {
                // This is called when user clicks "Rip into database"
                // For now we just reload (in real app → start DB import)
                interactor.loadWorkbookFromDroppedFile();
            }

            case NONE, SEND_MESSAGE -> {
                // no-op / future use
            }
        }
    }
}
