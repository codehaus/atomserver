/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.rwdao.WriteReadContentDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

import java.util.Map;

/**
 *
 */
public class WriteReadContentDAOiBatisImpl
        extends ReadContentDAOiBatisImpl
        implements WriteReadContentDAO {

    public void putContent(EntryMetaData
            entry, String content) {
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
            return (String) getSqlMapClientTemplate().queryForObject("selectContent",
                                                                     paramMap().param("entryStoreId", entry.getEntryStoreId()));
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
                                                         paramMap().param("entryStoreId", entry.getEntryStoreId()));
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
