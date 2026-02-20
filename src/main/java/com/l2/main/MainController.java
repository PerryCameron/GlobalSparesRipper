package com.l2.main;


import com.l2.interfaces.Controller;
import javafx.scene.layout.Region;


public class MainController extends Controller<MainMessage> {
    private final MainInteractor mainInteractor;
    private final MainView mainView;
    private final MainModel mainModel;

    public MainController() {
        mainModel = new MainModel();
        mainInteractor = new MainInteractor(mainModel);
        mainView = new MainView(mainModel, this::action);
    }

    @Override
    public Region getView() {
        return mainView.build();
    }

    @Override
    public void action(MainMessage actionEnum) {

    }

}
