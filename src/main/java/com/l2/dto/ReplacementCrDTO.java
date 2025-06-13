package com.l2.dto;

public class ReplacementCrDTO {
    private int id;
    private String item;
    private String replacement;
    private String comment;
    private Double oldQty;
    private Double newQty;
    private String lastUpdate;
    private String lastUpdatedBy;

    public ReplacementCrDTO() {
    }

    public ReplacementCrDTO(int id, String item, String replacement, String comment, Double oldQty, Double newQty, String lastUpdate, String lastUpdatedBy) {
        this.id = id;
        this.item = item;
        this.replacement = replacement;
        this.comment = comment;
        this.oldQty = oldQty;
        this.newQty = newQty;
        this.lastUpdate = lastUpdate;
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public void clear() {
        id = 0;
        item = "";
        replacement = "";
        comment = "";
        oldQty = 0.0;
        newQty = 0.0;
        lastUpdate = "";
        lastUpdatedBy = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Double getOldQty() {
        return oldQty;
    }

    public void setOldQty(Double oldQty) {
        this.oldQty = oldQty;
    }

    public Double getNewQty() {
        return newQty;
    }

    public void setNewQty(Double newQty) {
        this.newQty = newQty;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
