package org.atomserver.util;

import java.util.*;

public abstract class CollectionMap<K, V> extends AbstractMap<K, V> {

    protected abstract K extractKey(V v);


    private Collection<V> collection;

    protected CollectionMap(Collection<V> collection) {
        this.collection = Collections.unmodifiableCollection(collection);
    }

    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {

            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    Iterator<V> valIterator = collection.iterator();

                    V current;

                    Entry<K, V> entry = new Entry<K, V>() {
                        public K getKey() {
                            return extractKey(current);
                        }

                        public V getValue() {
                            return current;
                        }

                        public V setValue(V value) {
                            throw new UnsupportedOperationException(
                                    "CollectionMap instances are immutable");
                        }
                    };

                    public boolean hasNext() {
                        return valIterator.hasNext();
                    }

                    public Entry<K, V> next() {
                        current = valIterator.next();
                        return entry;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException(
                                "CollectionMap instances are immutable");
                    }
                };
            }

            public int size() {
                return collection.size();
            }
        };
    }
}
