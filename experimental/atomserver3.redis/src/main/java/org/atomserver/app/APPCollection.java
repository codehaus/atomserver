package org.atomserver.app;

import org.apache.abdera.model.*;
import org.apache.abdera.model.Collection;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.atomserver.AtomServerConstants;
import org.atomserver.app.jaxrs.AbderaMarshaller;
import org.atomserver.categories.CategoryQuery;
import org.atomserver.categories.CategoryQueryParseException;
import org.atomserver.categories.CategoryQueryParser;
import org.atomserver.content.*;
import org.atomserver.ext.Aggregate;
import org.atomserver.util.ArraySet;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

import static java.lang.String.format;
import static org.atomserver.app.APPResponses.feedResponse;

public class APPCollection extends BaseResource<Collection, APPWorkspace> {

    private static final Logger log = Logger.getLogger(APPCollection.class);

    private String title;

    public APPCollection(final APPWorkspace workspace,
                         String name,
                         Collection collection) {
        super(workspace, name);
        collectionIndex = new TreeSetCollectionIndex<SimpleEntryNode>() {
            protected SimpleEntryNode newEntryNode(String entryId) {
                return new SimpleEntryNode(
                        workspace.getName(), APPCollection.this.getName(), entryId);
            }
        };
        put(collection);
    }

    public Collection getStaticRepresentation() {
        Collection collection = AbderaMarshaller.factory().newCollection();
        collection.addSimpleExtension(AtomServerConstants.NAME, getName());
        collection.setTitle(this.title);
        return collection;
    }

    @GET
    public Response getFeedPage(
            @QueryParam("timestamp") @DefaultValue("0") long timestamp,
            @QueryParam("max-results") @DefaultValue("100") int maxResults) {
        return getFeedPage(timestamp, maxResults, null);
    }

    @GET
    @Path("/-/{categoryQuery : (AND|OR|NOT|\\([^\\)]+\\)[^\\/]+)(/(AND|OR|NOT|\\([^\\)]+\\)[^\\/]+))*}")
    public Response getFeedPage(
            @QueryParam("timestamp") @DefaultValue("0") long timestamp,
            @QueryParam("max-results") @DefaultValue("100") int maxResults,
            @PathParam("categoryQuery") String categoryQueryParam) {
        Feed feed = AbderaMarshaller.factory().newFeed();
        CategoryQuery categoryQuery;
        try {
            categoryQuery = categoryQueryParam == null ? null :
                            CategoryQueryParser.parse(categoryQueryParam);
        } catch (CategoryQueryParseException e) {
            throw new WebApplicationException();// TODO: what?
        }

        if (categoryQuery != null) {
            feed.addSimpleExtension(AtomServerConstants.CATEGORY_QUERY, categoryQuery.toString());
        }

        getService().lock.readLock().lock();
        Iterator<SimpleEntryNode> entryIterator;
        try {
            entryIterator = collectionIndex.buildIterator(categoryQuery, timestamp);
            int countdown = maxResults;
            long endIndex = timestamp; // TODO: scroll to the end when feed is empty
            StringBuffer entryEtagsConcatenated = new StringBuffer();
            while (entryIterator.hasNext() && countdown-- > 0) {
                SimpleEntryNode entryNode = entryIterator.next();
                Entry entry = convertToEntry(entryNode);
                feed.addEntry(entry);
                endIndex = entryNode.getTimestamp();
                entryEtagsConcatenated.append(entryNode.getEtag());
            }
            feed.addSimpleExtension(AtomServerConstants.END_INDEX, String.valueOf(endIndex));
            feed.addSimpleExtension(AtomServerConstants.ETAG,
                                    DigestUtils.md5Hex(entryEtagsConcatenated.toString()));
        } finally {
            getService().lock.readLock().unlock();
        }

        return feedResponse(feed);
    }


    @PUT
    public Collection put(Collection collection) {

        extractEntryFilters(collection);

        this.title = collection.getTitle();

        return getStaticRepresentation();
    }


