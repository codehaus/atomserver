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
import org.atomserver.exceptions.AtomServerException;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.*;

/**
 * Implementation class for AggregateFeedCacheDAO.
 */
@ManagedResource(description = "AggregateFeedCacheDAO")
public class AggregateFeedCacheDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements AggregateFeedCacheDAO {

    //==============================
    // Cache updates
    //==============================
    public void updateFeedCacheOnEntryAddOrUpdate(Set<String> aggregateFeedIds,
                                                  List<EntryCategory> categories,
                                                  long timestamp) {
        Set<String> termsInEntry = new HashSet<String>();
        for (EntryCategory category : categories) {
            termsInEntry.add(category.getTerm());
        }
        List<String> aggFeedIds = new ArrayList<String>(aggregateFeedIds);
        // convert termsInEntry set into list for sqlMap
        List<String> termsInEntryList = new ArrayList<String>(termsInEntry);

        // get terms for each joined
        ParamMap paramMap = paramMap()
                .param("joinWorkspaces", aggFeedIds)
                .param("terms", termsInEntryList)
                .param("timestamp", timestamp);

        // Note: Some terms may already be cached and some terms may be not.
        // We need to update the timestamp of terms already in the cache and add terms not yet in the cache.
        // This query determine which terms are already present in the cache so that new terms can be determined.
        List<AggregateFeedTerm> list = getSqlMapClientTemplate().queryForList("selectJoinedWorkspaceTerms", paramMap);
        String currentFeedId = null;
        Set<String> termset = null;
        Map<String, Set<String>> newTermsInFeed = new HashMap<String, Set<String>>();
        for (AggregateFeedTerm aft : list) {
            String cachedFeedId = aft.getCachedFeedId();
            if(!cachedFeedId.equals(currentFeedId)) {
                termset = new HashSet<String>(termsInEntry);
                newTermsInFeed.put(cachedFeedId, termset); // all expected terms from this entry
                currentFeedId = cachedFeedId;
            }
            termset.remove(aft.getTerm());  // remove if already in the cache for the feed
        }

        // Update existing timestamps of terms already in the cache.
        getSqlMapClientTemplate().update("updateTimestamps", paramMap);

        // Add new terms to the cache if they are not already there.
        Collection<String> termsToAdd = null;
        for (String feedId : aggregateFeedIds) {
            termsToAdd = newTermsInFeed.get(feedId);
            if(termsToAdd == null) {
                termsToAdd = termsInEntry;
            }

            for (String term : termsToAdd) {
                paramMap = paramMap()
                        .param("cachedfeedid", feedId)
                        .param("term", term)
                        .param("timestamp", timestamp);
                getSqlMapClientTemplate().insert("insertTimestamp", paramMap);
            }
        }
    }

    public List<String> getFeedsWithMatchingTimestamp(List<String> aggFeedIds, long timestamp) {
        ParamMap paramMap = paramMap()
                .param("joinWorkspaces", aggFeedIds)
                .param("timestamp", timestamp);
        return getSqlMapClientTemplate().queryForList("selectCachedFeedsWithTimestamp", paramMap);
    }


    //=====================================
    //   CachedFeed
    //=====================================
    public void addNewFeedToCache(CachedAggregateFeed cachedFeed) {
        ParamMap paramMap = paramMap()
                .param("cachedfeedid", cachedFeed.getCachedFeedId())
                .param("workspaces", cachedFeed.getOrderedJoinedWorkspaces())
                .param("scheme", cachedFeed.getScheme())
                .param("locale", cachedFeed.getLocale());
        System.out.println("locale=" + cachedFeed.getLocale());
        getSqlMapClientTemplate().insert("insertCachedFeed", paramMap);
    }

    public List<CachedAggregateFeed> getExistingCachedFeeds() {
        return getSqlMapClientTemplate().queryForList("selectExistingCachedFeeds");
    }

    public CachedAggregateFeed getCachedFeedById(String cachedFeedId) {
        ParamMap paramMap = paramMap().param("cachedfeedid", cachedFeedId);
        return (CachedAggregateFeed) getSqlMapClientTemplate().queryForObject("findCachedFeedById", paramMap);
    }

    public void removeFeedFromCacheById(String cachedFeedId) {
        ParamMap paramMap = paramMap().param("cachedfeedid", cachedFeedId);
        getSqlMapClientTemplate().delete("deleteCachedFeedById", paramMap);
    }

    //=====================================
    //   AggregateFeeds Cache In general
    //=====================================
    public void cacheAggregateFeedTimestamps(List<String> joinWorkspaces, String locale, String scheme, String feedId) {

        ParamMap paramMap = paramMap()
                .param("cachedfeedid", feedId)
                .param("collection", scheme)
                .param("joinWorkspaces", joinWorkspaces)
                .param("locale", locale);
        getSqlMapClientTemplate().insert("insertAggregateFeedCache", paramMap);
    }

    public void removeAggregateFeedTimestampsById(String cachedFeedId) {

        ParamMap paramMap = paramMap()
                .param("cachedfeedid", cachedFeedId);
        getSqlMapClientTemplate().delete("deleteAggregateFeedCache", paramMap);
    }

    public void removeAllCachedTimestamps() {
        ParamMap paramMap = paramMap();
        getSqlMapClientTemplate().delete("deleteAggregateFeedCache", paramMap);
    }

    //==============================
    // Locks
    //==============================

    // This code is a replica of the acquireLock in EntriesDAO implementation.
    // TODO: Initial intent is to lock based on the CachedFeed table for better
    // concurrency but this could also create a possiblity of deadlocks. Until
    // further analysis is done, we currently lock on the EntryStore to avoid
    // possible deadlocks.
    public void acquireLock() throws AtomServerException {
        log.debug("ACQUIRING LOCK");

        // JTDS forces us to actually "touch" a DB Table before it will begin the transaction
        // so we have to do this No Op which does a "SELECT COUNT(*) from CachedFeed"
        // If we don't do this, then sp_getapplock returns -999, which indicates that it
        // is NOT in a transaction
        getSqlMapClientTemplate().queryForObject("noop", paramMap());

        Integer status = (Integer) getSqlMapClientTemplate().queryForObject("acquireLock", paramMap());

        // in SQL Server, a status of 0 or 1 indicates successful acquisition (synchronously and
        // asynchronously, respectively) of the application lock.
        //
        // error codes < 0 indicate errors as follows:
        //  -1      : the lock request timed out
        //  -2      : the lock request was cancelled
        //  -3      : the lock request was chosen as a deadlock victim
        //  -999    : Indicates a parameter validation or other call error
        //
        // in other DBs, we artificially make the lock acquisition query return a non-negative
        // value when the lock is successfully acquired.
        log.debug("acquireLock() STATUS = " + status);
        if (status < 0) {
            String message = "Could not acquire the database lock (status= " + status + ")";
            log.error(message);
            throw new AtomServerException(message);
        }
    }
}