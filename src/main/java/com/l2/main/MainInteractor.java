package com.l2.main;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import javafx.application.Platform;

public class MainInteractor {
    private final MainModel mainModel;
    private final String peerIp;

    private HazelcastInstance hazelcastInstance;
    private ITopic<String> topic;

    public MainInteractor(MainModel mainModel, String peerIp) {
        this.mainModel = mainModel;
        this.peerIp = peerIp;
    }

    public void startP2PConnection() {
        new Thread(this::initHazelcast, "Hazelcast-Init").start();
    }

    private void initHazelcast() {
        try {
            Config config = new Config();
            config.setClusterName("VPNPartsP2P");

            JoinConfig joinConfig = config.getNetworkConfig().getJoin();
            joinConfig.getMulticastConfig().setEnabled(false);

            TcpIpConfig tcpIp = joinConfig.getTcpIpConfig();
            tcpIp.setEnabled(true);
            tcpIp.addMember(peerIp);
            tcpIp.setConnectionTimeoutSeconds(10);

            hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            topic = hazelcastInstance.getTopic("chatMessages");

            topic.addMessageListener(new MessageListener<String>() {
                @Override
                public void onMessage(Message<String> message) {
                    String from = (message.getPublishingMember() != null)
                            ? message.getPublishingMember().getAddress().getHost()
                            : "Unknown";
                    Platform.runLater(() ->
                            mainModel.appendChatLog("[" + from + "] " + message.getMessageObject())
                    );
                }
            });

            Platform.runLater(() ->
                    mainModel.appendChatLog("✅ Connected to peer: " + peerIp)
            );

        } catch (Exception e) {
            Platform.runLater(() ->
                    mainModel.appendChatLog("❌ P2P Error: " + e.getMessage())
            );
            e.printStackTrace();
        }
    }

    public void sendChatMessage() {
        String text = mainModel.getInputMessage().trim();
        if (text.isEmpty() || topic == null) return;

        topic.publish(text);
        mainModel.appendChatLog("[You] " + text);
        mainModel.setInputMessage("");
    }

    public void shutdownP2P() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }
}