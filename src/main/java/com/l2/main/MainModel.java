package com.l2.main;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MainModel {
    private final StringProperty chatLog = new SimpleStringProperty("");
    private final StringProperty inputMessage = new SimpleStringProperty("");

    public StringProperty chatLogProperty() { return chatLog; }
    public void appendChatLog(String line) {
        String current = chatLog.get() == null ? "" : chatLog.get();
        chatLog.set(current + line + "\n");
    }

    public StringProperty inputMessageProperty() { return inputMessage; }
    public String getInputMessage() { return inputMessage.get(); }
    public void setInputMessage(String value) { inputMessage.set(value); }
}