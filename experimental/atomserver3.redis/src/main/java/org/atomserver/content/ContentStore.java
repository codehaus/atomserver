package org.atomserver.content;

import java.nio.channels.ReadableByteChannel;

public interface ContentStore {

    Transaction put(EntryKey key, String type, ReadableByteChannel channel)
            throws ContentStoreException;

    EntryContent get(EntryKey key)
            throws ContentStoreException;

    public interface Transaction {
        void commit() throws ContentStoreException;

        void abort() throws ContentStoreException;

        byte[] digest() throws ContentStoreException;
    }
}
