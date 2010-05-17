package org.atomserver.core;

import org.jredis.JRedis;
import org.jredis.RedisException;
import org.jredis.ri.alphazero.JRedisClient;
import org.jredis.ri.alphazero.support.Convert;
import org.jredis.ri.alphazero.support.Opts;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.Callable;

//@Component
public class RedisSubstrate implements Substrate {
    private static final long INTERNAL_PAGE_SIZE = 5L; // TODO: should be bigger than 5, probably 2x default page size

    private JRedis redis;

    private final Map<String, Index> indices = new HashMap<String, Index>();
    private final Map<String, KeyValueStore<Long, EntryTuple>> entryByTimestampStores =
            new HashMap<String, KeyValueStore<Long, EntryTuple>>();
    private final Map<String, KeyValueStore<String, EntryTuple>> entryByIdStores =
            new HashMap<String, KeyValueStore<String, EntryTuple>>();

    private String host = "localhost";
    private int port = 6379;
    private int db = 0;

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDb(int db) {
        this.db = db;
    }

    @PostConstruct
    public void init() {
        this.redis = new JRedisClient(host, port, null /* TODO: password */, db);
    }

    public <T> T sync(String lockName, Callable<T> callable) throws Exception {
        try {
            long now = System.currentTimeMillis();
            // see if we can get the lock outright
            boolean acquired = redis.setnx(lockName, now + 1001L);
            while (!acquired) {
                // if not, let's see if the lock has expired
                Long timestamp = Convert.toLong(redis.get(lockName));
                if (timestamp < now) {
                    // if it HAS expired, atomically GETSET to acquire it
                    timestamp = Convert.toLong(redis.getset(lockName, now + 1001L));
                    // make damn sure that someone else didn't acquire in the meantime
                    acquired = timestamp < now;
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
                redis.del(lockName);
            } catch (RedisException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
        }
        return returnValue;
    }

    private String timestampKey(String key) {
        return String.format("TS::%s", key);
    }

    private String indexKey(String key) {
        return String.format("IX::%s", key);
    }

    public synchronized long getNextTimestamp(String key) {
        try {
            redis.setnx(timestampKey(key), 0L); // TODO: can we avoid making this call each time?
            return redis.incr(timestampKey(key));
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
                        boolean b = redis.zadd(indexKey(key), value.doubleValue(), value);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }

                public void remove(Long value) {
                    try {
                        redis.zrem(indexKey(key), value);
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
                                            List<byte[]> response = redis.zrangebyscore(
                                                    indexKey(key),
                                                    current == null ? from.doubleValue() : current + 1,
                                                    1000L /*TODO:MAX*/,
                                                    Opts.LIMIT(0L, INTERNAL_PAGE_SIZE));
                                            lastPage = response.size() < INTERNAL_PAGE_SIZE;
                                            page = response.iterator(); // TODO: max index
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
                redis.set(mapKey(key), value);
            } catch (RedisException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
            return get(key);
        }

        public EntryTuple get(T key) {
            try {
                byte[] bytes = redis.get(mapKey(key));
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
                redis.del(mapKey(key));
            } catch (RedisException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
            return value;
        }

        private String mapKey(T key) {
            return String.format("%s::%s::%s", ns, type, key);
        }
    }
}