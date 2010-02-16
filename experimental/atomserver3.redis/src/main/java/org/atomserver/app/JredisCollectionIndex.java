package org.atomserver.app;

import org.atomserver.categories.CategoryQuery;

import java.util.Iterator;

public class JredisCollectionIndex<E extends EntryNode> implements CollectionIndex<E> {


    public E getEntry(String entryId) {
        return null;
    }

    public E removeEntryNodeFromIndices(String entryId) {
        return null;
    }

    public void updateEntryNodeIntoIndices(E entryNode) {
    }

    public Iterator<E> buildIterator(CategoryQuery categoryQuery, long timestamp) {
        return null;
    }
}
