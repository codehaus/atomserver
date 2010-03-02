/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Aggregate feed id and Term in the feed. This class is used to retrieve
 * existing terms of an aggregated feed from the aggregate feed cache entries.
 */
public class AggregateFeedTerm {
    private String cachedFeedId;
    private String term;

    public AggregateFeedTerm() {}

    public AggregateFeedTerm(final String feedId, final String term) {
        this.cachedFeedId = feedId;
        this.term = term;
    }

    public String getCachedFeedId() {
        return cachedFeedId;
    }

    public void setCachedFeedId(final String cachedFeedId) {
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

    public int hashCode() {
        return new HashCodeBuilder(16661, 8675309)
                .append(cachedFeedId).append(term).toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || !AggregateFeedTerm.class.equals(obj.getClass())) {
            return false;
        }
        AggregateFeedTerm other = (AggregateFeedTerm) obj;
        return new EqualsBuilder()
                .append(cachedFeedId, other.cachedFeedId)
                .append(term, other.term)
                .isEquals();
    }
}
