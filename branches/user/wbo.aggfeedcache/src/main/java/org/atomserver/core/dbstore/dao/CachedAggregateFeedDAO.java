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
     * @param joinedWorkspaces
     * @param categories
     * @param timestamp
     */
    void updateFeedCacheOnEntryAddOrUpdate(Set<String>joinedWorkspaces, List<EntryCategory> categories, long timestamp);

    /**
     * Retrieves cached feed timestamp entries from the given list of feeds that matches that timestamp
     * @param feedIds
     * @param timestamp
     * @return
     */
    List<String> getFeedsWithMatchingTimestamp(List<String> feedIds, long timestamp);

    //========================================
    //  CRUD for mapping of feed id and aggregate feeds
    //========================================
    /**
     * Add an aggregate feed (joinedWorkspaces, locale, scheme) to be cached.
     * @param cachedFeed
     */
    void addNewFeedToCache(CachedAggregateFeed cachedFeed);

    /**
     * Retrieve a cached aggregate feed by feed id.
     * @param cachedFeedId
     * @return
     */
    CachedAggregateFeed getCachedFeedById(String cachedFeedId);

    /**
     * Remove a cached aggregate feed by id from the CachedFeed table.
     * @param cachedFeedId
     */
    void removeFeedFromCacheById(String cachedFeedId);

    /**
     * Retrieve a list of currently existing cached aggregate feeds
     * @return
     */
    List<CachedAggregateFeed> getExistingCachedFeeds();

    //========================================
    //   CRUD AggregateFeeds Timestamp Cache entries
    //========================================
    /**
     * Populate AggregateFeedTimestamp Cache table for a new feed
     * @param workspaces
     * @param feedId
     * @param scheme
     * @param locale
     */
    void cacheAggregateFeedTimestamps(List<String> workspaces, String locale, String scheme, String feedId);

    /**
     * Remove cached aggregate feed timestamp entries from AggregateFeedTimestamp table.
     * @param cachedFeedId
     */
    void removeAggregateFeedTimestampsById(String cachedFeedId);

    /**
     * Remove all rows in the AggregateFeedTimestamp table.
     */
    void removeAllCachedTimestamps();

    //==============================
    // Transaction support
    //==============================
    void acquireLock();

}