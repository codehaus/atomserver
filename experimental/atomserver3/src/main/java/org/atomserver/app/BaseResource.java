package org.atomserver.app;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.atomserver.AtomServerConstants;
import org.atomserver.ext.Filter;
import org.atomserver.filter.EntryFilter;
import org.atomserver.filter.EntryFilterChain;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BaseResource<S extends ExtensibleElement, P extends ContainerResource> {

    public abstract S getStaticRepresentation();

    private final P parent;
    private final String name;

    private final List<EntryFilter> entryFilters;

    public final ReentrantReadWriteLock lock;

    protected BaseResource(P parent,
                           String name) {
        this.parent = parent;
        this.name = name;
        entryFilters = new ArrayList<EntryFilter>();
        this.lock = new ReentrantReadWriteLock();
    }

    public P getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return getParent() == null ? "/" :
               String.format("%s%s/", getParent().getPath(), getName());
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
    
}
