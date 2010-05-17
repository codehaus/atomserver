package org.atomserver.app;

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.atomserver.AtomServerConstants;
import org.atomserver.categories.CategoryQuery;
import org.atomserver.content.*;
import org.atomserver.core.CategoryTuple;
import org.atomserver.core.EntryTuple;
import org.atomserver.core.Substrate;
import org.atomserver.util.IntersectionIterator;
import org.atomserver.util.SubtractIterator;
import org.atomserver.util.UnionIterator;

import javax.ws.rs.WebApplicationException;
import java.util.*;
import java.util.concurrent.Callable;

import static java.lang.String.format;

public class CollectionOperations {

    private static final Logger log = Logger.getLogger(CollectionOperations.class);

    public Entry updateEntry(final String entryId, final String etag, final Entry entry, final boolean delete) {

        log.debug(String.format("PUTting entry %s", entryId));

        // putting an entry consists of the following steps:
        // - validate the entry - make sure that we *think* we are going to be able to write
        // - do "phase 1" of the two-phase-commit for putting the content into the store
        // - acquire the write lock on the service meta-data
        //   - write the meta-data to the service
        //   - do "phase 2" of the two-phase-commit for the content
        // - release the write lock.

        // TODO: validate the <id> element (e.g. against the path)


        final ContentStore.Transaction contentTxn;
        try {
            // TODO: not all content is strings, some is links and some is base64-ed
            contentTxn = contentStore.put(getEntryKey(entryId),
                    "text/plain",
                    ContentUtils.toChannel(entry.getContent()));
        } catch (ContentStoreException e) {
            // TODO: handle for real
            throw new WebApplicationException(e);
        }

        final Entry newEntry = atompubFactory.newEntry();
        newEntry.setId(String.format("%s/%s", collectionKey, entryId));

        return sync(new Callable<Entry>() {
            public Entry call() throws Exception {
                try {
                    EntryTuple entryTuple = removeEntryNodeFromIndices(entryId);
                    if (entryTuple == null) {
                        if (etag != null &&
                                !AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE.equals(etag)) {
                            throw new OptimisticConcurrencyException(
                                    format("Optimistic Concurrency Exception - ETag (%s) provided, " +
                                            "but this is a new Entry",
                                            etag));
                        }
                        if (delete) {
                            throw new NotFoundException("could not delete entry - does not exist.");
                        }
                        long now = new Date().getTime();
                        entryTuple = new EntryTuple(
                                entryId, getNextTimestamp(), now, now, contentTxn.digest(),
                                convertCategories(entry.getCategories()));
                    } else {
                        if (!AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE.equals(etag) &&
                                !(entryTuple.digest == null ?
                                        etag == null :
                                        toHexString(entryTuple.digest).equals(etag))) {

                            throw new OptimisticConcurrencyException(
                                    format("Optimistic Concurrency Exception - provided ETag (%s) does " +
                                            "not match previous ETag value of (%s)",
                                            etag, toHexString(entryTuple.digest)));
                        }

                        entryTuple = entryTuple.update(getNextTimestamp(),
                                new Date().getTime(),
                                contentTxn.digest(),
                                convertCategories(entry.getCategories()),
                                delete);
                    }
                    updateEntryNodeIntoIndices(entryTuple);

                    Date updated = new Date(entryTuple.updated);
                    newEntry.setEdited(updated);
                    newEntry.setUpdated(updated);
                    newEntry.setPublished(new Date(entryTuple.created));
                    newEntry.setContent(entry.getContent(), entry.getContentType()); // TODO: deal with content...
                    newEntry.addSimpleExtension(AtomServerConstants.ENTRY_ID, entryId);
                    for (Category category : entry.getCategories()) {
                        newEntry.addCategory(category);
                    }
                    // TODO: should ETags hash the content only, or the metadata of an entry, too?
                    newEntry.addSimpleExtension(AtomServerConstants.ETAG, toHexString(entryTuple.digest));
                    // TODO: we need to preserve "unknown" extensions

                    contentTxn.commit();

                    log.debug(String.format("successfully PUT entry %s", entryId));
                    return newEntry;
                } catch (WebApplicationException e) {
                    contentTxn.abort();
                    throw e;
                } catch (AtompubException e) {
                    contentTxn.abort();
                    throw e;
                } catch (Exception e) {
                    contentTxn.abort();
                    throw new WebApplicationException(e); // TODO: more specific
                }
            }
        });
    }

    private Entry sync(Callable<Entry> callable) {
        try {
            return substrate.sync(collectionKey, callable);
        } catch (WebApplicationException e) {
            throw e; // TODO: handle
        } catch (AtompubException e) {
            throw e; // TODO: handle
        } catch (Exception e) {
            throw new IllegalStateException(e); // TODO: handle
        }
    }

    Entry getEntry(String entryId) {
        final EntryTuple entryTuple = entries.get(entryId);
        return entryTuple == null ? null : convertToEntry(entryTuple);
    }

