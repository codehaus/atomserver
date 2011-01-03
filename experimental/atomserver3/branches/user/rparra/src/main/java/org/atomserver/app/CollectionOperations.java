package org.atomserver.app;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.atomserver.AtomServerConstants;
import org.atomserver.categories.CategoryQuery;
import org.atomserver.content.*;
import org.atomserver.core.CategoryTuple;
import org.atomserver.core.EntryTuple;
import org.atomserver.core.Substrate;
import org.atomserver.util.HexUtil;
import org.atomserver.util.IntersectionIterator;
import org.atomserver.util.SubtractIterator;
import org.atomserver.util.UnionIterator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Callable;

import static java.lang.String.format;

public class CollectionOperations {

    private static final Logger log = Logger.getLogger(CollectionOperations.class);

    public Entry updateEntry(final String entryId, final String etag, final Entry entry) {

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
            contentTxn =
                    entry.getContentSrc() != null ?
                            new OutOfLineContentTransaction(entry.getContentSrc()) :
                            contentStore.put(getEntryKey(entryId), ContentUtils.toChannel(entry.getContent()));
        } catch (ContentStoreException e) {
            final String message = "error writing content to the content store";
            log.error(message, e);
            throw new AtompubServerException(message);
        }

        final Entry newEntry = atompubFactory.newEntry();
        newEntry.setId(String.format("%s/%s", collectionKey, entryId));

