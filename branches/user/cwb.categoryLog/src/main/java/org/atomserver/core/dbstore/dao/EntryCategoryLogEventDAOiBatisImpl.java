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
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import java.sql.SQLException;
import java.util.List;


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoryLogEventDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements EntryCategoryLogEventDAO {

    //======================================
    //           CRUD methods
    //======================================
    /**
     * Insert a single EntryCategoryLogEvent
     */
    public int insertEntryCategoryLogEvent(EntryCategory entry) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl INSERT ==> " + entry);
        }

        int numRowsAffected = 0;
        try {
            getSqlMapClientTemplate().insert("insertEntryCategoryLogEvent", entry);
            numRowsAffected = 1;
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.insertEntryCategoryLogEvent", perflog.getPerfLogEntryCategoryString(entry), stopWatch);
            }
        }
        return numRowsAffected;
    }

    /**
     * Select ALL EntryCategoryLogEvents for a given EntryCategory.
     * I.e. Return EntryCategoryLogEvents that match both Entry and Scheme/Term.
     */
    public List<EntryCategoryLogEvent> selectEntryCategoryLogEvent(EntryCategory entryQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)(getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEvents",
                                                                                        entryQuery));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.selectEntryCategoryLogEvents", perflog.getPerfLogEntryCategoryString(entryQuery), stopWatch);
            }
        }
    }

    /**
     * NOTE: This method is really only here for Unit Testing
     * Delete ALL EntryCategoryLogEvents for a given EntryCategory.
     * If an Entry has, say two LogEvents for (urn:foo)bar, both will be deleted
     */
    public void deleteEntryCategoryLogEvent(EntryCategory entryQuery) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl DELETE [ " + entryQuery + " ]");
        }
        try {
            getSqlMapClientTemplate().delete("deleteEntryCategoryLogEvents", entryQuery);
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.deleteEntryCategoryLogEvents", perflog.getPerfLogEntryCategoryString(entryQuery), stopWatch);
            }
        }
    }

    //======================================
    //          BATCH OPERATIONS
    //======================================
    static class EntryCategoryLogEventBatcher implements SqlMapClientCallback {
        static final Log log = LogFactory.getLog(EntryCategoryLogEventBatcher.class);

        private List<EntryCategory> entryCategoryList = null;

        EntryCategoryLogEventBatcher(List<EntryCategory> entryCategoryList) {
            this.entryCategoryList = entryCategoryList;
        }

        public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
            executor.startBatch();
            for (EntryCategory entryCategory : entryCategoryList) {
                executor.insert("insertEntryCategoryLogEvent", entryCategory);
            }
            executor.executeBatch();
            return null;
        }
    }

    public void insertEntryCategoryLogEventBatch(List<EntryCategory> entryCategoryList) {
        StopWatch stopWatch = new AutomaticStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoryLogEventDAOiBatisImpl INSERT BATCH==> " + entryCategoryList);
        }
        try {
            getSqlMapClientTemplate().execute(new EntryCategoryLogEventBatcher(entryCategoryList));
        }
        finally {
            if (perflog != null) {
                perflog.log("DB.insertEntryCategoryLogEventBATCH", "", stopWatch);
            }
        }
    }

    //======================================
    //          COUNT QUERIES
    //======================================
    public int getTotalCount(String workspace) {
        return super.getTotalCountInternal(workspace, null, "countEntryCategoryLogEventsTotal");
    }

    public int getTotalCount(String workspace, String collection) {
        return super.getTotalCountInternal(workspace, collection, "countEntryCategoryLogEventsTotal");
    }

    //======================================
    //          DELETE ALL ROWS
    //======================================
    public void deleteAllEntryCategoryLogEvents(String workspace) {
        super.deleteAllEntriesInternal(workspace, null, "deleteEntryCategoryLogEventsAll");
    }

    public void deleteAllEntryCategoryLogEvents(String workspace, String collection) {
        super.deleteAllEntriesInternal(workspace, collection, "deleteEntryCategoryLogEventsAll");
    }

    public void deleteAllRowsFromEntryCategoryLogEvent() {
        getSqlMapClientTemplate().delete("deleteAllRowsFromEntryCategoryLogEvent");
    }

}
