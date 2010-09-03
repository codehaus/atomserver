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

package org.atomserver.core.dbstore.dao.impl;

import com.ibatis.sqlmap.client.SqlMapClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.core.dbstore.dao.EntryCategoryLogEventDAO;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoryLogEventDAOiBatisImpl
        implements EntryCategoryLogEventDAO, InitializingBean {

    static protected final Log log = LogFactory.getLog(AbstractDAOiBatisImpl.class);
    static public final String DEFAULT_DB_TYPE = "sqlserver";

    /**
     * valid values are "hsql", "mysql" and "sqlserver"
     */
    protected String dbType = DEFAULT_DB_TYPE;
    protected DataSource dataSource;

    private ReadEntryCategoryLogEventDAOiBatisImpl readDAO;
    private WriteReadEntryCategoryLogEventDAOiBatisImpl writeReadDAO;

    private SqlMapClient sqlMapClient;

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeReadDAO == null) {
                writeReadDAO = new WriteReadEntryCategoryLogEventDAOiBatisImpl();
                setupDAO(writeReadDAO);
            }
            if (readDAO == null) {
                readDAO = new ReadEntryCategoryLogEventDAOiBatisImpl();
                setupDAO(readDAO);
            }
        }
    }

    private void setupDAO(ReadEntryCategoryLogEventDAOiBatisImpl dao) {
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    public String getDatabaseType() {
        return dbType;
    }

    public void setDatabaseType(String dbType) {
        if (DatabaseType.isValidType(dbType)) {
            log.info("Database Type = " + dbType);
            this.dbType = dbType;
        } else {
            throw new IllegalArgumentException(dbType + " is not a valid DatabaseType value");
        }
    }

    public Date selectSysDate() {
        return readDAO.selectSysDate();
    }

    public void testAvailability() {
        writeReadDAO.testAvailability();
    }

    public ReadEntryCategoryLogEventDAOiBatisImpl getReadEntryCategoryLogEventDAO() {
        return readDAO;
    }

    public void setReadEntryCategoryLogEventDAO(ReadEntryCategoryLogEventDAOiBatisImpl readDAO) {
        this.readDAO = readDAO;
    }

    public WriteReadEntryCategoryLogEventDAOiBatisImpl getWriteReadEntryCategoryLogEventDAO() {
        return writeReadDAO;
    }

    public void setWriteReadEntryCategoryLogEventDAO(WriteReadEntryCategoryLogEventDAOiBatisImpl writeReadDAO) {
        this.writeReadDAO = writeReadDAO;
    }

    public SqlMapClient getSqlMapClient() {
        return sqlMapClient;
    }

    public void setSqlMapClient(SqlMapClient sqlMapClient) {
        this.sqlMapClient = sqlMapClient;
    }

    // ----------------------------
    //  ReadEntryCategoryLogEventDAO
    // ----------------------------    

    public List<EntryCategoryLogEvent> selectEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery) {
        return readDAO.selectEntryCategoryLogEventBySchemeAndTerm(entryQuery);
    }

    public List<EntryCategoryLogEvent> selectEntryCategoryLogEventByScheme(EntryCategory entryQuery) {
        return readDAO.selectEntryCategoryLogEventByScheme(entryQuery);
    }

    public List<EntryCategoryLogEvent> selectEntryCategoryLogEvent(EntryCategory entryQuery) {
        return readDAO.selectEntryCategoryLogEvent(entryQuery);
    }

    public int getTotalCount(String workspace) {return readDAO.getTotalCount(workspace);}

    public int getTotalCount(String workspace, String collection) {return readDAO.getTotalCount(workspace, collection);}


    // ----------------------------
    //  WriteReadEntryCategoryLogEventDAO
    // ----------------------------    

    public int insertEntryCategoryLogEvent(EntryCategory entry) {
        return writeReadDAO.insertEntryCategoryLogEvent(entry);
    }

    public void deleteEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery) {
        writeReadDAO.deleteEntryCategoryLogEventBySchemeAndTerm(entryQuery);
    }

    public void deleteEntryCategoryLogEvent(EntryDescriptor entryQuery) {
        writeReadDAO.deleteEntryCategoryLogEvent(entryQuery);
    }

    public void insertEntryCategoryLogEventBatch(List<EntryCategory> entryCategoryList) {
        writeReadDAO.insertEntryCategoryLogEventBatch(entryCategoryList);
    }

    public void deleteAllEntryCategoryLogEvents(String workspace) {
        writeReadDAO.deleteAllEntryCategoryLogEvents(workspace);
    }

    public void deleteAllEntryCategoryLogEvents(String workspace, String collection) {
        writeReadDAO.deleteAllEntryCategoryLogEvents(workspace, collection);
    }

    public void deleteAllRowsFromEntryCategoryLogEvent() {writeReadDAO.deleteAllRowsFromEntryCategoryLogEvent();}
}

/*    
    //======================================
    //           CRUD methods
    //======================================
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

   public List<EntryCategoryLogEvent> selectEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)
                    (getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEventsBySchemeTerm",entryQuery));
        }
        finally {
              stopWatch.stop("DB.selectEntryCategoryLogEventsBySchemeTerm",
                            AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

   public List<EntryCategoryLogEvent> selectEntryCategoryLogEventByScheme(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)
                    (getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEventsByScheme",entryQuery));
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoryLogEventsByScheme",
                            AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

   public List<EntryCategoryLogEvent> selectEntryCategoryLogEvent(EntryCategory entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)
                    (getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEvents",entryQuery));
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoryLogEvents",
                            AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

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
*/
