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
    private Date published;
    private Date lastUpdated;
    private String etag;
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

    public Date getPublished() {
        return published;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getEtag() {
        return etag;
    }

    public Set<EntryCategory> getCategories() {
        return categories;
    }

    public void update(long timestamp,
                       Date lastUpdated,
                       Set<EntryCategory> categories,
                       String etag) {
        this.timestamp = timestamp;
        this.lastUpdated = lastUpdated;
        this.etag = etag;
        if (published == null) {
            // the first time update() is called, we set the published date - that way, we can
            // detect a newly created entry by comparing published and lastUpdated for equality
            published = lastUpdated;
        }
        this.categories = categories == null ? Collections.EMPTY_SET : categories;
    }

    public void delete(long timestamp, Date lastUpdated) {
        update(timestamp, lastUpdated, this.categories, null);
        this.deleted = true;
    }
}
