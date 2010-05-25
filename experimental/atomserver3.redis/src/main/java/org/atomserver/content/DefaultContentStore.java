package org.atomserver.content;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

public class DefaultContentStore implements ContentStore {

    public Transaction put(final EntryKey key, ReadableByteChannel channel)
            throws ContentStoreException {
        final byte[] bytes = ContentUtils.toBytes(channel);
        return new Transaction() {
            public void commit() {
                contentMap.put(key, bytes);
            }

            public void abort() {
                // do nothing...
            }

            public byte[] digest() {
                return DigestUtils.md5(bytes);
            }
        };
    }

    public ReadableByteChannel get(EntryKey key) throws ContentStoreException {
        byte[] contentBytes = contentMap.get(key);
        return contentBytes == null ? null : ContentUtils.toChannel(contentBytes);
    }

    private final Map<EntryKey, byte[]> contentMap = new HashMap<EntryKey, byte[]>();
}
