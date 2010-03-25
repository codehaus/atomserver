package org.atomserver.app;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.log4j.Logger;
import org.atomserver.AtomServer;
import org.atomserver.AtomServerConstants;
import org.atomserver.core.Substrate;
import org.atomserver.ext.Filter;
import org.atomserver.filter.EntryFilter;
import org.atomserver.filter.EntryFilterChain;

import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;

public abstract class BaseResource<S extends ExtensibleElement, P extends ContainerResource> {

    @DELETE
    public Response _delete() {
        // TODO: deal with null parent (DELETE at ROOT is unavailable)
        delete();
        return Response.ok(String.format("%s was deleted successfully.", getPath())).build();
    }


    private static final Logger log = Logger.getLogger(BaseResource.class);

    public abstract S getStaticRepresentation();

    private final P parent;
    private final String name;
    protected Substrate substrate;

    private final List<EntryFilter> entryFilters;

    // TODO: I don't think this way of dealing with updated will work -- locks are acquired at the
    // service level, but this extends to the ROOT.  
    private Date updated;

    protected BaseResource(P parent,
                           String name,
                           Substrate substrate) {
        this.parent = parent;
        this.name = name;
        entryFilters = new ArrayList<EntryFilter>();
        setUpdated(new Date());
        
        this.substrate = substrate;
    }

    public P getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    private void delete() {
        getParent().deleteChild(this);
    }

    public String getPath() {
        return getParent() == null ? "/" :
                String.format("%s%s/", getParent().getPath(), getName());
    }

    public URI getUri() {
        return URI.create(String.format("%s%s", AtomServer.getRootAppUri().toString(), getPath()));
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
        if (getParent() != null) {
            getParent().setUpdated(updated);
        }
    }

    protected void setEntryFilters(List<EntryFilter> entryFilters) {
        this.entryFilters.clear();
        this.entryFilters.addAll(entryFilters);
    }

    protected List<EntryFilter> getEntryFilters() {
        return Collections.unmodifiableList(entryFilters);
    }

    protected EntryFilterChain getEntryFilterChain() {
        // create an EntryFilterChain that iterates through this resources' EntryFilters, then
        // recursively through it's parents' and so on, until we run out of filters, or one of
        // them does not call .doChain() on the EntryFilterChain.
        return new EntryFilterChain() {
            BaseResource res = BaseResource.this;
            Iterator<EntryFilter> iterator = getEntryFilters().iterator();

            EntryFilter next() {
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    res = res.getParent();
                    if (res != null) {
                        iterator = res.getEntryFilters().iterator();
                        return next();
                    } else {
                        return null;
                    }
                }
            }

            public void doChain(Entry entry) {
                EntryFilter filter = next();
                if (filter != null) {
                    filter.filter(entry, this);
                }
            }
        };
    }

    protected void extractEntryFilters(ExtensibleElement collection) {
        List<Filter> list = collection.getExtensions(AtomServerConstants.FILTER);
        for (Filter filter : list) {
            List<EntryFilter> entryFilters = new ArrayList<EntryFilter>();
            try {
                Class<?> filterClass = Class.forName(filter.getClassname());

                Constructor<?> constructor = filterClass.getConstructor(ExtensibleElement.class);

                EntryFilter entryFilter = (EntryFilter) constructor.newInstance(filter);

                // TODO - make it possible to initialize a filter with (A) no config, (B) a DOM element, or (C) a raw XML string, too

                entryFilters.add(entryFilter);
            } catch (Exception e) {
                e.printStackTrace(); // TODO: handle
            }
            setEntryFilters(entryFilters);
        }
    }

    protected Substrate getSubstrate() {
        return substrate;
    }


    public static String toHexString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return builder.toString();
    }

    protected void sync(final Runnable runnable) {
        sync(new Callable<Object>() {
            public Object call() throws Exception {
                runnable.run();
                return null;
            }
        });
    }

    protected <T> T sync(Callable<T> callable) {
        try {
            return substrate.sync(getPath(), callable);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
