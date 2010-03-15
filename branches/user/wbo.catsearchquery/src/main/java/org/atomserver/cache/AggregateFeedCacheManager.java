/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

import org.atomserver.core.EntryMetaData;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.dbstore.dao.CachedAggregateFeedDAO;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.EntryDescriptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Cache Manager class handles the overall aggregate feed caching functions. It tracks which
 * joined workspaces and scheme are cached, updates the cache when an entry or a category is added,
 * updated or deleted. At statup time, it manages the sets of cached feeds by adding newly configured
 * ones and deleting the ones that are no longer listed in the spring configuration.
 * Two tables are related to the Cache Manager: AggregateFeedTimestampCache and CachedFeed.
 * AggregateFeedTimestampCache stores the actual cache time stamps for aggregate feed query based on cached
 * feed Id. CachedFeed table stores that mapping of the cached feed Id to the aggregate feed specified
 * by joined workspaces, locale and scheme.
 *
 * A note on locking for cache configuration changes at runtime:
 * AggregateFeedCacheManager API's are of two groups:
 * 1) APIs to manage the cache configuration and
 * 2) APIs to update the cached feed timestamps.
 *
 * The APIs to update the cached timestamps are called when changes are made to the Entry
 * and therefore the EntyStore lock will be already held by the caller.
 *
 * The APIs to change the cache configuration holds the same EntryStore lock. Here is the rational
 * behind this locking scheme: With EntryStore lock, adding a new cache is safe because the Entry change
 * operations (INSERT, UPDATE, DELETE) will be blocked while it is created. No issues on updating the timestamps in the
 * new cache due to changed Entries. Similarly, deleting the cache is also safe with respect to Entry change
 * operations because the lock blocks out entry changes during that time. However, in the current implementation,
 * when selecting aggregate feeds using cached timestamps, the EntryStore lock is not taken, the reason being to avoid
 * serialing selects on aggregate feed using cache. Thus, there is a potential incorrectness if the cached feed
 * is deleted while during an aggregate feed query which uses the cache. Since deleting a cache is going to be a very
 * very rare operation, the chance of hitting this is very low.

 * A note on checking cache configuration changes across servers:
 * AggregateFeedCacheManager keeps in local memory two maps, cachedFeedById and workspaceToCacheFeeds. When the cache
 * configuration is changed by adding a new cache or deleting an existing cache, they need to be updated. When the
 * cache configuration is changed, ChangedEvent table entry for "AggregateFeedCacheConfigRevision" has an updated
 * revision number. Servers will check for it in isFeedCached and isWorkspaceInCachedFeed APIs for the change
 * and reload the maps.
 */
public class AggregateFeedCacheManager {

    static private final Log log = LogFactory.getLog(AggregateFeedCacheManager.class);

    //==============================
    // Injected by spring IOC
    //==============================
    private List<String> cacheConfigList;

    private TransactionTemplate transactionTemplate = null; // for transaction support

    private CachedAggregateFeedDAO cachedFeedDAO;

    private EntriesDAO entriesDAO = null;

    private EntryCategoriesDAO entryCategoriesDAO = null;

    private long cacheConfigRevision = -1;

    // !!NOTE: Use this flag only if the cache will NEVER be used in the clustered environment.
    // Otherwise, all writes on all servers must be disabled before any server can turn on caching.
    private boolean noCache = false;

    //==============================
    // Local maps for quick lookups
    //==============================
    // cachedFeeds effected by workspace (workspace -> list of CachedAggregateFeed {cachedFeedId, workspacelist, scheme, locale})
    // Look up if a workspace is part of one or more cached feeds.
    // Note: whenever workspaceToCachedFeeds and cachedFeedById got changed, cacheConfigRevision should be updated.
    private final Map<String, Set<CachedAggregateFeed>> workspaceToCachedFeeds = new Hashtable<String, Set<CachedAggregateFeed>>();

    // Lookup map (cachedFeedId -> CachedAggregateFeed).
    private final Map<String, CachedAggregateFeed> cachedFeedById = new Hashtable<String, CachedAggregateFeed>();

    // list of existing workspaces used in $join (implicit join)
    private List<String> allWorkspaces = null;

    /**
     * Constructor
     */
    public AggregateFeedCacheManager() {}

