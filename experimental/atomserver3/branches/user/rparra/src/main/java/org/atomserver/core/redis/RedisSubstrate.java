package org.atomserver.core.redis;

import org.apache.log4j.Logger;
import org.atomserver.core.EntryTuple;
import org.atomserver.core.Substrate;
import org.atomserver.sharding.Distribution;
import org.jredis.RedisException;
import org.jredis.ri.alphazero.support.Convert;
import org.jredis.ri.alphazero.support.Opts;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.Callable;

//@Component
public class RedisSubstrate implements Substrate {

    private final Logger log = Logger.getLogger(RedisSubstrate.class);

    // TODO: should be bigger than 5, probably 2x default page size
    // TODO: perhaps should dynamically compute this based on the "complexity" of any category query
    private static final long INTERNAL_PAGE_SIZE = 5L;

    private DistributedRedisClient redis;

    private final Map<String, Index> indices = new HashMap<String, Index>();
    private final Map<String, KeyValueStore<Long, EntryTuple>> entryByTimestampStores =
            new HashMap<String, KeyValueStore<Long, EntryTuple>>();
    private final Map<String, KeyValueStore<String, EntryTuple>> entryByIdStores =
            new HashMap<String, KeyValueStore<String, EntryTuple>>();

    public void setDistribution(Distribution distribution) {
        redis = new DistributedRedisClient(distribution);
    }

    @PostConstruct
    public void init() {
    }

    public <T> T sync(String lockName, Callable<T> callable) throws Exception {
        try {
            long now = System.currentTimeMillis();
            // see if we can at the lock outright
            boolean acquired = redis.at(lockName).setnx(lockName, now + 1001L);
            while (!acquired) {
                // if not, let's see if the lock has expired
                Long timestamp = null;
                try {
                    Convert.toLong(redis.at(lockName).get(lockName));
                } catch (Exception e) {
                    // TODO: think about whether we need to do something here... certainly log
                }
                if (timestamp == null || timestamp < now) {
                    // if it HAS expired, atomically GETSET to acquire it
                    byte[] timestampBytes = redis.at(lockName).getset(lockName, now + 1001L);
                    timestamp = timestampBytes == null ? null : Convert.toLong(timestampBytes);
                    // make damn sure that someone else didn't acquire in the meantime
                    acquired = timestamp == null || timestamp < now;
                }
                if (!acquired) {
                    // if, after all that, we still haven't acquired the lock, sleep
                    // for a bit before retrying
                    Thread.sleep(500L);
                }
            }
        } catch (RedisException e) {
            throw new IllegalStateException(e); // TODO: handle
        } catch (InterruptedException e) {
            throw new IllegalStateException(e); // TODO: handle
        }
        T returnValue;
        try {
            returnValue = callable.call();
        } finally {
            try {
                redis.at(lockName).del(lockName);
            } catch (RedisException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
        }
        return returnValue;
    }

    private String timestampKey(String key) {
        return String.format("TS::%s", key).replaceAll("\\s", "_");
    }

    private String indexKey(String key) {
        return String.format("IX::%s", key).replaceAll("\\s", "_");
    }

    public synchronized long getNextTimestamp(String key) {
        try {
            redis.at(timestampKey(key)).setnx(timestampKey(key), 0L); // TODO: can we avoid making this call each time?
            return redis.at(timestampKey(key)).incr(timestampKey(key));
        } catch (RedisException e) {
            throw new IllegalStateException(e); // TODO: handle
        }
    }

    public synchronized Index getIndex(final String key) {
        Index index = indices.get(key);
        if (index == null) {
            indices.put(key, index = new Index() {

                public void add(Long value) {
                    try {
                        redis.at(indexKey(key)).zadd(indexKey(key), value.doubleValue(), value);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }

                public void remove(Long value) {
                    try {
                        String indexKey = indexKey(key);
                        log.debug(String.format("index key = [%s]", indexKey));
                        redis.at(indexKey).zrem(indexKey, value);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }

                public Iterable<Long> tail(final Long from) {
                    return new Iterable<Long>() {
                        public Iterator<Long> iterator() {
                            return new Iterator<Long>() {
                                Iterator<byte[]> page = null;
                                Long current = null;
                                boolean lastPage = false;

                                {
                                    // start up by doing a lookahead
                                    lookahead();
                                }

                                private void lookahead() {
                                    if ((page == null || !page.hasNext()) && !lastPage) {
                                        try {
                                            List<byte[]> response = redis.at(indexKey(key)).zrangebyscore(
                                                    indexKey(key),
                                                    current == null ? from.doubleValue() : current + 1,
                                                    10000000L /*TODO:MAX*/,
                                                    Opts.LIMIT(0L, INTERNAL_PAGE_SIZE));
                                            if (response == null) {
                                                lastPage = true;
                                                page = Collections.<byte[]>emptySet().iterator();
                                            } else {
                                                lastPage = response.size() < INTERNAL_PAGE_SIZE;
                                                page = response.iterator(); // TODO: max index
                                            }
                                        } catch (RedisException e) {
                                            throw new IllegalStateException(e);
                                        }
                                    }
                                    current = !page.hasNext() ? null : Convert.toLong(page.next());
                                }

                                public boolean hasNext() {
                                    return current != null;
                                }

                                public Long next() {
                                    if (!hasNext()) {
                                        throw new NoSuchElementException(); // TODO: message
                                    }
                                    Long val = current;
                                    lookahead();
                                    return val;
                                }

                                public void remove() {
                                    throw new UnsupportedOperationException(); // TODO: message
                                }
                            };
                        }
                    };
                }

                public Long max() {
                    try {
                        List<byte[]> maxList = redis.at(indexKey(key)).zrevrange(indexKey(key), 0, 0);
                        return maxList == null || maxList.isEmpty() ?
                                0L :
                                Convert.toLong(maxList.get(0));
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }
            });
        }
        return index;
    }

    public KeyValueStore<Long, EntryTuple> getEntriesByTimestamp(final String key) {
        KeyValueStore<Long, EntryTuple> store = entryByTimestampStores.get(key);
        if (store == null) {
            entryByTimestampStores.put(key, store = new RedisTupleStore<Long>("TS", key));
        }
        return store;
    }

    public KeyValueStore<String, EntryTuple> getEntriesById(final String key) {
        KeyValueStore<String, EntryTuple> store = entryByIdStores.get(key);
        if (store == null) {
            entryByIdStores.put(key, store = new RedisTupleStore<String>("ID", key));
        }
        return store;
    }

    private class RedisTupleStore<T> implements KeyValueStore<T, EntryTuple> {
        final String type;
        final String ns;

        public RedisTupleStore(String type, String ns) {
            this.type = type;
            this.ns = ns;
        }

        public EntryTuple put(T key, EntryTuple value) {
            try {
                redis.at(mapKey(key)).set(mapKey(key), value);
            } catch (RedisException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
            return get(key);
        }

        public EntryTuple get(T key) {
            try {
                byte[] bytes = redis.at(mapKey(key)).get(mapKey(key));
                if (bytes == null) return null;
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                return (EntryTuple) ois.readObject();
            } catch (Exception e) {
                throw new IllegalStateException(e); // TODO: handle
            }
        }

        public EntryTuple remove(T key) {
            EntryTuple value = get(key);
            try {
                redis.at(mapKey(key)).del(mapKey(key));
            } catch (RedisException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
            return value;
        }

        private String mapKey(T key) {
            return String.format("%s::%s::%s", ns, type, key).replaceAll("\\s", "_"); // TODO: proper grammar for values
        }
    }
}