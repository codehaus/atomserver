package org.atomserver.app;

import org.atomserver.categories.CategoryQuery;

import java.util.Iterator;

/**
 * Indexes a collection by timestamp, including indices for each Category used in the collection.
 *
 * @param <E> the implementation of EntryNode that this Collection indexes.
 */
public interface CollectionIndex<E extends EntryNode> {
    /**
     * retrieve the EntryNode instance for the given entryId.
     *
     * @param entryId the entryId to retrieve
     * @return the EntryNode for the given entryId, or null if none.
     */
    E getEntry(String entryId);

    /**
     * remove the EntryNode with the given entryId from all of the indices for this collection.
     *
     * @param entryId the entryId to remove
     * @return the EntryNode removed from the indices (this is a NEW entry if it wasn't already in the indices)
     */
    E removeEntryNodeFromIndices(String entryId);

    /**
     * update the given EntryNode into all of the indices for this collection.
     *
     * @param entryNode the EntryNode to update into the indices for this collection
     */
    void updateEntryNodeIntoIndices(E entryNode);

    /**
     * build an iterator, starting just after the given timestamp, of entries that match the given CategoryQuery.
     *
     * @param categoryQuery the CategoryQuery by which to filter entries (null returns all entries)
     * @param timestamp     the timestamp just before the beginning of the requested iterator
     * @return an iterator of EntryNodes that match the given CategoryQuery, starting after the given timestamp
     */
    Iterator<E> buildIterator(CategoryQuery categoryQuery, long timestamp);
}