    @POST
    public Response post(ExtensibleElement element) throws WebApplicationException {
        if (element instanceof Entry) {
            return postEntry((Entry) element);
        } else if (element instanceof Feed) {
            // TODO: implement batching
            return Response.status(Response.Status.OK).entity(postBatch((Feed) element)).build();
        } else {
            throw new BadRequestException(
                    format("unknown entity type %s", element.getClass().getSimpleName()));
        }
    }

    public Feed postBatch(Feed batch) {
        // TODO: write a test and fully implement batch handling
        Feed returnFeed = AbderaMarshaller.factory().newFeed();
//        for (Entry entry : batch.getEntries()) {
//            returnFeed.addEntry(postEntry(entry));
//        }
        return returnFeed;
    }

    public Response postEntry(Entry entry) {
        // TODO: pluggable strategies for id generation
        // TODO: etag should not necessarily always be null here - we need to check for errors
        entry = updateEntry(UUID.randomUUID().toString().replaceAll("\\W", ""), null, entry);
        return APPResponses.entryResponse(entry, entry.getPublished().equals(entry.getUpdated()));

    }

    @GET
    @Path("/{entryId : [^\\$][^/]*}")
    public Response getEntry(@PathParam("entryId") String entryId) {
        SimpleEntryNode entryNode = collectionIndex.getEntry(entryId);
        if (entryNode == null) {
            throw new NotFoundException(String.format("%s NOT FOUND", getFullEntryId(entryId)));
        }
        return APPResponses.entryResponse(convertToEntry(entryNode));
    }

    @PUT
    @Path("/{entryId : [^\\$][^/]*}")
    public Response put(@HeaderParam("ETag") String etagHeader,
                        @PathParam("entryId") String entryId,
                        ExtensibleElement element) throws WebApplicationException {
        String etag = extractEtag(etagHeader, element);
        if (element instanceof Entry) {
            Entry entry = updateEntry(entryId, etag, (Entry) element);
            return APPResponses.entryResponse(entry, entry.getPublished().equals(entry.getUpdated()));
        } else if (element instanceof Categories) {
            return Response.status(Response.Status.OK)
                    .entity(putCategories(entryId, (Categories) element)).build();
        } else {
            throw new BadRequestException(
                    format("unknown entity type %s", element.getClass().getSimpleName()));
        }
    }

    private String extractEtag(String etagHeader, ExtensibleElement element) {
        String etagXml = element.getSimpleExtension(AtomServerConstants.ETAG);
        if (etagHeader == null) {
            return etagXml;
        } else if (etagXml == null || etagHeader.equals(etagXml)) {
            return etagHeader;
        } else {
            throw new BadRequestException(
                    format("Header ETag (%s) and XML Body Etag (%s) differ - please remove the " +
                           "incorrect one and retry",
                           etagHeader, etagXml));
        }

    }

