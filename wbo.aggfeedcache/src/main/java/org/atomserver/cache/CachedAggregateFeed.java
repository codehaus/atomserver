/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;
import java.util.Arrays;

/**
 * This class represents a cached aggregate feed. It is mapped to the CachedFeed table where it persists the
 * ordered list of joined workspaces, locale and scheme. The aggregate feed is assigned
 * an id by MD5-hashing of the ordered joined workspaces, locale, and scheme.
 */
public class CachedAggregateFeed {

    String cachedFeedId = null;  // MD5 Hash of [orderedJoinedWorkspaces, locale, and scheme
    String orderedJoinedWorkspaces;    // ordered list of comma-separated workspaces
    String locale = null;              // locale of the form lanCode_CountryCode
    String scheme;                     // scheme

    public CachedAggregateFeed() {}

    /**
     * Constructor for CachedAggregateFeed.
     * @param cachedFeedId      MD5 encoded id string for the feed. If passed as null,
     *                          this class will generate the feedId
     * @param orderedJoinedWorkspaces a comma-separated list of workspaces in alphabetical order.
     * @param locale            locale string of the form "lanCode_CountryCode"
     * @param scheme
     */
    public CachedAggregateFeed(String cachedFeedId, String orderedJoinedWorkspaces, String locale, String scheme) {

        this.cachedFeedId = cachedFeedId;
        this.orderedJoinedWorkspaces = orderedJoinedWorkspaces;
        this.locale = locale;
        this.scheme = scheme;
    }

    public String getCachedFeedId() {
        if (this.cachedFeedId == null) {
            this.cachedFeedId = MD5ToHexString(generateCachedFeedId());
        }
        return this.cachedFeedId;
    }

    public void setCachedFeedId(String cachedFeedId) {
        this.cachedFeedId = cachedFeedId;
    }

    public String getOrderedJoinedWorkspaces() {
        return orderedJoinedWorkspaces;
    }

    public void setOrderedJoinedWorkspaces(String orderedJoinedWorkspaces) {
        this.orderedJoinedWorkspaces = orderedJoinedWorkspaces;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public List<String> getJoinWorkspaceList() {
        return Arrays.asList(getOrderedJoinedWorkspaces().split(","));
    }

    /**
     * Create an MD5 Key from workspaces, locale and scheme
     *
     * @return MD5 byte array
     */
    private byte[] generateCachedFeedId() {
        if (orderedJoinedWorkspaces == null || "".equals(orderedJoinedWorkspaces)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(orderedJoinedWorkspaces);
        if (locale != null && !"**_**".equals(locale)) {
            builder.append(".")
                    .append(locale);
        }
        builder.append("-").append(scheme);
        return DigestUtils.md5(builder.toString());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        CachedAggregateFeed caf = (CachedAggregateFeed) obj;
        return orderedJoinedWorkspaces.equals(caf.getOrderedJoinedWorkspaces()) &&
               ((locale == null && caf.getLocale() == null) || (locale != null && locale.equals(caf.getLocale()))) &&
               (scheme.equals(caf.getScheme()));
    }

    public int hashCode() {
        return getCachedFeedId().hashCode();
    }

    /*
     * Generate MD5 value as a hex string of the form 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
     * (where x is a hex digit) that the MS SQL server will treat it as a uniqueidentifier
     *  and other db servers will treat it as a 36 character string id.  
     */
    public static String MD5ToHexString(byte[] hval) {
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < hval.length; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                bld.append("-");
            }
            String s = Integer.toHexString(hval[i] & 0xff);
            if (s.length() == 1) {
                bld.append("0"); // prefix 0 if only 1 digit.
            }
            bld.append(s);
        }
        return bld.toString();
    }
}
