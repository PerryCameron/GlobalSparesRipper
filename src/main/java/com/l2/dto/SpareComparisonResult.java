package com.l2.dto;

public class SpareComparisonResult {
    private int added;
    private int removed;
    private int archived;
    private int unarchived;
    private int pimChanges;
    private int replacementItemChanges;
    private int standardExchangeItemChanges;
    private int spareDescriptionChanges;
    private int endOfServiceDateChanges;
    private int lastUpdateChanges;
    private int addedToCatalogueChanges;
    private int removedFromCatalogueChanges;
    private int commentsChanges;
    private int photosCopied;
    private int photosSkipped;
    private int customSparesAdded;
    private int customNotesAdded;
    private int updateByAdded;


    public SpareComparisonResult(int added, int removed, int archived, int unarchived, int pimChanges, int replacementItemChanges, int standardExchangeItemChanges, int spareDescriptionChanges, int endOfServiceDateChanges, int lastUpdateChanges, int addedToCatalogueChanges, int removedFromCatalogueChanges, int commentsChanges) {
        this.added = added;
        this.removed = removed;
        this.archived = archived;
        this.unarchived = unarchived;
        this.pimChanges = pimChanges;
        this.replacementItemChanges = replacementItemChanges;
        this.standardExchangeItemChanges = standardExchangeItemChanges;
        this.spareDescriptionChanges = spareDescriptionChanges;
        this.endOfServiceDateChanges = endOfServiceDateChanges;
        this.lastUpdateChanges = lastUpdateChanges;
        this.addedToCatalogueChanges = addedToCatalogueChanges;
        this.removedFromCatalogueChanges = removedFromCatalogueChanges;
        this.commentsChanges = commentsChanges;
    }

    public SpareComparisonResult() {
    }

    public void setValues(int added, int removed, int archived, int unarchived, int pimChanges, int replacementItemChanges, int standardExchangeItemChanges, int spareDescriptionChanges, int endOfServiceDateChanges, int lastUpdateChanges, int addedToCatalogueChanges, int removedFromCatalogueChanges, int commentsChanges) {
        this.added = added;
        this.removed = removed;
        this.archived = archived;
        this.unarchived = unarchived;
        this.pimChanges = pimChanges;
        this.replacementItemChanges = replacementItemChanges;
        this.standardExchangeItemChanges = standardExchangeItemChanges;
        this.spareDescriptionChanges = spareDescriptionChanges;
        this.endOfServiceDateChanges = endOfServiceDateChanges;
        this.lastUpdateChanges = lastUpdateChanges;
        this.addedToCatalogueChanges = addedToCatalogueChanges;
        this.removedFromCatalogueChanges = removedFromCatalogueChanges;
        this.commentsChanges = commentsChanges;
    }

    public int getAdded() {
        return added;
    }

    public int getRemoved() {
        return removed;
    }

    public int getArchived() {
        return archived;
    }

    public int getUnarchived() {
        return unarchived;
    }

    public int getPimChanges() {
        return pimChanges;
    }

    public int getReplacementItemChanges() {
        return replacementItemChanges;
    }

    public int getStandardExchangeItemChanges() {
        return standardExchangeItemChanges;
    }

    public int getSpareDescriptionChanges() {
        return spareDescriptionChanges;
    }

    public int getEndOfServiceDateChanges() {
        return endOfServiceDateChanges;
    }

    public int getLastUpdateChanges() {
        return lastUpdateChanges;
    }

    public int getAddedToCatalogueChanges() {
        return addedToCatalogueChanges;
    }

    public int getRemovedFromCatalogueChanges() {
        return removedFromCatalogueChanges;
    }

    public int getCommentsChanges() {
        return commentsChanges;
    }

    public int getPhotosCopied() {
        return photosCopied;
    }

    public void setPhotosCopied(int photosCopied) {
        this.photosCopied = photosCopied;
    }

    public int getPhotosSkipped() {
        return photosSkipped;
    }

    public void setPhotosSkipped(int photosSkipped) {
        this.photosSkipped = photosSkipped;
    }

    public int getCustomSparesAdded() {
        return customSparesAdded;
    }

    public void setCustomSparesAdded(int customSparesAdded) {
        this.customSparesAdded = customSparesAdded;
    }

    public int getCustomNotesAdded() {
        return customNotesAdded;
    }

    public void setCustomNotesAdded(int customNotesAdded) {
        this.customNotesAdded = customNotesAdded;
    }

    public int getUpdateByAdded() {
        return updateByAdded;
    }

    public void setUpdateByAdded(int updateByAdded) {
        this.updateByAdded = updateByAdded;
    }

    @Override
    public String toString() {
        return "SpareComparisonResult{" +
                "added=" + added +
                ", removed=" + removed +
                ", archived=" + archived +
                ", unarchived=" + unarchived +
                ", pimChanges=" + pimChanges +
                ", replacementItemChanges=" + replacementItemChanges +
                ", standardExchangeItemChanges=" + standardExchangeItemChanges +
                ", spareDescriptionChanges=" + spareDescriptionChanges +
                ", endOfServiceDateChanges=" + endOfServiceDateChanges +
                ", lastUpdateChanges=" + lastUpdateChanges +
                ", addedToCatalogueChanges=" + addedToCatalogueChanges +
                ", removedFromCatalogueChanges=" + removedFromCatalogueChanges +
                ", commentsChanges=" + commentsChanges +
                '}';
    }
}
