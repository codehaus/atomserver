package org.atomserver.core;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

//@Component
public class MemSubstrate implements Substrate {

    private final Map<String, AtomicLong> timestampMap = new HashMap<String, AtomicLong>();
    private final Map<String, Index> indices = new HashMap<String, Index>();
    private final Map<String, KeyValueStore<Long, EntryTuple>> entryByTimestampStores =
            new HashMap<String, KeyValueStore<Long, EntryTuple>>();
    private final Map<String, KeyValueStore<String, EntryTuple>> entryByIdStores =
            new HashMap<String, KeyValueStore<String, EntryTuple>>();

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

                public Long max() {
                    return _index.last();
                }
            });
        }
        return index;
    }

    class MemEntryStore<K, V> extends HashMap<K, V> implements KeyValueStore<K, V> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8677729853042334580L;
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
