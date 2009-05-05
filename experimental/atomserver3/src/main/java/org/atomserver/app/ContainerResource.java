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
import java.util.regex.Pattern;
import java.net.URI;
import java.net.URISyntaxException;

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
    public Response postChild(CS childStaticRepresentation) {
        C child = createChild(childStaticRepresentation);

        try {
            URI childUri = new URI(child.getPath());
            return Response.created(childUri).entity(child.getStaticRepresentation()).build();
        } catch (URISyntaxException e) {
            // TODO: what to do with this?
            throw new WebApplicationException(e);
        }
    }

    public C createChild(CS childStaticRepresentation) {
        String name = childStaticRepresentation.getSimpleExtension(AtomServerConstants.NAME);
        if (name == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("You must provide an <as:name> element.").build());
        }
        if (!Pattern.compile("[a-zA-Z0-9-_]{1,32}").matcher(name).matches()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(String.format("Invalid name [%s] in <as:name> element.", name)).build());
        }
        C child;
        lock.writeLock().lock();
        try {
            child = children.get(name);
            if (child != null) {
                throw new WebApplicationException(
                        Response.status(Response.Status.CONFLICT)
                                .entity(String.format("Duplicate error - %s already exists in %s.",
                                                      name, getPath())).build());
            }
            child = createChild(name, childStaticRepresentation);
            children.put(name, child);
        } finally {
            lock.writeLock().unlock();
        }

        return child;
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

    protected void deleteChild(C child) {
        lock.writeLock().lock();
        try {
            children.remove(child.getName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected Collection<String> getChildNames() {
        return children.keySet();
    }

    protected Collection<C> getChildren() {
        return children.values();
    }
}
