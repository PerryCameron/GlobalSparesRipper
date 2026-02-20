package com.l2.main;


import interfaces.Controller;
import javafx.scene.layout.Region;


public class MainController extends Controller<MainMessage> {
    private final MainInteractor mainInteractor;
    private final MainView mainView;
    private final MainModel mainModel;

    public MainController(String peerIp) {
        mainModel = new MainModel();
        mainInteractor = new MainInteractor(mainModel, peerIp);
        mainView = new MainView(mainModel, this::action);

        // Auto-start P2P
        mainInteractor.startP2PConnection();
    }

    @Override
    public Region getView() {
        return mainView.build();
    }

    @Override
    public void action(MainMessage actionEnum) {
        if (actionEnum == MainMessage.SEND_MESSAGE) {
            mainInteractor.sendChatMessage();
        }
    }

    public void shutdown() {
        mainInteractor.shutdownP2P();
    }
}
