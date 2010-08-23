/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ReadEntriesDAOiBatisImpl
        extends BaseEntriesDAOiBatisImpl
        implements ReadEntriesDAO {

    /**
     * Use the improved selectFeedPage form, which uses SQL Set operands.
     */
    private boolean isUsingSetOpsFeedPage = true;

    @ManagedAttribute
    public boolean isUsingSetOpsFeedPage() {
        return isUsingSetOpsFeedPage;
    }

    @ManagedAttribute
    public void setUsingSetOpsFeedPage(boolean usingSetOpsFeedPage) {
        isUsingSetOpsFeedPage = usingSetOpsFeedPage;
    }


//-----------------------
//     SELECT BATCH
//-----------------------

    public List<EntryMetaData> selectEntryBatch(Collection<? extends EntryDescriptor> entryQueries) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            AbstractDAOiBatisImpl.ParamMap paramMap = prepareBatchParamMap(entryQueries);

            if (log.isTraceEnabled()) {
                log.trace("SELECT EntriesDAOiBatisImpl selectEntryBatch:: paramMap= " + paramMap);
            }

            return getSqlMapClientTemplate().queryForList("selectEntryBatch", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectEntryBATCH", "");
        }
    }


//-----------------------
//       SELECT
//-----------------------


    public List<EntryMetaData> selectEntries(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Map<String, Object> paramMap = paramMap()
                    .param("workspace", entryQuery.getWorkspace())
                    .param("collection", entryQuery.getCollection())
                    .param("entryId", entryQuery.getEntryId())
                    .addLocaleInfo(entryQuery.getLocale());

            if (log.isDebugEnabled()) {
                log.debug("SELECT EntriesDAOiBatisImpl selectEntries:: paramMap= " + paramMap);
            }

            return getSqlMapClientTemplate().queryForList("selectEntries", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectEntries", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));

        }
    }


    public List<EntryMetaData> selectFeedPage(Date updatedMin,
                                              Date updatedMax,
                                              int startIndex,
                                              int endIndex,
                                              int pageSize,
                                              String locale,
                                              FeedDescriptor feed,
                                              Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            AbstractDAOiBatisImpl.ParamMap paramMap = prepareParamMapForSelectEntries(updatedMin, updatedMax,
                                                                                      startIndex, endIndex,
                                                                                      pageSize, locale, feed);

            if (isUsingSetOpsFeedPage) {
                addSetOpsSelectFeedPageParams(paramMap, categoryQuery);
            } else {
                throw new RuntimeException("REMOVED OLD QUERY");
            }

            return getSqlMapClientTemplate().queryForList("selectFeedPage", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectFeedPage",
                           AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale, feed.getWorkspace(), feed.getCollection()));
        }
    }


    /**
     * Use the improved selectFeedPage form, which uses SQL Set operands.
     */
    private void addSetOpsSelectFeedPageParams(AbstractDAOiBatisImpl.ParamMap paramMap, Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        if (categoryQuery != null && !categoryQuery.isEmpty()) {
            paramMap.param("categoryQuerySql",
                           SetOpCategoryQueryGenerator.generateCategorySearch(categoryQuery));
        }
        if (latencySeconds > 0) {
            paramMap.param("latencySeconds", latencySeconds);
        }
        paramMap.param("usequery", "setOps");
    }


    /**
     */
    public List<EntryMetaData> selectEntriesByLastModifiedSeqNum(FeedDescriptor feed,
                                                                 Date updatedMin) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectEntriesByLastModifiedSeqNum",
                                                          paramMap()
                                                                  .param("updatedMin", updatedMin)
                                                                  .param("workspace", feed.getWorkspace())
                                                                  .param("collection", feed.getCollection()));
        }
        finally {
            stopWatch.stop("DB.selectEntriesByLastModifiedSeqNum", "");
        }
    }

