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
import org.atomserver.AtomCategory;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.*;

/**
 * Implementation class for AggregateFeedCacheDAO.
 */
@ManagedResource(description = "CachedAggregateFeedDAO")
public class CachedAggregateFeedDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements CachedAggregateFeedDAO {

    //==============================
    // Cache updates
    //==============================
    public void updateFeedCacheOnEntryAddOrUpdate(Map<String, CachedAggregateFeed> feedMap,
                                                  List<EntryCategory> categories,
                                                  Locale entryLocale,
                                                  long timestamp) {

        Set<AtomCategory> entryCat = new HashSet<AtomCategory>();
        String entryLanCode = (entryLocale == null ) ? "**" : entryLocale.getLanguage();
        for (EntryCategory category : categories) {
            entryCat.add(new AtomCategory(category.getScheme(), category.getTerm()));
        }

        // Note: Some terms may already be cached and some terms may be not.
        // We need to update the timestamp of terms already in the cache and add terms not yet in the cache.
        // This query determine which terms are already present in the cache so that new terms can be determined.
        List<String> aggFeedIds = new ArrayList<String>(feedMap.keySet());
        ParamMap paramMap = paramMap()
                .param("joinWorkspaces", aggFeedIds);
        List<AggregateFeedTerm> list = getSqlMapClientTemplate().queryForList("selectJoinedWorkspaceTerms", paramMap);

        List<AggregateFeedTerm> updateCache = new ArrayList<AggregateFeedTerm>();
        Set<AggregateFeedTerm> addToCache = new HashSet<AggregateFeedTerm>();

        if(list == null || list.isEmpty()) {
            for(String feedId: feedMap.keySet()) {
                CachedAggregateFeed caf = feedMap.get(feedId);
                for(AtomCategory st: entryCat) {
                    if(st.getScheme().equals(caf.getScheme())) {
                        addToCache.add( new AggregateFeedTerm(caf.getCachedFeedId(), st.getTerm()));
                    }
                }
            }
        } else {
            String currentFeedId = null;
            String currentScheme = null;
            Map<String, Set<AtomCategory>> categoriesPerFeed = new HashMap<String, Set<AtomCategory>>();
            Set<AtomCategory> feedCat = null;

            for(AggregateFeedTerm aft: list) {
                String cachedFeedId = aft.getCachedFeedId();
                if(!cachedFeedId.equals(currentFeedId)) {
                    feedCat = new HashSet<AtomCategory>();
                    categoriesPerFeed.put(cachedFeedId, feedCat);
                    currentScheme = feedMap.get(cachedFeedId).getScheme();
                    currentFeedId = cachedFeedId;
                }
                feedCat.add(new AtomCategory(currentScheme, aft.getTerm()));
            }

            for(String feedId: feedMap.keySet()) { // loop through effected feeds
                
                feedCat = categoriesPerFeed.get(feedId);
                if(feedCat == null) {
                    feedCat = new HashSet<AtomCategory>();
                }

                // existing categories for the feed
                Set<AtomCategory> existingCat = new HashSet<AtomCategory>(feedCat);
                existingCat.retainAll(entryCat);
                for(AtomCategory cat: existingCat) {
                    updateCache.add(new AggregateFeedTerm(feedId, cat.getTerm()));
                }

                // new categories for the feed
                Set<AtomCategory> newCat = new HashSet<AtomCategory>(entryCat);
                newCat.removeAll(feedCat);

                if(!newCat.isEmpty()) {
                    // cache only if the feed locale and scheme matches with entry.
                    CachedAggregateFeed feed = feedMap.get(feedId);
                    String lanCode = (feed.getLocale() == null) ? "**" : feed.getLocale().substring(0,2);
                    boolean localeMatch = (lanCode.equals(entryLanCode)||lanCode.equals("**"));
                    if(localeMatch) {
                        String scheme = feed.getScheme();
                        for(AtomCategory c: newCat) {
                            if(c.getScheme().equals(scheme)) {
                                addToCache.add(new AggregateFeedTerm(feedId, c.getTerm()));
                            }
                        }
                    }
                }
            }
            // cleanup
            categoriesPerFeed.clear();
            if(feedCat != null) {
                feedCat.clear();
            }
        }

        // Update existing timestamps of terms already in the cache.
        if(!updateCache.isEmpty()) {
            paramMap = paramMap()
                    .param("terms", updateCache)
                    .param("timestamp", timestamp);
            getSqlMapClientTemplate().update("updateTimestamps", paramMap);
        }

        // Add new terms to the cache if they are not already there.
        for (AggregateFeedTerm fterm : addToCache) {
                paramMap = paramMap()
                        .param("cachedfeedid", fterm.getCachedFeedId())
                        .param("term", fterm.getTerm())
                        .param("timestamp", timestamp);
                getSqlMapClientTemplate().insert("insertTimestamp", paramMap);
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
                .param("joinWorkspaces", joinWorkspaces);
        
        if(locale != null) {
            String lang = locale.substring(0,2);
            String country = locale.substring(3,5);
            if(!lang.equals("**")) {
                paramMap.param("language",lang)
                    .param("country", country);
            }
        }
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