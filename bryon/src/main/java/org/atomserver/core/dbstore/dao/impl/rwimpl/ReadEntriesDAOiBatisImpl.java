/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.rwdao.ReadEntriesDAO;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ReadEntriesDAOiBatisImpl
        extends BaseEntriesDAOiBatisImpl
        implements ReadEntriesDAO {

    static public final long FETCH_INTERVAL = 300000;
    static public final long STARTUP_INTERVAL = 900000;

    static private long startupTime = System.currentTimeMillis();
    static private boolean isFirstPass = true;

    static private Set<String> workspaces = new CopyOnWriteArraySet<String>();
    static long lastWorkspacesSelectTime = 0L;

    static private ConcurrentHashMap<String, HashSet<String>> collections = new ConcurrentHashMap<String, HashSet<String>>();
    static long lastCollectionsSelectTime = 0L;

    private boolean useWorkspaceCollectionCache = true;

    public boolean isUseWorkspaceCollectionCache() {
        return useWorkspaceCollectionCache;
    }

    public void setUseWorkspaceCollectionCache(boolean useCache) {
        useWorkspaceCollectionCache = useCache;
    }

//-----------------------
//     SELECT BATCH
//-----------------------

    public List<EntryMetaData> selectEntryBatch(Collection<? extends EntryDescriptor> entryQueries) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = prepareBatchParamMap(entryQueries);

            if (log.isTraceEnabled()) {
                log.trace("SELECT EntriesDAOiBatisImpl selectEntryBatch:: paramMap= " + paramMap);
            }

            return getSqlMapClientTemplate().queryForList("selectEntryBatch", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectEntryBATCH", "");
        }
    }

    protected ParamMap prepareBatchParamMap(Collection<? extends EntryDescriptor> entryQueries) {
        ParamMap paramMap = paramMap();

        String workspace = null;
        String collection = null;
        Locale locale = null;
        List<String> entryIds = new ArrayList<String>();

        for (EntryDescriptor entryQuery : entryQueries) {
            if (workspace != null && !workspace.equals(entryQuery.getWorkspace())) {
                String msg = "Attempt to use more than one workspace";
                log.error(msg);
                throw new AtomServerException(msg);
            } else {
                workspace = entryQuery.getWorkspace();
                paramMap.param("workspace", workspace);
            }
            if (collection != null && !collection.equals(entryQuery.getCollection())) {
                String msg = "Attempt to use more than one collection";
                log.error(msg);
                throw new AtomServerException(msg);
            } else {
                collection = entryQuery.getCollection();
                paramMap.param("collection", collection);
            }
            if (locale != null && !locale.equals(entryQuery.getLocale())) {
                String msg = "Attempt to use more than one locale";
                log.error(msg);
                throw new AtomServerException(msg);
            } else {
                locale = entryQuery.getLocale();
                paramMap.addLocaleInfo(locale);
            }

            entryIds.add(entryQuery.getEntryId());
        }

        return paramMap.param("entryIds", entryIds);
    }

