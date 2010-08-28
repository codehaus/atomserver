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

import com.ibatis.sqlmap.client.SqlMapExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.EntryCategory;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.BooleanTerm;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import java.sql.SQLException;
import java.util.*;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "EntriesDAO")
public class EntriesDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements EntriesDAO {

    public static final int UNDEFINED = -1;
    public static final long UNDEFINED_SEQNUM = -1L;
    public static final Date ZERO_DATE = new Date(0L);

    public static final long FETCH_INTERVAL = 60000;
    public static final long STARTUP_INTERVAL = 900000;

    private ContentDAO contentDAO;
    private EntryCategoriesDAO entryCategoriesDAO;
    private EntryCategoryLogEventDAO entryCategoryLogEventDAO;
    private int latencySeconds = UNDEFINED;

    /**
     * Use the improved selectFeedPage form, which uses SQL Set operands.
     */
    private boolean isUsingSetOpsFeedPage = true;

    private static FeedQueryHeuristicsHelper heuristicsHelper = null;

    private static long startupTime = System.currentTimeMillis();

    private Set<String> workspaces = new HashSet<String>();
    long lastWorkspacesSelectTime = 0L;

    private Set<String> collections = new HashSet<String>();
    long lastCollectionsSelectTime = 0L;

    protected void initDao() throws Exception {
        super.initDao();
        heuristicsHelper = new FeedQueryHeuristicsHelper();
    }

    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setEntryCategoryLogEventDAO(EntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
    }

    @ManagedAttribute(description="Maximum index")
    public long getMaxIndex() {
        return heuristicsHelper.maxIndex;
    }

    @ManagedAttribute(description="Minimum index")
    public long getMinIndex() {
        return heuristicsHelper.minIndex;
    }

    @ManagedAttribute(description="Switch-over timestamp")
    public long getSwitchOverTimestamp() {
        return heuristicsHelper.switchOverTimestamp;
    }

    @ManagedAttribute(description="Latency for updating Entry statistics (minutes).")
    public int getEntryStatisticsLatency() {
        return heuristicsHelper.getEntryStatisticsLatency();
    }

    @ManagedAttribute(description="Latency for updating Entry statistics(minutes).")
    public void setEntryStatisticsLatency(int entryStatisticsLatency) {
       heuristicsHelper.setEntryStatisticsLatency(entryStatisticsLatency);
    }

    @ManagedAttribute(description="Percentage of index span to switch index scan to seek in aggregate feed query. (0 is always seek)")
    public double getSwitchOverPercent() {
        return heuristicsHelper.getSwitchOverPercent();
    }

    @ManagedAttribute(description="Percentage of index span to switch index scan to seek in aggregate feed query. (0 is always seek)")
    public void setSwitchOverPercent(double switchOverPercent) {
        heuristicsHelper.setSwitchOverPercent(switchOverPercent);
    }

    @ManagedAttribute
    public int getLatencySeconds() {
      return latencySeconds;
    }

    @ManagedAttribute
    public void setLatencySeconds(int latencySeconds) {
        // this will be true if we are setting this for the second time through JMX
        if ( this.latencySeconds != UNDEFINED ) {
            // protect against a wacky value coming in through JMX
            int txnTimeout = UNDEFINED;
            String txnTimeoutStr = ConfigurationAwareClassLoader.getENV().getProperty( "db.timeout.txn.put" );
            if ( txnTimeoutStr != null ) {
                try {
                    txnTimeout = Integer.parseInt( txnTimeoutStr );
                } catch ( NumberFormatException ee ) {
                    log.error( "setLatencySeconds; NumberFormatException:: ", ee );
                }
                txnTimeout = txnTimeout/1000;
            } else {
                log.error( "db.timeout.txn.put is NULL " );
            }

            if ( ! (latencySeconds < 0 || ((txnTimeout != UNDEFINED) && (latencySeconds < txnTimeout))) ) {
                this.latencySeconds = latencySeconds;
            } else {
                log.error( "The latency provided (" + latencySeconds + ") is less than txnTimeout (" +
                           txnTimeout + ")" );
            }
        }
        this.latencySeconds = latencySeconds;
    }

