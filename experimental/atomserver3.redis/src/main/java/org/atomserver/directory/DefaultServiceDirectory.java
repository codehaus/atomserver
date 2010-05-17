package org.atomserver.directory;

import org.apache.abdera.model.*;
import org.apache.abdera.model.Collection;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.stax.FOMParser;
import org.atomserver.AtomServerConstants;
import org.atomserver.ext.Filter;
import org.atomserver.filter.EntryFilter;
import org.atomserver.filter.EntryFilterChain;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import static org.atomserver.AtomServerConstants.UPDATED;

@Component()
public class DefaultServiceDirectory implements ServiceDirectory {

    private static final FileFilter XML_FILE_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().endsWith(".xml");
        }
    };

    private final Parser parser = new FOMParser();

    private Map<String, AtomServerService> memStorage = new HashMap<String, AtomServerService>();
    private File diskStorage;
    private Map<String, EntryFilterChain> entryFilterChains =
            new HashMap<String, EntryFilterChain>();

    public void setDiskStorage(File diskStorage) {
        this.diskStorage = diskStorage;
    }

    @PostConstruct 
    public synchronized void refresh() {
        // TODO: configure the disk storage above - switch to Spring 3.0?
        setDiskStorage(new File("target/service-directory"));
        diskStorage.mkdirs();


        Map<String, AtomServerService> memStorage = new HashMap<String, AtomServerService>();
        for (File file : diskStorage.listFiles(XML_FILE_FILTER)) {
            try {
                FileInputStream in = new FileInputStream(file);
                memStorage.put(
                        file.getName().replaceFirst("\\.xml$", ""),
                        new SimpleAtomServerService(parser.<Service>parse(in).getRoot()));
                in.close();
            } catch (IOException e) {
                throw new IllegalStateException(e); // TODO: handle properly
            }
        }
        this.memStorage = memStorage;
    }

    public synchronized AtomServerService get(String name) {
        return memStorage.get(name);
    }

    public synchronized Service remove(String name) {
        File realFile = new File(diskStorage, String.format("%s.xml", name));
        realFile.delete();
        AtomServerService service = memStorage.remove(name);
        return service == null ? null : service.getService();
    }

    public synchronized void put(String name, Service service) {
        try {
            service.addSimpleExtension(UPDATED, AtomDate.format(new Date()));
            File realFile = new File(diskStorage, String.format("%s.xml", name));
            if (!realFile.exists() || (realFile.isFile() && realFile.canWrite())) { // TODO: error checking and logging
                File tmpFile = File.createTempFile(name, "xml", diskStorage);
                FileOutputStream out = new FileOutputStream(tmpFile);
                service.writeTo(out);
                out.close();
                memStorage.put(name, new SimpleAtomServerService(service));
                tmpFile.renameTo(realFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e); // TODO: handle properly
        }
    }

    public synchronized Iterable<Service> list() {
        HashSet<Service> services = new HashSet<Service>();
        for (AtomServerService atomServerService : memStorage.values()) {
            services.add(atomServerService.getService());
        }
        return Collections.unmodifiableSet(services);
    }

    private class SimpleAtomServerService implements AtomServerService {
        final Service service;
        final Map<String, Map<String, List<EntryFilter>>> entryFilterChains;

        private SimpleAtomServerService(Service service) {
            this.service = service;
            this.entryFilterChains = new HashMap<String, Map<String, List<EntryFilter>>>();
            List<EntryFilter> serviceFilters = extractEntryFilters(service);
            for (Workspace workspace : service.getWorkspaces()) {
                List<EntryFilter> workspaceFilters = extractEntryFilters(workspace);
                HashMap<String, List<EntryFilter>> workspaceFilterMap =
                        new HashMap<String, List<EntryFilter>>();
                this.entryFilterChains.put(workspace.getTitle(), workspaceFilterMap);
                for (Collection collection : workspace.getCollections()) {
                    List<EntryFilter> collectionFilters = extractEntryFilters(collection);
                    List<EntryFilter> entryFilters = new ArrayList<EntryFilter>();
                    entryFilters.addAll(collectionFilters);
                    entryFilters.addAll(workspaceFilters);
                    entryFilters.addAll(serviceFilters);
                    workspaceFilterMap.put(collection.getTitle(), entryFilters);
                }
            }
        }

        public Service getService() {
            return service;
        }

        public EntryFilterChain getEntryFilterChain(final String workspaceId, final String collectionId) {
            return new EntryFilterChain() {
                Iterator<EntryFilter> iterator = entryFilterChains.get(workspaceId).get(collectionId).iterator();
                public void doChain(Entry entry) {
                    if (iterator.hasNext()) {
                        iterator.next().filter(entry, this);
                    }
                }
            };
        }
    }

    private static List<EntryFilter> extractEntryFilters(ExtensibleElement element) {
        List<Filter> list = element.getExtensions(AtomServerConstants.FILTER);
        List<EntryFilter> entryFilters = new ArrayList<EntryFilter>();
        for (Filter filter : list) {
            try {
                Class<?> filterClass = Class.forName(filter.getClassname());
                Constructor<?> constructor = filterClass.getConstructor(ExtensibleElement.class);
                EntryFilter entryFilter = (EntryFilter) constructor.newInstance(filter);
                // TODO - make it possible to initialize a filter with (A) no config, (B) a DOM element, or (C) a raw XML string, too
                entryFilters.add(entryFilter);
            } catch (Exception e) {
                e.printStackTrace(); // TODO: handle
            }
        }
        return entryFilters;
    }

}
