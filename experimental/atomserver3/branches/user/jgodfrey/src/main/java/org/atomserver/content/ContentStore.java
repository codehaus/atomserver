package org.atomserver.content;

import java.nio.channels.ReadableByteChannel;

public interface ContentStore {

    Transaction put(EntryKey key, ReadableByteChannel channel)
            throws ContentStoreException;

    ReadableByteChannel get(EntryKey key)
            throws ContentStoreException;

    public interface Transaction {
        void commit() throws ContentStoreException;

        void abort() throws ContentStoreException;

        byte[] digest() throws ContentStoreException;
    }
}