     @ManagedOperation(description = "force update of entry statistics used for calculating switch over timestamp.")
     public synchronized void updateEntryStats() {
        heuristicsHelper.readStats();
     }

    @ManagedAttribute
    public boolean isUsingSetOpsFeedPage() {
        return isUsingSetOpsFeedPage;
    }

    @ManagedAttribute
    public void setUsingSetOpsFeedPage(boolean usingSetOpsFeedPage) {
        isUsingSetOpsFeedPage = usingSetOpsFeedPage;
    }

    //======================================
    //   BATCH methods for entries table
    //======================================
    static class EntryBatcher implements SqlMapClientCallback {
        static final Log log = LogFactory.getLog(EntryBatcher.class);

        static enum OperationType {
            update, insert, delete
        }

        private Collection<? extends EntryDescriptor> entryList = null;
        private OperationType opType = null;
        private EntriesDAOiBatisImpl entriesDAO = null;

        EntryBatcher(EntriesDAOiBatisImpl entriesDAO,
                     Collection<? extends EntryDescriptor> entryList,
                     OperationType opType) {
            this.entriesDAO = entriesDAO;
            this.entryList = entryList;
            this.opType = opType;
        }

        public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
            List<EntryDescriptor> validEntries = new ArrayList<EntryDescriptor>();
            executor.startBatch();
            for (EntryDescriptor uriData : entryList) {

                if (opType == OperationType.insert) {
                    Map<String, Object> paramMap = entriesDAO.prepareInsertParamMap(uriData);
                    executor.insert("insertEntry-" + entriesDAO.getDatabaseType(), paramMap);
                    validEntries.add(uriData);

                } else if (opType == OperationType.update || opType == OperationType.delete) {
                    boolean deleted = (opType == OperationType.delete);
                    EntryMetaData metaData = entriesDAO.safeCastToEntryMetaData(uriData);
                    if (metaData != null) {
                        executor.update("updateEntry",
                                         entriesDAO.prepareUpdateParamMap(deleted,
                                                                          uriData.getRevision(),
                                                                          metaData));
                        validEntries.add(uriData);
                    } 
                } else {
                    String msg = "Unknown OperationType";
                    log.error(msg);
                    throw new SQLException(msg);
                }
            }
            Object obj = executor.executeBatch();

