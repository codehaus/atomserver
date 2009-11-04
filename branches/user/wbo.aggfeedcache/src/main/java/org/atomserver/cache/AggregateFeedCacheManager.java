/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.cache;

import org.atomserver.core.EntryMetaData;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.dbstore.dao.AggregateFeedCacheDAO;
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
 * 
 * Cache Manager class handles the overall aggregate feed caching functions. It tracks which
 * joined workspaces and scheme are cached, updates the cache when an entry or a category is added,
 * updated or deleted. It also manages the sets of cached values by adding newly configured
 * ones and deleting the ones that are no longer listed in the spring configuration.
 * Two tables are related to the Cache Manager: AggregateFeedTimestampCache and CachedFeed.
 * AggregateFeedTimestampCache stores the actual cache entry for aggregate feed query based on cached
 * feed Id. CachedFeed table stores that mapping of the cached feed Id to the aggregate feed specified
 * by joined workspaces, locale and scheme.
 *
 */
public class AggregateFeedCacheManager {

    static private final Log log = LogFactory.getLog(AggregateFeedCacheManager.class);

    //==============================
    // Injected by spring IOC
    //==============================
    private List<String> cacheList;

    private AggregateFeedCacheDAO feedCacheDAO;

    private TransactionTemplate transactionTemplate;      // Transaction support

    private EntriesDAO entriesDAO = null;

    private EntryCategoriesDAO entryCategoriesDAO = null;

    //==============================
    // Local maps for quick lookups
    //==============================
    // workspace -> list of CachedAggregateFeed {cachedFeedId, workspacelist, scheme, locale}
    // Look up if a workspace is part of one or more cached feeds.
    private final Map<String, Set<CachedAggregateFeed>> workspaceToCachedFeeds = new HashMap<String, Set<CachedAggregateFeed>>();

    // Lookup map for aggredate feed id to CacheAggregateFeed (cachedFeedId -> CachedAggregateFeed).
    private final Map<String, CachedAggregateFeed> cachedFeedById = new HashMap<String, CachedAggregateFeed>();

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

        // initialize when all dependent fields have been injected.
        if (cacheList == null || feedCacheDAO == null || transactionTemplate == null) {
            return;
        }

        // parse and initialize workspaceToCachedFeeds and feedCacheSetIds
        parseConfigAndPopulateMaps(cacheList);

        if (this.workspaceToCachedFeeds.isEmpty() || cachedFeedById.isEmpty()) {
            return;
        }

        // Look up existing Feed Caches from CachedFeed table and compare them with the configured caches.
        List<CachedAggregateFeed> existingCacheList = feedCacheDAO.getExistingCachedFeeds();
        Set<String> existingCacheSets = new HashSet<String>();
        for (CachedAggregateFeed f : existingCacheList) {
            existingCacheSets.add(f.getCachedFeedId());
        }

        if(log.isDebugEnabled()) {
           log.debug("Number of existing cached feeds:" + existingCacheSets.size());
        }
        Set<String> configuredCacheSets = new HashSet<String>();
        configuredCacheSets.addAll(cachedFeedById.keySet());

        // caches no longer in the configured list.
        Set<String> deletedCaches = new HashSet<String>();
        deletedCaches.addAll(existingCacheSets);
        deletedCaches.removeAll(configuredCacheSets);


        // caches newly added to the configured list.
        Set<String> addedCaches = new HashSet<String>();
        addedCaches.addAll(configuredCacheSets);
        addedCaches.removeAll(existingCacheSets);

        if(log.isDebugEnabled()) {
            log.debug("Number of old cached feeds to delete:" + deletedCaches.size());
            log.debug("Number of new cached feeds to add:" + addedCaches.size());
        }

        // remove the missing caches
        for (String deletedCache : deletedCaches) {
            this.removeFeedFromCache(deletedCache);
        }

