package org.atomserver.app;

import org.apache.abdera.model.*;
import org.apache.log4j.Logger;
import org.atomserver.AtomServerConstants;
import org.atomserver.app.jaxrs.DELETE;
import org.atomserver.app.jaxrs.GET;
import org.atomserver.app.jaxrs.PUT;
import org.atomserver.categories.CategoryQuery;
import org.atomserver.content.ContentStore;
import org.atomserver.core.Substrate;
import org.atomserver.directory.ServiceDirectory;
import org.atomserver.filter.EntryFilterChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.UUID;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.atomserver.AtomServerConstants.*;
import static org.atomserver.AtomServerConstants.DEFAULT_PAGE_SIZE_PARAMETER;

@Path(Atompub.APP_CONTEXT)
@Component
@Produces({APPLICATION_ATOM_XML, APPLICATION_XML, TEXT_XML})
public class Atompub {
    private static final Logger log = Logger.getLogger(Atompub.class);

    // TODO: make /app in to /app/v3, or some suitably versioned context
    // TODO: pull this constant in programatically where it is referenced in code and tests
    public static final String APP_CONTEXT = "/app";
    private Substrate substrate;
    private ServiceDirectory serviceDirectory;
    private ContentStore contentStore;
    private AtompubFactory atompubFactory;

    @Autowired
    public void setSubstrate(Substrate substrate) {
        this.substrate = substrate;
    }

    @Autowired
    public void setServiceDirectory(ServiceDirectory serviceDirectory) {
        this.serviceDirectory = serviceDirectory;
    }

    @Autowired
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Autowired
    public void setAtompubFactory(AtompubFactory atompubFactory) {
        this.atompubFactory = atompubFactory;
    }

    public enum EntryType {
        link, full
    }

    @GET("service-feed")
    public Feed get() {
        final Feed serviceFeed = atompubFactory.newFeed("/", "Service Feed", "/");
        Date latestUpdated = null;
        for (Service service : serviceDirectory.list()) {
            final Entry entry = serviceFeed.addEntry();
            String serviceId = service.getSimpleExtension(NAME);
            entry.setTitle(serviceId);
            String updatedExtension = service.getSimpleExtension(UPDATED);
            if (updatedExtension == null) {
                // if there was no updated extension on the service, save it to add one.
                service(serviceId).put(service);
                updatedExtension = service.getSimpleExtension(UPDATED);
            }
            final Date updated = AtomDate.parse(updatedExtension);
            latestUpdated = (latestUpdated == null || latestUpdated.before(updated)) ?
                    updated : latestUpdated;
            entry.setUpdated(updated);
            entry.addLink(String.format("%s/%s", APP_CONTEXT, entry.getTitle()), "alternate");
            entry.setContent(service);
        }
        serviceFeed.setUpdated(latestUpdated);
        return serviceFeed;
    }

    @Path("{service}")
    public AppService service(@PathParam("service") String serviceId) {
        return new AppService(serviceId);
    }

