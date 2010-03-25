package org.atomserver.core;

import org.apache.abdera.model.Service;
import org.apache.commons.codec.digest.DigestUtils;
import org.atomserver.AtomServerConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

//@Component
public class MemSubstrate implements Substrate {

    private final Map<String, ServiceMetadata> serviceMetadata =
            new HashMap<String, ServiceMetadata>();
    private final Map<String, AtomicLong> timestampMap = new HashMap<String, AtomicLong>();
    private final Map<String, Index> indices = new HashMap<String, Index>();
    private final Map<String, KeyValueStore<Long, EntryTuple>> entryByTimestampStores =
            new HashMap<String, KeyValueStore<Long, EntryTuple>>();
    private final Map<String, KeyValueStore<String, EntryTuple>> entryByIdStores =
            new HashMap<String, KeyValueStore<String, EntryTuple>>();

    public Collection<ServiceMetadata> getAllServices() {
        return serviceMetadata.values();
    }

    private static class SimpleServiceMetadata implements ServiceMetadata {
        final String name;
        final Service service;
        final byte[] digest;

        private SimpleServiceMetadata(Service service) {
            this.name = service.getSimpleExtension(AtomServerConstants.NAME);
            this.service = service;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                service.writeTo(os);
                os.close();
                this.digest = DigestUtils.md5(os.toByteArray());
            } catch (IOException e) {
                throw new IllegalStateException(e); // TODO: is this good?
            }
        }

        public String getName() {
            return name;
        }

        public Service getService() {
            return service;
        }

        public byte[] getDigest() {
            return digest;
        }
    }

    public ServiceMetadata getService(String name, byte[] digest) {
        ServiceMetadata service = this.serviceMetadata.get(name);
        return service == null ? null :
                digest == null ? service :
                        Arrays.equals(digest, service.getDigest()) ? null : service;
        // TODO: how to handle removed service?
    }

    public ServiceMetadata putService(Service service) {
        SimpleServiceMetadata metadata = new SimpleServiceMetadata(service);
        this.serviceMetadata.put(metadata.getName(), metadata);
        return metadata;
    }

    public <T> T sync(String lockName, Callable<T> callable) throws Exception {
        T returnValue;
        synchronized (lockName.intern()) {
            returnValue = callable.call();
        }
        return returnValue;
    }

    public synchronized long getNextTimestamp(String key) {
        AtomicLong timestamp = timestampMap.get(key);
        if (timestamp == null) {
            timestampMap.put(key, timestamp = new AtomicLong(1L));
        }
        return timestamp.getAndIncrement();
    }

    public synchronized Index getIndex(String key) {
        Index index = indices.get(key);
        if (index == null) {
            indices.put(key, index = new Index() {
                SortedSet<Long> _index = new TreeSet<Long>();

                public void add(Long value) {
                    _index.add(value);
                }

                public void remove(Long value) {
                    _index.remove(value);
                }

                public Iterable<Long> tail(Long from) {
                    return _index.tailSet(from);
                }
            });
        }
        return index;
    }

    class MemEntryStore<K, V> extends HashMap<K, V> implements KeyValueStore<K, V> {
    }

    public KeyValueStore<Long, EntryTuple> getEntriesByTimestamp(String key) {
        KeyValueStore<Long, EntryTuple> store = entryByTimestampStores.get(key);
        if (store == null) {
            entryByTimestampStores.put(key, store = new MemEntryStore<Long, EntryTuple>());
        }
        return store;
    }

    public KeyValueStore<String, EntryTuple> getEntriesById(String key) {
        KeyValueStore<String, EntryTuple> store = entryByIdStores.get(key);
        if (store == null) {
            entryByIdStores.put(key, store = new MemEntryStore<String, EntryTuple>());
        }
        return store;
    }
}
