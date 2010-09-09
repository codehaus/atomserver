/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl.rwimpl;

import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.rwdao.ReadContentDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ReadContentDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements ReadContentDAO {

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

    public boolean contentExists(EntryMetaData entry) {
        Integer count = (Integer)
                getSqlMapClientTemplate().queryForObject("selectContentExists",
                                                         paramMap().param("entryStoreId", entry.getEntryStoreId()));
        return count > 0;
    }
}
