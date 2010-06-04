package org.atomserver.app;

import java.util.Date;
import java.util.Set;

/**
 * Represents the metadata for a single entry in AtomServer.
 */
public interface EntryNode {
    /**
     * getter for entryId.
     *
     * @return entryId
     */
    String getEntryId();

    /**
     * getter for categories.
     *
     * @return categories
     */
    Set<EntryCategory> getCategories();

    /**
     * getter for timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * getter for lastUpdated.
     *
     * @return lastUpdated
     */
    Date getLastUpdated();

    /**
     * getter for ETag.
     *
     * @return the etag for the entry content
     */
    String getEtag();

    /**
     * update this EntryNode to have the given timestamp, lastUpdated, and categories.
     *
     * @param timestamp   the new timestamp value
     * @param lastUpdated the new lastUpdated value
     * @param categories  the new vategories value
     * @param etag        the etag for the entry content
     */
    void update(long timestamp, Date lastUpdated, Set<EntryCategory> categories, String etag);
}
