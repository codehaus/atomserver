package org.atomserver.app;

import org.atomserver.categories.CategoryQuery;
import org.atomserver.util.IntersectionIterator;
import org.atomserver.util.SubtractIterator;
import org.atomserver.util.UnionIterator;

import java.util.*;

public abstract class TreeSetCollectionIndex<E extends EntryNode> implements CollectionIndex<E> {

    protected abstract E newEntryNode(String entryId);

    private final Map<Long, E> entriesByTimestamp = new HashMap<Long, E>();
    private final Map<String, E> entries = new HashMap<String, E>();
    private final SortedSet<Long> entryIndex = new TreeSet<Long>();
    private final Map<CategoryNode, SortedSet<Long>> categoryIndices =
            new HashMap<CategoryNode, SortedSet<Long>>();

    public E getEntry(String entryId) {
        return this.entries.get(entryId);
    }

    public E removeEntryNodeFromIndices(String entryId) {
        E entryNode = entries.get(entryId);
        if (entryNode != null) {
            for (EntryCategory entryCategory : entryNode.getCategories()) {
                categoryIndices.get(entryCategory.getCategory()).remove(entryNode.getTimestamp());
            }
            entryIndex.remove(entryNode.getTimestamp());
            entriesByTimestamp.remove(entryNode.getTimestamp());
        } else {
            entries.put(entryId, entryNode = newEntryNode(entryId));
        }
        return entryNode;
    }


    public void updateEntryNodeIntoIndices(E entryNode) {
        for (EntryCategory entryCategory : entryNode.getCategories()) {
            SortedSet<Long> categoryIndex = categoryIndices.get(entryCategory.getCategory());
            if (categoryIndex == null) {
                categoryIndices.put(entryCategory.getCategory(),
                                    categoryIndex = new TreeSet<Long>());
            }
            categoryIndex.add(entryNode.getTimestamp());
        }
        entryIndex.add(entryNode.getTimestamp());
        entriesByTimestamp.put(entryNode.getTimestamp(), entryNode);
    }

    public Iterator<E> buildIterator(final CategoryQuery categoryQuery, final long timestamp) {
        return new Iterator<E>() {
            Iterator<Long> timestampIterator = buildTimestampIterator(categoryQuery, timestamp);

            public boolean hasNext() {
                return timestampIterator.hasNext();
            }

            public E next() {
                return entriesByTimestamp.get(timestampIterator.next());
            }

            public void remove() {
                throw new UnsupportedOperationException("iterator is read-only");
            }
        };
    }

    private Iterator<Long> buildTimestampIterator(CategoryQuery categoryQuery, long timestamp) {
        if (categoryQuery == null) {
            return entryIndex.tailSet(timestamp).iterator();
        } else {
            switch (categoryQuery.type) {
            case SIMPLE:
                CategoryQuery.SimpleCategoryQuery simpleCategoryQuery =
                        (CategoryQuery.SimpleCategoryQuery) categoryQuery;
                SortedSet<Long> categoryIndex = categoryIndices.get(
                        CategoryNode.category(simpleCategoryQuery.scheme, simpleCategoryQuery.term));
                return categoryIndex == null ? Collections.EMPTY_SET.iterator() :
                       categoryIndex.tailSet(timestamp).iterator();
            case AND:
                CategoryQuery.AndQuery andQuery = (CategoryQuery.AndQuery) categoryQuery;
                return new IntersectionIterator(buildTimestampIterator(andQuery.left, timestamp),
                                                buildTimestampIterator(andQuery.right, timestamp));
            case OR:
                CategoryQuery.OrQuery orQuery = (CategoryQuery.OrQuery) categoryQuery;
                return new UnionIterator(buildTimestampIterator(orQuery.left, timestamp),
                                         buildTimestampIterator(orQuery.right, timestamp));
            case NOT:
                CategoryQuery.NotQuery notQuery = (CategoryQuery.NotQuery) categoryQuery;
                return new SubtractIterator(entryIndex.tailSet(timestamp).iterator(),
                                            buildTimestampIterator(notQuery.inner, timestamp));
            default:
                throw new IllegalStateException(
                        String.format("invalid category query %s", categoryQuery));
            }
        }
    }
}
