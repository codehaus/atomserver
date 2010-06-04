package org.atomserver.content;

public class EntryKey {
    private final String service;
    private final String workspace;
    private final String collection;
    private final String entryId;

    public EntryKey(String service, String workspace, String collection, String entryId) {
        if (service == null || workspace == null || collection == null || entryId == null) {
            throw new NullPointerException(
                    String.format("(service, workspace, collection, and entryId) must all be " +
                                  "non-null!  (you passed (%s, %s, %s, %s)",
                                  service, workspace, collection, entryId));
        }
        this.service = service;
        this.workspace = workspace;
        this.collection = collection;
        this.entryId = entryId;
    }

    public String getService() {
        return service;
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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EntryKey entryKey = (EntryKey) o;

        return service.equals(entryKey.service) &&
               workspace.equals(entryKey.workspace) &&
               collection.equals(entryKey.collection) &&
               entryId.equals(entryKey.entryId);

    }

    public int hashCode() {
        return service.hashCode() +
               227 * workspace.hashCode() +
               233 * collection.hashCode() +
               8675309 * entryId.hashCode();
    }
}
