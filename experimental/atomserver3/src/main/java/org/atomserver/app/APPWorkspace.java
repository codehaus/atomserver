package org.atomserver.app;

import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Workspace;
import org.apache.abdera.model.Service;
import org.apache.commons.lang.StringUtils;
import org.atomserver.AtomServerConstants;
import org.atomserver.app.jaxrs.AbderaMarshaller;
import static org.atomserver.AtomServerConstants.APPLICATION_APP_XML;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;

public class APPWorkspace extends ContainerResource<Workspace, Collection, APPService, APPCollection> {
    private String title;

    public APPWorkspace(APPService service,
                        String name,
                        Workspace workspace) {
        super(service, name);
        put(workspace);
    }

    @GET
    @Produces({APPLICATION_APP_XML, APPLICATION_XML, TEXT_XML})    
    public Service getServiceDocument() {
        Service service = AbderaMarshaller.factory().newService();
        service.addWorkspace(getStaticRepresentation());
        return service;
    }

    public Workspace getStaticRepresentation() {
        Workspace workspace = AbderaMarshaller.factory().newWorkspace();
        workspace.addSimpleExtension(AtomServerConstants.NAME, getName());
        workspace.setTitle(this.title);
        for (APPCollection collection : getChildren()) {
            workspace.addCollection(collection.getStaticRepresentation());
        }
        return workspace;
    }

    @PUT
    public Workspace put(Workspace workspace) {

        extractEntryFilters(workspace);

        for (Collection collection : workspace.getCollections()) {
            String name = workspace.getSimpleExtension(AtomServerConstants.NAME);
            try {
                getChild(name).put(collection);
            } catch (NotFoundException e) {
                createChild(collection);
            }
        }
        this.title = workspace.getTitle();
        return getStaticRepresentation();
    }

    protected APPCollection createChild(String name,
                                        Collection staticRepresentation) {
        return new APPCollection(this, name, staticRepresentation);
    }

    protected void validateChildStaticRepresentation(Collection collection) {
        String name = collection.getSimpleExtension(AtomServerConstants.NAME);
        if (name != null) {
            validateName(name);
        }
        if (collection.getTitle() == null) {
            throw new BadRequestException("Collections require an <atom:title> element.");
        }
    }

    protected String getChildName(Collection collection) {
        String name = collection.getSimpleExtension(AtomServerConstants.NAME);
        return (name == null) ?
               StringUtils.left(
                       collection.getTitle().replaceAll("\\s", "_")
                               .replaceAll("[^a-zA-Z0-9-_]", ""), 32)
               : name;
    }

}