        // add the new caches.
        for (String addedCache : addedCaches) {
            CachedAggregateFeed fcs = cachedFeedById.get(addedCache);
            this.addFeedToCache(fcs.getJoinWorkspaceList(), fcs.getLocale(), fcs.getScheme(), fcs.getCachedFeedId());
        }
    }

    //====================
    // Setters and Getters
    //====================
    public void setCacheList(List<String> cacheList) {
        this.cacheList = cacheList;
        init();
    }

    public void setFeedCacheDAO(AggregateFeedCacheDAO feedCacheDAO) {
        this.feedCacheDAO = feedCacheDAO;
        init();
    }

    TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
        init();
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

    //==============================
    // Methods to update Cached Timestamps
    //==============================

    /**
     * Returns true if the given joined worksapces, locale and scheme are being cached.
     *
     * @param workspaces List of workspaces in the join of the aggregate feed.
     * @param scheme     scheme used in the aggregate feed
     * @param locale     locale used in the aggregate feed
     * @return feed id corresponding to the aggregate feed that is cached, or null if it is not cached.
     */
    public String isFeedCached(List<String> workspaces, Locale locale, String scheme) {
        if (workspaces == null || workspaces.isEmpty()) {
            return null;
        }
        String strLocale = "**_**";
        if (locale != null) {
            strLocale = locale.getLanguage() + "_" + locale.getCountry();
        }
        CachedAggregateFeed caf = new CachedAggregateFeed(null, getOrderedWorkspaceList(workspaces), strLocale, scheme);
        String feedId = caf.getCachedFeedId();
        return (this.cachedFeedById.get(feedId) != null) ? feedId : null;
    }

    /**
     * Returns true if the given workspace is part of the joined workspaces in the aggregate feeds that are cached.
     *
     * @param workspace workspace to check if it belongs to one of the cached aggregated feeds.
     * @return true if it does and false otherwise.
     */
    public boolean isWorkspaceInCachedFeeds(String workspace) {
        return (this.workspaceToCachedFeeds.get(workspace) != null);
    }

    /**
     * This method is called when an entry is added or updated
     *
     * @param entryMetaData Meta data of the entry that is added or updated.
     */
    public void updateCacheOnEntryAddOrUpdate(EntryMetaData entryMetaData) {
        String workspace = entryMetaData.getWorkspace();
        if (isWorkspaceInCachedFeeds(workspace)) {
            List<EntryCategory> categories = entryMetaData.getCategories();
            if (categories.isEmpty()) {
                return;
            }
            boolean nullScheme = true;
            for (EntryCategory cat : categories) {
                if (cat.getScheme() != null) {
                    nullScheme = false;
                    break;
                }
            }
            if (nullScheme) {
                return;
            }

            Set<String> joinedWorkspaces = this.getEffectedCachedFeeds(workspace, categories);
            if (joinedWorkspaces.isEmpty()) {
                return;
            }

            // filter out scheme with null
            List<EntryCategory> cats = new ArrayList<EntryCategory>();
            for (EntryCategory cat : categories) {
                if (cat.getScheme() != null) {
                    cats.add(cat);
                }
            }
            feedCacheDAO.updateFeedCacheOnEntryAddOrUpdate(joinedWorkspaces,
                                                           cats, //categories,
                                                           entryMetaData.getUpdateTimestamp());
        }
    }

    /**
     * This method is called to updated the cache when a category is added to an entry.
     *
     * @param category
     */
    // TODO: Currently when a new entry with categories is inserted, the entry is first inserted
    // and then the categories are later added. This causes update cache to be called twice, one
    // for entry add, and another for category add. This needs to be changed so that only one
    // update is called for any entry add.
    public void updateCacheOnEntryCategoryAddedToEntry(EntryCategory category) {

        Long entryStoreId = category.getEntryStoreId();

        EntryMetaData entryMetaData = (entryStoreId != null) ?
                                      entriesDAO.selectEntryByInternalId(entryStoreId) :
                                      entriesDAO.selectEntry(new BaseEntryDescriptor(
                                              category.getWorkspace(),
                                              category.getCollection(),
                                              category.getEntryId(),
                                              new Locale(category.getLanguage(), category.getCountry()))
                                      );

        if (!this.isWorkspaceInCachedFeeds(entryMetaData.getWorkspace())) {
            return;
        }
        // merge old and new categories
        List<EntryCategory> categories = entryMetaData.getCategories();
        if (!categories.contains(category)) {
            categories.add(category);
            entryMetaData.setCategories(categories);
        }
        // Update the cache
        updateCacheOnEntryAddOrUpdate(entryMetaData);
    }

    /**
     * This method is called when an entry is deleted.
     *
     * @param entryMetaData Meta data of the entry that is deleted.
     */
    public void updateCacheOnEntryDelete(EntryMetaData entryMetaData) {
        updateCacheOnCategoryRemovedFromEntry(entryMetaData, null);
    }

    public void updateCacheOnCategoryRemovedFromEntry(EntryDescriptor entryQuery, String scheme) {
        EntryMetaData entryMetaData = entriesDAO.selectEntry(entryQuery);
        updateCacheOnCategoryRemovedFromEntry(entryMetaData, scheme);
    }

    public void updateCacheOnCategoryRemoval(EntryCategory entryCategory) {
        EntryMetaData entryMetaData = entriesDAO.selectEntryByInternalId(entryCategory.getEntryStoreId());
        updateCacheOnCategoryRemovedFromEntry(entryMetaData, entryCategory.getScheme());
    }

    /**
     * Update the cache entries on removing a category
     *
     * @param entryMetaData Meta data of the entry from which the scheme is removed.
     * @param scheme        the scheme that is removed.
     */
    public void updateCacheOnCategoryRemovedFromEntry(final EntryMetaData entryMetaData, final String scheme) {
        if(entryMetaData == null) {
            return;
        }
        List<EntryCategory> categories;
        if (scheme == null) {
            categories = entryMetaData.getCategories();
        } else {
            categories = new ArrayList<EntryCategory>();
            if(entryMetaData.getCategories() == null) {
                for (EntryCategory cat : entryMetaData.getCategories()) {
                    if (scheme.equals(cat.getScheme())) {
                        categories.add(cat);
                    }
                }
            }
        }

        Set<String> feedIds = this.getEffectedCachedFeeds(entryMetaData.getWorkspace(), categories);
        if (feedIds == null || feedIds.isEmpty()) {
            return;
        }

        // To narrow the number of feeds for which the caches are to be rebuild, get the subset of the
        // effected feeds which have terms with timestamp value matching the deleted entry. For those feeds
        // which do not have terms with matching timestamp, this deleted entry does not contribute to
        // the cached timestamp.
        List<String> feedsToUpdate = feedCacheDAO.getFeedsWithMatchingTimestamp(new ArrayList<String>(feedIds),
                                                                                    entryMetaData.getUpdateTimestamp());
        for (String feedId : feedsToUpdate) {
            CachedAggregateFeed caf = this.cachedFeedById.get(feedId);
            rebuildCachedTimestampByFeed(caf.getJoinWorkspaceList(), caf.getLocale(), caf.getScheme(), caf.getCachedFeedId());
        }
    }

    /**
     * Delete all the feeds with the given workspace:
     *
     * @param workspace the workspace to delete the cached entries from.
     */
    public void removeCachedTimestampsByWorkspace(String workspace) {
        Set<CachedAggregateFeed> cachedfeeds = this.workspaceToCachedFeeds.get(workspace);
        if (cachedfeeds == null || cachedfeeds.isEmpty()) {
            return;
        }
        for (CachedAggregateFeed feed : cachedfeeds) {
            feedCacheDAO.removeAggregateFeedTimestampsById(feed.getCachedFeedId());
        }
    }

    /**
     * Remove all cached entries in the cache table (AggregateFeedTimestamp)
     */
    public void removeAllCachedTimestamps() {
        feedCacheDAO.removeAllCachedTimestamps();
    }

    /**
     * This method rebuild the Cache for a workspace when its categories got changed. This is for a more
     * drastic situation such as deleting all categories on a workspace.
     *
     * @param workspace workspace to rebuild the cached entries for.
     */
    public void rebuildCachedTimestampByWorkspace(String workspace) {

        Set<CachedAggregateFeed> feedIds = this.workspaceToCachedFeeds.get(workspace);
        if (feedIds == null) {
            return;
        }
        for (CachedAggregateFeed caf : feedIds) {
            rebuildCachedTimestampByFeed(caf.getJoinWorkspaceList(), caf.getLocale(), caf.getScheme(), caf.getCachedFeedId());
        }
    }

    /**
     * Rebuild the cached timestamps for the given feed.
     *
     * @param workspaces List of the workspaces in the aggregate feed
     * @param locale     locale of the aggregate feed
     * @param scheme     scheme of the aggregate feed
     * @param feedId     MD5 hash id of the feed.
     */
    public void rebuildCachedTimestampByFeed(final List<String> workspaces,
                                             final String locale,
                                             final String scheme,
                                             final String feedId) {
        try {
            executeTransactionally(new TransactionalTask<Object>() {
                public Object execute() {
                    removeCachedTimestampsByFeedId(feedId);
                    addCachedTimestamps(workspaces, locale, scheme, feedId);
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while rebuilding cached feed", e);
        }
    }

    /**
      * Remove cached timestamps for a gvien feed id.
      *
      * @param feedId MD5 hashed id of the feed.
      */
    private void removeCachedTimestampsByFeedId(String feedId) {
        if(feedId != null) {
            feedCacheDAO.removeAggregateFeedTimestampsById(feedId);
        }
    }

    /**
     * Remove the feed being cached by removing cached entries and the entry in the CachedFeed table.
     *
     * @param feedId MD5 hash Id of the aggregate feed to remove from the cache.
     */
    private void removeFeedFromCache(final String feedId) {
        try {
            executeTransactionally(new TransactionalTask<Object>() {
                public Object execute() {
                    feedCacheDAO.removeAggregateFeedTimestampsById(feedId);  // This need to be removed first because of FK.
                    feedCacheDAO.removeFeedFromCacheById(feedId);
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while removing cached feed", e);
        }
    }


    /**
     * Add a new feed to Cache and cache timestamps
     *
     * @param workspaces List of the workspaces in the aggregate feed
     * @param locale     locale of the aggregate feed
     * @param scheme     scheme of the aggregate feed
     * @param feedId     MD5 hash id of the feed.
     */
    private void addFeedToCache(final List<String> workspaces,
                                final String locale,
                                final String scheme,
                                final String feedId) {
        try {
            executeTransactionally(new TransactionalTask<Object>() {
                public Object execute() {
                    // Create CachedFeed first because of FK constraint
//                    System.out.println("Executing addFeedToCache");
                    feedCacheDAO.addNewFeedToCache(new CachedAggregateFeed(feedId,
                                                                       getOrderedWorkspaceList(workspaces),
                                                                       locale,
                                                                       scheme));
//                    System.out.println("CachedFeed added");
                    feedCacheDAO.cacheAggregateFeedTimestamps(workspaces, locale, scheme, feedId);
//                    System.out.println("Timestamps added");
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("exception occurred while addeing cached feed", e);
        }
    }


    /**
     * Cache timestamps for the given feed
     *
     * @param workspaces workspaces to compute and insert timestamps into the cache table (AggregateFeedTimestamp)
     * @param locale     locale of the aggregate feed
     * @param scheme     scheme of the aggregate feed
     * @param feedId     MD5 hashed id of the feed
     */
    private void addCachedTimestamps(List<String> workspaces, String locale, String scheme, String feedId) {
        feedCacheDAO.cacheAggregateFeedTimestamps(workspaces, locale, scheme, feedId);
    }


    /**
     * Returns feed Ids for cached aggregate feeds effected by the given workspace, locale and categories.
     *
     * @param workspace  workspace of the entry that is updated.
     * @param categories a list of categories of the entry that is updated.
     * @return a list of cached feed ids effected by the entry change.
     */
    Set<String> getEffectedCachedFeeds(String workspace, List<EntryCategory> categories) {
        Set<String> schemes = new HashSet<String>();
        for (EntryCategory cat : categories) {
            if (cat.getScheme() != null) {
                schemes.add(cat.getScheme());
            }
        }
        return getEffectedCachedFeeds(workspace, schemes);
    }

    //=== generic method to get effected cached feeds

    private Set<String> getEffectedCachedFeeds(String workspace, Set<String> schemes) {

        Set<String> cachedFeedIds = new HashSet<String>();
        Set<CachedAggregateFeed> feedsWithWorkspace = this.workspaceToCachedFeeds.get(workspace);
        if (feedsWithWorkspace != null) {
            for (CachedAggregateFeed caf : feedsWithWorkspace) {
                // get workspaces with matching schemes.
                if (schemes.contains(caf.getScheme())) {
                    // scheme and locale matches with this feed
                    cachedFeedIds.add(caf.getCachedFeedId());
                }
            }
        }
        return cachedFeedIds;
    }

    //=== Parse spring configuration strings for cached aggregate feeds.

    private final Pattern jws = Pattern.compile("\\(|\\)");
    private final Pattern sep = Pattern.compile(",");

    private void parseConfigAndPopulateMaps(List<String> cachelist) {
        if (cacheList == null || cacheList.isEmpty()) {
            return;
        }
        String[] allWorkspaceList = null;  // For $join

        for (String cacheSetEntry : cachelist) {
            String[] wsSchemeLocale = trimList(jws.split(cacheSetEntry)); // split on ( or )
            boolean joinAll = (wsSchemeLocale.length == 1);
            if (joinAll && allWorkspaceList == null) {
                allWorkspaceList = getWorkspaceList();
            }
            String[] wkspaces = (joinAll) ? allWorkspaceList : trimList(sep.split(wsSchemeLocale[1]));
            String[] schemeLocale = trimList(sep.split(wsSchemeLocale[(!joinAll) ? 2 : 0]));
            String scheme = schemeLocale[1];
            String locale = (schemeLocale.length > 2) ? schemeLocale[2] : "**_**";
            String workspaces = getOrderedWorkspaceList(wkspaces);

            CachedAggregateFeed cachedFeed = new CachedAggregateFeed(null, workspaces, locale, scheme);
            addCachedFeedToMaps(cachedFeed);
        }

        // Dump out the workspaceToCachedFeeds map
//        System.out.println("workspaceTocachedFeeds:");
//        for (String ws : this.workspaceToCachedFeeds.keySet()) {
//            Set<CachedAggregateFeed> jws = this.workspaceToCachedFeeds.get(ws);
//            for (CachedAggregateFeed caf : jws) {
//                System.out.println(" workspace:" + ws + "=>" + caf.getJoinWorkspaceList() +
//                                   "." + caf.getLocale() + "-" + caf.getScheme() + "(" + caf.getCachedFeedId() + ")");
//            }
//        }
    }

    // Retrieve all workspaces defined in the system.

    private String[] getWorkspaceList() {
        List<String> workspaces = entriesDAO.listWorkspaces();
        String[] ws = new String[workspaces.size()];
        workspaces.toArray(ws);
        return ws;
    }

    // Add the configured feed to workspaceToCacheFeeds map and cachedFeedById map.

    private void addCachedFeedToMaps(final CachedAggregateFeed cachedFeed) {

        List<String> workspaces = cachedFeed.getJoinWorkspaceList();
        for (String workspace : workspaces) {
            Set<CachedAggregateFeed> feedSet = this.workspaceToCachedFeeds.get(workspace);
            if (feedSet == null) {
                feedSet = new HashSet<CachedAggregateFeed>();
                this.workspaceToCachedFeeds.put(workspace, feedSet);
            }
            feedSet.add(cachedFeed);
            cachedFeedById.put(cachedFeed.getCachedFeedId(), cachedFeed);
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

    private String getOrderedWorkspaceList(final String[] workspaceList) {
        List<String> wkspaceList = Arrays.asList(workspaceList);
        return getOrderedWorkspaceList(wkspaceList);
    }

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

    //==============================
    // Transaction support
    //==============================

    protected interface TransactionalTask<T> {
        T execute();
    }

    <T> T executeTransactionally(final TransactionalTask<T> task) {
        return (T) getTransactionTemplate().execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus transactionStatus) {
                feedCacheDAO.acquireLock();
                return task.execute();
            }
        });
    }

}
