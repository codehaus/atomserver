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

    public static final String CACHE_CFG_REVISION = "AggregateFeedCacheConfigRevision";

    //==============================
    // Cache updates
    //==============================

    public void updateFeedCacheOnEntryAddOrUpdate(final Map<String, CachedAggregateFeed> feedMap,
                                                  final List<EntryCategory> categories,
                                                  final Locale entryLocale,
                                                  final long timestamp) {
        if( categories.isEmpty()) {
            return;
        }

        String entryLanCode = (entryLocale == null ) ? "**" : entryLocale.getLanguage();

        // get AtomCategory set from EntryCategory
        Set<AtomCategory> atomCat = convertToAtomCategories(categories);

        List<AggregateFeedTerm> updatesForCache = prepareUpdatesForCache(atomCat, feedMap);

        // Try updating all categories/Terms in the cache
        int missingRows = updateCache(updatesForCache, timestamp);

        if(missingRows > 0) {
            // If there are missing rows, some of the updates failed due to non-existing timestamp entries.
            Set<AggregateFeedTerm> addToCache = prepareNewCategoriesToCache(updatesForCache, atomCat, feedMap, entryLanCode);
            insertCache(addToCache, timestamp);
        }

    }

    public List<AggregateFeedTerm> getFeedTermsWithMatchingTimestamp(List<String> aggFeedIds, long timestamp) {
        ParamMap param = paramMap()
                        .param("feedids", aggFeedIds)
                        .param("timestamp", timestamp);
        return getSqlMapClientTemplate().queryForList("selectFeedIdTermsWithTimestamp", param);
    }

    //=====================================
    //   CachedFeed
    //=====================================
    public void addNewFeedToCache(final CachedAggregateFeed cachedFeed) {
        ParamMap paramMap = paramMap()
                .param("cachedfeedid", cachedFeed.getCachedFeedId())
                .param("workspaces", cachedFeed.getOrderedJoinedWorkspaces())
                .param("scheme", cachedFeed.getScheme())
                .param("locale", cachedFeed.getLocale());
        getSqlMapClientTemplate().insert("insertCachedFeed", paramMap);
    }

    public List<CachedAggregateFeed> getExistingCachedFeeds() {
        return getSqlMapClientTemplate().queryForList("selectExistingCachedFeeds", paramMap());
    }

    public long getCacheConfigRevision() {
        ParamMap paramMap = paramMap().param("paramname", CACHE_CFG_REVISION);
        Long updatedRev = (Long) getSqlMapClientTemplate().queryForObject("selectCacheConfigRevision", paramMap);
        if(updatedRev == null) {
            getSqlMapClientTemplate().insert("insertCacheConfigRevision", paramMap);
            updatedRev = 0L;
        }
        return updatedRev;
    }

    public void updateCacheConfigRevision() {
        ParamMap paramMap = paramMap().param("paramname", CACHE_CFG_REVISION);
        getSqlMapClientTemplate().update("updateCacheConfigRevision", paramMap);
    }

    public CachedAggregateFeed getCachedFeedById(final String cachedFeedId) {
        ParamMap paramMap = paramMap().param("cachedfeedid", cachedFeedId);
        return (CachedAggregateFeed) getSqlMapClientTemplate().queryForObject("findCachedFeedById", paramMap);
    }

    public void removeFeedFromCacheById(final String cachedFeedId) {
        ParamMap paramMap = paramMap().param("cachedfeedid", cachedFeedId);
        getSqlMapClientTemplate().delete("deleteCachedFeedById", paramMap);
    }

    //=====================================
    //   AggregateFeedTimestamps Cache In general
    //=====================================
    public void cacheAggregateFeedTimestamps(final List<String> joinWorkspaces, final String locale,
                                             final String scheme, final String feedId) {

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

    public void cacheAggregateFeedTimestampsByTerms(final List<String> joinWorkspaces,
                                                  final String locale,
                                                  final String scheme,
                                                  final String feedId,
                                                  final List<String> terms) {
        ParamMap paramMap = paramMap()
                .param("cachedfeedid", feedId)
                .param("collection", scheme)
                .param("joinWorkspaces", joinWorkspaces)
                .param("terms", terms);  

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

    public void removeAggregateFeedTimestampsByTerms(final List<AggregateFeedTerm> feedTerms) {
        ParamMap paramMap = paramMap()
                .param("feedterms", feedTerms);
        getSqlMapClientTemplate().insert("deleteAggregateFeedCacheByFeedIdTerm", paramMap);
    }

    public void removeAggregateFeedTimestampsById(final String cachedFeedId) {

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

    // ----- private methods -----
    
    // Returns a list of cachedFeedId and terms to update in the AggregateFeedTimestamp table.
     private List<AggregateFeedTerm> prepareUpdatesForCache(final Set<AtomCategory> atomCat,
                                    final Map<String, CachedAggregateFeed> feedMap) {
         List<AggregateFeedTerm> updatesForCache = new ArrayList<AggregateFeedTerm>();

         for (String feedId : feedMap.keySet()) {
             CachedAggregateFeed caf = feedMap.get(feedId);
             for (AtomCategory st : atomCat) {
                 if (st.getScheme().equals(caf.getScheme())) {
                     updatesForCache.add(new AggregateFeedTerm(caf.getCachedFeedId(), st.getTerm()));
                 }
             }
         }
         return updatesForCache;
     }

     private int updateCache( final List<AggregateFeedTerm> updatesForCache, final long timestamp) {
         // Try adding all the terms in the categories.

        // Note: Some terms may already be cached and some terms may be not. If the number of terms
        // updates are not the same as the terms actually updated
        // We need to update the timestamp of terms already in the cache and add terms not yet in the cache.
        // This method updates the existing ones only.
         int rc = 1;
         int rowsUpdated;
         if(updatesForCache != null && !updatesForCache.isEmpty()) {
//             System.out.println(" updateCache :" + updatesForCache.size() + " -->" + updatesForCache);
             ParamMap paramMap = paramMap()
                     .param("feedterms", updatesForCache)
                     .param("timestamp", timestamp);
             try {
                rowsUpdated = getSqlMapClientTemplate().update("updateTimestamps", paramMap);
             } catch (org.springframework.dao.DataAccessResourceFailureException ex)      {
                log.info(" CachedAggregateFeedDAO.updateCache :" + updatesForCache.size() + " -->" + updatesForCache);
                throw ex;
             } catch (org.springframework.dao.ConcurrencyFailureException ex) {
                log.info(" CachedAggregateFeedDAO.updateCache :" + updatesForCache.size() + " -->" + updatesForCache);
                throw ex;
             }
             rc = updatesForCache.size() - rowsUpdated; //
         }
         return rc;
     }

     private Set<AggregateFeedTerm> prepareNewCategoriesToCache( final List<AggregateFeedTerm> updatesForCache,
                                                             final Set<AtomCategory> atomCat,
                                                             final Map<String, CachedAggregateFeed> feedMap,
                                                             final String entryLanCode) {

         // This query determines which FeedId-Terms are present in the table, so that it can
         // compute which ones to add.
         ParamMap paramMap = paramMap()
                 .param("terms", updatesForCache);
         List<AggregateFeedTerm> list = getSqlMapClientTemplate().queryForList("selectFeedIdTerms", paramMap);

         Set<AggregateFeedTerm> exists = new HashSet<AggregateFeedTerm>(list);
         Set<AggregateFeedTerm> newFeedTerms = new HashSet<AggregateFeedTerm>(updatesForCache);
         newFeedTerms.removeAll(exists);

         // create a map feedId => new entries
         Map<String,Set<AtomCategory>> feedToNew = new HashMap<String,Set<AtomCategory>>();
         for(AggregateFeedTerm ft: newFeedTerms) {
             CachedAggregateFeed feed = feedMap.get(ft.getCachedFeedId());
             Set<AtomCategory> newTerms = feedToNew.get(feed.getCachedFeedId());
             if(newTerms == null) {
                 newTerms = new HashSet<AtomCategory>();
                 feedToNew.put(feed.getCachedFeedId(), newTerms);
             }
             newTerms.add(new AtomCategory(feed.getScheme(),ft.getTerm()));
         }

         Set<AggregateFeedTerm> addToCache = new HashSet<AggregateFeedTerm>();

         // Filter out non-cached combinations
         for(String feedId: feedMap.keySet()) { // loop through effected feeds
             Set<AtomCategory> newCat = feedToNew.get(feedId);

             // Additional filter for non-existings terms
             if(newCat != null && !newCat.isEmpty()) {
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

       return addToCache;
     }

     private void insertCache(final Set<AggregateFeedTerm> addToCache, final long timestamp) {
         // Add new terms to the cache
         for (AggregateFeedTerm fterm : addToCache) {
                 ParamMap paramMap = paramMap()
                         .param("cachedfeedid", fterm.getCachedFeedId())
                         .param("term", fterm.getTerm())
                         .param("timestamp", timestamp);
                 getSqlMapClientTemplate().insert("insertTimestamp", paramMap);
         }
     }

    /*
     * Convert EntryCategory collection to AtomCategory set.
     */
    private Set<AtomCategory> convertToAtomCategories(final Collection<EntryCategory> categories) {
        Set<AtomCategory> atomCat = new HashSet<AtomCategory>();
        for (EntryCategory category : categories) {
            if(category.getScheme() != null) {      // filter out categories with null scheme
                atomCat.add(new AtomCategory(category.getScheme(), category.getTerm()));
            }
        }
        return atomCat;
    }

}