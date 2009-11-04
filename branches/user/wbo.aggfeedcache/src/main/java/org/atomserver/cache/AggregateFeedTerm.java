/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

/**
 * Aggregate feed id and Term in the feed. This class is used to retrieve
 * existing terms of an aggregated feed from the aggregate feed cache entries.
 */
public class AggregateFeedTerm {
    String cachedFeedId;
    String term;

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
}