    Feed getFeed(long timestamp, int maxResults, CategoryQuery categoryQuery) {
        Feed feed = atompubFactory.newFeed(collectionKey, collectionKey, collectionKey);

        if (categoryQuery != null) {
            feed.addSimpleExtension(AtomServerConstants.CATEGORY_QUERY, categoryQuery.toString());
        }

        Iterator<EntryTuple> entryIterator;
        entryIterator = buildIterator(categoryQuery, timestamp);
        int countdown = maxResults;
        long endIndex = timestamp; // TODO: scroll to the end when feed is empty
        StringBuffer entryEtagsConcatenated = new StringBuffer();
        while (entryIterator.hasNext() && countdown-- > 0) {
            EntryTuple entryNode = entryIterator.next();
            Entry entry = convertToEntry(entryNode);
            feed.addEntry(entry);
            endIndex = entryNode.timestamp;
            entryEtagsConcatenated.append(toHexString(entryNode.digest));
        }
        feed.addSimpleExtension(AtomServerConstants.END_INDEX, String.valueOf(endIndex));
        feed.addSimpleExtension(AtomServerConstants.ETAG,
                DigestUtils.md5Hex(entryEtagsConcatenated.toString()));

        return feed;
    }

    private final String serviceId;
    private final String workspaceId;
    private final String collectionId;
    private final String collectionKey;
    private final Substrate substrate;
    private final ContentStore contentStore;
    private final AtompubFactory atompubFactory;

    private final Substrate.KeyValueStore<Long, EntryTuple> entriesByTimestamp;
    private final Substrate.KeyValueStore<String, EntryTuple> entries;
    private final Substrate.Index entryIndex;

    public CollectionOperations(String serviceId,
                                String workspaceId,
                                String collectionId,
                                Substrate substrate,
                                ContentStore contentStore,
                                AtompubFactory atompubFactory) {
        this.serviceId = serviceId;
        this.workspaceId = workspaceId;
        this.collectionId = collectionId;
        this.collectionKey = String.format("%s/%s/%s", serviceId, workspaceId, collectionId);
        this.substrate = substrate;
        this.contentStore = contentStore;
        this.atompubFactory = atompubFactory;
        this.entries = substrate.getEntriesById(collectionKey);
        this.entriesByTimestamp = substrate.getEntriesByTimestamp(collectionKey);
        this.entryIndex = substrate.getIndex(collectionKey);
    }

    private Long getNextTimestamp() {
        return substrate.getNextTimestamp(collectionKey);
    }

    private Substrate.Index categoryIndex(CategoryTuple category) {
        return substrate.getIndex(
                String.format("%s::(%s:%s)", collectionKey, category.scheme, category.term));
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
                    return categoryIndex == null ? Collections.<Long>emptySet().iterator() :
                            categoryIndex.tail(timestamp).iterator();
                case AND:
                    CategoryQuery.AndQuery andQuery = (CategoryQuery.AndQuery) categoryQuery;
                    return new IntersectionIterator<Long>(
                            buildTimestampIterator(andQuery.left, timestamp),
                            buildTimestampIterator(andQuery.right, timestamp));
                case OR:
                    CategoryQuery.OrQuery orQuery = (CategoryQuery.OrQuery) categoryQuery;
                    return new UnionIterator<Long>(
                            buildTimestampIterator(orQuery.left, timestamp),
                            buildTimestampIterator(orQuery.right, timestamp));
                case NOT:
                    CategoryQuery.NotQuery notQuery = (CategoryQuery.NotQuery) categoryQuery;
                    return new SubtractIterator<Long>(
                            entryIndex.tail(timestamp).iterator(),
                            buildTimestampIterator(notQuery.inner, timestamp));
                default:
                    throw new IllegalStateException(
                            String.format("invalid category query %s", categoryQuery));
            }
        }
    }


    //    -------utility methods

    private Set<CategoryTuple> convertCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptySet();
        }
        Set<CategoryTuple> entryCategories = new HashSet<CategoryTuple>();
        for (Category category : categories) {
            entryCategories.add(new CategoryTuple(
                    category.getScheme().toString(),
                    category.getTerm(),
                    category.getLabel()));
        }
        return entryCategories;
    }

    protected Entry convertToEntry(EntryTuple entryTuple) {

        Entry entry = atompubFactory.newEntry();

        String id = String.format("%s/%s", collectionKey, entryTuple.entryId);
        entry.setId(id);
        entry.setTitle(entryTuple.entryId);
        Date updated = new Date(entryTuple.updated);
        entry.setEdited(updated);
        entry.setUpdated(updated);
        entry.setPublished(new Date(entryTuple.created));
        EntryKey key = getEntryKey(entryTuple.entryId);
        try {
            EntryContent entryContent = contentStore.get(key);
            // TODO: not all content is strings, some is links and some is base64-ed
            entry.setContent(ContentUtils.toString(entryContent.getChannel()), Content.Type.XML);
        } catch (ContentStoreException e) {
            throw new WebApplicationException(e);
        }
        for (CategoryTuple entryCategory : entryTuple.categories) {
            Category category = atompubFactory.newCategory();
            category.setScheme(entryCategory.scheme);
            category.setTerm(entryCategory.term);
            category.setLabel(entryCategory.label);
            entry.addCategory(category);
        }
        entry.addSimpleExtension(AtomServerConstants.TIMESTAMP, String.valueOf(entryTuple.timestamp));
        entry.addSimpleExtension(AtomServerConstants.ETAG, toHexString(entryTuple.digest));
        return entry;
    }

    private EntryKey getEntryKey(String entryId) {return new EntryKey(serviceId, workspaceId, collectionId, entryId);}

    public static String toHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }

}