        try {
            return sync(new Callable<Entry>() {
                public Entry call() throws Exception {
                    EntryTuple entryTuple = removeEntryNodeFromIndices(entryId);
                    if (entryTuple == null) {
                        if (etag != null &&
                                !AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE.equals(etag)) {
                            throw new OptimisticConcurrencyException(
                                    format("Optimistic Concurrency Exception - ETag (%s) provided, " +
                                            "but this is a new Entry",
                                            etag));
                        }
                        long now = new Date().getTime();
                        String contentType =
                                entry.getContentMimeType() != null ? entry.getContentMimeType().toString() :
                                        entry.getContentType() != null ? entry.getContentType().toString() :
                                                "application/xml"; // TODO: is this a reasonable default?

                        entryTuple = new EntryTuple(
                                entryId,
                                getNextTimestamp(),
                                now,
                                now,
                                contentTxn.digest(),
                                entry.getContentSrc() == null ? null : entry.getContentSrc().toString(),
                                contentType,
                                convertCategories(entry.getCategories()));
                    } else {
                        if (!AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE.equals(etag) &&
                                !(entryTuple.digest == null ?
                                        etag == null :
                                        HexUtil.toHexString(entryTuple.digest).equals(etag))) {

                            throw new OptimisticConcurrencyException(
                                    format("Optimistic Concurrency Exception - provided ETag (%s) does " +
                                            "not match previous ETag value of (%s)",
                                            etag, HexUtil.toHexString(entryTuple.digest)));
                        }

                        entryTuple = entryTuple.update(getNextTimestamp(),
                                new Date().getTime(),
                                contentTxn.digest(),
                                entry.getContentSrc() == null ? null : entry.getContentSrc().toString(),
                                entry.getContentMimeType().toString(),
                                convertCategories(entry.getCategories()),
                                false);
                    }
                    updateEntryNodeIntoIndices(entryTuple);

                    Date updated = new Date(entryTuple.updated);
                    newEntry.setEdited(updated);
                    newEntry.setUpdated(updated);
                    newEntry.setPublished(new Date(entryTuple.created));
                    if (entry.getContentSrc() != null) {
                        newEntry.setContent(entry.getContentSrc(), entry.getContentMimeType().toString());
                    } else {
                        newEntry.setContent(entry.getContent(), entry.getContentType());
                    }
                    newEntry.addSimpleExtension(AtomServerConstants.ENTRY_ID, entryId);
                    for (Category category : entry.getCategories()) {
                        newEntry.addCategory(category);
                    }
                    // TODO: should ETags hash the content only, or the metadata of an entry, too?
                    newEntry.addSimpleExtension(AtomServerConstants.ETAG, HexUtil.toHexString(entryTuple.digest));
                    // TODO: we need to preserve "unknown" extensions

                    contentTxn.commit();

                    log.debug(String.format("successfully PUT entry %s", entryId));
                    return newEntry;
                }
            });
        } catch (Exception e) {
            try {
                contentTxn.abort();
            } catch (ContentStoreException inner) {
                log.error("exception aborting content transaction", inner);
            }
            throw asAtompubException(e);
        }
    }

    // TODO: this has no tests - probably doesn't work - just checking in to complete the API, but will still need much attention.
    public void deleteEntry(final String entryId, final String etag) {

        log.debug(String.format("DELETEing entry %s", entryId));

        sync(new Runnable() {
            public void run() {
                EntryTuple entryTuple = removeEntryNodeFromIndices(entryId);
                if (entryTuple == null) {
                    throw new NotFoundException("could not delete entry - does not exist.");
                } else {
                    if (!AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE.equals(etag) &&
                            !(entryTuple.digest == null ?
                                    etag == null :
                                    HexUtil.toHexString(entryTuple.digest).equals(etag))) {

                        throw new OptimisticConcurrencyException(
                                format("Optimistic Concurrency Exception - provided ETag (%s) does " +
                                        "not match previous ETag value of (%s)",
                                        etag, HexUtil.toHexString(entryTuple.digest)));
                    }
                    entryTuple = entryTuple.delete(getNextTimestamp(), new Date().getTime());
                }
                updateEntryNodeIntoIndices(entryTuple);

                log.debug(String.format("successfully DELETED entry %s", entryId));
            }
        });
    }    

    private AtompubException asAtompubException(Exception e) {
        if (e instanceof AtompubException) {
            return (AtompubException) e;
        }
        final String message = "unknown internal exception";
        log.error(message, e);
        return new AtompubServerException(message);
    }

    private Entry sync(Callable<Entry> callable) throws Exception {
        return substrate.sync(collectionKey, callable);
    }

    private void sync(final Runnable task) {
        try {
            substrate.sync(collectionKey, new Callable<Object>() {
                public Object call() throws Exception {
                    task.run();
                    return null;
                }
            });
        } catch (Exception e) {
            throw asAtompubException(e);
        }
    }

    Entry getEntry(String entryId) {
        final EntryTuple entryTuple = entries.get(entryId);
        return entryTuple == null ? null : convertToEntry(entryTuple, true);
    }

    Feed getFeed(long timestamp, int maxResults, CategoryQuery categoryQuery, boolean fullEntries, UriInfo uriInfo) {
        Feed feed = atompubFactory.newFeed(collectionKey, collectionKey, collectionKey);

        if (categoryQuery != null) {
            feed.addSimpleExtension(AtomServerConstants.CATEGORY_QUERY, categoryQuery.toString());
        }

        Iterator<EntryTuple> entryIterator;
        long endIndex = entryIndex.max(); // if the iterator is empty, we use the highest index value
        entryIterator = buildIterator(categoryQuery, timestamp);
        int countdown = maxResults;
        StringBuffer entryEtagsConcatenated = new StringBuffer();
        while (entryIterator.hasNext() && countdown > 0) {
            EntryTuple entryNode = entryIterator.next();
            if(entryNode==null){continue;}
            countdown--;
            Entry entry = convertToEntry(entryNode, fullEntries);
            feed.addEntry(entry);
            endIndex = entryNode.timestamp;
            entryEtagsConcatenated.append(HexUtil.toHexString(entryNode.digest));
        }
        feed.addSimpleExtension(AtomServerConstants.END_INDEX, String.valueOf(endIndex));
        feed.addSimpleExtension(AtomServerConstants.ETAG,
                DigestUtils.md5Hex(entryEtagsConcatenated.toString()));

        if (entryIterator.hasNext()) {
            URI next = uriInfo.getBaseUri().relativize(
                    uriInfo.getRequestUriBuilder().replaceQueryParam(
                            "start-index", String.valueOf(endIndex)).build());

            feed.addLink(String.format("/%s", next.toString()), "next");
        }

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


    private static class OutOfLineContentTransaction implements ContentStore.Transaction {
        byte[] digest;

        OutOfLineContentTransaction(IRI src) {
            try {
                DigestInputStream in =
                        new DigestInputStream(src.toURL().openStream(), MessageDigest.getInstance("MD5"));
                IOUtils.copy(in, new OutputStream() {
                    public void write(int b) throws IOException { /* NO-OP */ }
                });
                this.digest = in.getMessageDigest().digest();
            } catch (Exception e) {
                log.warn(String.format(
                        "unable to retrieve content at %s to compute ETag - " +
                                "etag is based on IRI only", src), e);
                this.digest = DigestUtils.md5(src.toString());
            }
        }

        public void commit() throws ContentStoreException { /* NO-OP */ }

        public void abort() throws ContentStoreException { /* NO-OP */ }

        public byte[] digest() throws ContentStoreException { return this.digest; }
    }


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

    protected Entry convertToEntry(EntryTuple entryTuple, boolean fullEntry) {

        Entry entry = atompubFactory.newEntry();

        String id = String.format("%s/%s", collectionKey, entryTuple.entryId);
        entry.setId(id);
        entry.setTitle(entryTuple.entryId);
        Date updated = new Date(entryTuple.updated);
        entry.setEdited(updated);
        entry.setUpdated(updated);
        entry.setPublished(new Date(entryTuple.created));
        EntryKey key = getEntryKey(entryTuple.entryId);
        if (fullEntry) {
            if (entryTuple.contentSrc != null) {
                entry.setContent(new IRI(entryTuple.contentSrc), entryTuple.contentType);
            } else {
                try {
                    ReadableByteChannel entryContent = contentStore.get(key);
                    Content.Type type = Content.Type.typeFromString(entryTuple.contentType);
                    if (type == null || type == Content.Type.MEDIA) {
                        // TODO: does this stream the content??  verify.
                        entry.setContent(Channels.newInputStream(entryContent), entryTuple.contentType);
                    } else {
                        entry.setContent(ContentUtils.toString(entryContent), type);
                    }
                } catch (ContentStoreException e) {
                    throw new WebApplicationException(e);
                }
            }
        }

        String entryUri = String.format("%s/%s/%s/%s/%s",
                UriBuilder.fromResource(Atompub.class).build().toString(),
                serviceId, workspaceId, collectionId, entryTuple.entryId);
        entry.addLink(entryUri, "alternate");
        entry.addLink(entryUri, "self");

        for (CategoryTuple entryCategory : entryTuple.categories) {
            Category category = atompubFactory.newCategory();
            category.setScheme(entryCategory.scheme);
            category.setTerm(entryCategory.term);
            category.setLabel(entryCategory.label);
            entry.addCategory(category);
        }
        entry.addSimpleExtension(AtomServerConstants.TIMESTAMP, String.valueOf(entryTuple.timestamp));
        entry.addSimpleExtension(AtomServerConstants.ETAG, HexUtil.toHexString(entryTuple.digest));
        return entry;
    }

    private EntryKey getEntryKey(String entryId) {
        return new EntryKey(serviceId, workspaceId, collectionId, entryId);
    }
}
