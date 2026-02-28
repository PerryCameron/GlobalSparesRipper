package com.l2.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TaskItemDTO {
    private final StringProperty taskName = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    public TaskItemDTO(String name) {
        taskName.set(name);
    }

    public StringProperty taskNameProperty()  { return taskName; }
    public BooleanProperty completedProperty() { return completed; }
    public String getTaskName()   { return taskName.get(); }
    public boolean isCompleted()  { return completed.get(); }
    public void setCompleted(boolean v) { completed.set(v); }
}