            return obj;
        }
    }

    //-----------------------
    private int internalEntryBatch(Collection<? extends EntryDescriptor> entryList,
                                   EntryBatcher.OperationType opType) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryDAOiBatisImpl " + opType + " BATCH==> " + entryList);
        }
        try {
            return (Integer) (getSqlMapClientTemplate().execute(new EntryBatcher(this, entryList, opType)));
        }
        finally {
            stopWatch.stop("DB." + opType + "EntryBATCH", "") ;
        }
    }

    //-----------------------
    //     INSERT BATCH 
    //-----------------------
    public int insertEntryBatch(String workspace, Collection<? extends EntryDescriptor> entryList) {
        return internalEntryBatch(entryList, EntryBatcher.OperationType.insert);
    }

    //-----------------------
    //     UPDATE BATCH 
    //-----------------------
    public int updateEntryBatch(String workspace, Collection<? extends EntryDescriptor> entryList) {
        return internalEntryBatch(entryList, EntryBatcher.OperationType.update);
    }

    //-----------------------
    //     DELETE BATCH 
    //-----------------------
    public int deleteEntryBatch(String workspace, Collection<? extends EntryDescriptor> entryList) {
        return internalEntryBatch(entryList, EntryBatcher.OperationType.delete);
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

    private ParamMap prepareBatchParamMap(Collection<? extends EntryDescriptor> entryQueries) {
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

    //======================================
    //   CRUD methods for entries table
    //======================================
    //-----------------------
    //       INSERT
    //-----------------------
    /**
     * Insert this record into the DB
     * This form adds a Sequence Number for lastModified. And we ARE setting lastModified here.
     * So whatever lastModified you pass in <i>will be ignored</i>
     * Thus, it is not appropriate for use
     * when you are seeding the DB the first time around from a file system
     * NOTE: we are using [collection, locale, entryId] as a composite primary key.
     */
    public Object insertEntry(EntryDescriptor entry) {
        return insertEntry(entry, false);
    }

    /**
     * Insert this record into the DB
     * This form conditionally adds a updateDate
     *
     * @param isSeedingDB = true indicates that we WILL use the updateDate defined elsewhere
     *                    (most likely the file's lastModified timestamp).
     */
    public Object insertEntry(EntryDescriptor entry, boolean isSeedingDB) {
        return insertEntry(entry, false, null, null);
    }

    public Object insertEntry(EntryDescriptor entry,
                              boolean isSeedingDB,
                              Date published,
                              Date updated) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl INSERT ==> " + entry);
        }

        // now insert
        try {
            ParamMap paramMap = prepareInsertParamMap(entry);

            if (isSeedingDB) {
                paramMap.param("publishedDate", published)
                        .param("updatedDate", updated);
                Object obj = getSqlMapClientTemplate().insert("insertEntrySeedingDB-" + getDatabaseType(), paramMap);
                return obj;
            } else {
                Object obj = getSqlMapClientTemplate().insert("insertEntry-" + getDatabaseType(), paramMap);
                return obj;
            }
        }
        finally {
            stopWatch.stop("DB.insertEntry", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
        }
    }

    ParamMap prepareInsertParamMap(EntryDescriptor entryQuery) {
        ParamMap paramMap = paramMap()
                .param("workspace", entryQuery.getWorkspace())
                .param("collection", entryQuery.getCollection())
                .param("entryId", entryQuery.getEntryId())
                .param("revision", 0)
                .param("deleted", false)
                .param("contentHashCode", entryQuery.getContentHashCode())
                .addLocaleInfo(entryQuery.getLocale());

        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl UPDATE:: paramMap= " + paramMap);
        }
        return paramMap;
    }


    //-----------------------
    //       SELECT
    //-----------------------
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

    //-----------------------
    //       UPDATE
    //-----------------------
    /**
     * Update this record in the DB
     * This form updates the Sequence Number for lastModified. And we ARE resetting lastModified here.
     * So whatever lastModified you pass in <i>will be ignored</i>
     * <p/>
     * All entry data is "set once", except for filePath and the deleted flag
     * Once set we CANNOT reset collection, locale, and entryId !!!
     * This is because we use these 3 values as a composite key
     * <p/>
     * NOTE: you MUST update records whenever the Entry is updated, even if you are NOT resetting
     * filePath or the deleted flag. This is because we MUST have new update timestamp and date created
     * so that Pagination will work properly.
     */
    public int updateEntry(EntryDescriptor entryQuery, boolean deleted) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            if (log.isDebugEnabled()) {
                log.debug("EntriesDAOiBatisImpl UPDATE ==> [ " + entryQuery + " " + deleted + "]");
            }
            EntryMetaData metaData = safeCastToEntryMetaData(entryQuery);
            if (metaData == null) {
                return 0;
            }
            metaData.setContentHashCode(entryQuery.getContentHashCode());

            int rc = getSqlMapClientTemplate().update("updateEntry",
                                                     prepareUpdateParamMap(deleted,
                                                                           entryQuery.getRevision(),
                                                                           metaData));
            return rc;
        }
        finally {
            stopWatch.stop("DB.updateEntry", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));
        }
    }

    ParamMap prepareUpdateParamMap(boolean deleted,
                                   int revision,
                                   EntryMetaData entryMetaData) {
        ParamMap paramMap = paramMap()
                .param("entryStoreId", entryMetaData.getEntryStoreId())
                .param("revision", revision)
                .param("deleted", deleted)
                .param("contentHashCode", entryMetaData.getContentHashCode());

        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl UPDATE:: paramMap= " + paramMap);
        }
        return paramMap;
    }

    private int updateEntryOverwrite(EntryMetaData entry, boolean resetRevision, Date published, Date updated) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            if (log.isDebugEnabled()) {
                log.debug("EntriesDAOiBatisImpl UPDATE ==> [resetRevision= " + resetRevision + "  entry= " + entry + "]");
            }

            int revision = (resetRevision) ? 0 : -1;
            return getSqlMapClientTemplate().update("updateEntryOverwrite",
                                                    prepareUpdateParamMap(false, revision, entry)
                                                            .param("publishedDate", published)
                                                            .param("updatedDate", updated));

        }
        finally {
            stopWatch.stop("DB.updateEntryOverwrite", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
        }
    }

    //-----------------------
    //       DELETE
    //-----------------------
    /**
     * Delete this Entry DB entry.
     * BUT note that we do NOT actually delete anything!!
     * We simply update the Record to now have its deleted flag set to true.
     */
    public int deleteEntry(EntryDescriptor entryQuery) {
        return deleteEntry(entryQuery, true);
    }

    public int deleteEntry(EntryDescriptor entryQuery, boolean setDeletedFlag) {
        if (log.isDebugEnabled()) {
            log.debug("DELETE EntriesDAOiBatisImpl [ " + entryQuery + " ]");
        }
        return updateEntry(entryQuery, setDeletedFlag);
    }
    
    /**
     * Obliterate this Entry DB entry.
     * This form does delete the actual record from the DB.
     *
     * @param entryQuery the EntryDescriptor that descrbes the entry to delete
     */
    public synchronized void obliterateEntry(EntryDescriptor entryQuery) {
        log.info("OBLITERATE EntriesDAOiBatisImpl [ " + entryQuery + " ]");
        EntryMetaData metaData = null;
        List<EntryCategory> categoriesToRemove = null;

        if (contentDAO != null || entryCategoriesDAO != null) {
            metaData = (entryQuery instanceof EntryMetaData) ? (EntryMetaData) entryQuery : selectEntry(entryQuery);

            if(metaData != null) {
                categoriesToRemove = metaData.getCategories();
            }
            if (metaData != null && entryCategoryLogEventDAO != null) {
                entryCategoryLogEventDAO.deleteEntryCategoryLogEvent(entryQuery);
            }
            if (metaData != null && contentDAO != null) {
                contentDAO.deleteContent(metaData);
            }
            if (metaData != null && entryCategoriesDAO != null) {
                entryCategoriesDAO.deleteEntryCategoriesWithoutCacheUpdate(metaData);
            }
        }

        getSqlMapClientTemplate().delete("deleteEntry",
                                         paramMap()
                                                 .param("workspace", entryQuery.getWorkspace())
                                                 .param("collection", entryQuery.getCollection())
                                                 .param("entryId", entryQuery.getEntryId())
                                                 .addLocaleInfo(entryQuery.getLocale()));
    }


    public AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor, List<String> joinWorkspaces) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = paramMap()
                    .param("collection", entryDescriptor.getCollection())
                    .param("entryId", entryDescriptor.getEntryId())
                    .param("pageSize", 1);
            if (entryDescriptor.getLocale() != null) {
                paramMap.addLocaleInfo(entryDescriptor.getLocale());
            }
            if (joinWorkspaces != null && !joinWorkspaces.isEmpty()) {
                paramMap.param("joinWorkspaces", joinWorkspaces);
            }

            paramMap.put("usequery", FeedQueryHeuristicsHelper.SEEK); // Always use seek
            Map<String, AggregateEntryMetaData> map =
                    AggregateEntryMetaData.aggregate(entryDescriptor.getWorkspace(),
                                                     entryDescriptor.getCollection(),
                                                     entryDescriptor.getLocale(),
                                                     getSqlMapClientTemplate().queryForList("selectAggregateEntries", paramMap));

            return map.get(entryDescriptor.getEntryId());
        } finally {
            stopWatch.stop("DB.selectAggregateEntry",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryDescriptor) );
        }
    }

    public List<AggregateEntryMetaData> selectAggregateEntriesByPage(FeedDescriptor feed,
                                                                     Date updatedMin,
                                                                     Date updatedMax,
                                                                     Locale locale,
                                                                     int startIndex,
                                                                     int endIndex,
                                                                     int pageSize,
                                                                     Collection<BooleanExpression<AtomCategory>> categoriesQuery,
                                                                     List<String> joinWorkspaces) {

        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = prepareParamMapForSelectEntries(updatedMin, updatedMax,
                                                                startIndex, endIndex, pageSize,
                                                                locale == null ? null : locale.toString(), feed);

            if (joinWorkspaces != null && !joinWorkspaces.isEmpty()) {
                paramMap.param("joinWorkspaces", joinWorkspaces);
            }

            if (categoriesQuery != null) {
                paramMap.param("categoryFilterSql",
                               CategoryQueryGenerator.generateCategoryFilter(categoriesQuery));
                paramMap.param("categoryQuerySql",
                               CategoryQueryGenerator.generateCategorySearch(categoriesQuery));
            }

            if (latencySeconds > 0) {
                paramMap.param("latencySeconds", latencySeconds);
            }

            heuristicsHelper.applyHeuristics(paramMap, FeedQueryHeuristicsHelper.SEEK);
            List entries = getSqlMapClientTemplate().queryForList("selectAggregateEntries", paramMap);

            Map<String, AggregateEntryMetaData> map =
                    AggregateEntryMetaData.aggregate(feed.getWorkspace(), feed.getCollection(), locale, entries);
            return new ArrayList(map.values());
        } finally {
            stopWatch.stop("DB.selectAggregateEntriesByPage",
                     AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale == null ? null : locale.toString(),feed.getWorkspace(), feed.getCollection())
            );
        }
    }


    
    public List<EntryMetaData> selectFeedPage(Date updatedMin,
                                              Date updatedMax,
                                              int startIndex,
                                              int endIndex,
                                              int pageSize,
                                              String locale,
                                              FeedDescriptor feed,
                                              Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = prepareParamMapForSelectEntries(updatedMin, updatedMax,
                                                                startIndex, endIndex,
                                                                pageSize, locale, feed);

            if ( isUsingSetOpsFeedPage ) {
                addSetOpsSelectFeedPageParams(paramMap, categoryQuery);
            } else {
                addSelectFeedPageParams(paramMap, categoryQuery);
            }

            return getSqlMapClientTemplate().queryForList("selectFeedPage", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectFeedPage",
                             AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale, feed.getWorkspace(), feed.getCollection()));
        }
    }

    /**
     * Use the original selectFeedPage form, which uses SQL HAVING...SUM.
     * This form produces a significantly less efficient query plan.
     */
    private void addSelectFeedPageParams(ParamMap paramMap, Collection<BooleanExpression<AtomCategory>> categoryQuery ) {
        if (categoryQuery != null && !categoryQuery.isEmpty()) {
            BooleanExpression<AtomCategory> firstExpression = categoryQuery.iterator().next();
            if (categoryQuery.size() == 1 && firstExpression instanceof BooleanTerm) {
                BooleanTerm<AtomCategory> singleTermQuery = (BooleanTerm<AtomCategory>) firstExpression;
                paramMap.param("categoryQueryScheme", singleTermQuery.getValue().getScheme());
                paramMap.param("categoryQueryTerm", singleTermQuery.getValue().getTerm());
            } else {
                paramMap.param("categoryFilterSql",
                               CategoryQueryGenerator.generateCategoryFilter(categoryQuery));
                paramMap.param("categoryQuerySql",
                               CategoryQueryGenerator.generateCategorySearch(categoryQuery));
            }
        }
        if (latencySeconds > 0) {
            paramMap.param("latencySeconds", latencySeconds);
        }
        heuristicsHelper.applyHeuristics(paramMap, FeedQueryHeuristicsHelper.SCAN);
    }

    /**
     * Use the improved selectFeedPage form, which uses SQL Set operands.
     */
    private void addSetOpsSelectFeedPageParams(ParamMap paramMap, Collection<BooleanExpression<AtomCategory>> categoryQuery ) {
        if (categoryQuery != null && !categoryQuery.isEmpty()) {
            paramMap.param("categoryQuerySql",
                           SetOpCategoryQueryGenerator.generateCategorySearch(categoryQuery));
        }
        if (latencySeconds > 0) {
            paramMap.param("latencySeconds", latencySeconds);
        }
        paramMap.param("usequery", "setOps");
    }



    // NOTE: package scoped for use by EntryCategoryIBatisImpl
    ParamMap prepareParamMapForSelectEntries(Date updatedMin, Date updatedMax,
                                             int startIndex, int endIndex,
                                             int pageSize, String locale, FeedDescriptor feed) {

        if ( updatedMin != null && updatedMin.equals(ZERO_DATE) ) {
            updatedMin = null;
        }

        ParamMap paramMap = paramMap()
                .param("workspace", feed.getWorkspace())
                .param("updatedMin", updatedMin)
                .param("updatedMax", updatedMax)
                .param("startIndex", (long)startIndex )
                .param("endIndex", (long)endIndex )
                .param("pageSize", pageSize)
                .param("collection", feed.getCollection());

        if (locale != null) {
            paramMap.param("undefinedCountry", "**").addLocaleInfo(LocaleUtils.toLocale(locale));
        }

        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl prepareParamMapForSelectEntries:: paramMap= " + paramMap);
        }
        return paramMap;
    }

    /**
     * NOTE: package scoped for use by JUnits
     */
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

    /**
     */
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

    /**
     * Meant for use by <b>only</b> the DBSeeder !!
     */
    public List<EntryMetaData> updateLastModifiedSeqNumForAllEntries(ServiceDescriptor service) {

        // Now let's sort it all by lastModified
        // Sort from the begining of time, in ascending order, so we get everything
        List sortedList = selectEntriesByLastModified(service.getWorkspace(), null, ZERO_DATE);

        // And now, let's walk the List and update
        // NOTE: this causes updateTimestamp to be generated in the correct order
        for (Object obj : sortedList) {
            EntryMetaData entry = (EntryMetaData) obj;
            updateEntryOverwrite(entry, true, entry.getPublishedDate(), entry.getUpdatedDate());

        }
        return sortedList;
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
    //          DELETE ALL ROWS
    //======================================
    public void deleteAllEntries(ServiceDescriptor service) {
        if (contentDAO != null) {
            //contentDAO.deleteAllContent();
            contentDAO.deleteAllContent(service.getWorkspace());
        }
        if (entryCategoriesDAO != null) {
            entryCategoriesDAO.deleteAllEntryCategories(service.getWorkspace());
        }
        super.deleteAllEntriesInternal(service.getWorkspace(), null, "deleteEntriesAll");
    }

    public void deleteAllEntries(FeedDescriptor feed) {
        super.deleteAllEntriesInternal(feed.getWorkspace(), feed.getCollection(), "deleteEntriesAll");
    }

    public void deleteAllRowsFromEntries() {
        getSqlMapClientTemplate().delete("deleteAllRowsFromEntries");
    }


    /**
     * returns the corresponding EntryMetaData for the given EntryDescriptor.
     * <p/>
     * this method returns the object it is passed if it happens to be an EntryMetaData, or it
     * retrieves the corresponding EntryMetaData from the database if not.
     *
     * @param entryDescriptor the EntryDescriptor of the EntryMetaData to retrieve
     * @return the EntryMetaData corresponding to the given EntryDescriptor
     */
    private EntryMetaData safeCastToEntryMetaData(EntryDescriptor entryDescriptor) {
        return entryDescriptor instanceof EntryMetaData ?
               (EntryMetaData) entryDescriptor :
               selectEntry(entryDescriptor);
    }
    public void clearWorkspaceCollectionCaches() {
        workspaces = new HashSet<String>();
        lastWorkspacesSelectTime = 0L;
        collections = new HashSet<String>();
        lastCollectionsSelectTime = 0L;
    }

    public void ensureCollectionExists(String workspace, String collection) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ensureWorkspaceExists(workspace);
            ParamMap paramMap = paramMap()
                    .param("workspace", workspace)
                    .param("collection", collection);
            Integer count = (Integer) getSqlMapClientTemplate().queryForObject("collectionExists", paramMap);
            if (count == 0) {
                try {
                    getSqlMapClientTemplate().insert("createCollection", paramMap);
                    collections.add(collection);
                } catch (DataIntegrityViolationException e) {
                    log.warn("race condition while guaranteeing existence of collection " +
                             workspace + "/" + collection + " - this is probably okay.");
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
                    workspaces.add(workspace);
                } catch (DataIntegrityViolationException e) {
                    log.warn("race condition while guaranteeing existence of workspace " +
                             workspace + " - this is probably okay.");
                }
            }
        }
        finally {
            stopWatch.stop("DB.ensureWorkspaceExists", "");
        }
    }

    public List<String> listWorkspaces() {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            if ( workspacesIsExpired() ) {
                 lastWorkspacesSelectTime = System.currentTimeMillis();
                 List<String> dbworkspaces =  getSqlMapClientTemplate().queryForList( "listWorkspaces");
                 workspaces.addAll( dbworkspaces );
             }
             return new ArrayList( workspaces );

            //return getSqlMapClientTemplate().queryForList("listWorkspaces");
        }
        finally {
            stopWatch.stop("DB.listWorkspaces", "");
        }
    }

    public List<String> listCollections(String workspace) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            if ( collectionsIsExpired() ) {
                lastCollectionsSelectTime = System.currentTimeMillis();
                List<String> dbcollections =  getSqlMapClientTemplate().queryForList( "listCollections",
                                                                                    paramMap().param("workspace", workspace));
                collections.addAll( dbcollections );
            }
            return new ArrayList( collections );

            //return getSqlMapClientTemplate().queryForList( "listCollections",
            //                                                paramMap().param("workspace", workspace));
        }
        finally {
            stopWatch.stop("DB.listCollections", "");
        }
    }

    private boolean collectionsIsExpired() {
        long currentTime = System.currentTimeMillis();
        return (collections == null || collections.isEmpty()) ? true
                : ((currentTime - lastCollectionsSelectTime) >  FETCH_INTERVAL ) ? true
                : ((currentTime - startupTime) >  STARTUP_INTERVAL ) ? false : true;
    }

    private boolean workspacesIsExpired() {
        long currentTime = System.currentTimeMillis();
        return (workspaces == null || workspaces.isEmpty()) ? true
                : ((currentTime - lastWorkspacesSelectTime) >  FETCH_INTERVAL ) ? true
                : ((currentTime - startupTime) >  STARTUP_INTERVAL ) ? false : true;
    }

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
        return (EntryMetaData) getSqlMapClientTemplate().queryForObject(
                "selectEntryByInternalId",
                paramMap().param("internalId", internalId));
    }

    public long selectMaxIndex(Date updatedMax) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            ParamMap paramMap = paramMap();
            if (latencySeconds > 0) {
                paramMap.param("latencySeconds", latencySeconds);
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

    public void acquireLock() throws AtomServerException {
        if (latencySeconds <= 0) {
            log.debug("ACQUIRING LOCK");

            // JTDS forces us to actually "touch" a DB Table before it will begin the transaction
            // so we have to do this No Op which does a "SELECT COUNT(*) from AtomWorkspace"
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
            log.debug( "acquireLock() STATUS = " + status );
            if ( status < 0 ) {
                String message = "Could not acquire the database lock (status= " + status + ")";
                log.error(message);
                throw new AtomServerException(message);
            }
        } else {
            log.debug("NO NEED TO APPLOCK - using enforced latency instead.");
        }
    }


    //=================================================
    //  Support to decide Aggregate Feed Seek or Scan.        
    //=================================================

    /*
     * Class/Object used by iBatis to return Entry stats.
     */
    public static class EntryStats {
        long maxTimestamp;
        long minTimestamp;

        public long getMaxTimestamp() {
            return maxTimestamp;
        }

        public void setMaxTimestamp(Long maxTimestamp) {
            this.maxTimestamp = maxTimestamp;
        }

        public long getMinTimestamp() {
            return minTimestamp;
        }

        public void setMinTimestamp(Long minTimestamp) {
            this.minTimestamp = minTimestamp;
        }
    }


    /*
     * Helper class apply entry statistics to decide if index seek or index scan should be used.
     * Generally, seek is better than scan, but when the feed query start index is too
     * far away from the tip, SQL server needs to bring in a lot of matching rows and in
     * this case, scan helps.
     */
    class FeedQueryHeuristicsHelper {

        // configuration settings
        static final int DEAULT_STATS_LATENCY = 15; // minutes
        static final double DEFAULT_SWITCHOVERPERCENT = 50.0; // % of overall timestamp span to switch to index seek.
        // query mode
        static final String SEEK = "indexSeek";
        static final String SCAN = "indexScan";


        private int entryStatisticsLatency = DEAULT_STATS_LATENCY; // minutes
        private double switchOverPercent = DEFAULT_SWITCHOVERPERCENT; // percentage to switch over to indexSeek

        // statistics of interest
        long minIndex = 0;
        long maxIndex = 0;

        // local computed variables
        long switchOverTimestamp = 0;
        private long nextSyncTime = 0;


        FeedQueryHeuristicsHelper() {
            readStats();
        }

        double getSwitchOverPercent() {
            return switchOverPercent;
        }

        long getSwitchOverTimestamp() {
             return switchOverTimestamp;
         }

        int getEntryStatisticsLatency() {
            return entryStatisticsLatency;
        }

        void setEntryStatisticsLatency(int entryStatisticsLatency) {
            if(entryStatisticsLatency <= 0) {
                this.entryStatisticsLatency = DEAULT_STATS_LATENCY; // default
            }
            this.entryStatisticsLatency = entryStatisticsLatency;
            nextSyncTime = 0;
        }

        void setSwitchOverPercent(double switchOverPercent) {
             if(switchOverPercent < 0.0) {
                this.switchOverPercent = 0.0;
            } else if( switchOverPercent > 100.0){
                this.switchOverPercent = 100.0;
            } else {
                this.switchOverPercent = switchOverPercent;
            }
            computeSwitchOverTimestamp();
        }
        
        synchronized void readStats() {
            long currentTime = System.currentTimeMillis();
            if (nextSyncTime < currentTime) {
                if (Double.compare(switchOverPercent, 0.0) == 0) {
                    switchOverTimestamp = 0;
                } else if (Double.compare(switchOverPercent, 100.0) == 0) {
                    switchOverTimestamp = Long.MAX_VALUE;
                } else {
                    EntryStats entryStats = getEntryStats();
                    if (entryStats != null) {
                        maxIndex = entryStats.getMaxTimestamp();
                        minIndex = entryStats.getMinTimestamp();
                    }
                    computeSwitchOverTimestamp();
                }
                nextSyncTime = currentTime + entryStatisticsLatency * 60 * 1000;
            }
        }

        void computeSwitchOverTimestamp() {
            long span = maxIndex - minIndex;
            switchOverTimestamp = minIndex + (long) (span * switchOverPercent / 100.0);
            if (log.isDebugEnabled()) {
                log.debug(" Update timestamps:");
                log.debug("  min timestamp= " + minIndex);
                log.debug("  max timestamp= " + maxIndex);
                log.debug("  switchover time stamp= " + switchOverTimestamp);
            }
        }

        // Apply heuristics for adjusting the aggregate feed query for MS SQL Server.
        // Currently, it looks at the timestamp only. It can be extended to look at
        // other parameter vaues such as updateDate as well.
        void applyHeuristics(HashMap<String, Object> paramMap, String defaultMode) {

            paramMap.put("usequery", defaultMode); // default to seek
            Long startIndex = (Long) paramMap.get("startIndex");

            // no need to handle it if there is no start index or not sql server
            if (startIndex != null && "sqlserver".equals(getDatabaseType())) {

                if (startIndex < minIndex) {
                    startIndex = minIndex;
                    paramMap.put("startIndex", startIndex);
                }

                String entryId = (String) paramMap.get("entryId");    // use seek when EntryId is not null
                if(entryId == null) {
                    readStats();
                    paramMap.put("usequery", (startIndex < switchOverTimestamp) ? SCAN: SEEK);
                }
            }
            if(log.isDebugEnabled()) {
                log.debug(" usequery = " + paramMap.get("usequery"));
            }
        }

        // database read
        EntryStats getEntryStats() {
            ParamMap paramMap = paramMap();
            List<EntryStats> list = (List<EntryStats>) getSqlMapClientTemplate().queryForList("selectEntryStats", paramMap);
            return (list.size() == 0) ? null : list.get(0);
        }
    }
}
