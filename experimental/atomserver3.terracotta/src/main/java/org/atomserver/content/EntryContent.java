package org.atomserver.content;

import java.nio.channels.ReadableByteChannel;

public class EntryContent {
    private final String type;
    private final ReadableByteChannel channel;

    public EntryContent(String type, ReadableByteChannel channel) {
        this.type = type;
        this.channel = channel;
    }

    public String getType() {
        return type;
    }

    public ReadableByteChannel getChannel() {
        return channel;
    }
}
