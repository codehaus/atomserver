package org.atomserver.app;

import org.atomserver.util.ArraySet;

import java.util.Date;
import java.util.Set;
import java.util.Collections;

public class SimpleEntryNode implements EntryNode {
    private final APPCollection collection;
    private final String entryId;

    private boolean deleted;
    private long timestamp;
    private Date lastUpdated;
    private Set<EntryCategory> categories;

    private final Set<AggregateNode> aggregates = new ArraySet<AggregateNode>();

    public SimpleEntryNode(APPCollection collection, String entryId) {
        this.collection = collection;
        this.entryId = entryId;
        this.deleted = false;
    }

    public Set<AggregateNode> getAggregates() {
        return aggregates;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public APPCollection getCollection() {
        return collection;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getEntryId() {
        return entryId;
    }

    public Set<EntryCategory> getCategories() {
        return categories;
    }

    public void update(long timestamp, Date lastUpdated, Set<EntryCategory> categories) {
        this.timestamp = timestamp;
        this.lastUpdated = lastUpdated;
        this.categories = categories == null ? Collections.EMPTY_SET : categories;
    }

    public void delete(long timestamp, Date lastUpdated) {
        update(timestamp, lastUpdated, this.categories);
        this.deleted = true;
    }
}
