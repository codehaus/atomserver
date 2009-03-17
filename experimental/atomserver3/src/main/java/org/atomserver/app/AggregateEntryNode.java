package org.atomserver.app;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

public class AggregateEntryNode implements EntryNode {
    private final String entryId;
    private final Set<SimpleEntryNode> members = new HashSet<SimpleEntryNode>();
    private long timestamp;
    private Date lastUpdated;
    private Set<EntryCategory> categories;

    public AggregateEntryNode(String entryId) {
        this.entryId = entryId;
    }

    public Set<SimpleEntryNode> getMembers() {
        return members;
    }

    public String getEntryId() {
        return this.entryId;
    }

    public Set<EntryCategory> getCategories() {
        return categories;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public Date getLastUpdated() {
        return this.lastUpdated;
    }

    public void update(long timestamp, Date lastUpdated, Set<EntryCategory> categories) {
        this.timestamp = timestamp;
        this.lastUpdated = lastUpdated;
        this.categories = categories == null ? Collections.EMPTY_SET : categories;
    }
}
