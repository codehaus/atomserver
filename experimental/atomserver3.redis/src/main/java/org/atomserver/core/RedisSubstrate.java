package org.atomserver.core;

import org.apache.abdera.model.Service;
import org.apache.commons.codec.digest.DigestUtils;
import org.atomserver.AtomServerConstants;
import org.jredis.ClientRuntimeException;
import org.jredis.ProviderException;
import org.jredis.RedisException;
import org.jredis.connector.ConnectionSpec;
import org.jredis.protocol.Command;
import org.jredis.protocol.MultiBulkResponse;
import org.jredis.ri.alphazero.JRedisClient;
import org.jredis.ri.alphazero.support.Convert;
import org.jredis.ri.alphazero.support.DefaultCodec;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

//@Component
public class RedisSubstrate implements Substrate {

    static class ExtJRedisClient extends JRedisClient {
        ExtJRedisClient(String host, int port, int db) throws ClientRuntimeException {
            super(host, port, null, db);
        }

        public List<byte[]> zrangebyscore(String key,
                                          double minScore,
                                          double maxScore,
                                          long offset,
                                          long count) throws RedisException {
            byte[] keybytes = null;
            byte[] bytes = new byte[0];
            try {
                bytes = key.getBytes(DefaultCodec.SUPPORTED_CHARSET_NAME);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e); // TODO: handle
            }
            if ((keybytes = bytes) == null)
                throw new IllegalArgumentException("invalid key => [" + key + "]");

            byte[] fromBytes = Convert.toBytes(minScore);
            byte[] toBytes = Convert.toBytes(maxScore);

            List<byte[]> multiBulkData = null;
            try {
                MultiBulkResponse MultiBulkResponse = (MultiBulkResponse) this.serviceRequest(
                        Command.ZRANGEBYSCORE, keybytes, fromBytes, toBytes,
                        (" LIMIT " + offset + count).getBytes());
                multiBulkData = MultiBulkResponse.getMultiBulkData();
            }
            catch (ClassCastException e) {
                throw new ProviderException("Expecting a MultiBulkResponse here => " + e.getLocalizedMessage(), e);
            }
            return multiBulkData;
        }
    }

    ExtJRedisClient redis;

    private final Map<String, ServiceMetadata> serviceMetadata =
            new HashMap<String, ServiceMetadata>();
    private final Map<String, Lock> locks = new HashMap<String, Lock>();
    private final Map<String, AtomicLong> timestampMap = new HashMap<String, AtomicLong>();
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
        this.redis = new ExtJRedisClient(host, port, db);
    }

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

    public Lock getLock(String key) {
        final String lockName = "LOCK:" + key;
        Lock lock = locks.get(key);
        if (lock == null) {
            // This lock is implemented according to the suggestions at:
            //  http://code.google.com/p/redis/wiki/SetnxCommand
            locks.put(key, lock = new Lock() {
                public void lock() {
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
                }

                public void unlock() {
                    try {
                        redis.del(lockName);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }
            });
        }
        return lock;
    }

    public synchronized long getNextTimestamp(String key) {
        try {
            redis.setnx("INCR:" + key, 0L); // TODO: can we avoid making this call each time?
            return redis.incr("INCR:" + key);
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
                        boolean b = redis.zadd("IDX::" + key, value.doubleValue(), value);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }

                public void remove(Long value) {
                    try {
                        redis.zrem("IDX::" + key, value);
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
                                            List<byte[]> response = redis.zrangebyscore("IDX::" + key, from.doubleValue(), 1000L /*TODO:MAX*/, 0L, 101L);
                                            lastPage = response.size() < 101;
                                            page = response.iterator(); // TODO: max index
                                        } catch (RedisException e) {
                                            throw new IllegalStateException(e);
                                        }
                                    }
                                    current = !page.hasNext() ? null :
                                            Convert.toLong(page.next());
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
            entryByTimestampStores.put(key, store = new KeyValueStore<Long, EntryTuple>() {
                String ns = key;
                @Override
                public EntryTuple put(Long key, EntryTuple value) {
                    try {
                        String key1 = ns + ":TS:" + key;
                        redis.set(key1, value);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                    return get(key);
                }

                public EntryTuple get(Long key) {
                    try {
                        byte[] bytes = redis.get(ns + ":TS:" + key);
                        if (bytes == null) return null;
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                        return (EntryTuple) ois.readObject();
                    } catch (Exception e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }

                public EntryTuple remove(Long key) {
                    EntryTuple value = get(key);
                    try {
                        redis.del(ns + ":TS:" + key);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                    return value;
                }
            });
        }
        return store;
    }

    public KeyValueStore<String, EntryTuple> getEntriesById(final String key) {
        KeyValueStore<String, EntryTuple> store = entryByIdStores.get(key);
        if (store == null) {
            entryByIdStores.put(key, store = new KeyValueStore<String, EntryTuple>() {
                String ns = key;
                public EntryTuple put(String key, EntryTuple value) {
                    try {
                        redis.set(ns + ":KEY:" + key, value);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                    return get(key);
                }

                public EntryTuple get(String key) {
                    try {
                        byte[] bytes = redis.get(ns + ":KEY:" + key);
                        if (bytes == null) return null;
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                        return (EntryTuple) ois.readObject();
                    } catch (Exception e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                }

                public EntryTuple remove(String key) {
                    EntryTuple value = get(key);
                    try {
                        redis.del(ns + ":KEY:" + key);
                    } catch (RedisException e) {
                        throw new IllegalStateException(e); // TODO: handle
                    }
                    return value;
                }
            });
        }
        return store;
    }
}