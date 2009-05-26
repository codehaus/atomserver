package org.atomserver.app;

import org.apache.commons.codec.digest.DigestUtils;

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

    public String getEtag() {
        // TODO: make sure that this is the way we want to handle aggregate ETags
        StringBuffer concatenatedEntryEtags = new StringBuffer();
        for (SimpleEntryNode entryNode : members) {
            concatenatedEntryEtags.append(entryNode.getEtag());
        }
        return DigestUtils.md5Hex(concatenatedEntryEtags.toString());
    }

    // TODO: refactor this API - the etag shouldn't come in here.
    public void update(long timestamp, Date lastUpdated, Set<EntryCategory> categories, String etag) {
        this.timestamp = timestamp;
        this.lastUpdated = lastUpdated;
        this.categories = categories == null ? Collections.EMPTY_SET : categories;
    }
}
