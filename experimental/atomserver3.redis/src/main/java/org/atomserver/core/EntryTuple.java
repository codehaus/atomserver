package org.atomserver.core;

import org.atomserver.util.HexUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

public class EntryTuple implements Serializable {
    public final String entryId;
    public final long timestamp;
    public final long created;
    public final long updated;
    public final byte[] digest;
    public final boolean deleted;
    public final String contentSrc;
    public final String contentType;
    public final Set<CategoryTuple> categories;

    public EntryTuple(String entryId,
                      long timestamp,
                      long created,
                      long updated,
                      byte[] digest,
                      String contentSrc,
                      String contentType,
                      Set<CategoryTuple> categories) {
        this(entryId, timestamp, created, updated, digest, contentSrc, contentType, categories, false);
    }

    private EntryTuple(String entryId,
                      long timestamp,
                      long created,
                      long updated,
                      byte[] digest,
                      String contentSrc,
                      String contentType,
                      Set<CategoryTuple> categories,
                      boolean deleted) {
        this.entryId = entryId;
        this.timestamp = timestamp;
        this.created = created;
        this.updated = updated;
        this.digest = digest;
        this.contentType = contentType;
        this.contentSrc = contentSrc;
        this.categories = categories;
        this.deleted = deleted;
    }


    public EntryTuple update(long timestamp,
                             long updated,
                             byte[] digest,
                             String contentSrc,
                             String contentType,
                             Set<CategoryTuple> categories) {
        return update(timestamp, updated, digest, contentSrc, contentType, categories, false);
    }

    public EntryTuple delete(Long nextTimestamp, long time) {
        return update(nextTimestamp, time, this.digest, this.contentSrc, this.contentType, this.categories, true);
    }

    public EntryTuple update(long timestamp,
                             long updated,
                             byte[] digest,
                             String contentSrc,
                             String contentType,
                             Set<CategoryTuple> categories,
                             boolean deleted) {
        return new EntryTuple(entryId, timestamp, created, updated, digest, contentSrc, contentType, categories, deleted);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntryTuple that = (EntryTuple) o;

        return created == that.created &&
                timestamp == that.timestamp &&
                updated == that.updated &&
                contentSrc == null ? that.contentSrc == null : contentSrc.equals(that.contentSrc) &&
                contentType == null ? that.contentType == null : contentType.equals(that.contentType) &&
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
        result = 31 * result + (contentSrc == null ? 0 : contentSrc.hashCode());
        result = 31 * result + (contentType == null ? 0 : contentType.hashCode());
        result = 31 * result + categories.hashCode();
        return result;
    }

    public String toString() {
        return "EntryTuple{" +
                "entryId='" + entryId + '\'' +
                ", timestamp=" + timestamp +
                ", created=" + created +
                ", updated=" + updated +
                ", digest=" + HexUtil.toHexString(digest) +
                ", contentSrc=" + contentSrc +
                ", contentType=" + contentType +
                ", categories=" + categories +
                '}';
    }
}
