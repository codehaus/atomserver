/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl;

import com.ibatis.sqlmap.client.SqlMapExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.WriteReadCategoriesDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class WriteReadCategoriesDAOiBatisImpl
        extends ReadCategoriesDAOiBatisImpl
        implements WriteReadCategoriesDAO {

    static class EntryCategoryBatcher implements SqlMapClientCallback {
        static final Log log = LogFactory.getLog(EntryCategoryBatcher.class);

        static enum OperationType {
            INSERT, DELETE
        }

        private List<EntryCategory> entryCategoryList = null;
        private OperationType opType = null;

        EntryCategoryBatcher(List<EntryCategory> entryCategoryList, OperationType opType) {
            this.entryCategoryList = entryCategoryList;
            this.opType = opType;
        }

        public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {

            executor.startBatch();
            for (EntryCategory entryCategory : entryCategoryList) {
                if (opType == OperationType.INSERT) {
                    executor.insert("insertEntryCategory", entryCategory);
                } else if (opType == OperationType.DELETE) {
                    executor.delete("deleteEntryCategory", entryCategory);
                } else {
                    String msg = "Unknown OperationType";
                    log.error(msg);
                    throw new SQLException(msg);
                }
            }
            executor.executeBatch();

            return null;
        }
    }

    //======================================
    //   CRUD methods for entries table
    //======================================
    //-----------------------
    //       INSERT
    //-----------------------

    public int insertEntryCategory(EntryCategory entry) {
        return insertEntryCategory(entry, true);
    }

    public int insertEntryCategoryWithNoCacheUpdate(EntryCategory entry) {
        return insertEntryCategory(entry, false);
    }

    private int insertEntryCategory(EntryCategory entry, boolean updateCache) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl INSERT ==> " + entry);
        }

        int numRowsAffected = 0;
        try {
            getSqlMapClientTemplate().insert("insertEntryCategory", entry);
            numRowsAffected = 1;
        }
        finally {
            stopWatch.stop("DB.insertEntryCategory", AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entry));
        }
        return numRowsAffected;
    }

    public void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesDAOiBatisImpl INSERT BATCH==> " + entryCategoryList);
        }
        try {
            getSqlMapClientTemplate().execute(new EntryCategoryBatcher(entryCategoryList,
                                                                       EntryCategoryBatcher.OperationType.INSERT));
        }
        finally {
            stopWatch.stop("DB.insertEntryCategoryBATCH", "");
        }
    }

    //-----------------------
    //       DELETE
    //-----------------------

    /**
     * Delete this Entry DB entry.
     * This form does delete the actual record from the DB.
     * It will also update the cache if the entry belongs to a cached feed.
     */
    public void deleteEntryCategory(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl DELETE [ " + entryQuery + " ]");
        }
        try {
            getSqlMapClientTemplate().delete("deleteEntryCategory", entryQuery);
        }
        finally {
            stopWatch.stop("DB.deleteEntryCategory",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

    public void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesDAOiBatisImpl DELETE BATCH==> " + entryCategoryList);
        }
        try {
            getSqlMapClientTemplate().execute(new EntryCategoryBatcher(entryCategoryList,
                                                                       EntryCategoryBatcher.OperationType.DELETE));
        }
        finally {
            stopWatch.stop("DB.deleteEntryCategoryBATCH", "");
        }
    }

    //======================================
    //          LIST QUERIES
    //======================================

    public void deleteEntryCategories(EntryDescriptor entryQuery) {
        if (entryQuery instanceof EntryMetaData) {
            deleteEntryCategoriesInScheme((EntryMetaData) entryQuery, null);
        } else {
            deleteEntryCategoriesInScheme(entryQuery, null, null);
        }
    }

    public void deleteEntryCategoriesWithoutCacheUpdate(EntryDescriptor entryQuery) {
        if (entryQuery instanceof EntryMetaData) {
            deleteEntryCategoriesInScheme(entryQuery, ((EntryMetaData) entryQuery).getEntryStoreId(), null);
        } else {
            deleteEntryCategoriesInScheme(entryQuery, null, null);
        }
    }

    public void deleteEntryCategoriesInScheme(EntryMetaData entryQuery, String scheme) {
        deleteEntryCategoriesInScheme(entryQuery, entryQuery.getEntryStoreId(), null);
    }

    private void deleteEntryCategoriesInScheme(EntryDescriptor entryQuery, Long entryStoreId, String scheme) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {

            EntryCategory paramMap = new EntryCategory();
            if (entryStoreId == null) {
                paramMap.setWorkspace(entryQuery.getWorkspace());
                paramMap.setCollection(entryQuery.getCollection());
                paramMap.setEntryId(entryQuery.getEntryId());
                paramMap.setLocale(entryQuery.getLocale());
            }
            paramMap.setEntryStoreId(entryStoreId);
            paramMap.setScheme(scheme);
            getSqlMapClientTemplate().delete("deleteEntryCategories", paramMap);
        }
        finally {
            stopWatch.stop("DB.deleteEntryCategoriesInScheme",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));
        }
    }

    //======================================
    //          UPDATE
    //======================================

    public int updateEntryCategory(EntryCategory updateCategory, String oldTerm) {
        StopWatch stopWatch = new AtomServerStopWatch();
        String workspace = updateCategory.getWorkspace();
        String collection = updateCategory.getCollection();
        if (log.isDebugEnabled()) {
            log.info("EntryCategoriesDAOiBatisImpl::updateEntryCategory [ " + workspace + " " + collection + " ]");
        }
        try {
            ParamMap map = paramMap()
                    .param("entryStoreId", updateCategory.getEntryStoreId())
                    .param("scheme", updateCategory.getScheme())
                    .param("fromTerm", oldTerm)
                    .param("toTerm", updateCategory.getTerm());
            if (updateCategory.getLabel() != null) {
                map.param("toLabel", updateCategory.getLabel());
            }

            return getSqlMapClientTemplate().update("updateSingleCategory", map);
        }
        finally {
            stopWatch.stop("DB.updateEntryCategory", "[" + workspace + "." + collection + "]");
        }
    }

    //======================================
    //          DELETE ALL ROWS
    //======================================

    public void deleteAllEntryCategories(String workspace) {
        super.deleteAllEntriesInternal(workspace, null, "deleteEntryCategoriesAll");
    }

    public void deleteAllEntryCategories(String workspace, String collection) {
        super.deleteAllEntriesInternal(workspace, collection, "deleteEntryCategoriesAll");
    }

    public void deleteAllRowsFromEntryCategories() {
        getSqlMapClientTemplate().delete("deleteAllRowsFromEntryCategories");
    }

}
