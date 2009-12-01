/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

/**
 * Aggregate feed id and Term in the feed. This class is used to retrieve
 * existing terms of an aggregated feed from the aggregate feed cache entries.
 */
public class AggregateFeedTerm {
    private String cachedFeedId;
    private String term;

    public AggregateFeedTerm() {}

    public AggregateFeedTerm(String feedId, String term) {
        this.cachedFeedId = feedId;
        this.term = term;
    }

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

    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("[")
                .append(cachedFeedId).append(", ")
                .append(term)
                .append("]");
        return builder.toString();
    }
}