//-----------------------
//       SELECT
//-----------------------

    public Object selectEntryInternalId(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForObject("selectEntryInternalId",
                                                            paramMap()
                                                                    .param("workspace", entryQuery.getWorkspace())
                                                                    .param("collection", entryQuery.getCollection())
                                                                    .param("entryId", entryQuery.getEntryId())
                                                                    .addLocaleInfo(entryQuery.getLocale()));
        }
        finally {
            stopWatch.stop("DB.selectEntryInternalId", "");
        }
    }

    public EntryMetaData selectEntryByInternalId(Object internalId) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return (EntryMetaData) getSqlMapClientTemplate().queryForObject("selectEntryByInternalId",
                                                                            paramMap().param("internalId", internalId));
        }
        finally {
            stopWatch.stop("DB.selectEntryInternalId2", "");
        }
    }

    public EntryMetaData selectEntry(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Map<String, Object> paramMap = paramMap()
                    .param("workspace", entryQuery.getWorkspace())
                    .param("collection", entryQuery.getCollection())
                    .param("entryId", entryQuery.getEntryId())
                    .addLocaleInfo(entryQuery.getLocale());

            if (log.isDebugEnabled()) {
                log.debug("SELECT EntriesDAOiBatisImpl selectEntry:: paramMap= " + paramMap);
            }
            return (EntryMetaData) (getSqlMapClientTemplate().queryForObject("selectEntry", paramMap));
        }
        finally {
            stopWatch.stop("DB.selectEntry", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));
        }
    }


    public List<EntryMetaData> selectEntries(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Map<String, Object> paramMap = paramMap()
                    .param("workspace", entryQuery.getWorkspace())
                    .param("collection", entryQuery.getCollection())
                    .param("entryId", entryQuery.getEntryId())
                    .addLocaleInfo(entryQuery.getLocale());

            if (log.isDebugEnabled()) {
                log.debug("SELECT EntriesDAOiBatisImpl selectEntries:: paramMap= " + paramMap);
            }

            return getSqlMapClientTemplate().queryForList("selectEntries", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectEntries", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));

        }
    }

    public List<EntryMetaData> selectFeedPage(Date updatedMin,
                                              Date updatedMax,
                                              int startIndex,
                                              int endIndex,
                                              int pageSize,
                                              boolean noLatency,
                                              String locale,
                                              FeedDescriptor feed,
                                              Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = prepareParamMapForSelectEntries(updatedMin, updatedMax,
                                                                startIndex, endIndex,
                                                                pageSize, locale, feed);
            addSetOpsSelectFeedPageParams(paramMap, categoryQuery, noLatency);
            return getSqlMapClientTemplate().queryForList("selectFeedPage", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectFeedPage",
                           AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale, feed.getWorkspace(), feed.getCollection()));
        }
    }


    private void addSetOpsSelectFeedPageParams(ParamMap paramMap, Collection<BooleanExpression<AtomCategory>> categoryQuery, boolean noLatency) {
        if (categoryQuery != null && !categoryQuery.isEmpty()) {
            paramMap.param("categoryQuerySql",
                           SetOpCategoryQueryGenerator.generateCategorySearch(categoryQuery));
        }
        if (!noLatency && getLatencySeconds() > 0) {
            paramMap.param("latencySeconds", getLatencySeconds());
        }
        paramMap.param("usequery", "setOps");
    }

    public List<EntryMetaData> selectEntriesByLastModified(String workspace, String collection,
                                                           Date updatedMin) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectEntriesByLastModified",
                                                          paramMap()
                                                                  .param("updatedMin", updatedMin)
                                                                  .param("workspace", workspace)
                                                                  .param("collection", collection));
        }
        finally {
            stopWatch.stop("DB.selectEntriesByLastModified", "");
        }
    }

    public List<EntryMetaData> selectEntriesByLastModifiedSeqNum(FeedDescriptor feed,
                                                                 Date updatedMin) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectEntriesByLastModifiedSeqNum",
                                                          paramMap()
                                                                  .param("updatedMin", updatedMin)
                                                                  .param("workspace", feed.getWorkspace())
                                                                  .param("collection", feed.getCollection()));
        }
        finally {
            stopWatch.stop("DB.selectEntriesByLastModifiedSeqNum", "");
        }
    }

//======================================
//          COUNT QUERIES
//======================================

    public int getTotalCount(ServiceDescriptor service) {
        return super.getTotalCountInternal(service.getWorkspace(), null, "countEntriesTotal");
    }

    public int getTotalCount(FeedDescriptor feed) {
        return super.getTotalCountInternal(feed.getWorkspace(), feed.getCollection(), "countEntriesTotal");
    }

    public int getCountByLastModified(ServiceDescriptor service, Date lastModified) {
        return getCountByLastModifiedInternal(service.getWorkspace(), null, lastModified);
    }

    public int getCountByLastModified(FeedDescriptor feed, Date lastModified) {
        return getCountByLastModifiedInternal(feed.getWorkspace(), feed.getCollection(), lastModified);
    }

    int getCountByLastModifiedInternal(String workspace, String collection, Date updatedMin) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Integer count =
                    (Integer) (getSqlMapClientTemplate().queryForObject("$join".equals(workspace) ?
                                                                        "countModifiedAggregateEntries" :
                                                                        "countEntriesByLastModified",
                                                                        paramMap()
                                                                                .param("updatedMin", updatedMin)
                                                                                .param("workspace", workspace)
                                                                                .param("collection", collection)));
            return count == null ? 0 : count;
        }
        finally {
            stopWatch.stop("DB.getCountByLastModified", "");
        }
    }

