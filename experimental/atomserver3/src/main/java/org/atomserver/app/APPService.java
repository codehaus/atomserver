package org.atomserver.app;

import org.apache.abdera.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.atomserver.AtomServerConstants;
import org.atomserver.categories.CategoryQuery;
import org.atomserver.categories.CategoryQueryParseException;
import org.atomserver.categories.CategoryQueryParser;

import javax.ws.rs.*;
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
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(
                            "Invalid Service Document - services must contain at least one " +
                            "workspace.").build());
        }
        for (Workspace workspace : service.getWorkspaces()) {
            String name = workspace.getSimpleExtension(AtomServerConstants.NAME);
            try {
                getChild(name).put(workspace);
            } catch (WebApplicationException e) {
                createChild(workspace);
            }
        }
        return getStaticRepresentation();
    }

    @GET
    @Path("/{join : \\$join}/{collection}")
    public Feed aggregateFeed(@PathParam("collection") String collection) {
        return categorizedAggregateFeed(collection, null);
    }

    @GET
    @Path("/{join : \\$join}/{collection}/-/" +
          "{categoryQuery : (AND|OR|NOT|\\([^\\)]+\\)[^\\/]+)(/(AND|OR|NOT|\\([^\\)]+\\)[^\\/]+))*}")
    public Feed categorizedAggregateFeed(@PathParam("collection") String collection,
                                         @PathParam("categoryQuery") String categoryQueryParam) {
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
            while (entryIterator.hasNext() && countdown-- > 0) {
                feed.addEntry(aggregateEntry(collection, entryIterator.next().getEntryId()));
            }
        } finally {
            lock.readLock().unlock();
        }

        return feed;
    }

    @GET
    @Path("/{join : \\$join}/{collection}/{entryId}")
    public Entry aggregateEntry(@PathParam("collection") String collection,
                                @PathParam("entryId") String entryId) {
        CollectionIndex<AggregateEntryNode> collectionIndex =
                aggregateCollectionIndices.get(collection);

        AggregateEntryNode aggregateEntryNode = collectionIndex.getEntry(entryId);
        Entry entry = AbderaMarshaller.factory().newEntry();
        entry.setId(String.format("$join/%s/%s", collection, entryId));
        entry.setTitle(String.format("Aggregate Entry : %s", entry.getId()));

        Element aggregateContent =
                AbderaMarshaller.factory().newElement(AtomServerConstants.AGGREGATE_CONTENT);

        for (SimpleEntryNode entryNode : aggregateEntryNode.getMembers()) {
            Entry memberEntry = entryNode.getCollection().convertToEntry(entryNode);
            ((OMElement) aggregateContent).addChild((OMNode) memberEntry);
        }
        entry.setContent(aggregateContent);

        return entry;
    }


    protected APPWorkspace createChild(String name,
                                       Workspace workspace) {
        return new APPWorkspace(this, name, workspace);
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
