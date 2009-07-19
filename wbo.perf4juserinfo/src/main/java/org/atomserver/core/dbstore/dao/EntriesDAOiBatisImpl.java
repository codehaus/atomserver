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
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
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

    private ContentDAO contentDAO;
    private EntryCategoriesDAO entryCategoriesDAO;
    private EntryCategoryLogEventDAO entryCategoryLogEventDAO;
    private int latencySeconds = UNDEFINED;

    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setEntryCategoryLogEventDAO(EntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
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
            executor.startBatch();
            for (EntryDescriptor uriData : entryList) {

                if (opType == OperationType.insert) {
                    Map<String, Object> paramMap = entriesDAO.prepareInsertParamMap(uriData);

                    executor.insert("insertEntry-" + entriesDAO.getDatabaseType(), paramMap);

                } else if (opType == OperationType.update || opType == OperationType.delete) {
                    boolean deleted = (opType == OperationType.delete);
                    EntryMetaData metaData = entriesDAO.safeCastToEntryMetaData(uriData);
                    if (metaData != null) {
                        executor.update("updateEntry",
                                         entriesDAO.prepareUpdateParamMap(deleted,
                                                                          uriData.getRevision(),
                                                                          metaData));
                    }
                } else {
                    String msg = "Unknown OperationType";
                    log.error(msg);
                    throw new SQLException(msg);
                }
            }
            return executor.executeBatch();
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
                return getSqlMapClientTemplate().insert("insertEntrySeedingDB-" + getDatabaseType(), paramMap);
            } else {
                return getSqlMapClientTemplate().insert("insertEntry-" + getDatabaseType(), paramMap);
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
        Thread.dumpStack();
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

            return getSqlMapClientTemplate().update("updateEntry",
                                                     prepareUpdateParamMap(deleted,
                                                                           entryQuery.getRevision(),
                                                                           metaData));
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
                .param("deleted", deleted);

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

        if (contentDAO != null || entryCategoriesDAO != null) {
            EntryMetaData metaData = selectEntry(entryQuery);
            if (metaData != null && entryCategoryLogEventDAO != null) {
                entryCategoryLogEventDAO.deleteEntryCategoryLogEvent(entryQuery);
            }
            if (metaData != null && contentDAO != null) {
                contentDAO.deleteContent(metaData);
            }
            if (metaData != null && entryCategoriesDAO != null) {
                entryCategoriesDAO.deleteEntryCategories(metaData);
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

        Map<String, AggregateEntryMetaData> map =
                AggregateEntryMetaData.aggregate(entryDescriptor.getWorkspace(),
                                                 entryDescriptor.getCollection(),
                                                 entryDescriptor.getLocale(),
                                                 getSqlMapClientTemplate().queryForList("selectAggregateEntries",
                                                                                        paramMap));

        return map.get(entryDescriptor.getEntryId());
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

            List entries = getSqlMapClientTemplate().queryForList("selectAggregateEntries",
                                                                  paramMap);

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

            if (categoryQuery != null && !categoryQuery.isEmpty()) {
                paramMap.param("categoryFilterSql",
                               CategoryQueryGenerator.generateCategoryFilter(categoryQuery));
                paramMap.param("categoryQuerySql",
                               CategoryQueryGenerator.generateCategorySearch(categoryQuery));
            }

            if (latencySeconds > 0) {
                paramMap.param("latencySeconds", latencySeconds);
            }

            return getSqlMapClientTemplate().queryForList("selectFeedPage", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectFeedPage",
                             AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale, feed.getWorkspace(), feed.getCollection()));
        }
    }

    // NOTE: package scoped for use by EntryCategoryIBatisImpl
    ParamMap prepareParamMapForSelectEntries(Date updatedMin, Date updatedMax,
                                             int startIndex, int endIndex,
                                             int pageSize, String locale, FeedDescriptor feed) {
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

    public void ensureCollectionExists(String workspace, String collection) {
        ensureWorkspaceExists(workspace);
        ParamMap paramMap = paramMap()
                .param("workspace", workspace)
                .param("collection", collection);
        Integer count =
                (Integer) getSqlMapClientTemplate().queryForObject("collectionExists",
                                                                   paramMap);
        if (count == 0) {
            try {
                getSqlMapClientTemplate().insert("createCollection", paramMap);
            } catch (DataIntegrityViolationException e) {
                log.warn("race condition while guaranteeing existence of collection " +
                         workspace + "/" + collection + " - this is probably okay.");
            }
        }
    }

    public void ensureWorkspaceExists(String workspace) {
        ParamMap paramMap = paramMap().param("workspace", workspace);
        Integer count = workspace == null ? 0 :
                        (Integer) getSqlMapClientTemplate().queryForObject("workspaceExists",
                                                                           paramMap);
        if (count == 0) {
            try {
                getSqlMapClientTemplate().insert("createWorkspace", paramMap);
            } catch (DataIntegrityViolationException e) {
                log.warn("race condition while guaranteeing existence of workspace " +
                         workspace + " - this is probably okay.");
            }
        }
    }

    public List<String> listWorkspaces() {
        return getSqlMapClientTemplate().queryForList("listWorkspaces");
    }

    public List<String> listCollections(String workspace) {
        return getSqlMapClientTemplate().queryForList(
                "listCollections",
                paramMap().param("workspace", workspace));
    }

    public Object selectEntryInternalId(EntryDescriptor entryQuery) {
        return getSqlMapClientTemplate().queryForObject("selectEntryInternalId",
                                                        paramMap()
                                                                .param("workspace", entryQuery.getWorkspace())
                                                                .param("collection", entryQuery.getCollection())
                                                                .param("entryId", entryQuery.getEntryId())
                                                                .addLocaleInfo(entryQuery.getLocale()));
    }

    public EntryMetaData selectEntryByInternalId(Object internalId) {
        return (EntryMetaData) getSqlMapClientTemplate().queryForObject(
                "selectEntryByInternalId",
                paramMap().param("internalId", internalId));
    }

    public long selectMaxIndex(Date updatedMax) {
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
}
