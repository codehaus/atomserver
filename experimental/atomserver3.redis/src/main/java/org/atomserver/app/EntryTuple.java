package org.atomserver.app;

public class EntryTuple {
    private final String workspace;
    private final String collection;
    private final String entryId;

    public EntryTuple(String workspace, String collection, String entryId) {
        this.workspace = workspace;
        this.collection = collection;
        this.entryId = entryId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getCollection() {
        return collection;
    }

    public String getEntryId() {
        return entryId;
    }

    public class AggregateTuple extends EntryTuple {
        public AggregateTuple(String collection, String entryId) {
            super("$join", collection, entryId);
        }
    }
}
