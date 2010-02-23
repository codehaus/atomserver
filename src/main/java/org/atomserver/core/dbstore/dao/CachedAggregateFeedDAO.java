/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.atomserver.core.dbstore.dao;

import org.atomserver.cache.CachedAggregateFeed;
import org.atomserver.cache.AggregateFeedTerm;
import org.atomserver.core.EntryCategory;

import java.util.*;

/**
 * AggregateFeedCacheDAO for accessing Cached Feeds and Cached Aggregate Feed Timestamps
 */
public interface CachedAggregateFeedDAO
        extends AtomServerDAO {

    //========================================
    // Update and query of Cached feed timestamp entries
    //========================================
    /**
     * Upadte the feed timestamp cache with the new timestamp for givne workspaces and categories. This is called
     * when an Entry is updated or added.
     * @param feedMap  a list of feeds mapped to its cached information link to each feed
     * @param categories  categories of the entry
     * @param locale      locale of the entry
     * @param timestamp   time stamp value to update
     */
    void updateFeedCacheOnEntryAddOrUpdate(Map<String,CachedAggregateFeed> feedMap, List<EntryCategory> categories, Locale locale, long timestamp);

    /**
     * Retrieves feed ids from a given list and terms matching a given timestamp.
     * @param feedIds       Ids of feend to search in
     * @param timestamp     timestamp to match
     * @return  a list of Feed and Term values
     */
    List<AggregateFeedTerm> getFeedTermsWithMatchingTimestamp(List<String> feedIds, long timestamp);

    /**
     * Batch updates and inserts of cached timestamps.
     * @param existingTerms  list of feedId and corresponding terms to update
     * @param newTerms       list of feedId and corresponding terms to add
     * @param maxTimestampMap   timestamp for each feedId and term.
     */
    void updateFeedCacheBatch(Map<String, Set<String>> existingTerms,
                              Map<String, Set<String>> newTerms,
                              Map<String, Long> maxTimestampMap);
    
    //========================================
    //  CRUD for mapping of feed id and aggregate feeds
    //========================================
    /**
     * Add an aggregate feed (joinedWorkspaces, locale, scheme) to be cached.
     * @param cachedFeed a new feed to cache
     */
    void addNewFeedToCache(CachedAggregateFeed cachedFeed);

    /**
     * Retrieve a cached aggregate feed by feed id.
     * @param cachedFeedId  id of cached feed
     * @return <code>CachedAggregateFedd</code> object.
     */
    CachedAggregateFeed getCachedFeedById(String cachedFeedId);

    /**
     * Remove a cached aggregate feed by id from the CachedFeed table.
     * @param cachedFeedId id of cached feed
     */
    void removeFeedFromCacheById(String cachedFeedId);

    /**
     * Retrieve a list of currently existing cached aggregate feeds
     * @return  a list of Cached Aggregate feed objects.
     */
    List<CachedAggregateFeed> getExistingCachedFeeds();

    /**
     * Get Existing Terms in the feed from the given set of terms.
     * @param feedIdTerms A map of feedId to a corresponding set of terms in the feed
     * @return a map with feedIds and their corresponding terms.
     */
    Map<String, Set<String>> getTermsInFeed(Map<String,Set<String>> feedIdTerms);

    /**
     * Get Last Cache configuration revision which indicates if the
     * cache configuration was changed.
     * @return a counter value
     */
    long getCacheConfigRevision();

    /**
     * Update Cache configuration revision
     */
    void updateCacheConfigRevision();
    //========================================
    //   CRUD AggregateFeeds Timestamp Cache entries
    //========================================

    /**
     * Populate AggregateFeedTimestamp Cache table for a new feed
     * @param workspaces   list of workspace in the feed
     * @param locale       locale of the cached feed
     * @param scheme       scheme of the cached feed
     * @param feedId       id of cached feed
     */
    void cacheAggregateFeedTimestamps(List<String> workspaces, String locale, String scheme, String feedId);

    /**
     * Populate AggregateFeedTimestamp Cache table with entries for a given feed and terms.
     *
     * @param workspaces   list of workspace in the feed
     * @param locale       locale of the cached feed
     * @param scheme       scheme of the cached feed
     * @param feedId       id of cached feed
     * @param terms        terms to cache
     */
    void cacheAggregateFeedTimestampsByTerms(List<String> workspaces, String locale, String scheme, String feedId,
                                      List<String> terms);

    /**
     * Remove cached aggregate feed timestamp entries from AggregateFeedTimestamp table.
     * @param cachedFeedId Id of cached feed
     */
    void removeAggregateFeedTimestampsById(String cachedFeedId);

    /**
     * Remove cached aggregate feed timestamp entries with the given list of <code>AggregateFeedTerm</code>.
     * @param feedTerms    a list of Cached Feed-Term pairs.
     */
    void removeAggregateFeedTimestampsByTerms(List<AggregateFeedTerm> feedTerms);

    /**
     * Remove all rows in the AggregateFeedTimestamp table.
     */
    void removeAllCachedTimestamps();

    //==============================
    // Transaction support
    //==============================
    void acquireLock();

}