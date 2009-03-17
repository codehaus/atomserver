package org.atomserver.app;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Service;
import org.atomserver.content.ContentStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.*;

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

    @GET
    public Feed getStaticRepresentation() {
        Feed feed = AbderaMarshaller.factory().newFeed();
        for (APPService service : getChildren()) {
            Entry entry = AbderaMarshaller.factory().newEntry();
            entry.setTitle(service.getName());
            entry.setContent(service.getStaticRepresentation());
            feed.addEntry(entry);
        }
        return feed;
    }

    protected APPService createChild(String name,
                                     Service service) {
        return new APPService(this, name, service);
    }

    protected ContentStore getDefaultContentStore() {
        return this.defaultContentStore;
    }
}