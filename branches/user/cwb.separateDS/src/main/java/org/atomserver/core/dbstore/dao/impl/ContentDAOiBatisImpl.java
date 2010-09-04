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

import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.ContentDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ContentDAOiBatisImpl
        extends AbstractDAOiBatisImplDelegator
        implements ContentDAO, InitializingBean {

    private ReadContentDAOiBatisImpl readContentDAO;
    private WriteReadContentDAOiBatisImpl writeReadContentDAO;

    public WriteReadContentDAOiBatisImpl getWriteReadContentDAO() { return writeReadContentDAO; }

    public void setWriteReadContentDAO(WriteReadContentDAOiBatisImpl writeReadContentDAO) {
        this.writeReadContentDAO = writeReadContentDAO;
    }

    public ReadContentDAOiBatisImpl getReadContentDAO() { return readContentDAO; }

    public void setReadContentDAO(ReadContentDAOiBatisImpl readContentDAO) { this.readContentDAO = readContentDAO; }

    public void afterPropertiesSet() throws Exception {
        if (dataSource != null) {
            if (writeReadContentDAO == null) {
                writeReadContentDAO = new WriteReadContentDAOiBatisImpl();
                setupDAO(writeReadContentDAO);
            }
            if (readContentDAO == null) {
                readContentDAO = new ReadContentDAOiBatisImpl();
                setupDAO(readContentDAO);
            }
        }
    }

    private void setupDAO(ReadContentDAOiBatisImpl dao) {
        dao.setSqlMapClient(sqlMapClient);
        dao.setDatabaseType(dbType);
        dao.setDataSource(dataSource);
        dao.afterPropertiesSet();
    }

    
    public AbstractDAOiBatisImpl getReadDAO() { return readContentDAO; }

    public String selectContent(EntryMetaData entry) {return readContentDAO.selectContent(entry);}


    public void deleteContent(EntryMetaData entry) {writeReadContentDAO.deleteContent(entry);}

    public boolean contentExists(EntryMetaData entry) {return readContentDAO.contentExists(entry);}

    public void deleteAllContent(String workspace) {writeReadContentDAO.deleteAllContent(workspace);}

    public void deleteAllContent(String workspace, String collection) {
        writeReadContentDAO.deleteAllContent(workspace, collection);
    }

    public void deleteAllRowsFromContent() {writeReadContentDAO.deleteAllRowsFromContent();}

    public void putContent(EntryMetaData entry, String content) {writeReadContentDAO.putContent(entry, content);}
}
    /*
    public void putContent(EntryMetaData entry, String content) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Map paramMap = paramMap()
                    .param("entryStoreId", entry.getEntryStoreId())
                    .param("content", content);
            if (contentExists(entry)) {
                getSqlMapClientTemplate().update("updateContent", paramMap);
            } else {
                getSqlMapClientTemplate().insert("insertContent", paramMap);
            }
        }
        finally {
            stopWatch.stop("DB.putContent", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
        }
    }

    public String selectContent(EntryMetaData entry) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return (String)
                    getSqlMapClientTemplate().queryForObject("selectContent",
                                                             paramMap().param("entryStoreId",
                                                                              entry.getEntryStoreId()));
        }
        finally {
            stopWatch.stop("DB.selectContent", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
        }
    }

    public void deleteContent(EntryMetaData entry) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            getSqlMapClientTemplate().delete("deleteContent",
                                             paramMap().param("entryStoreId", entry.getEntryStoreId()));
        }
        finally {
            stopWatch.stop("DB.deleteContent", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entry));
        }
    }

    public boolean contentExists(EntryMetaData entry) {
        Integer count = (Integer)
                getSqlMapClientTemplate().queryForObject("selectContentExists",
                                                         paramMap().param("entryStoreId",
                                                                          entry.getEntryStoreId()));
        return count > 0;
    }

    //======================================
    //          DELETE ALL ROWS
    //======================================

    public void deleteAllContent(String workspace) {
        super.deleteAllEntriesInternal(workspace, null, "deleteContentAll");
    }

    public void deleteAllContent(String workspace, String collection) {
        super.deleteAllEntriesInternal(workspace, collection, "deleteContentAll");
    }

    public void deleteAllRowsFromContent() {
        getSqlMapClientTemplate().delete("deleteAllRowsFromContent");
    }


}
*/