    /**
     * Initialization for CacheManager.
     * <ul>
     * <li>It parses the spring configuration for cached feeds and build a map from workspace to aggregate feeds it is
     * part of and a map to lookup cached feeds by feed id</li>
     * <li>It compares with the existing list of cached feeds with the configured list of cached feeds. If the two
     * lists are different, it adds the missing ones and removes the non-listed ones.</li>
     * </ul>
     */
    private void init() {

        log.info("Initializing cached aggregate feeds.");
        // parse and initialize workspaceToCachedFeeds and cachedFeedIds
        parseConfigAndPopulateMaps(cacheConfigList);

        // Look up existing Feed Caches from CachedFeed table and compare them with the configured caches.
        List<CachedAggregateFeed> existingCacheList = cachedFeedDAO.getExistingCachedFeeds();

        if ((this.workspaceToCachedFeeds.isEmpty() || cachedFeedById.isEmpty()) && existingCacheList.isEmpty()) {
            return;
        }

        Set<String> existingCacheSet = new HashSet<String>();
        for (CachedAggregateFeed f : existingCacheList) {
            existingCacheSet.add(f.getCachedFeedId());
        }

        log.info("Number of existing cached feeds:" + existingCacheSet.size());

        Set<String> configuredCacheSet = new HashSet<String>();
        configuredCacheSet.addAll(cachedFeedById.keySet());

        // caches no longer in the configured list.
        Set<String> deletedSet = new HashSet<String>(existingCacheSet);
        deletedSet.removeAll(configuredCacheSet);

        // caches newly added to the configured list.
        Set<String> addedSet = new HashSet<String>(configuredCacheSet);
        addedSet.removeAll(existingCacheSet);

        log.info("Number of old cached feeds to delete:" + deletedSet.size());
        log.info("Number of new cached feeds to add:" + addedSet.size());

        // remove the missing caches
        for (String deletedCache : deletedSet) {
            this.removeFeedFromCacheInDB(deletedCache);
        }

        // add the new caches. If fail to add, remove from local maps.
        for (String addedCache : addedSet) {
            CachedAggregateFeed cfeed = cachedFeedById.get(addedCache);

            long start = System.currentTimeMillis();
            if (addFeedToCacheInDB(cfeed)) {
                long elapse = System.currentTimeMillis() - start;
                log.debug("Added cache:" + cfeed.toString() + "  takes " + (elapse/1000) + " secs.");
            } else { // failed to add
                log.warn("Failed to add feed cache : " + cfeed.toString());
                removeCachedFeedFromMaps(cfeed.getCachedFeedId());
            }
        }
        this.cacheConfigRevision = cachedFeedDAO.getCacheConfigRevision();
        log.info("cached aggregate feeds init done.");             
    }

    //====================
    // Setters and Getters
    //====================
    public void setCacheConfigList(List<String> cacheConfigList) {
        this.cacheConfigList = cacheConfigList;
    }

    public void setCachedFeedDAO(CachedAggregateFeedDAO cachedFeedDAO) {
        this.cachedFeedDAO = cachedFeedDAO;
    }

    TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setEntriesDAO(EntriesDAO entriesDAO) {
        this.entriesDAO = entriesDAO;
        // Inject cacheManager into entriesDAO to avoid circular dependency in spring config.
        this.entriesDAO.setCacheManager(this);
    }

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
        // Inject cacheManager into entryCategoriesDAO to avoid circular dependency in spring config.
        this.entryCategoriesDAO.setCacheManager(this);
    }

    public boolean getNoCache() {
        return this.noCache;
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    /**
     * Returns true if the given joined worksapces, locale and scheme are being cached.
     *
     * @param workspaces List of workspaces in the join of the aggregate feed.
     * @param scheme     scheme used in the aggregate feed
     * @param locale     locale used in the aggregate feed
     * @return feed id corresponding to the aggregate feed that is cached, or null if it is not cached.
     */
    public String isFeedCached(final List<String> workspaces, final Locale locale, final String scheme) {
        if (noCache || workspaces == null) {
            return null;
        }
        List<String> wkspaces = workspaces;
        if (wkspaces.isEmpty()) { // joinAll case
            wkspaces = getWorkspaceList();
        }
        String strLocale = "**_**";
        if (locale != null) {
            strLocale = locale.getLanguage() + "_" + locale.getCountry();
        }
        CachedAggregateFeed caf = new CachedAggregateFeed(null, getOrderedWorkspaceList(wkspaces), strLocale, scheme);
        String feedId = caf.getCachedFeedId();
        syncCacheConfigMaps();
        return (this.cachedFeedById.get(feedId) != null) ? feedId : null;
    }


    /**
     * Returns true if the workspace is in one of the cached joined workspaces.
     *
     * @param workspace workspace to check if it belongs to one of the cached aggregated feeds.
     * @return true if it does and false otherwise.
     */
    public boolean isWorkspaceInCachedFeeds(final String workspace) {
        if(this.noCache) {
            return false;
        }
        syncCacheConfigMaps();
        return isWorkspaceInCachedFeedsNosync(workspace);
    }

    /**
     * Returns true if the workspace is in one of the cached joined workspaces.
     * @param workspace  workspace to check
     * @param sync if true, the in-memory configuration maps will be sync'ed
     *        with the database before checking
     * @return   true if the workspace belongs to one or more cached feeds.
     */
    public boolean isWorkspaceInCachedFeeds(final String workspace, final boolean sync) {
        if(this.noCache) {
            return false;
        }
        if(sync) {
            syncCacheConfigMaps();
        }
        return isWorkspaceInCachedFeedsNosync(workspace);
    }

    //==============================
    // Methods to update Cached Timestamps
    //==============================

    /**
     * This method is called when an entry is added or updated
     *
     * @param entryMetaData Meta data of the entry that is added or updated.
     */
    public void updateCacheOnEntryAddOrUpdate(final EntryMetaData entryMetaData) {

        if(entryMetaData == null) {
            return;
        }
        
        String workspace = entryMetaData.getWorkspace();

        if (isWorkspaceInCachedFeedsNosync(workspace)) {
            List<EntryCategory> categories = entryMetaData.getCategories();
            if (categories.isEmpty()) {
                return;
            }
            
            Set<String> schemes = getSchemesFromCategories(categories);
            if (schemes.isEmpty()) {
                return;
            }

            // get cached feeds with matching workspace and scheme
            Map<String, CachedAggregateFeed> feedMap = this.getEffectedCachedFeeds(workspace, schemes);
            if (feedMap.keySet().isEmpty()) {
                return;
            }

            updateCacheOnEntryAddOrUpdateInDB(feedMap, categories, entryMetaData);
        }
    }

    /**
     * Batch update of feed cache timestamps. Note: entries in the list must have
     * workspaces in the cached feeds.
     * 
     * @param entryList a list of entries with workspaces in the cached feeds.
     */
    public void updateCacheOnEntryAddOrUpdateBatch(final List<EntryMetaData> entryList) {
        // steps for batch add/update:
            // get feedId -> terms for all entries to update or add in batch
            // get feedId+term -> max-timestamp
            // break up feedId -> terms for existing terms to update and new terms to add
            // update existing terms with new timestamps, and insert new terms and timestamps
        
        Map<String, Set<String>> batchTermsInFeed = new HashMap<String, Set<String>>();
        Map<String, Long>        maxTimestampMap  = new HashMap<String, Long>();

        // Get feed -> terms and feed+term -> timestamp maps
        prepareCacheInfoForBatch(entryList, batchTermsInFeed, maxTimestampMap);
        if(batchTermsInFeed.keySet().isEmpty()) { // none of the entries have a cached feed.
            return;
        }

        // query database to get existing feed->terms
        Map<String, Set<String>> existingTermsInFeed = getExistingCacheInfo(batchTermsInFeed);

        // subtract two maps to get newTerms in feed: newTermsInFeed = batchTermsInFeed - existingFeedTerms;
        Map<String,Set<String>> newTermsInFeed = getNewTermsInFeed( batchTermsInFeed, existingTermsInFeed);

        // existingTermsInFeed to update, newTermsInFeed to add and timestampMap to look up maximum timestamp.
        cachedFeedDAO.updateFeedCacheBatch(existingTermsInFeed, newTermsInFeed, maxTimestampMap);
    }

    /**
     * This method updates the cache when a category is added to an entry.
     *
     * @param category EntryCategory added to an entry
     */
    // TODO: Currently when a new entry with categories is inserted, the entry is first inserted
    // and then the categories are later added. This causes update cache to be called mutiple times.
    // Change the insert entry so that only one update is called for all categories in the entry.
    public void updateCacheOnCategoryAddedToEntry(final EntryCategory category) {

        Long entryStoreId = category.getEntryStoreId();

        EntryMetaData entryMetaData = (entryStoreId != null) ?
                                      entriesDAO.selectEntryByInternalId(entryStoreId) :
                                      entriesDAO.selectEntry(new BaseEntryDescriptor(
                                              category.getWorkspace(),
                                              category.getCollection(),
                                              category.getEntryId(),
                                              new Locale(category.getLanguage(), category.getCountry()))
                                      );

        if (!this.isWorkspaceInCachedFeedsNosync(entryMetaData.getWorkspace())) {
            return;
        }

        // merge old and new categories - This may be NO-OP if entry already have the category.
        List<EntryCategory> categories = entryMetaData.getCategories();
        if (!categories.contains(category)) {
            categories.add(category);
            entryMetaData.setCategories(categories);
        }
        // Update the cache
        updateCacheOnEntryAddOrUpdate(entryMetaData);
    }

    /**
     * This method updates the cache when EntryCategories are added to an entry. It assumes
     * that the categories in the list belongs to a single EntryStore.
     *
     * @param entryCategoryList list of <code>EntryCategory</code> added to the Entry.
     */
    public void updateCacheOnCategoriesAddedToEntry(final List<EntryCategory> entryCategoryList) {
        if (entryCategoryList == null || entryCategoryList.isEmpty()) {
            return;
        }

        EntryCategory category = entryCategoryList.get(0);

        Long entryStoreId = category.getEntryStoreId();

        EntryMetaData entryMetaData = (entryStoreId != null) ?
                                      entriesDAO.selectEntryByInternalId(entryStoreId) :
                                      entriesDAO.selectEntry(new BaseEntryDescriptor(
                                              category.getWorkspace(),
                                              category.getCollection(),
                                              category.getEntryId(),
                                              new Locale(category.getLanguage(), category.getCountry()))
                                      );
        if (!this.isWorkspaceInCachedFeedsNosync(entryMetaData.getWorkspace())) {
            return;
        }
        
        // merge old and new categories 
        List<EntryCategory> categories = entryMetaData.getCategories();
        for(EntryCategory cat: entryCategoryList) {
            if (!categories.contains(cat)) {
                categories.add(cat);
            }
        }
        entryMetaData.setCategories(categories);

        // Update the cache
        updateCacheOnEntryAddOrUpdate(entryMetaData);
    }

    /**
     * This method is called when an entry is obliterated.
     * @param entryMetaData entry to obliterate
     * @param categoriesToRemove categories to remove
     */
    public void updateCacheOnEntryObliteration(final EntryMetaData entryMetaData, final List<EntryCategory> categoriesToRemove) {

        Set<String> schemeSet = getSchemesFromCategories(categoriesToRemove);
        updateCacheOnSchemeRemoval(entryMetaData, schemeSet);
    }

    /**
     * Update the cache on removing a category  from an Entry
     *
     * @param entryQuery EntryDescriptor of the entry from which the category is remove
     * @param scheme     Scheme of the category removed.
     */
    public void updateCacheOnCategoryRemovedFromEntry(final EntryDescriptor entryQuery, final String scheme) {

        EntryMetaData entryMetaData = (entryQuery instanceof EntryMetaData) ? (EntryMetaData) entryQuery:
                                       entriesDAO.selectEntry(entryQuery);
        Set<String> schemeSet = new HashSet<String>();
        schemeSet.add(scheme);
        updateCacheOnSchemeRemoval(entryMetaData, schemeSet);

    }

    /**
     * Update the cache on removing a category.
     *
     * @param entryCategory Cateogry that is removed.
     */
    public void updateCacheOnCategoryRemoval(final EntryCategory entryCategory) {

        EntryMetaData entryMetaData = entriesDAO.selectEntryByInternalId(entryCategory.getEntryStoreId());
        Set<String> schemeSet = new HashSet<String>();
        schemeSet.add(entryCategory.getScheme());
        updateCacheOnSchemeRemoval(entryMetaData, schemeSet);
    }

    /**
     * Update the cache on removing a list of categories. The list is assumed to
     * belong to a single entry.
     *
     * @param entryCategoryList Cateogries to remove.
     */
    public void updateCacheOnCategoriesRemoval(final List<EntryCategory> entryCategoryList) {

        EntryCategory entryCategory = entryCategoryList.get(0);
        EntryMetaData entryMetaData = entriesDAO.selectEntryByInternalId(entryCategory.getEntryStoreId());

        Set<String> schemeSet = getSchemesFromCategories(entryCategoryList);
        updateCacheOnSchemeRemoval(entryMetaData, schemeSet);
    }


    /**
     * Remove all cached entries in the cache table (AggregateFeedTimestamp)
     */
    public synchronized void removeAllCachedTimestamps() {
        cachedFeedDAO.removeAllCachedTimestamps();
    }

    /**
     * This method rebuild the Cache for a workspace when its categories got changed. This is for a more
     * drastic situation such as deleting all categories on a workspace.
     *
     * @param workspace workspace to rebuild the cached entries for.
     */
    public void rebuildCachedTimestampByWorkspace(final String workspace) {

        Set<CachedAggregateFeed> feeds = this.workspaceToCachedFeeds.get(workspace);
        if (feeds == null) {
            return;
        }
        for (CachedAggregateFeed caf : feeds) {
            rebuildCachedTimestampByFeed(caf);
        }
    }

    //==============================
    // Manage Cached Aggregate Feeds
    //==============================

    /**
     * Cache a given aggregate feed. This call is not for multiple server environment.
     * It is used for Unit tests.
     *
     * @param cacheCfg a string specifying a feed to cache in the format:
     *                 $join(list-of-workspaces),scheme, locale
     * @return cachedFeedId
     */
    public String cacheAggregateFeed(final String cacheCfg) {
        CachedAggregateFeed feed = parseConfig(cacheCfg);

        if (cachedFeedById.get(feed.getCachedFeedId()) == null) {
            this.addFeedToCacheInDB(feed);  // Lock held here
            this.addCachedFeedToMaps(feed);
            this.cacheConfigRevision = cachedFeedDAO.getCacheConfigRevision();
        }
        return feed.getCachedFeedId();
    }

    /**
     * Cache a given list of aggregate feeds. Note: This call is not for multiple server environment.
     * It is used for unit tests.
     *
     * @param cacheCfgList a listof strings specifying each feed to cache in the format:
     *                     $join(list-of-workspaces),scheme, locale
     * @return a list of cachedFeedId in the same order as the given feed list.
     */
    public List<String> cacheAggregateFeed(final List<String> cacheCfgList) {
        List<String> feedIds = new ArrayList<String>();
        for (String cacheCfg : cacheCfgList) {
            String feedId = cacheAggregateFeed(cacheCfg);
            feedIds.add(feedId);
        }
        return feedIds;
    }

    /**
     * Remmove from cache a feed given by an entry of the format: $join(list-of-workspaces),scheme,locale.
     *
     * @param cacheConfig configuration string of an aggregate feed.
     */
    public void removeCachedAggregateFeed(final String cacheConfig) {
        CachedAggregateFeed feed = parseConfig(cacheConfig);
        removeCachedAggregateFeedByFeedId(feed.getCachedFeedId());
    }

    /**
     * Remmove from cache given feeds each specified by an entry of the format:
     * $join(list-of-workspaces),scheme,locale.
     *
     * @param cacheCfgList a list of feed entries
     */
    public void removeCachedAggregateFeeds(final List<String> cacheCfgList) {
        for (String cacheConfig : cacheCfgList) {
            removeCachedAggregateFeed(cacheConfig);
        }
    }

    /**
     * Remove the feed with given (single) feed id from cache
     *
     * @param feedId a cached feed id
     */
    public void removeCachedAggregateFeedByFeedId(final String feedId) {
        if (feedId == null) {
            return;
        }
        // Remove from db
        this.removeFeedFromCacheInDB(feedId); // Lock held here.
        this.removeCachedFeedFromMaps(feedId);
        this.cacheConfigRevision = cachedFeedDAO.getCacheConfigRevision();
    }

    /**
     * Remove the feeds with given feed Ids from cache
     *
     * @param feedIds a list of cached feed ids.
     */
    public void removeCachedAggregateFeedsByFeedIds(final List<String> feedIds) {
        if (feedIds != null && !feedIds.isEmpty()) {
            for (String feedId : feedIds) {
                removeCachedAggregateFeedByFeedId(feedId);
            }
        }
    }

    /**
     * Returns the current Cache configuration revision value
     * @return cache configuration revision number
     */
    public long getCacheConfigRevision() {
        return this.cacheConfigRevision;
    }

    /**
     * Remove existing caches
     */
    public void removeExistingCaches() {
        List<CachedAggregateFeed> existingCaches = cachedFeedDAO.getExistingCachedFeeds();
        List<String> feedIds = new ArrayList<String>();
        for(CachedAggregateFeed caf: existingCaches) {
            feedIds.add(caf.getCachedFeedId());
        }
        removeCachedAggregateFeedsByFeedIds(feedIds);
    }

    // ----- private methods -----

    private boolean isWorkspaceInCachedFeedsNosync(final String workspace) {
        return (this.workspaceToCachedFeeds.get(workspace) != null);
    }

    private void syncCacheConfigMaps() {
        if(!this.noCache) {
            long currentRevision = cachedFeedDAO.getCacheConfigRevision();
            if( this.cacheConfigRevision != currentRevision) {
                reloadCacheConfigMaps(currentRevision);
            }
        }
    }

    synchronized void reloadCacheConfigMaps(long currentRevision) {
      // caller already should hold EntryStore lock here.
        List<CachedAggregateFeed> existingCacheList = cachedFeedDAO.getExistingCachedFeeds();
        Set<String> cachedFeedIdsFromDB = new HashSet<String>();
        Set<String> deletedSet = new HashSet<String>(cachedFeedById.keySet());

        // handle new cached feeds
        for (CachedAggregateFeed f : existingCacheList) {
            if(this.cachedFeedById.get(f.getCachedFeedId()) == null) {
                this.addCachedFeedToMaps(f);
            }
            // collect feed ids for use in handling deleted feeds
            cachedFeedIdsFromDB.add(f.getCachedFeedId());
        }

        // handle deleted feeds
        deletedSet.removeAll(cachedFeedIdsFromDB);

        for(String deleted: deletedSet) {
            removeCachedFeedFromMaps(deleted);
        }
        this.cacheConfigRevision = currentRevision;
    }

    private  void updateCacheOnSchemeRemoval(final EntryMetaData entryMetaData, final Set<String> schemes) {

        if (entryMetaData == null) {
            return;
        }

        // get schemes from Entry
        Set<String> schemeSet = schemes;

        if (schemes == null || schemes.isEmpty()) {
            List<EntryCategory> categories = entryMetaData.getCategories();
            schemeSet = (categories == null)? new HashSet<String>() :
                        getSchemesFromCategories(categories);
        }

        if (schemeSet.isEmpty()) {
            return;
        }

        // Filter out feeds that do not match the workspace and scheme.
        Set<String> feedIds = this.getEffectedCachedFeeds(entryMetaData.getWorkspace(), schemeSet).keySet();
        if (feedIds == null || feedIds.isEmpty()) {
            return;
        }

        // To narrow the number of feeds for which the caches are to be rebuild, get the subset of the
        // effected feeds which have terms with timestamp value matching the deleted entry. For those feeds
        // which do not have terms with matching timestamp, this deleted entry does not contribute to
        // the cached timestamp in the feeds and therefore those feeds do not need to be rebuilt.
        List<AggregateFeedTerm> feedTermsToUpdate = cachedFeedDAO.getFeedTermsWithMatchingTimestamp(
                new ArrayList<String>(feedIds),
                entryMetaData.getUpdateTimestamp());

        if (feedTermsToUpdate == null || feedTermsToUpdate.isEmpty()) {
            return;
        }

        // group all terms for a feedId, i.e. feedId --> a list of terms in the feed with the timestamp
        Map<String, List<String>> feedToTerms = new HashMap<String, List<String>>();
        for (AggregateFeedTerm ft : feedTermsToUpdate) {
            List<String> terms = feedToTerms.get(ft.getCachedFeedId());
            if (terms == null) {
                terms = new ArrayList<String>();
                feedToTerms.put(ft.getCachedFeedId(), terms);
            }
            terms.add(ft.getTerm());
        }

        // Update the database
        rebuildCacheByFeedTermsInDB(feedTermsToUpdate, feedToTerms);
    }

    /*
    * Rebuild the cached timestamps for the given feed.
    *
    * @param CachedAggregateFeed Cached aggregate feed to rebuild the cached time stamps.
    */
    private synchronized void rebuildCachedTimestampByFeed(final CachedAggregateFeed feed) {
        //TODO: May not need lock
        try {
            executeTransactionally(new TransactionalTask<Object>() {
                public Object execute() {
                    removeCachedTimestampsByFeedId(feed.getCachedFeedId());
                    addCachedTimestamps(feed.getJoinWorkspaceList(), feed.getLocale(), feed.getScheme(), feed.getCachedFeedId());
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while rebuilding cached feed", e);
        }
    }


    /*
    * Remove cached timestamps for a gvien feed id.
    *
    * @param feedId cached feed id
    */
    private void removeCachedTimestampsByFeedId(final String feedId) {
        // caller already hold EntryStore lock.
        if (feedId != null) {
            cachedFeedDAO.removeAggregateFeedTimestampsById(feedId);
        }
    }

    private synchronized void updateCacheOnEntryAddOrUpdateInDB(final Map<String, CachedAggregateFeed> feedMap,
                                                                final List<EntryCategory> categories,
                                                                final EntryMetaData entryMetaData) {
        // No lock is acquired since the caller should already have EntryStore lock.
        cachedFeedDAO.updateFeedCacheOnEntryAddOrUpdate(feedMap, categories,
                                                            entryMetaData.getLocale(),
                                                            entryMetaData.getUpdateTimestamp());

    }

    /*
     * Remove the feed being cached by removing cached entries and the entry in the CachedFeed table.
     *
     * @param feedId MD5 hash Id of the aggregate feed to remove from the cache.
     */
    private synchronized boolean removeFeedFromCacheInDB(final String feedId) {
        try {
            executeTransactionally(new TransactionalTask<Object>() {
                public Object execute() {
                    // This need to be removed first because of FK.
                    cachedFeedDAO.removeAggregateFeedTimestampsById(feedId);
                    cachedFeedDAO.removeFeedFromCacheById(feedId);
                    cachedFeedDAO.updateCacheConfigRevision();
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while removing cached feed", e);
            return false;
        }
        return true;
    }


    /*
     * Add a new feed to Cache and cache timestamps
     *
     * @param CachedAggregateFeed feed to be saved in the database.
     *
     */
    private synchronized boolean addFeedToCacheInDB(final CachedAggregateFeed caf) {
        try {
            executeTransactionally(new TransactionalTask<CachedAggregateFeed>() {
                public CachedAggregateFeed execute() {

                    // Create CachedFeed first because of FK constraint
                    cachedFeedDAO.addNewFeedToCache(caf);
                    cachedFeedDAO.cacheAggregateFeedTimestamps(caf.getJoinWorkspaceList(), caf.getLocale(),
                                                               caf.getScheme(), caf.getCachedFeedId());
                    cachedFeedDAO.updateCacheConfigRevision();
                    return caf;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while adding cached feed", e);
            return false;
        }
        return true;
    }

    /*
     * Rebuild cache for each category
     */
    private synchronized void rebuildCacheByFeedTermsInDB(final List<AggregateFeedTerm> feedTermsToUpdate,
                                                          final Map<String,List<String>> map) {
        try {
            executeTransactionally(new TransactionalTask<CachedAggregateFeed>() {
                public CachedAggregateFeed execute() {
                    // Remove timestamps corresponding to feeds and terms.
                    cachedFeedDAO.removeAggregateFeedTimestampsByTerms(feedTermsToUpdate);
                    for (String feedId : map.keySet()) {
                        
                        CachedAggregateFeed feed = cachedFeedById.get(feedId);
                        cachedFeedDAO.cacheAggregateFeedTimestampsByTerms(feed.getJoinWorkspaceList(),
                                                                          feed.getLocale(),
                                                                          feed.getScheme(),
                                                                          feedId,
                                                                          map.get(feedId));
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while updating cached feed", e);
        }
    }

    /*
     * Cache timestamps for the given feed. This should be called within a transaction
     *
     * @param workspaces workspaces to compute and insert timestamps into the cache table (AggregateFeedTimestamp)
     * @param locale     locale of the aggregate feed
     * @param scheme     scheme of the aggregate feed
     * @param feedId     MD5 hashed id of the feed
     */
    private void addCachedTimestamps(final List<String> workspaces, final String locale, final String scheme, final String feedId) {
        // Already holds lock
        cachedFeedDAO.cacheAggregateFeedTimestamps(workspaces, locale, scheme, feedId);
    }

    /*
     * Returns feed Ids for cached aggregate feeds effected by the given workspace, locale and categories.
     *
     * @param workspace workspace of the entry that is updated.
     * @param schemes   a set of schemes of the entry that is updated.
     * @return a list of cached feed ids effected by the entry change.
     */

    private Map<String, CachedAggregateFeed> getEffectedCachedFeeds(final String workspace, final Set<String> schemes) {

        Map<String, CachedAggregateFeed> cachedFeedIds = new HashMap<String, CachedAggregateFeed>();
        Set<CachedAggregateFeed> feedsWithWorkspace = this.workspaceToCachedFeeds.get(workspace);
        if (feedsWithWorkspace != null) {
            for (CachedAggregateFeed caf : feedsWithWorkspace) {
                // get workspaces with matching schemes
                // if the schemes is empty, pick all workspaces.
                if (schemes.contains(caf.getScheme()) || schemes.isEmpty()) {
                    // scheme and locale matches with this feed
                    cachedFeedIds.put(caf.getCachedFeedId(), caf);
                }
            }
        }
        return cachedFeedIds;
    }

    // === Support methods for Batch update ===
    // Returns Map<FeedId -> terms from entries
    // Map<FeedId+Term, Timestamp>
    private  void prepareCacheInfoForBatch(final List<EntryMetaData> entryList,
                              Map<String, Set<String>> termsInFeed,
                              Map<String, Long> maxTimestampForFeedTerm ) {

        for(EntryMetaData entry: entryList) {
            // no need to update cache if workspace is not in cached feeds.
            if(!this.isWorkspaceInCachedFeedsNosync(entry.getWorkspace())) {
                continue;
            }
            List<EntryCategory> categories = entry.getCategories();
            if(categories == null || categories.isEmpty()) {
                continue;
            }

            // build maps Maps<feedId --> set of terms> and Map<feedId+term --> maxtimestamp
            Set<CachedAggregateFeed> feedsWithWorkspace = this.workspaceToCachedFeeds.get(entry.getWorkspace());
            if (feedsWithWorkspace != null) {
                for (CachedAggregateFeed caf : feedsWithWorkspace) {
                    for(EntryCategory cat: categories) {
                        if(cat.getScheme() == null || cat.getTerm() == null) {
                            continue;
                        }
                        if(cat.getScheme().equals(caf.getScheme())) {
                            String feedId = caf.getCachedFeedId();

                            // get all the terms in the given feed
                            Set<String> terms = termsInFeed.get(feedId);
                            if(terms == null) {
                                terms = new HashSet<String>();
                                termsInFeed.put(feedId, terms);
                            }
                            terms.add(cat.getTerm());

                            // get Max TS among entries with same cachedFeedid and term
                            String feedTerm = feedId + cat.getTerm();
                            Long ts = maxTimestampForFeedTerm.get(feedTerm);
                            if(ts == null || (ts < entry.getUpdateTimestamp())) {
                                ts = entry.getUpdateTimestamp();
                            }
                            maxTimestampForFeedTerm.put(feedTerm, ts);
                        }
                    }
                }
            }
        }
    }

    private  Map<String, Set<String>> getExistingCacheInfo(final Map<String,Set<String>> feedIdTerms ) {
        return cachedFeedDAO.getTermsInFeed(feedIdTerms);
    }

    private Map<String, Set<String>> getNewTermsInFeed(final
                                                Map<String, Set<String>> existing,
                                                Map<String, Set<String>> batch) {
        Map<String, Set<String>> newInBatch = new HashMap<String, Set<String>>();
        for(String feedId: batch.keySet()) {
            Set<String> newTerms = batch.get(feedId);
            Set<String> existingTerms = existing.get(feedId);
            if(existingTerms == null) {
                newInBatch.put(feedId, newTerms);
            } else {
                Set<String> diff = new HashSet<String>(newTerms);
                diff.removeAll(existingTerms);
                if(!diff.isEmpty()){
                    newInBatch.put(feedId, diff);
                }
            }
        }
        return newInBatch;
    }

    //=== Parse spring configuration strings for cached aggregate feeds.

    private final Pattern jws = Pattern.compile("\\(|\\)");
    private final Pattern sep = Pattern.compile(",");

    private void parseConfigAndPopulateMaps(final List<String> cachelist) {
        if (cacheConfigList == null || cacheConfigList.isEmpty()) {
            return;
        }
        for (String cacheSetEntry : cachelist) {
            CachedAggregateFeed cachedFeed = parseConfig(cacheSetEntry);
            addCachedFeedToMaps(cachedFeed);
        }
    }

    // Parse config string
    private CachedAggregateFeed parseConfig(final String configuredFeedStr) {

        String[] wsSchemeLocale = trimList(jws.split(configuredFeedStr)); // split on ( or )
        boolean joinAll = (wsSchemeLocale.length == 1);

        List<String> wkspaces = (joinAll) ? getWorkspaceList() : Arrays.asList(trimList(sep.split(wsSchemeLocale[1])));
        String[] schemeLocale = trimList(sep.split(wsSchemeLocale[(!joinAll) ? 2 : 0]));
        String scheme = schemeLocale[1];
        String locale = (schemeLocale.length > 2) ? schemeLocale[2] : "**_**";
        String workspaces = getOrderedWorkspaceList(wkspaces);

        return new CachedAggregateFeed(null, workspaces, locale, scheme);
    }

    // Retrieve all workspaces defined in the system.

    List<String> getWorkspaceList() {
        if (allWorkspaces == null) {
            allWorkspaces = entriesDAO.listWorkspaces();
        }
        return allWorkspaces;
    }

    // Add the configured feed to local in-memory maps.

    private void addCachedFeedToMaps(final CachedAggregateFeed cachedFeed) {

        cachedFeedById.put(cachedFeed.getCachedFeedId(), cachedFeed);

        List<String> workspaces = cachedFeed.getJoinWorkspaceList();
        for (String workspace : workspaces) {
            Set<CachedAggregateFeed> feedSet = this.workspaceToCachedFeeds.get(workspace);
            if (feedSet == null) {
                feedSet = new HashSet<CachedAggregateFeed>();
                this.workspaceToCachedFeeds.put(workspace, feedSet);
            }
            feedSet.add(cachedFeed);
        }
    }

    // Remove the configured feed from local in-memory maps.

    private void removeCachedFeedFromMaps(String cachedFeedId) {
        CachedAggregateFeed caf = cachedFeedById.get(cachedFeedId);
        if(caf != null) {
            cachedFeedById.remove(cachedFeedId);
            // remove from workspaceToCachedFeeds
            for(String wkspace: caf.getJoinWorkspaceList()) {
                Set<CachedAggregateFeed> cafSet = workspaceToCachedFeeds.get(wkspace);
                cafSet.remove(caf);
                if(cafSet.isEmpty()) {
                    workspaceToCachedFeeds.remove(wkspace);
                }
            }
        }
    }

    private String[] trimList(String[] list) {
        String[] trimmed = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            trimmed[i] = list[i].trim();
        }
        return trimmed;
    }

    //=== Utility to order the workspace list ==========

    private String getOrderedWorkspaceList(final List<String> workspaceList) {
        List<String> list = new ArrayList<String>(workspaceList);
        Collections.sort(list);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String wksp : list) {
            if (!first) {
                builder.append(",");
            }
            builder.append(wksp);
            first = false;
        }
        return builder.toString();
    }

    // Get Schemes from EntryCategories
    
    private Set<String> getSchemesFromCategories(final List<EntryCategory> categories) {
        Set<String> schemes = new HashSet<String>();
        for(EntryCategory cat: categories) {
            String scheme = cat.getScheme();
            if(scheme != null && !"".equals(scheme)) {
                schemes.add(scheme);
            }
        }
        return schemes;
    }

    //==============================
    // Transaction support
    //==============================

    protected interface TransactionalTask<T> {
        T execute();
    }

    <T> T executeTransactionally(final TransactionalTask<T> task) {
        return (T) getTransactionTemplate().execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus transactionStatus) {
                cachedFeedDAO.acquireLock();
                return task.execute();
            }
        });
    }

}
