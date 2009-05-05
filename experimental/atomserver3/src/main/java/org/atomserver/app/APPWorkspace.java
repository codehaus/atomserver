package org.atomserver.app;

import org.apache.abdera.model.Collection;
import org.apache.abdera.model.Workspace;
import org.atomserver.AtomServerConstants;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;

public class APPWorkspace extends ContainerResource<Workspace, Collection, APPService, APPCollection> {

    public APPWorkspace(APPService service,
                        String name,
                        Workspace workspace) {
        super(service, name);
        put(workspace);
    }

    @GET
    public Workspace getStaticRepresentation() {
        Workspace workspace = AbderaMarshaller.factory().newWorkspace();
        workspace.addSimpleExtension(AtomServerConstants.NAME, getName());
        workspace.setTitle(getPath());
        for (APPCollection collection : getChildren()) {
            workspace.addCollection(collection.getStaticRepresentation());
        }
        return workspace;
    }

    @PUT
    public Workspace put(Workspace workspace) {
        for (Collection collection : workspace.getCollections()) {
            String name = workspace.getSimpleExtension(AtomServerConstants.NAME);
            try {
                getChild(name).put(collection);
            } catch (WebApplicationException e) {
                createChild(collection);
            }
        }
        return getStaticRepresentation();
    }

    protected APPCollection createChild(String name,
                                        Collection staticRepresentation) {
        return new APPCollection(this, name, staticRepresentation);
    }
}
