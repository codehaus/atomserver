package org.atomserver.content;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultContentStore implements ContentStore {

    public Transaction put(final EntryKey key, final String type, ReadableByteChannel channel)
            throws ContentStoreException {
        final byte[] bytes = ContentUtils.toBytes(channel);
        return new Transaction() {
            public void commit() {
                contentMap.put(key, new ContentBytes(type, bytes));
            }

            public void abort() {
                // do nothing...
            }

            public String etag() {
                return DigestUtils.md5Hex(bytes);
            }
        };
    }

    public EntryContent get(EntryKey key) throws ContentStoreException {
        ContentBytes contentBytes;
        contentBytes = contentMap.get(key);
        return contentBytes == null ? null :
               new EntryContent(contentBytes.type, ContentUtils.toChannel(contentBytes.bytes));
    }

    private final Map<EntryKey, ContentBytes> contentMap =
            new HashMap<EntryKey, ContentBytes>();

    private class ContentBytes {
        String type;
        byte[] bytes;

        private ContentBytes(String type, byte[] bytes) {
            this.type = type;
            this.bytes = bytes;
        }
    }
}
