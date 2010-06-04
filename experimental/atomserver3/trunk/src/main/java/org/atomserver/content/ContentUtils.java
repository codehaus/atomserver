package org.atomserver.content;

import org.apache.commons.io.IOUtils;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

public class ContentUtils {

    public static ReadableByteChannel toChannel(byte[] content) {
        return Channels.newChannel(new ByteArrayInputStream(content));
    }

    public static ReadableByteChannel toChannel(String content) {
        return toChannel(content.getBytes());
    }

    public static ReadableByteChannel toChannel(URI uri) throws ContentStoreException {
        try {
            return Channels.newChannel(uri.toURL().openStream());
        } catch (IOException e) {
            throw new ContentStoreException(e);
        }
    }

    public static byte[] toBytes(ReadableByteChannel channel) throws ContentStoreException {
        try {
            return IOUtils.toByteArray(Channels.newInputStream(channel));
        } catch (IOException e) {
            throw new ContentStoreException(e);
        }
    }

    public static String toString(ReadableByteChannel channel) throws ContentStoreException {
        return new String(toBytes(channel));
    }
}
