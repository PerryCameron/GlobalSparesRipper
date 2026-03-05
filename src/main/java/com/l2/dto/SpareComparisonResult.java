package com.l2.dto;

public class SpareComparisonResult {
    private final int added;
    private final int removed;
    private final int archived;
    private final int unarchived;
    private final int pimChanges;
    private final int replacementItemChanges;
    private final int standardExchangeItemChanges;
    private final int spareDescriptionChanges;
    private final int endOfServiceDateChanges;
    private final int lastUpdateChanges;
    private final int addedToCatalogueChanges;
    private final int removedFromCatalogueChanges;
    private final int commentsChanges;

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
