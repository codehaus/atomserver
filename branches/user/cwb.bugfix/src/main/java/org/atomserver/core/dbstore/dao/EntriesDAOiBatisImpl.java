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
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.*;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntriesDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements EntriesDAO {

    public static final long UNDEFINED_SEQNUM = -1L;
    public static final Date ZERO_DATE = new Date(0L);

    private ContentDAO contentDAO;
    private EntryCategoriesDAO entryCategoriesDAO;
    private EntryCategoryLogEventDAO entryCategoryLogEventDAO;

    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setEntryCategoryLogEventDAO(EntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
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
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryDAOiBatisImpl " + opType + " BATCH==> " + entryList);
        }
        try {
            return (Integer) (getSqlMapClientTemplate().execute(new EntryBatcher(this, entryList, opType)));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB." + opType + "EntryBATCH", "", stopWatch);
            }
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
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            ParamMap paramMap = prepareBatchParamMap(entryQueries);

            if (log.isTraceEnabled()) {
                log.trace("SELECT EntriesDAOiBatisImpl selectEntryBatch:: paramMap= " + paramMap);
            }

            return getSqlMapClientTemplate().queryForList("selectEntryBatch", paramMap);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntryBATCH", "", stopWatch);
            }
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
     * This form conditionally adds a Sequence Number for lastModified.
     *
     * @param isSeedingDB = true indicates that we WILL use the lastModified defined elsewhere
     *                    (most likely the file's lastModified). And we will NOT set lastModifiedSeqNum
     */
    public Object insertEntry(EntryDescriptor entry, boolean isSeedingDB) {
        return insertEntry(entry, false, null, null);
    }

    public Object insertEntry(EntryDescriptor entry,
                              boolean isSeedingDB,
                              Date published,
                              Date lastModified) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl INSERT ==> " + entry);
        }

        // now insert
        try {
            ParamMap paramMap = prepareInsertParamMap(entry);

            if (isSeedingDB) {
                paramMap.param("publishedDate", published)
                        .param("lastModifiedDate", lastModified);
                return getSqlMapClientTemplate().insert("insertEntrySeedingDB-" + getDatabaseType(), paramMap);
            } else {
                return getSqlMapClientTemplate().insert("insertEntry-" + getDatabaseType(), paramMap);
            }
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.insertEntry", perflog.getPerfLogEntryString(entry), stopWatch);
            }
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
        StopWatch stopWatch = new AutomaticStopWatch();
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
            if (perflog != null) {
                perflog.log("DB.selectEntry", perflog.getPerfLogEntryString(entryQuery), stopWatch);
            }
        }
    }

    public List<EntryMetaData> selectEntries(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
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
            if (perflog != null) {
                perflog.log("DB.selectEntries", perflog.getPerfLogEntryString(entryQuery), stopWatch);
            }
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
     * filePath or the deleted flag. This is because we MUST have new lastModified and lastModifiedSeqNum
     * so that Pagination will work properly.
     */
    public int updateEntry(EntryDescriptor entryQuery, boolean deleted) {
        StopWatch stopWatch = new AutomaticStopWatch();
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
            if (perflog != null) {
                perflog.log("DB.updateEntry", perflog.getPerfLogEntryString(entryQuery), stopWatch);
            }
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

    private int updateEntryOverwrite(EntryMetaData entry, boolean resetRevision, Date published, Date lastModified) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            if (log.isDebugEnabled()) {
                log.debug("EntriesDAOiBatisImpl UPDATE ==> [resetRevision= " + resetRevision + "  entry= " + entry + "]");
            }

            int revision = (resetRevision) ? 0 : -1;

            return getSqlMapClientTemplate().update("updateEntryOverwrite",
                                                    prepareUpdateParamMap(false, revision, entry)
                                                            .param("publishedDate", published)
                                                            .param("lastModifiedDate", lastModified));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.updateEntryOverwrite", perflog.getPerfLogEntryString(entry), stopWatch);
            }
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


    public List<EntryMetaData> selectEntriesByPageAndLocale(
            FeedDescriptor feed,
            Date lastModifiedDate,
            int pageDelim,
            int pageSize,
            String locale) {
        return selectFeedPage(lastModifiedDate, pageDelim, pageSize,
                                     locale, feed, null);
    }

    public AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor, List<String> joinWorkspaces) {
        ParamMap paramMap = paramMap()
                .param("collection", entryDescriptor.getCollection())
                .param("entryId", entryDescriptor.getEntryId());
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

    public List<AggregateEntryMetaData> selectAggregateEntriesByPage(
            FeedDescriptor feed,
            Date lastModifiedDate,
            Locale locale,
            int pageDelim,
            int pageSize,
            Collection<BooleanExpression<AtomCategory>> categoriesQuery,
            List<String> joinWorkspaces) {
        ParamMap paramMap = prepareParamMapForSelectEntries(
                lastModifiedDate, pageDelim, pageSize,
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

        List entries = getSqlMapClientTemplate().queryForList("selectAggregateEntries", paramMap);
        Map<String, AggregateEntryMetaData> map =
                AggregateEntryMetaData.aggregate(feed.getWorkspace(), feed.getCollection(), locale, entries);
        return new ArrayList(map.values());
    }

    public List<EntryMetaData> selectFeedPage(
            Date lastModifiedDate,
            int pageDelim,
            int pageSize,
            String locale,
            FeedDescriptor feed,
            Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            ParamMap paramMap =
                    prepareParamMapForSelectEntries(
                            lastModifiedDate, pageDelim, pageSize, locale, feed);

            if (categoryQuery != null && !categoryQuery.isEmpty()) {
                paramMap.param("categoryFilterSql",
                               CategoryQueryGenerator.generateCategoryFilter(categoryQuery));
                paramMap.param("categoryQuerySql",
                               CategoryQueryGenerator.generateCategorySearch(categoryQuery));
            }

            return getSqlMapClientTemplate().queryForList("selectFeedPage", paramMap);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectFeedPage", perflog.getPerfLogFeedString(locale, feed.getWorkspace(), feed.getCollection()), stopWatch);
            }
        }
    }

    // NOTE: package scoped for use by EntryCategoryIBatisImpl
    ParamMap prepareParamMapForSelectEntries(Date lastModifiedDate, int pageDelim,
                                             int pageSize, String locale, FeedDescriptor feed) {
        ParamMap paramMap = paramMap()
                .param("workspace", feed.getWorkspace())
                .param("lastModifiedDate", lastModifiedDate)
                .param("lastModifiedSeqNum", (long) pageDelim)
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
                                                           Date lastModifiedDate) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectEntriesByLastModified",
                                                          paramMap()
                                                                  .param("lastModifiedDate", lastModifiedDate)
                                                                  .param("workspace", workspace)
                                                                  .param("collection", collection));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntriesByLastModified", "", stopWatch);
            }
        }
    }

    /**
     */
    public List<EntryMetaData> selectEntriesByLastModifiedSeqNum(FeedDescriptor feed,
                                                                 Date lastModifiedDate) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectEntriesByLastModifiedSeqNum",
                                                          paramMap()
                                                                  .param("lastModifiedDate", lastModifiedDate)
                                                                  .param("workspace", feed.getWorkspace())
                                                                  .param("collection", feed.getCollection()));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntriesByLastModifiedSeqNum", "", stopWatch);
            }
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
        // NOTE: this causes lastModifiedSeqNum to be generated in the correct order
        for (Object obj : sortedList) {
            EntryMetaData entry = (EntryMetaData) obj;
            updateEntryOverwrite(entry, true, entry.getPublishedDate(), entry.getLastModifiedDate());

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

    int getCountByLastModifiedInternal(String workspace, String collection, Date lastModified) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            Integer count =
                    (Integer) (getSqlMapClientTemplate().queryForObject("$join".equals(workspace) ?
                                                                        "countModifiedAggregateEntries" :
                                                                        "countEntriesByLastModified",
                                                                        paramMap()
                                                                                .param("lastModifiedDate", lastModified)
                                                                                .param("workspace", workspace)
                                                                                .param("collection", collection)));
            return count == null ? 0 : count;
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.getCountByLastModified", "", stopWatch);
            }
        }
    }

    //======================================
    //          DELETE ALL ROWS
    //======================================
    public void deleteAllEntries(ServiceDescriptor service) {
        if (contentDAO != null) {
            contentDAO.deleteAllContent();
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
            getSqlMapClientTemplate().insert("createWorkspace", paramMap);
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

    public void acquireLock() {
        log.debug("ACQUIRING LOCK");
        getSqlMapClientTemplate().queryForObject("acquireLock", paramMap());
    }
}
