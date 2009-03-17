package org.atomserver.app;

import org.apache.abdera.model.ExtensibleElement;
import org.atomserver.AtomServerConstants;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class ContainerResource<
        S extends ExtensibleElement,
        CS extends ExtensibleElement,
        P extends ContainerResource,
        C extends BaseResource<CS, ? extends ContainerResource>>
        extends BaseResource<S, P> {

    protected abstract C createChild(String name,
                                     CS staticRepresentation);

    private final SortedMap<String, C> children = new TreeMap<String, C>();

    protected ContainerResource(P parent,
                                String name) {
        super(parent, name);
    }

    @POST
    public CS postChild(CS childStaticRepresentation) {
        String name = childStaticRepresentation.getSimpleExtension(AtomServerConstants.NAME);
        if (name == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("you must provide an <as:name> element.").build());
        }
        C child;
        lock.writeLock().lock();
        try {
            child = children.get(name);
            if (child != null) {
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(String.format("%s already exists in %s.",
                                                      name, getPath())).build());
            }
            child = createChild(name, childStaticRepresentation);
            children.put(name, child);
        } finally {
            lock.writeLock().unlock();
        }

        return child.getStaticRepresentation();
    }

    @Path("/{name : [^\\$][^/]*}")
    public C getChild(@PathParam("name")String name) {
        C child = children.get(name);
        if (child == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(String.format("%s not found within %s.",
                                                  name, getPath())).build());
        }
        return child;
    }

    protected Collection<String> getChildNames() {
        return children.keySet();
    }

    protected Collection<C> getChildren() {
        return children.values();
    }
}
