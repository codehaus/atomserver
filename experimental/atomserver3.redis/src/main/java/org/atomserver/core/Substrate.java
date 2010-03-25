package org.atomserver.core;

import org.apache.abdera.model.Service;

import java.util.Collection;

public interface Substrate {

    interface ServiceMetadata {
        Service getService();
        byte[] getDigest();
        String getName();
    }

    Collection<ServiceMetadata> getAllServices();

    ServiceMetadata getService(String name, byte[] digest);

    ServiceMetadata putService(Service service);

    interface Lock {
        void lock();
        void unlock();
    }

    Lock getLock(String name);

    long getNextTimestamp(String key);

    interface Index {
        void add(Long value);

        void remove(Long value);

        Iterable<Long> tail(Long from);
    }

    Index getIndex(String key);

    interface KeyValueStore<K, V> {
        V put(K key, V value);

        V get(K key);

        V remove(K key);
    }

    KeyValueStore<Long, EntryTuple> getEntriesByTimestamp(String key);

    KeyValueStore<String, EntryTuple> getEntriesById(String key);
}
