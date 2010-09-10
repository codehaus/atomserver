/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

import com.ibatis.sqlmap.client.SqlMapExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.dbstore.dao.rwdao.WriteReadCategoryLogEventsDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.orm.ibatis.SqlMapClientCallback;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class WriteReadCategoryLogEventsDAOiBatisImpl
        extends ReadCategoryLogEventsDAOiBatisImpl
        implements WriteReadCategoryLogEventsDAO {

    //======================================
    //           CRUD methods
    //======================================

    /**
     * Insert a single EntryCategoryLogEvent
     */
    public int insertEntryCategoryLogEvent(EntryCategory entry) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl INSERT ==> " + entry);
        }

        int numRowsAffected = 0;
        try {
            getSqlMapClientTemplate().insert("insertEntryCategoryLogEvent", entry);
            numRowsAffected = 1;
        }
        finally {
            stopWatch.stop("DB.insertEntryCategoryLogEvent",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entry));
        }
        return numRowsAffected;
    }

    /**
     * NOTE: This method is really only here for Unit Testing
     * Delete ALL EntryCategoryLogEvents for a given EntryCategory.
     * If an Entry has, say two LogEvents for (urn:foo)bar, both will be deleted
     */
    public void deleteEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl DELETE [ " + entryQuery + " ]");
        }
        try {
            getSqlMapClientTemplate().delete("deleteEntryCategoryLogEventsBySchemeTerm", entryQuery);
        }
        finally {
            stopWatch.stop("DB.deleteEntryCategoryLogEvents",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

    /**
     * NOTE: This method is really only here for Unit Testing
     * Delete ALL EntryCategoryLogEvents for a given EntryCategory.
     * If an Entry has, say two LogEvents for (urn:foo)bar, both will be deleted
     */
    public void deleteEntryCategoryLogEvent(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl DELETE [ " + entryQuery + " ]");
        }
        try {
            Map<String, Object> paramMap = paramMap()
                    .param("workspace", entryQuery.getWorkspace())
                    .param("collection", entryQuery.getCollection())
                    .param("entryId", entryQuery.getEntryId())
                    .addLocaleInfo(entryQuery.getLocale());

            getSqlMapClientTemplate().delete("deleteEntryCategoryLogEvents", paramMap);
        }
        finally {
            stopWatch.stop("DB.deleteEntryCategoryLogEvents", "");

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
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isTraceEnabled()) {
            log.trace("EntryCategoryLogEventDAOiBatisImpl INSERT BATCH==> " + entryCategoryList);
        }
        try {
            getSqlMapClientTemplate().execute(new EntryCategoryLogEventBatcher(entryCategoryList));
        }
        finally {
            stopWatch.stop("DB.insertEntryCategoryLogEventBATCH", "");
        }
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
