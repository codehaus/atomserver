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
import org.atomserver.core.Substrate;

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
                      Service service,
                      Substrate substrate) {
        super(root, name, substrate);
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

        extractEntryFilters(service);

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

    protected APPWorkspace createChild(String name,
                                       Workspace workspace) {
        return new APPWorkspace(this, name, workspace, substrate);
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
}
