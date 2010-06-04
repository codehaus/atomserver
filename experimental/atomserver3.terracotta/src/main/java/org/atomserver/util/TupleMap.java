package org.atomserver.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TupleMap<K, V> {

    private final Map<Key, V> map;


    public TupleMap() {
        this(new HashMap<Key, V>());
    }

    public TupleMap(Map<Key, V> map) {
        this.map = map;
    }

    public class Key {
        Object[] vals;

        private Key(K... vals) {
            this.vals = vals;
        }

        public int hashCode() {
            return Arrays.hashCode(this.vals);
        }

        public boolean equals(Object obj) {
            return obj != null &&
                   obj.getClass().equals(Key.class) &&
                   Arrays.equals(this.vals, ((Key) obj).vals);
        }

        public V get() {
            return map.get(this);
        }

        public V put(V v) {
            return map.put(this, v);
        }

        public V remove() {
            return map.remove(this);
        }
    }

    public Key atKey(K... vals) {
        return new Key(vals);
    }

    public V get(K... vals) {
        return atKey(vals).get();
    }
}
