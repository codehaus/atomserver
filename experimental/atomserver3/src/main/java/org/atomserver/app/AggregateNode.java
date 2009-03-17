package org.atomserver.app;

public class AggregateNode {
    private final String collection;
    private final String entryId;

    public AggregateNode(String collection, String entryId) {
        this.collection = collection;
        this.entryId = entryId;
    }

    public String getCollection() {
        return collection;
    }

    public String getEntryId() {
        return entryId;
    }

    public int hashCode() {
        return getCollection().hashCode() + 8675309 * getEntryId().hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj ||
               (obj != null && AggregateNode.class.equals(obj.getClass()) &&
                getCollection().equals(((CategoryNode) obj).getScheme()) &&
                getEntryId().equals(((CategoryNode) obj).getTerm()));
    }
}
