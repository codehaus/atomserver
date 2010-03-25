package org.atomserver.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

public class EntryTuple implements Serializable {
    public final String entryId;
    public final long timestamp;
    public final long created;
    public final long updated;
    public final byte[] digest;
    public final Set<CategoryTuple> categories;

    public EntryTuple(String entryId,
                      long timestamp,
                      long created,
                      long updated,
                      byte[] digest,
                      Set<CategoryTuple> categories) {
        this.entryId = entryId;
        this.timestamp = timestamp;
        this.created = created;
        this.updated = updated;
        this.digest = digest;
        this.categories = categories;
    }

    public EntryTuple update(long timestamp,
                             long updated,
                             byte[] digest,
                             Set<CategoryTuple> categories) {
        return new EntryTuple(entryId, timestamp, created, updated, digest, categories);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryTuple that = (EntryTuple) o;

        return created == that.created &&
                timestamp == that.timestamp &&
                updated == that.updated &&
                categories.equals(that.categories) &&
                Arrays.equals(digest, that.digest) &&
                entryId.equals(that.entryId);

    }

    public int hashCode() {
        int result = entryId.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (int) (created ^ (created >>> 32));
        result = 31 * result + (int) (updated ^ (updated >>> 32));
        result = 31 * result + Arrays.hashCode(digest);
        result = 31 * result + categories.hashCode();
        return result;
    }

    public String toString() {
        return "EntryTuple{" +
                "entryId='" + entryId + '\'' +
                ", timestamp=" + timestamp +
                ", created=" + created +
                ", updated=" + updated +
                ", digest=" + digest +   // TODO: tostring this digest better
                ", categories=" + categories +
                '}';
    }
}
