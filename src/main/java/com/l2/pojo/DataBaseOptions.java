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

    public boolean includesCustomParts() {
        return includeCustomParts;
    }

    public boolean includes3PhaseRanges() {
        return include3PhaseRanges;
    }

    public boolean includeaCoolingRanges() {
        return includeCoolingRanges;
    }

    public boolean includesCustomNotes() {
        return includeCustomNotes;
    }

    public boolean includesPhotos() {
        return includePhotos;
    }


    @Override
    public String toString() {
        return "DataBaseOptions{" +
                "includeCustomParts=" + includeCustomParts +
                ", include3PhaseRanges=" + include3PhaseRanges +
                ", includeCoolingRanges=" + includeCoolingRanges +
                ", includeCustomNotes=" + includeCustomNotes +
                ", includePhotos=" + includePhotos +
                '}';
    }
}
