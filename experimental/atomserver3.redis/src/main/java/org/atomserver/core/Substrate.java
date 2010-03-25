package org.atomserver.core;

import org.apache.abdera.model.Service;

import java.util.Collection;
import java.util.concurrent.Callable;

public interface Substrate {

    interface ServiceMetadata {
        Service getService();
        byte[] getDigest();
        String getName();
    }

    Collection<ServiceMetadata> getAllServices();

    ServiceMetadata getService(String name, byte[] digest);

    ServiceMetadata putService(Service service);

    public <T> T sync(String lockName, Callable<T> callable) throws Exception;

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
