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
import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoriesDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements EntryCategoriesDAO {

    /**
     */
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
    /**
     */
    public int insertEntryCategory(EntryCategory entry) {
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

    /**
     */
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
    //       SELECT
    //-----------------------
    /**
     */
    public EntryCategory selectEntryCategory(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (EntryCategory) (getSqlMapClientTemplate().queryForObject("selectEntryCategory", entryQuery));
        }
        finally {

            stopWatch.stop("DB.selectEntryCategory",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

    //-----------------------
    //       DELETE
    //-----------------------
    /**
     * Delete this Entry DB entry.
     * This form does delete the actual record from the DB.
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

    /**
     */
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

    public List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList(
                    "selectCategoriesForEntries",
                    paramMap()
                            .param("workspace", workspace)
                            .param("collection", collection)
                            .param("entryIds", new ArrayList<String>(entryIds)));
        }
        finally {
            stopWatch.stop("DB.selectEntriesForCategories", "[" + workspace + "." + collection + "]");            
        }
    }

    public List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery) {
        return selectEntryCategoriesInScheme(entryQuery, null);
    }

    public List<EntryCategory> selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            EntryCategory paramMap = new EntryCategory();
            if (entryQuery instanceof EntryMetaData) {
                paramMap.setEntryStoreId(((EntryMetaData) entryQuery).getEntryStoreId());
            } else {
                paramMap.setWorkspace(entryQuery.getWorkspace());
                paramMap.setCollection(entryQuery.getCollection());
                paramMap.setEntryId(entryQuery.getEntryId());
                paramMap.setLocale(entryQuery.getLocale());
            }
            paramMap.setScheme(scheme);
            return getSqlMapClientTemplate().queryForList("selectEntryCategories", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoriesInScheme",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));
        }
    }


    public void deleteEntryCategories(EntryDescriptor entryQuery) {
        if (entryQuery instanceof EntryMetaData) {
            deleteEntryCategoriesInScheme((EntryMetaData) entryQuery, null);
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

    /**
     */
    public List<String> selectDistictCollections(String workspace) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectDistinctCollections", workspace);
        }
        finally {
            stopWatch.stop("DB.selectDistinctCollections", "[" + workspace + "]");            
        }
    }

    /**
     */
    public List<Map<String, String>> selectDistictCategoriesPerCollection(String workspace, String collection) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl::selectDistictCategoriesPerCollection [ " + workspace + " " + collection + " ]");
        }
        try {
            return getSqlMapClientTemplate().queryForList(
                    "selectDistinctCategoriesPerCollection",
                    paramMap()
                            .param("workspace", workspace)
                            .param("collection", collection));
        }
        finally {
            stopWatch.stop("DB.selectDistinctCollections", "[" + workspace + "." + collection + "]");
        }
    }


    //======================================
    //          COUNT QUERIES
    //======================================
    public int getTotalCount(String workspace) {
        return super.getTotalCountInternal(workspace, null, "countEntryCategoriesTotal");
    }

    public int getTotalCount(String workspace, String collection) {
        return super.getTotalCountInternal(workspace, collection, "countEntryCategoriesTotal");
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
