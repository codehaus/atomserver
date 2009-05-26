package org.atomserver.app;

import org.apache.abdera.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.atomserver.AtomServerConstants;
import org.atomserver.app.jaxrs.AbderaMarshaller;
import static org.atomserver.AtomServerConstants.APPLICATION_APP_XML;
import org.atomserver.categories.CategoryQuery;
import org.atomserver.categories.CategoryQueryParseException;
import org.atomserver.categories.CategoryQueryParser;

import javax.ws.rs.*;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class APPService extends ContainerResource<Service, Workspace, APPRoot, APPWorkspace> {

    public APPService(APPRoot root,
                      String name,
                      Service service) {
        super(root, name);
        put(service);
    }

    @GET
    @Produces({APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
    public Service getStaticRepresentation() {
        Service service = AbderaMarshaller.factory().newService();
        service.addSimpleExtension(AtomServerConstants.NAME, getName());
        for (APPWorkspace workspace : getChildren()) {
            service.addWorkspace(workspace.getStaticRepresentation());
        }
        return service;
    }

    @PUT
    public Service put(Service service) {
        if (service.getWorkspaces().isEmpty()) {
            throw new BadRequestException(
                    "Invalid Service Document - services must contain at least one workspace.");
        }
        for (Workspace workspace : service.getWorkspaces()) {
            String name = workspace.getSimpleExtension(AtomServerConstants.NAME);
            try {
                getChild(name).put(workspace);
            } catch (NotFoundException e) {
                createChild(workspace);
            }
        }
        return getStaticRepresentation();
    }

    @GET
    @Path("/{join : \\$join}/{collection}")
    public Response aggregateFeed(@PathParam("collection")String collection) {
        return categorizedAggregateFeed(collection, null);
    }

    @GET
    @Path("/{join : \\$join}/{collection}/-/" +
          "{categoryQuery : (AND|OR|NOT|\\([^\\)]+\\)[^\\/]+)(/(AND|OR|NOT|\\([^\\)]+\\)[^\\/]+))*}")
    public Response categorizedAggregateFeed(@PathParam("collection")String collection,
                                         @PathParam("categoryQuery")String categoryQueryParam) {
        System.out.println("APPService.aggregateFeed");
        Feed feed = AbderaMarshaller.factory().newFeed();
        CollectionIndex<AggregateEntryNode> collectionIndex = aggregateCollectionIndices.get(collection);
        CategoryQuery categoryQuery;
        try {
            categoryQuery = categoryQueryParam == null ?
                            null :
                            CategoryQueryParser.parse(categoryQueryParam);
        } catch (CategoryQueryParseException e) {
            throw new WebApplicationException(); // TODO: what?
        }

        lock.readLock().lock();
        try {
            Iterator<AggregateEntryNode> entryIterator =
                    collectionIndex.buildIterator(categoryQuery, 0L);// TODO: timestamp parameter
            int countdown = 10; // TODO: maxResults parameter & check fencepost error on --
            StringBuffer concatenatedEntryEtags = new StringBuffer();
            while (entryIterator.hasNext() && countdown-- > 0) {
                AggregateEntryNode aggregateEntryNode = entryIterator.next();
                feed.addEntry(aggregateEntry(collection, aggregateEntryNode.getEntryId()));
                concatenatedEntryEtags.append(aggregateEntryNode.getEtag());
            }
            feed.addSimpleExtension(AtomServerConstants.ETAG,
                                    DigestUtils.md5Hex(concatenatedEntryEtags.toString()));
        } finally {
            lock.readLock().unlock();
        }

        return APPResponses.feedResponse(feed);
    }

    @GET
    @Path("/{join : \\$join}/{collection}/{entryId}")
    public Response getAggregateEntry(@PathParam("collection")String collection,
                                @PathParam("entryId")String entryId) {
        return APPResponses.entryResponse(aggregateEntry(collection, entryId));
    }

    public Entry aggregateEntry(@PathParam("collection")String collection,
                                @PathParam("entryId")String entryId) {
        CollectionIndex<AggregateEntryNode> collectionIndex =
                aggregateCollectionIndices.get(collection);

        AggregateEntryNode aggregateEntryNode = collectionIndex.getEntry(entryId);
        Entry entry = AbderaMarshaller.factory().newEntry();
        entry.setId(String.format("%s/$join/%s/%s", getName(), collection, entryId));
        entry.setTitle(String.format("Aggregate Entry : %s", entry.getId()));

        Element aggregateContent =
                AbderaMarshaller.factory().newElement(AtomServerConstants.AGGREGATE_CONTENT);

        for (SimpleEntryNode entryNode : aggregateEntryNode.getMembers()) {
            Entry memberEntry = entryNode.getCollection().convertToEntry(entryNode);
            ((OMElement) aggregateContent).addChild((OMNode) memberEntry);
        }
        for (EntryCategory entryCategory : aggregateEntryNode.getCategories()) {
            Category category = AbderaMarshaller.factory().newCategory();
            category.setScheme(entryCategory.getCategory().getScheme());
            category.setTerm(entryCategory.getCategory().getTerm());
            category.setLabel(entryCategory.getLabel());
            entry.addCategory(category);
        }
        entry.addSimpleExtension(AtomServerConstants.ETAG, aggregateEntryNode.getEtag());

        entry.setContent(aggregateContent);

        return entry;
    }


    protected APPWorkspace createChild(String name,
                                       Workspace workspace) {
        return new APPWorkspace(this, name, workspace);
    }

    protected void validateChildStaticRepresentation(Workspace workspace) {
        String name = workspace.getSimpleExtension(AtomServerConstants.NAME);
        if (name != null) {
            validateName(name);
        }
        if (workspace.getTitle() == null) {
            throw new BadRequestException("Workspaces require an <atom:title> element.");
        }
    }

    protected String getChildName(Workspace workspace) {
        String name = workspace.getSimpleExtension(AtomServerConstants.NAME);
        return (name == null) ?
               StringUtils.left(
                       workspace.getTitle().replaceAll("\\s", "_")
                               .replaceAll("[^a-zA-Z0-9-_]", ""), 32)
               : name;
    }

    //---------

    private final Map<String, CollectionIndex<AggregateEntryNode>> aggregateCollectionIndices =
            new HashMap<String, CollectionIndex<AggregateEntryNode>>();

    public CollectionIndex<AggregateEntryNode> getAggregateCollectionIndex(String collection) {
        lock.writeLock().lock();
        try {
            CollectionIndex<AggregateEntryNode> index =
                    aggregateCollectionIndices.get(collection);
            if (index == null) {
                aggregateCollectionIndices.put(collection, index =
//                        new TreeSetCollectionIndex<AggregateEntryNode>() {
                        new LuceneCollectionIndex<AggregateEntryNode>() {
                            protected AggregateEntryNode newEntryNode(String entryId) {
                                return new AggregateEntryNode(entryId);
                            }
                        });
            }
            return index;
        } finally {
            lock.writeLock().unlock();
        }
    }

    //---------

    protected final AtomicLong timestamp = new AtomicLong(1L);
}
