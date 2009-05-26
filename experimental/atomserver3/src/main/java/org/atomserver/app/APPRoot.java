package org.atomserver.app;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Service;
import org.atomserver.content.ContentStore;
import org.atomserver.AtomServerConstants;
import org.atomserver.app.jaxrs.AbderaMarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import static javax.ws.rs.core.MediaType.*;
import javax.ws.rs.core.Response;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;

@Path("/")
@Produces({APPLICATION_ATOM_XML, APPLICATION_XML, TEXT_XML})
@Component(APPRoot.BEAN_NAME)
public class APPRoot extends ContainerResource<Feed, Service, ContainerResource, APPService> {
    public static final String BEAN_NAME = "org.atomserver.app.APPRoot";
    private final ContentStore defaultContentStore;

    @Autowired
    public APPRoot(ContentStore defaultContentStore) {
        super(null, null);
        this.defaultContentStore = defaultContentStore;
    }

    /**
     * return the Service Document feed for this instance of AtomServer.
     *
     * @return the Service Document feed for this instance of AtomServer
     */
    @GET
    public Feed getStaticRepresentation() {
        Feed feed = AbderaMarshaller.factory().newFeed();

        // set the Author for the Service Document Feed
        feed.addAuthor("AtomServer v3");

        // set the ID for the Service Document Feed
        feed.setId(getUri().toString());

        // set the Title for the Service Document Feed
        feed.setTitle("AtomServer v3 Service Documents Feed");

        // set the Updated value for the Service Document Feed
        feed.setUpdated(getUpdated());

        // create the "self" link for the Service Document feed
        Link feedSelfLink = AbderaMarshaller.factory().newLink();
        feedSelfLink.setRel("self");
        feedSelfLink.setHref(getUri().toString());
        feed.addLink(feedSelfLink);

        // sort the services in reverse order by their updated date
        SortedSet<APPService> services = new TreeSet<APPService>(
                new Comparator<APPService>() {
                    public int compare(APPService o1, APPService o2) {
                        return o2.getUpdated().compareTo(o1.getUpdated());
                    }
                }
        );
        services.addAll(getChildren());

        // create an Entry for each Service on this server
        for (APPService service : services) {
            Entry entry = AbderaMarshaller.factory().newEntry();

            // set the ID for the service 
            entry.setId(service.getUri().toString());

            // set the Title for the Service
            entry.setTitle(service.getName());

            // set the Updated value for the Service
            entry.setUpdated(service.getUpdated());

            // create the "alternate" link for the Service
            Link entryAltLink = AbderaMarshaller.factory().newLink();
            entryAltLink.setRel("alternate");
            entryAltLink.setHref(service.getUri().toString());
            entry.addLink(entryAltLink);
            
            // set the Summary element for the Service
            entry.setSummary(String.format("Service Document for the %s Service",
                                           service.getName()));
            
            feed.addEntry(entry);
        }
        return feed;
    }

    protected void validateChildStaticRepresentation(Service childStaticRepresentation) {
        String name = childStaticRepresentation.getSimpleExtension(AtomServerConstants.NAME);
        if (name == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("You must provide an <as:name> element.").build());
        }
        validateName(name);
        
    }

    protected APPService createChild(String name,
                                     Service service) {
        return new APPService(this, name, service);
    }

    protected ContentStore getDefaultContentStore() {
        return this.defaultContentStore;
    }
}