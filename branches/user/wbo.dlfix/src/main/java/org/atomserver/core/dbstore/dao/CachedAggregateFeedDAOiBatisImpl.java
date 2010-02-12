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
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.perf4j.StopWatch;

import java.util.*;
import java.sql.SQLException;

import com.ibatis.sqlmap.client.SqlMapExecutor;

/**
 * Implementation class for CachedAggregateFeedDAO.
 */
@ManagedResource(description = "CachedAggregateFeedDAO")
public class CachedAggregateFeedDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements CachedAggregateFeedDAO {

    private static final String CACHE_CFG_REVISION = "AggregateFeedCacheConfigRevision";

    //==============================
    // Cache updates
    //==============================

    /**
     * {@inheritDoc}
     */
    public void updateFeedCacheOnEntryAddOrUpdate(final Map<String, CachedAggregateFeed> feedMap,
                                                  final List<EntryCategory> categories,
                                                  final Locale entryLocale,
                                                  final long timestamp) {
        if( categories.isEmpty()) {
            return;
        }

        String entryLanCode = (entryLocale == null ) ? "**" : entryLocale.getLanguage();

        List<AggregateFeedTerm> updatesForCache = prepareUpdatesForCache(categories, feedMap);

        // Try updating all categories/Terms in the cache with later timestamp.
        int missingRows = updateFeedCache(updatesForCache, timestamp);

        if(missingRows > 0) {
            // missingRows > 0 can be due to two things:
            //    1. there are non-existing feedid-terms, or
            //    2. some feedid-terms have been concurrently updated with a later timestamp.
            // prepareAddsForCache() will determine if there are real new feedid-terms.
            // If the returned set is empty, it means some one has already updated the missing rows
            // with a later timestamp and therefore no need to update those rows.
            Set<AggregateFeedTerm> addToCache = prepareAddsForCache(updatesForCache, feedMap, entryLanCode);

            if(!addToCache.isEmpty()) {
                // add the actual missing rows.
                insertFeedCache(addToCache, timestamp);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<AggregateFeedTerm> getFeedTermsWithMatchingTimestamp(List<String> aggFeedIds, long timestamp) {
        ParamMap param = paramMap()
                        .param("feedids", aggFeedIds)
                        .param("timestamp", timestamp);
        return getSqlMapClientTemplate().queryForList("selectFeedIdTerms", param);
    }

    /**
     * {@inheritDoc}
     */
    public void updateFeedCacheBatch(final Map<String, Set<String>> existingTerms,
                                     final Map<String, Set<String>> newTerms,
                                     final Map<String, Long> maxTimestampMap) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            getSqlMapClientTemplate().execute(
                new SqlMapClientCallback() {
                   public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                        // existing terms
                        for(String feedId: existingTerms.keySet()) {
                            for(String term: existingTerms.get(feedId)) {
                                Long timestamp = maxTimestampMap.get(feedId+term);
                                if(timestamp != null) {
                                    ParamMap paramMap = paramMap()
                                        .param("cachedfeedid", feedId)
                                        .param("term", term)
                                        .param("timestamp", timestamp);
                                    executor.update("updateTimestamp", paramMap);
                                }
                            }
                        }
                        // new terms
                        for(String feedId: newTerms.keySet()) {
                            for(String term: newTerms.get(feedId)) {
                                Long timestamp = maxTimestampMap.get(feedId+term);
                                if(timestamp != null) {
                                    ParamMap paramMap = paramMap()
                                        .param("cachedfeedid", feedId)
                                        .param("term", term)
                                        .param("timestamp", timestamp);
                                    executor.insert("insertTimestamp", paramMap);
                                }
                            }
                        }
                        executor.executeBatch();
                       return null;
                   }
                }
            );
        } finally {
            stopWatch.stop("DB.updateFeedCacheBatch", "");
        }
    }

    //=====================================
    //   CachedFeed
    //=====================================
    /**
     * {@inheritDoc}
     */
    public void addNewFeedToCache(final CachedAggregateFeed cachedFeed) {
        ParamMap paramMap = paramMap()
                .param("cachedfeedid", cachedFeed.getCachedFeedId())
                .param("workspaces", cachedFeed.getOrderedJoinedWorkspaces())
                .param("scheme", cachedFeed.getScheme())
                .param("locale", cachedFeed.getLocale());
        getSqlMapClientTemplate().insert("insertCachedFeed", paramMap);
    }

