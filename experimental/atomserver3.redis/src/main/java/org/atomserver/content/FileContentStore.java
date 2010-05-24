package org.atomserver.content;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileContentStore implements ContentStore {
    private File root;

    public FileContentStore(File root) {
        this.root = root;
        this.root.mkdirs();
    }

    public Transaction put(final EntryKey key, ReadableByteChannel channel) throws ContentStoreException {
        try {
            final File tempFile = File.createTempFile("entry" + key.entryId, ".temp", root);
            DigestOutputStream os = new DigestOutputStream(
                    new FileOutputStream(tempFile), MessageDigest.getInstance("MD5"));

            IOUtils.copy(Channels.newInputStream(channel), os);
            os.close();
            final byte[] digest = os.getMessageDigest().digest();

            return new Transaction() {
                public void commit() {
                    final File file = getFile(key);
                    file.getParentFile().mkdirs();
                    tempFile.renameTo(file);
                }

                public void abort() {
                    tempFile.delete();
                }

                public byte[] digest() {
                    return digest;
                }
            };


        } catch (FileNotFoundException e) {
            throw new ContentStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new ContentStoreException(e);
        }
    }

    public ReadableByteChannel get(EntryKey key) throws ContentStoreException {
        try {
            return ContentUtils.toChannel(FileUtils.readFileToByteArray(getFile(key)));
        } catch (IOException e) {
            throw new ContentStoreException(e);
        }
    }

    private File getFile(EntryKey key) {
        return new File(root,
                String.format("%s/%s/%s/%s",
                        key.service, key.workspace, key.collection, key.entryId));
    }
}