    @Consumes({APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
    @Produces({APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
    public class AppService {
        private final String serviceId;

        public AppService(String serviceId) {this.serviceId = serviceId;}

        @PUT("service")
        public Service put(Service service) {
            serviceDirectory.put(serviceId, service);
            return service;
        }

        @DELETE("service")
        public Service delete() {
            return serviceDirectory.remove(serviceId);
        }

        @GET("service")
        public Service get() {
            return serviceDirectory.get(serviceId).getService();
        }

        public String toString() {
            return serviceId;
        }

        @Path("{workspace}")
        public AppWorkspace workspace(@PathParam("workspace") String workspaceId) {
            return new AppWorkspace(workspaceId);
        }

        @Consumes({APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
        @Produces({APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})
        public class AppWorkspace {
            private final String workspaceId;

            public AppWorkspace(String workspaceId) {this.workspaceId = workspaceId;}

            public String toString() {
                return String.format("%s/%s", serviceId, workspaceId);
            }

            @Path("{collection}")
            public AppCollection collection(@PathParam("collection") String collectionId) {
                return new AppCollection(collectionId);
            }

            @Produces({APPLICATION_ATOM_XML, APPLICATION_XML, TEXT_XML})
            public class AppCollection extends CollectionOperations {
                private String collectionId;

                public AppCollection(String collectionId) {
                    super(serviceId, workspaceId, collectionId, substrate, contentStore, atompubFactory);
                    this.collectionId = collectionId;
                }

                @GET("feed")
                public Response get(
                        @QueryParam("start-index") @DefaultValue("0") long timestamp,
                        @QueryParam("max-results") @DefaultValue(DEFAULT_PAGE_SIZE_PARAMETER) int maxResults,
                        @QueryParam("entry-type") @DefaultValue("link") EntryType entryType,
                        @Context UriInfo uriInfo) {

                    return get(timestamp, maxResults, entryType, null, uriInfo);
                }

                @Path("-/{categoryQuery : .*}")
                @GET("cat-feed")
                public Response get(
                        @QueryParam("start-index") @DefaultValue("0") long timestamp,
                        @QueryParam("max-results") @DefaultValue(DEFAULT_PAGE_SIZE_PARAMETER) int maxResults,
                        @QueryParam("entry-type") @DefaultValue("link") EntryType entryType,
                        @PathParam("categoryQuery") CategoryQuery categoryQuery,
                        @Context UriInfo uriInfo) {

                    return feedResponse(getFeed(timestamp, maxResults, categoryQuery,
                            entryType == EntryType.full,
                            uriInfo));
                }

                @POST
                public Response post(
                        @HeaderParam("ETag") String etagHeader,
                        Entry entry) {
                    return entry(UUID.randomUUID().toString().replaceAll("\\W", "")).put(etagHeader, entry);
                }

                public String toString() {
                    return String.format("%s/%s/%s", serviceId, workspaceId, collectionId);
                }

                @Path("{entry}")
                public AppEntry entry(@PathParam("entry") String entryId) {
                    return new AppEntry(entryId);
                }

                @Produces({APPLICATION_ATOM_XML, APPLICATION_XML, TEXT_XML})
                @Consumes({APPLICATION_ATOM_XML, APPLICATION_XML, TEXT_XML})
                public class AppEntry {
                    private final String entryId;

                    public AppEntry(String entryId) {this.entryId = entryId;}

                    @GET("entry")
                    public Entry get() {
                        final Entry entry = getEntry(entryId);
                        if (entry == null) {
                            throw new NotFoundException(String.format("%s NOT FOUND", toString()));
                        }
                        return entry;
                    }

                    @PUT("entry")
                    public Response put(
                            @HeaderParam("ETag") String etagHeader,
                            Entry entry) {
                        final EntryFilterChain entryFilterChain =
                                serviceDirectory.get(serviceId).getEntryFilterChain(workspaceId, collectionId);
                        entryFilterChain.doChain(entry);
                        final Entry updatedEntry = updateEntry(entryId, extractEtag(etagHeader, entry), entry);
                        return entryResponse(
                                updatedEntry,
                                updatedEntry.getUpdated().equals(updatedEntry.getPublished()));
                    }

                    @DELETE("entry")
                    public void delete(@HeaderParam("ETag") String etagHeader) {
                        deleteEntry(entryId, etagHeader);
                    }

                    public String toString() {
                        return String.format("%s/%s/%s/%s", serviceId, workspaceId, collectionId, entryId);
                    }
                }
            }
        }
    }


    private static String extractEtag(String etagHeader, ExtensibleElement element) {
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

    public static Response entryResponse(Entry entry, boolean created) {
        return Response.status(created ? CREATED : OK)
                .entity(entry).tag(new EntityTag(entry.getSimpleExtension(ETAG)))
                .build();
    }

    public static Response feedResponse(Feed feed) {
        return Response.status(OK).entity(feed)
                .tag(new EntityTag(feed.getSimpleExtension(ETAG), true))
                .build();
    }


}