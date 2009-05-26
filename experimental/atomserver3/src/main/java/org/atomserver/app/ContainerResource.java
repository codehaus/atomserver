package org.atomserver.app;

import org.apache.abdera.model.ExtensibleElement;
import org.atomserver.AtomServerConstants;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import static java.lang.String.format;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

public abstract class ContainerResource<
        S extends ExtensibleElement,
        CS extends ExtensibleElement,
        P extends ContainerResource,
        C extends BaseResource<CS, ? extends ContainerResource>>
        extends BaseResource<S, P> {

    @POST
    public Response postChild(CS childStaticRepresentation) {
        C child = createChild(childStaticRepresentation);

        try {
            URI childUri = new URI(child.getName());
            return Response.created(childUri).entity(child.getStaticRepresentation()).build();
        } catch (URISyntaxException e) {
            // TODO: what to do with this?
            throw new WebApplicationException(e);
        }
    }

    @Path("/{name : [^\\$][^/]*}")
    public C getChild(@PathParam("name") String name) {
        C child = children.get(name);
        if (child == null) {
            throw new NotFoundException(
                    format("%s not found within %s.", name, getPath()));
        }
        return child;
    }


    protected abstract C createChild(String name,
                                     CS staticRepresentation);

    protected abstract void validateChildStaticRepresentation(CS childStaticRepresentation);


    private final SortedMap<String, C> children = new TreeMap<String, C>();

    protected ContainerResource(P parent,
                                String name) {
        super(parent, name);
    }

    protected static void validateName(String name) {
        if (!Pattern.compile("[a-zA-Z0-9-_]{1,32}").matcher(name).matches()) {
            throw new BadRequestException(
                    format("Invalid name [%s] in <as:name> element.", name));
        }
    }

    protected String getChildName(CS childStaticRepresentation) {
        return childStaticRepresentation.getSimpleExtension(AtomServerConstants.NAME);
    }

    public C createChild(CS childStaticRepresentation) {
        validateChildStaticRepresentation(childStaticRepresentation);
        String name = getChildName(childStaticRepresentation);

        C child;
        lock.writeLock().lock();
        try {
            child = children.get(name);
            if (child != null) {
                throw new DuplicateException(
                        format("Duplicate error - %s already exists in %s.",
                               name, getPath()));
            }
            child = createChild(name, childStaticRepresentation);
            children.put(name, child);
        } finally {
            lock.writeLock().unlock();
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