    public Entry updateEntry(String entryId, String etag, Entry entry) {

        log.debug(String.format("PUTting entry %s", entryId));

        getEntryFilterChain().doChain(entry);

        // putting an entry consists of the following steps:
        // - validate the entry - make sure that we *think* we are going to be able to write
        // - do "phase 1" of the two-phase-commit for putting the content into the store
        // - acquire the write lock on the service meta-data
        //   - write the meta-data to the service
        //   - do "phase 2" of the two-phase-commit for the content
        // - release the write lock.

        // TODO: validate the <id> element (e.g. against the path)


        ContentStore.Transaction contentTxn;
        try {
            // TODO: not all content is strings, some is links and some is base64-ed
            contentTxn = getContentStore().put(getEntryKey(entryId),
                                               "text/plain",
                                               ContentUtils.toChannel(entry.getContent()));
        } catch (ContentStoreException e) {
            // TODO: handle for real
            throw new WebApplicationException(e);
        }

        Entry newEntry = AbderaMarshaller.factory().newEntry();
        newEntry.setId(getFullEntryId(entryId));

        log.debug(String.format("acquiring service %s write lock", getService().getName()));
        getService().lock.writeLock().lock(); // TODO: timeouts

        try {
            try {
                SimpleEntryNode entryNode = collectionIndex.removeEntryNodeFromIndices(entryId);

                if (!AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE.equals(etag) &&
                    !(entryNode.getEtag() == null ?
                      etag == null :
                      entryNode.getEtag().equals(etag))) {

                    throw new OptimisticConcurrencyException(
                            format("Optimistic Concurrency Exception - provided ETag (%s) does " +
                                   "not match previous ETag value of (%s)",
                                   etag, entryNode.getEtag()));
                }

                entryNode.update(getService().timestamp.getAndIncrement(),
                                 new Date(),
                                 convertCategories(entry.getCategories()),
                                 contentTxn.etag());

                collectionIndex.updateEntryNodeIntoIndices(entryNode);

                List<Aggregate> aggregates = entry.getExtensions(AtomServerConstants.AGGREGATE);
                if (!entryNode.getAggregates().isEmpty() ||
                    (aggregates != null && !aggregates.isEmpty())) {
                    Set<AggregateNode> newAggregateNodes = new HashSet<AggregateNode>();
                    for (Aggregate aggregate : aggregates) {
                        newAggregateNodes.add(new AggregateNode(aggregate.getCollection(),
                                                                aggregate.getEntryId()));
                    }
                    updateAggregates(entryNode, newAggregateNodes);
                }

                newEntry.setEdited(entryNode.getLastUpdated());
                newEntry.setPublished(entryNode.getPublished());
                newEntry.setUpdated(entryNode.getLastUpdated());
                newEntry.setContent(entry.getContent(), entry.getContentType()); // TODO: deal with content...
                newEntry.addSimpleExtension(AtomServerConstants.ENTRY_ID, entryId);
                for (Category category : entry.getCategories()) {
                    newEntry.addCategory(category);
                }
                for (Aggregate aggregate : aggregates) {
                    newEntry.addExtension(aggregate);
                }
                // TODO: should ETags hash the content only, or the metadata of an entry, too?
                newEntry.addSimpleExtension(AtomServerConstants.ETAG, entryNode.getEtag());
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

        } finally {
            getService().lock.writeLock().unlock();
            log.debug(String.format("released service %s write lock", getService().getName()));
        }
    }

    private void updateAggregates(SimpleEntryNode entryNode, Set<AggregateNode> newAggregateNodes) {
        Set<AggregateNode> allAggregateNodes =
                new HashSet<AggregateNode>(entryNode.getAggregates());
        allAggregateNodes.addAll(newAggregateNodes);

        Map<AggregateNode, AggregateEntryNode> agMap =
                new HashMap<AggregateNode, AggregateEntryNode>();
        for (AggregateNode aggregateNode : allAggregateNodes) {
            CollectionIndex<AggregateEntryNode> index =
                    getService().getAggregateCollectionIndex(
                            aggregateNode.getCollection());
            agMap.put(aggregateNode,
                      index.removeEntryNodeFromIndices(aggregateNode.getEntryId()));
        }
        for (AggregateNode aggregateNode : allAggregateNodes) {
            CollectionIndex<AggregateEntryNode> index =
                    getService().getAggregateCollectionIndex(
                            aggregateNode.getCollection());

            AggregateEntryNode aggregateEntryNode = agMap.get(aggregateNode);
            if (newAggregateNodes.contains(aggregateNode)) {
                aggregateEntryNode.getMembers().add(entryNode);
                entryNode.getAggregates().add(aggregateNode);
            } else {
                aggregateEntryNode.getMembers().remove(entryNode);
                entryNode.getAggregates().remove(aggregateNode);
            }

            Set<EntryCategory> aggregateCategories = new HashSet<EntryCategory>();
            for (SimpleEntryNode member : aggregateEntryNode.getMembers()) {
                for (EntryCategory entryCategory : member.getCategories()) {
                    aggregateCategories.add(
                            new EntryCategory(entryCategory.getCategory(), null));
                }
            }

            aggregateEntryNode.update(entryNode.getTimestamp(),
                                      entryNode.getLastUpdated(),
                                      new ArraySet(aggregateCategories),
                                      null);
            index.updateEntryNodeIntoIndices(aggregateEntryNode);
        }
    }

    private EntryKey getEntryKey(String entryId) {
        return new EntryKey(getService().getName(),
                            getWorkspace().getName(),
                            getName(),
                            entryId);
    }

    private String getFullEntryId(String entryId) {
        return format("%s/%s/%s/%s",
                      getService().getName(),
                      getWorkspace().getName(),
                      getName(),
                      entryId);
    }

    public Categories putCategories(String entryId, Categories categories) {
        // TODO: update the categories
        return categories;
    }

    // -----------------------
    private ContentStore getContentStore() {
        // TODO: we should be able to configure a content storage at any point along the path...
        // this is the default content store to use if we can't find anything else.
        return getRoot().getDefaultContentStore();
    }

    private APPRoot getRoot() {return getService().getParent();}

    private APPService getService() {return getWorkspace().getParent();}

    private APPWorkspace getWorkspace() {return getParent();}

    // -----------------------
    private Set<EntryCategory> convertCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        EntryCategory[] entryCategories = new EntryCategory[categories.size()];
        int i = 0;
        for (Category category : categories) {
            entryCategories[i++] = new EntryCategory(
                    new CategoryNode(category.getScheme().toString(), category.getTerm()),
                    category.getLabel());
        }
        return new ArraySet(entryCategories);
    }

