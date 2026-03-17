package com.l2.pojo;

public class DataBaseOptions {


    private boolean includeCustomParts;
    private boolean include3PhaseRanges;
    private boolean includeCoolingRanges;
    private boolean includeCustomNotes;
    private boolean includePhotos;

    public DataBaseOptions(boolean includeCustomParts, boolean include3PhaseRanges, boolean includeCoolingRanges, boolean includeCustomNotes, boolean includePhotos) {
        this.includeCustomParts = includeCustomParts;
        this.include3PhaseRanges = include3PhaseRanges;
        this.includeCoolingRanges = includeCoolingRanges;
        this.includeCustomNotes = includeCustomNotes;
        this.includePhotos = includePhotos;
    }

    public boolean isIncludeCustomParts() {
        return includeCustomParts;
    }

    public void setIncludeCustomParts(boolean includeCustomParts) {
        this.includeCustomParts = includeCustomParts;
    }

    public boolean isInclude3PhaseRanges() {
        return include3PhaseRanges;
    }

    public void setInclude3PhaseRanges(boolean include3PhaseRanges) {
        this.include3PhaseRanges = include3PhaseRanges;
    }

    public boolean isIncludeCoolingRanges() {
        return includeCoolingRanges;
    }

    public void setIncludeCoolingRanges(boolean includeCoolingRanges) {
        this.includeCoolingRanges = includeCoolingRanges;
    }

    public boolean isIncludeCustomNotes() {
        return includeCustomNotes;
    }

    public void setIncludeCustomNotes(boolean includeCustomNotes) {
        this.includeCustomNotes = includeCustomNotes;
    }

    public boolean isIncludePhotos() {
        return includePhotos;
    }

    public void setIncludePhotos(boolean includePhotos) {
        this.includePhotos = includePhotos;
    }
}
