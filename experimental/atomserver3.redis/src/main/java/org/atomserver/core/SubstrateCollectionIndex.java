package org.atomserver.core;

import org.atomserver.categories.CategoryQuery;
import org.atomserver.util.IntersectionIterator;
import org.atomserver.util.SubtractIterator;
import org.atomserver.util.UnionIterator;

import java.util.Collections;
import java.util.Iterator;

public class SubstrateCollectionIndex {

    private final String collectionKey;
    private final Substrate substrate;

    private final Substrate.KeyValueStore<Long, EntryTuple> entriesByTimestamp;
    private final Substrate.KeyValueStore<String, EntryTuple> entries;
    private final Substrate.Index entryIndex;

    public SubstrateCollectionIndex(String collectionKey, Substrate substrate) {
        this.collectionKey = collectionKey;
        this.substrate = substrate;
        this.entries = substrate.getEntriesById(collectionKey);
        this.entriesByTimestamp = substrate.getEntriesByTimestamp(collectionKey);
        this.entryIndex = substrate.getIndex(collectionKey);
    }

    private Substrate.Index categoryIndex(CategoryTuple category) {
        return substrate.getIndex(
                String.format("%s::(%s:%s)", collectionKey, category.scheme, category.term));
    }


    public EntryTuple getEntry(String entryId) {
        return this.entries.get(entryId);
    }

    public EntryTuple removeEntryNodeFromIndices(String entryId) {
        EntryTuple entryTuple = entries.get(entryId);
        if (entryTuple != null) {
            for (CategoryTuple entryCategory : entryTuple.categories) {
                categoryIndex(entryCategory).remove(entryTuple.timestamp);
            }
            entryIndex.remove(entryTuple.timestamp);
            entriesByTimestamp.remove(entryTuple.timestamp);
        }
        return entryTuple;
    }


    public void updateEntryNodeIntoIndices(EntryTuple entryTuple) {
        entries.put(entryTuple.entryId, entryTuple);
        for (CategoryTuple entryCategory : entryTuple.categories) {
            Substrate.Index categoryIndex = categoryIndex(entryCategory);
            categoryIndex.add(entryTuple.timestamp);
        }
        entryIndex.add(entryTuple.timestamp);
        entriesByTimestamp.put(entryTuple.timestamp, entryTuple);
    }

    public Iterator<EntryTuple> buildIterator(final CategoryQuery categoryQuery, final long timestamp) {
        return new Iterator<EntryTuple>() {
            Iterator<Long> timestampIterator = buildTimestampIterator(categoryQuery, timestamp + 1);

            public boolean hasNext() {
                return timestampIterator.hasNext();
            }

            public EntryTuple next() {
                return entriesByTimestamp.get(timestampIterator.next());
            }

            public void remove() {
                throw new UnsupportedOperationException("iterator is read-only");
            }
        };
    }

    private Iterator<Long> buildTimestampIterator(CategoryQuery categoryQuery, long timestamp) {
        if (categoryQuery == null) {
            return entryIndex.tail(timestamp).iterator();
        } else {
            switch (categoryQuery.type) {
            case SIMPLE:
                CategoryQuery.SimpleCategoryQuery simpleCategoryQuery =
                        (CategoryQuery.SimpleCategoryQuery) categoryQuery;
                Substrate.Index categoryIndex = categoryIndex(
                        new CategoryTuple(simpleCategoryQuery.scheme, simpleCategoryQuery.term, null));
                return categoryIndex == null ? Collections.EMPTY_SET.iterator() :
                       categoryIndex.tail(timestamp).iterator();
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
                return new SubtractIterator(entryIndex.tail(timestamp).iterator(),
                                            buildTimestampIterator(notQuery.inner, timestamp));
            default:
                throw new IllegalStateException(
                        String.format("invalid category query %s", categoryQuery));
            }
        }
    }
}