//======================================
//          COUNT QUERIES
//======================================

    public int getTotalCount(ServiceDescriptor service) {
        return super.getTotalCountInternal(service.getWorkspace(), null, "countEntriesTotal");
    }

    public int getTotalCount(FeedDescriptor feed) {
        return super.getTotalCountInternal(feed.getWorkspace(), feed.getCollection(), "countEntriesTotal");
    }

    public int getCountByLastModified(ServiceDescriptor service, Date lastModified) {
        return getCountByLastModifiedInternal(service.getWorkspace(), null, lastModified);
    }

    public int getCountByLastModified(FeedDescriptor feed, Date lastModified) {
        return getCountByLastModifiedInternal(feed.getWorkspace(), feed.getCollection(), lastModified);
    }

    int getCountByLastModifiedInternal(String workspace, String collection, Date updatedMin) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Integer count =
                    (Integer) (getSqlMapClientTemplate().queryForObject("$join".equals(workspace) ?
                                                                        "countModifiedAggregateEntries" :
                                                                        "countEntriesByLastModified",
                                                                        paramMap()
                                                                                .param("updatedMin", updatedMin)
                                                                                .param("workspace", workspace)
                                                                                .param("collection", collection)));
            return count == null ? 0 : count;
        }
        finally {
            stopWatch.stop("DB.getCountByLastModified", "");
        }
    }


    public void ensureCollectionExists(String workspace, String collection) {
        ensureWorkspaceExists(workspace);
        AbstractDAOiBatisImpl.ParamMap paramMap = paramMap()
                .param("workspace", workspace)
                .param("collection", collection);
        Integer count =
                (Integer) getSqlMapClientTemplate().queryForObject("collectionExists",
                                                                   paramMap);
        if (count == 0) {
            try {
                getSqlMapClientTemplate().insert("createCollection", paramMap);
            } catch (DataIntegrityViolationException e) {
                log.warn("race condition while guaranteeing existence of collection " +
                         workspace + "/" + collection + " - this is probably okay.");
            }
        }
    }

    public void ensureWorkspaceExists(String workspace) {
        AbstractDAOiBatisImpl.ParamMap paramMap = paramMap().param("workspace", workspace);
        Integer count = workspace == null ? 0 :
                        (Integer) getSqlMapClientTemplate().queryForObject("workspaceExists",
                                                                           paramMap);
        if (count == 0) {
            try {
                getSqlMapClientTemplate().insert("createWorkspace", paramMap);
            } catch (DataIntegrityViolationException e) {
                log.warn("race condition while guaranteeing existence of workspace " +
                         workspace + " - this is probably okay.");
            }
        }
    }

    public List<String> listWorkspaces() {
        return getSqlMapClientTemplate().queryForList("listWorkspaces");
    }

    public List<String> listCollections(String workspace) {
        return getSqlMapClientTemplate().queryForList(
                "listCollections",
                paramMap().param("workspace", workspace));
    }

    public Object selectEntryInternalId(EntryDescriptor entryQuery) {
        return getSqlMapClientTemplate().queryForObject("selectEntryInternalId",
                                                        paramMap()
                                                                .param("workspace", entryQuery.getWorkspace())
                                                                .param("collection", entryQuery.getCollection())
                                                                .param("entryId", entryQuery.getEntryId())
                                                                .addLocaleInfo(entryQuery.getLocale()));
    }

    public EntryMetaData selectEntryByInternalId(Object internalId) {
        return (EntryMetaData) getSqlMapClientTemplate().queryForObject(
                "selectEntryByInternalId",
                paramMap().param("internalId", internalId));
    }

    public long selectMaxIndex(Date updatedMax) {
        AbstractDAOiBatisImpl.ParamMap paramMap = paramMap();
        if (latencySeconds > 0) {
            paramMap.param("latencySeconds", latencySeconds);
        }
        if (updatedMax != null) {
            paramMap.param("updatedMax", updatedMax);
        }
        Long retVal = (Long) getSqlMapClientTemplate().queryForObject("selectMaxIndex", paramMap);
        return (retVal == null) ? 0L : retVal;
    }

}