    /**
     * {@inheritDoc}
     */
    public List<CachedAggregateFeed> getExistingCachedFeeds() {
        return getSqlMapClientTemplate().queryForList("selectExistingCachedFeeds", paramMap());
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Set<String>> getTermsInFeed(Map<String, Set<String>> feedIdTerms) {
        Map<String, Set<String>> termsInFeed = new HashMap<String, Set<String>>();
        if(!feedIdTerms.isEmpty()) {
            ParamMap paramMap = paramMap()
                    .param("feedterms", convertToFeedTermList(feedIdTerms));
            List<AggregateFeedTerm> list = getSqlMapClientTemplate().queryForList("selectFeedIdTerms", paramMap);

            for(AggregateFeedTerm fterm: list) {
                String feedId = fterm.getCachedFeedId();
                Set<String> terms = termsInFeed.get(feedId);
                if(terms == null) {
                    terms = new HashSet<String>();
                    termsInFeed.put(feedId, terms);
                }
                terms.add(fterm.getTerm());
            }
        }
        return termsInFeed;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheConfigRevision() {
        ParamMap paramMap = paramMap().param("paramname", CACHE_CFG_REVISION);
        Long updatedRev = (Long) getSqlMapClientTemplate().queryForObject("selectCacheConfigRevision", paramMap);
        if(updatedRev == null) {
            getSqlMapClientTemplate().insert("insertCacheConfigRevision", paramMap);
            updatedRev = 0L;
        }
        return updatedRev;
    }

    /**
     * {@inheritDoc}
     */
    public void updateCacheConfigRevision() {
        ParamMap paramMap = paramMap().param("paramname", CACHE_CFG_REVISION);
        getSqlMapClientTemplate().update("updateCacheConfigRevision", paramMap);
    }

    /**
     * {@inheritDoc}
     */
    public CachedAggregateFeed getCachedFeedById(final String cachedFeedId) {
        ParamMap paramMap = paramMap().param("cachedfeedid", cachedFeedId);
        return (CachedAggregateFeed) getSqlMapClientTemplate().queryForObject("findCachedFeedById", paramMap);
    }

    /**
     * {@inheritDoc}
     */
    public void removeFeedFromCacheById(final String cachedFeedId) {
        ParamMap paramMap = paramMap().param("cachedfeedid", cachedFeedId);
        getSqlMapClientTemplate().delete("deleteCachedFeedById", paramMap);
    }

    //=====================================
    //   AggregateFeedTimestamps Cache In general
    //=====================================
    /**
     * {@inheritDoc}
     */
    public void cacheAggregateFeedTimestamps(final List<String> joinWorkspaces, final String locale,
                                             final String scheme, final String feedId) {

        ParamMap paramMap = paramMap();
        internalCacheAggregateFeedTimestamps(joinWorkspaces, locale, scheme, feedId, paramMap);
    }

    /**
     * {@inheritDoc}
     */
    public void cacheAggregateFeedTimestampsByTerms(final List<String> joinWorkspaces,
                                                  final String locale,
                                                  final String scheme,
                                                  final String feedId,
                                                  final List<String> terms) {
        ParamMap paramMap = paramMap()
                .param("terms", terms);
        internalCacheAggregateFeedTimestamps(joinWorkspaces, locale, scheme, feedId, paramMap);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAggregateFeedTimestampsByTerms(final List<AggregateFeedTerm> feedTerms) {
        ParamMap paramMap = paramMap()
                .param("feedterms", feedTerms);
        getSqlMapClientTemplate().insert("deleteAggregateFeedCacheByFeedIdTerm", paramMap);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAggregateFeedTimestampsById(final String cachedFeedId) {

        ParamMap paramMap = paramMap()
                .param("cachedfeedid", cachedFeedId);
        getSqlMapClientTemplate().delete("deleteAggregateFeedCache", paramMap);
    }

    /**
     * {@inheritDoc}
     */
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
   /**
    * {@inheritDoc}
    */
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
     private List<AggregateFeedTerm> prepareUpdatesForCache(final List<EntryCategory> categories,
                                                            final Map<String, CachedAggregateFeed> feedMap)
     {
         List<AggregateFeedTerm> updatesForCache = new ArrayList<AggregateFeedTerm>();

         for (String feedId : feedMap.keySet()) {
             CachedAggregateFeed caf = feedMap.get(feedId);
             for (EntryCategory st : categories) {
                 if (st.getScheme().equals(caf.getScheme())) {
                     updatesForCache.add(new AggregateFeedTerm(caf.getCachedFeedId(), st.getTerm()));
                 }
             }
         }
         return updatesForCache;
     }

     private int updateFeedCache( final List<AggregateFeedTerm> updatesForCache, final long timestamp) {

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
             rowsUpdated = getSqlMapClientTemplate().update("updateTimestamp", paramMap);
             rc = updatesForCache.size() - rowsUpdated; //
         }
         return rc;
     }

     private Set<AggregateFeedTerm> prepareAddsForCache( final List<AggregateFeedTerm> updatesForCache,
                                                             final Map<String, CachedAggregateFeed> feedMap,
                                                             final String entryLanCode) {

         Set<AggregateFeedTerm> addToCache = new HashSet<AggregateFeedTerm>();

         // This query determines which FeedId-Terms are present in the table, so that it can
         // compute which ones to add.
         ParamMap paramMap = paramMap()
                    .param("feedterms", convertToFeedTermList(updatesForCache));
         List<AggregateFeedTerm> list = getSqlMapClientTemplate().queryForList("selectFeedIdTerms", paramMap);

         Set<AggregateFeedTerm> exists = new HashSet<AggregateFeedTerm>(list);
         Set<AggregateFeedTerm> newFeedTerms = new HashSet<AggregateFeedTerm>(updatesForCache);
         newFeedTerms.removeAll(exists);
         if(newFeedTerms.isEmpty()) {
             return addToCache;
         }

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

    private void internalCacheAggregateFeedTimestamps(final List<String> joinWorkspaces,
                                                      final String locale,
                                                      final String scheme,
                                                      final String feedId,
                                                      ParamMap paramMap) {

        paramMap.param("cachedfeedid", feedId)
                .param("collection", scheme)
                .param("joinWorkspaces", joinWorkspaces);

        if (locale != null) {
            String lang = locale.substring(0, 2);
            String country = locale.substring(3, 5);
            if (!lang.equals("**")) {
                paramMap.param("language", lang)
                        .param("country", country);
            }
        }
        getSqlMapClientTemplate().insert("insertAggregateFeedCache", paramMap);
    }

    private void insertFeedCache(final Set<AggregateFeedTerm> addToCache, final long timestamp) {
         // Add new terms to the cache
         for (AggregateFeedTerm fterm : addToCache) {
                 ParamMap paramMap = paramMap()
                         .param("cachedfeedid", fterm.getCachedFeedId())
                         .param("term", fterm.getTerm())
                         .param("timestamp", timestamp);
                 getSqlMapClientTemplate().insert("insertTimestamp", paramMap);
         }
     }

    private List<FeedTermList> convertToFeedTermList(final Map<String, Set<String>> feedTerms ) {
         List<FeedTermList> feedterms = new ArrayList<FeedTermList>();
         for(String t: feedTerms.keySet()) {
             Set<String> terms = feedTerms.get(t);
             if(!terms.isEmpty()) {
                feedterms.add(new FeedTermList(t, new ArrayList<String>(terms)));
             }
         }
         return feedterms;
     }

    private List<FeedTermList> convertToFeedTermList(final List<AggregateFeedTerm> aggFeedTerms) {
        List<FeedTermList> feedterms = new ArrayList<FeedTermList>();
        Map<String, FeedTermList> feedToFeedTermList = new HashMap<String, FeedTermList>(); // for quick look up
        
        for(AggregateFeedTerm aft: aggFeedTerms) {
            String feedId = aft.getCachedFeedId();
            String term = aft.getTerm();
            FeedTermList ftermlist = feedToFeedTermList.get(feedId);
            if(ftermlist == null) {
                ftermlist = new FeedTermList(feedId, new ArrayList<String>());
                feedToFeedTermList.put(feedId, ftermlist);
                feedterms.add(ftermlist);
            }
            ftermlist.addTerm(term);
        }
        feedToFeedTermList.clear();
        return feedterms;
    }

    // class which maps a feed to its list of terms.
    static class FeedTermList {
        final String feedId;
        List<String> terms = null;

        public FeedTermList(String feedId, List<String> terms) {
            this.feedId = feedId;
            this.terms = terms;
        }

        public String getFeedId() {
            return feedId;
        }

        public List<String> getTerms() {
            return terms;
        }

        public void addTerm(String term) {
            this.terms.add(term);
        }
    }
}