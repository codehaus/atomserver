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
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl INSERT ==> " + entry);
        }

        int numRowsAffected = 0;
        try {
            getSqlMapClientTemplate().insert("insertEntryCategory", entry);
            numRowsAffected = 1;
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.insertEntryCategory", perflog.getPerfLogEntryCategoryString(entry), stopWatch);
            }
        }
        return numRowsAffected;
    }

    /**
     */
    public void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesDAOiBatisImpl INSERT BATCH==> " + entryCategoryList);
        }
        try {
            getSqlMapClientTemplate().execute(new EntryCategoryBatcher(entryCategoryList,
                                                                       EntryCategoryBatcher.OperationType.INSERT));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.insertEntryCategoryBATCH", "", stopWatch);
            }
        }
    }

    //-----------------------
    //       SELECT
    //-----------------------
    /**
     */
    public EntryCategory selectEntryCategory(EntryCategory entryQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (EntryCategory) (getSqlMapClientTemplate().queryForObject("selectEntryCategory", entryQuery));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntryCategory", perflog.getPerfLogEntryCategoryString(entryQuery), stopWatch);
            }
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
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoriesDAOiBatisImpl DELETE [ " + entryQuery + " ]");
        }
        try {
            getSqlMapClientTemplate().delete("deleteEntryCategory", entryQuery);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.deleteEntryCategory", perflog.getPerfLogEntryCategoryString(entryQuery), stopWatch);
            }
        }
    }

    /**
     */
    public void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoriesDAOiBatisImpl DELETE BATCH==> " + entryCategoryList);
        }
        try {
            getSqlMapClientTemplate().execute(new EntryCategoryBatcher(entryCategoryList,
                                                                       EntryCategoryBatcher.OperationType.DELETE));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.deleteEntryCategoryBATCH", "", stopWatch);
            }
        }
    }

    //======================================
    //          LIST QUERIES
    //======================================

    public List selectEntriesCategories(String workspace, String collection, Set<String> entryIds) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList(
                    "selectCategoriesForEntries",
                    paramMap()
                            .param("workspace", workspace)
                            .param("collection", collection)
                            .param("entryIds", new ArrayList<String>(entryIds)));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntriesForCategories", "[" + workspace + "." + collection + "]", stopWatch);
            }
        }
    }

    public List selectEntryCategories(EntryDescriptor entryQuery) {
        return selectEntryCategoriesInScheme(entryQuery, null);
    }

    public List selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme) {
        StopWatch stopWatch = new AutomaticStopWatch();
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
            if (perflog != null) {
                perflog.log("DB.selectEntryCategoriesInScheme", perflog.getPerfLogEntryString(entryQuery), stopWatch);
            }
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
        StopWatch stopWatch = new AutomaticStopWatch();
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
            if (perflog != null) {
                perflog.log("DB.deleteEntryCategoriesInScheme", perflog.getPerfLogEntryString(entryQuery), stopWatch);
            }
        }
    }

    /**
     */
    public List selectDistictCollections(String workspace) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectDistinctCollections", workspace);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectDistinctCollections", "[" + workspace + "]", stopWatch);
            }
        }
    }

    /**
     */
    public List selectDistictCategoriesPerCollection(String workspace, String collection) {
        StopWatch stopWatch = new AutomaticStopWatch();
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
            if (perflog != null) {
                perflog.log("DB.selectDistinctCollections", "[" + workspace + "." + collection + "]", stopWatch);
            }
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
