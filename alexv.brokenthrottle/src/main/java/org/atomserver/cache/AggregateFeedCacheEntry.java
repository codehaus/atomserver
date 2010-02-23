/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

/**
 * This class represents a row in the Aggregate Feed Timestamp cache table. All the cached entries for
 * a given feed have the same cachedFeedId.
 * 
 */
public class AggregateFeedCacheEntry {
    String cachedFeedId;             // MD5 Hashed value of ordered workspace list, locale, and scheme
    String term;
    Long   updateTimestampValue;

    public String getCachedFeedId() {
        return cachedFeedId;
    }

    public void setCachedFeedId(String cachedFeedId) {
        this.cachedFeedId = cachedFeedId;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
    
    public Long getUpdateTimestampValue() {
        return updateTimestampValue;
    }

    public void setUpdateTimestampValue(Long updateTimestampValue) {
        this.updateTimestampValue = updateTimestampValue;
    }

}
