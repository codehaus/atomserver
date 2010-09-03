/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.impl;

import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.core.dbstore.dao.ReadCategoryLogEventsDAO;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;

import java.util.List;

/**
 *
 */
public class ReadCategoryLogEventsDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements ReadCategoryLogEventsDAO {

    /**
     * Select ALL EntryCategoryLogEvents for a given EntryCategory.
     * I.e. Return EntryCategoryLogEvents that match both Entry and Scheme/Term.
     */
    public List<EntryCategoryLogEvent> selectEntryCategoryLogEventBySchemeAndTerm(EntryCategory
            entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)
                    (getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEventsBySchemeTerm", entryQuery));
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoryLogEventsBySchemeTerm",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

    /**
     * Select ALL EntryCategoryLogEvents for a given EntryCategory.
     * I.e. Return EntryCategoryLogEvents that match both Entry and Scheme/Term.
     */
    public List<EntryCategoryLogEvent> selectEntryCategoryLogEventByScheme(EntryCategory
            entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)
                    (getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEventsByScheme", entryQuery));
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoryLogEventsByScheme",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
        }
    }

    /**
     * Select ALL EntryCategoryLogEvents for a given Entry
     */
    public List<EntryCategoryLogEvent> selectEntryCategoryLogEvent(EntryCategory
            entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        if (log.isDebugEnabled()) {
            log.debug("EntryCategoryLogEventDAOiBatisImpl SELECT ==> " + entryQuery);
        }
        try {
            return (List<EntryCategoryLogEvent>)
                    (getSqlMapClientTemplate().queryForList("selectEntryCategoryLogEvents", entryQuery));
        }
        finally {
            stopWatch.stop("DB.selectEntryCategoryLogEvents",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryCategoryString(entryQuery));
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
}