//======================================
//     COLLECTION/WORKSPACE QUERIES
//======================================

    public void clearWorkspaceCollectionCaches() {
        workspaces = new CopyOnWriteArraySet<String>();
        lastWorkspacesSelectTime = 0L;
        collections = new ConcurrentHashMap<String, HashSet<String>>();
        lastCollectionsSelectTime = 0L;
    }

    public void ensureCollectionExists(String workspace, String collection) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ensureWorkspaceExists(workspace);
            ParamMap paramMap = paramMap().param("workspace", workspace).param("collection", collection);
            Integer count = (Integer) getSqlMapClientTemplate().queryForObject("collectionExists", paramMap);
            if (count == 0) {
                try {
                    getSqlMapClientTemplate().insert("createCollection", paramMap);
                } catch (DataIntegrityViolationException e) {
                    log.warn("race condition while guaranteeing existence of collection " +
                             workspace + "/" + collection + " - this is probably okay.");
                }
                if ( useWorkspaceCollectionCache ) {
                    log.debug("Adding " + workspace + " " + collection );
                    HashSet<String> workspaceCollections = getWorkspaceCollections(workspace);
                    workspaceCollections.add(collection);
                }
            }
        }
        finally {
            stopWatch.stop("DB.ensureCollectionExists", "");
        }
    }

    public void ensureWorkspaceExists(String workspace) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = paramMap().param("workspace", workspace);
            Integer count = workspace == null ? 0 :
                            (Integer) getSqlMapClientTemplate().queryForObject("workspaceExists", paramMap);
            if (count == 0) {
                try {
                    getSqlMapClientTemplate().insert("createWorkspace", paramMap);
                } catch (DataIntegrityViolationException e) {
                    log.warn("race condition while guaranteeing existence of workspace " +
                             workspace + " - this is probably okay.");
                }
                if ( useWorkspaceCollectionCache ) workspaces.add(workspace);
            }
        }
        finally {
            stopWatch.stop("DB.ensureWorkspaceExists", "");
        }
    }

    public List<String> listWorkspaces() {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            if (!useWorkspaceCollectionCache ) {
                return getSqlMapClientTemplate().queryForList("listWorkspaces");
            } else {
                if ( workspacesIsExpired() ) {
                    lastWorkspacesSelectTime = System.currentTimeMillis();
                    List<String> dbworkspaces = getSqlMapClientTemplate().queryForList("listWorkspaces");
                    if ((dbworkspaces != null) && (!workspaces.equals(dbworkspaces))) {
                        workspaces.addAll(dbworkspaces);
                    }
                }
                return new ArrayList(workspaces);
            }
        }
        finally {
            stopWatch.stop("DB.listWorkspaces", "");
        }
    }

    public List<String> listCollections(String workspace) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            if ( !useWorkspaceCollectionCache ) {
                return getSqlMapClientTemplate().queryForList("listCollections",
                                                              paramMap().param("workspace", workspace));
            } else {
                HashSet<String> workspaceCollections = getWorkspaceCollections(workspace);
                if ( collectionsIsExpired() ) {
                    lastCollectionsSelectTime = System.currentTimeMillis();
                    if ( isFirstPass ) {
                        loadWorkspaceCollections();
                        workspaceCollections = getWorkspaceCollections(workspace);
                        isFirstPass = false;
                    } else {
                        List<String> dbcollections = getSqlMapClientTemplate().queryForList("listCollections",
                                                                                            paramMap().param("workspace", workspace));
                        if ((dbcollections != null) && (!workspaceCollections.equals(dbcollections))) {
                            workspaceCollections.addAll(dbcollections);
                        }
                    }
                }
                return new ArrayList(workspaceCollections);
            }
        }
        finally {
            stopWatch.stop("DB.listCollections", "");
        }
    }

    private HashSet<String> getWorkspaceCollections(String workspace) {
        HashSet<String> workspaceCollections = collections.get(workspace);
        if (workspaceCollections == null) {
            workspaceCollections = new HashSet<String>();
            collections.put(workspace, workspaceCollections);
        }
        return workspaceCollections;
    }

    private boolean collectionsIsExpired() {
        long currentTime = System.currentTimeMillis();
        return (collections == null || collections.isEmpty())
               ? true
               : ((currentTime - lastCollectionsSelectTime) > FETCH_INTERVAL)
                 ? true
                 : ((currentTime - startupTime) > STARTUP_INTERVAL)
                   ? false : true;
    }

    private boolean workspacesIsExpired() {
        long currentTime = System.currentTimeMillis();
        return (workspaces == null || workspaces.isEmpty())
               ? true
               : ((currentTime - lastWorkspacesSelectTime) > FETCH_INTERVAL)
                 ? true
                 : ((currentTime - startupTime) > STARTUP_INTERVAL)
                   ? false : true;
    }

    public void loadWorkspaceCollections() {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            List<WorkspaceCollection> wcs = getSqlMapClientTemplate().queryForList("selectWorkspaceCollections", paramMap());
            if (wcs != null) {
                for( WorkspaceCollection wc : wcs ) {
                    log.info("ADDING " + wc.getWorkspace() + " " + wc.getCollection() );
                    HashSet<String> workspaceCollections = getWorkspaceCollections(wc.getWorkspace());
                    workspaceCollections.add(wc.getCollection());
                }
            }
        } finally {
            stopWatch.stop("DB.loadWorkspaceCollections", "");
        }
    }

//======================================
//                MISC
//======================================

    public long selectMaxIndex(Date updatedMax, boolean noLatency) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = paramMap();
            if (!noLatency && getLatencySeconds() > 0) {
                paramMap.param("latencySeconds", getLatencySeconds());
            }
            if (updatedMax != null) {
                paramMap.param("updatedMax", updatedMax);
            }
            Long retVal = (Long) getSqlMapClientTemplate().queryForObject("selectMaxIndex", paramMap);
            return (retVal == null) ? 0L : retVal;
        }
        finally {
            stopWatch.stop("DB.selectMaxIndex", "");
        }
    }

}