    private Set<AggregateNode> convertAggregates(List<Aggregate> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return Collections.EMPTY_SET;
        }
        AggregateNode[] aggregateNodes = new AggregateNode[aggregates.size()];
        int i = 0;
        for (Aggregate aggregate : aggregates) {
            aggregateNodes[i++] = new AggregateNode(
                    aggregate.getCollection(), aggregate.getEntryId());
        }
        return new ArraySet<AggregateNode>(aggregateNodes);
    }

    protected Entry convertToEntry(SimpleEntryNode entryNode) {
        // TODO: make this a method on EntryNode?
        Entry entry = AbderaMarshaller.factory().newEntry();

        entry.setId(getFullEntryId(entryNode.getEntryId()));
        entry.setEdited(entryNode.getLastUpdated());
        entry.setUpdated(entryNode.getLastUpdated());
        EntryKey key = getEntryKey(entryNode.getEntryId());
        try {
            EntryContent entryContent = getContentStore().get(key);
            // TODO: not all content is strings, some is links and some is base64-ed
            entry.setContent(ContentUtils.toString(entryContent.getChannel()), Content.Type.XML);
        } catch (ContentStoreException e) {
            throw new WebApplicationException(e);
        }
        for (EntryCategory entryCategory : entryNode.getCategories()) {
            Category category = AbderaMarshaller.factory().newCategory();
            category.setScheme(entryCategory.getCategory().getScheme());
            category.setTerm(entryCategory.getCategory().getTerm());
            category.setLabel(entryCategory.getLabel());
            entry.addCategory(category);
        }
        for (AggregateNode aggregateNode : entryNode.getAggregates()) {
            Aggregate aggregate = AbderaMarshaller.factory().newExtensionElement(AtomServerConstants.AGGREGATE);
            aggregate.setCollection(aggregateNode.getCollection());
            aggregate.setEntryId(aggregateNode.getEntryId());
            entry.addExtension(aggregate);
        }
        entry.addSimpleExtension(AtomServerConstants.TIMESTAMP, String.valueOf(entryNode.getTimestamp()));
        entry.addSimpleExtension(AtomServerConstants.ETAG, entryNode.getEtag());
        return entry;
    }

    // -----------------------

    private final CollectionIndex<SimpleEntryNode> collectionIndex;
}