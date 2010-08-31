/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.util.*;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BaseEntriesDAOiBatisImpl
        extends AbstractDAOiBatisImpl {

    public static final int UNDEFINED = -1;
    public static final Date ZERO_DATE = new Date(0L);

    protected ContentDAO contentDAO;
    protected EntryCategoriesDAO entryCategoriesDAO;
    protected EntryCategoryLogEventDAO entryCategoryLogEventDAO;
    protected int latencySeconds = UNDEFINED;


    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public void setEntryCategoriesDAO(EntryCategoriesDAO entryCategoriesDAO) {
        this.entryCategoriesDAO = entryCategoriesDAO;
    }

    public void setEntryCategoryLogEventDAO(EntryCategoryLogEventDAO entryCategoryLogEventDAO) {
        this.entryCategoryLogEventDAO = entryCategoryLogEventDAO;
    }

    @ManagedAttribute
    public int getLatencySeconds() {
        return latencySeconds;
    }

    @ManagedAttribute
    public void setLatencySeconds(int latencySeconds) {
        // this will be true if we are setting this for the second time through JMX
        if (this.latencySeconds != UNDEFINED) {
            // protect against a wacky value coming in through JMX
            int txnTimeout = UNDEFINED;
            String txnTimeoutStr = ConfigurationAwareClassLoader.getENV().getProperty("db.timeout.txn.put");
            if (txnTimeoutStr != null) {
                try {
                    txnTimeout = Integer.parseInt(txnTimeoutStr);
                } catch (NumberFormatException ee) {
                    log.error("setLatencySeconds; NumberFormatException:: ", ee);
                }
                txnTimeout = txnTimeout / 1000;
            } else {
                log.error("db.timeout.txn.put is NULL ");
            }

            if (!(latencySeconds < 0 || ((txnTimeout != UNDEFINED) && (latencySeconds < txnTimeout)))) {
                this.latencySeconds = latencySeconds;
            } else {
                log.error("The latency provided (" + latencySeconds + ") is less than txnTimeout (" +
                          txnTimeout + ")");
            }
        }
        this.latencySeconds = latencySeconds;
    }

    public Object selectEntryInternalId(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForObject("selectEntryInternalId",
                                                            paramMap()
                                                                    .param("workspace", entryQuery.getWorkspace())
                                                                    .param("collection", entryQuery.getCollection())
                                                                    .param("entryId", entryQuery.getEntryId())
                                                                    .addLocaleInfo(entryQuery.getLocale()));
        }
        finally {
            stopWatch.stop("DB.selectEntryInternalId", "");
        }
    }

    public EntryMetaData selectEntryByInternalId(Object internalId) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return (EntryMetaData) getSqlMapClientTemplate().queryForObject("selectEntryByInternalId",
                                                                            paramMap().param("internalId", internalId));
        }
        finally {
            stopWatch.stop("DB.selectEntryInternalId2", "");
        }
    }
    
    public EntryMetaData selectEntry(EntryDescriptor entryQuery) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            Map<String, Object> paramMap = paramMap()
                    .param("workspace", entryQuery.getWorkspace())
                    .param("collection", entryQuery.getCollection())
                    .param("entryId", entryQuery.getEntryId())
                    .addLocaleInfo(entryQuery.getLocale());

            if (log.isDebugEnabled()) {
                log.debug("SELECT EntriesDAOiBatisImpl selectEntry:: paramMap= " + paramMap);
            }
            return (EntryMetaData) (getSqlMapClientTemplate().queryForObject("selectEntry", paramMap));
        }
        finally {
            stopWatch.stop("DB.selectEntry", AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryQuery));
        }
    }


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

    /**
     * NOTE: package scoped for use by JUnits
     */
    public List<EntryMetaData> selectEntriesByLastModified(String workspace, String collection,
                                                           Date updatedMin) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            return getSqlMapClientTemplate().queryForList("selectEntriesByLastModified",
                                                          paramMap()
                                                                  .param("updatedMin", updatedMin)
                                                                  .param("workspace", workspace)
                                                                  .param("collection", collection));
        }
        finally {
            stopWatch.stop("DB.selectEntriesByLastModified", "");
        }
    }

    // TODO -- here only for tests
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
            addSetOpsSelectFeedPageParams(paramMap, categoryQuery);

            return getSqlMapClientTemplate().queryForList("selectFeedPage", paramMap);
        }
        finally {
            stopWatch.stop("DB.selectFeedPage",
                           AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale, feed.getWorkspace(), feed.getCollection()));
        }
    }

    protected void addSetOpsSelectFeedPageParams(AbstractDAOiBatisImpl.ParamMap paramMap, Collection<BooleanExpression<AtomCategory>> categoryQuery) {
        if (categoryQuery != null && !categoryQuery.isEmpty()) {
            paramMap.param("categoryQuerySql",
                           SetOpCategoryQueryGenerator.generateCategorySearch(categoryQuery));
        }
        if (latencySeconds > 0) {
            paramMap.param("latencySeconds", latencySeconds);
        }
        paramMap.param("usequery", "setOps");
    }

    protected AbstractDAOiBatisImpl.ParamMap prepareBatchParamMap(Collection<? extends EntryDescriptor> entryQueries) {
        AbstractDAOiBatisImpl.ParamMap paramMap = paramMap();

        String workspace = null;
        String collection = null;
        Locale locale = null;
        List<String> entryIds = new ArrayList<String>();

        for (EntryDescriptor entryQuery : entryQueries) {
            if (workspace != null && !workspace.equals(entryQuery.getWorkspace())) {
                String msg = "Attempt to use more than one workspace";
                log.error(msg);
                throw new AtomServerException(msg);
            } else {
                workspace = entryQuery.getWorkspace();
                paramMap.param("workspace", workspace);
            }
            if (collection != null && !collection.equals(entryQuery.getCollection())) {
                String msg = "Attempt to use more than one collection";
                log.error(msg);
                throw new AtomServerException(msg);
            } else {
                collection = entryQuery.getCollection();
                paramMap.param("collection", collection);
            }
            if (locale != null && !locale.equals(entryQuery.getLocale())) {
                String msg = "Attempt to use more than one locale";
                log.error(msg);
                throw new AtomServerException(msg);
            } else {
                locale = entryQuery.getLocale();
                paramMap.addLocaleInfo(locale);
            }

            entryIds.add(entryQuery.getEntryId());
        }

        return paramMap.param("entryIds", entryIds);
    }

// NOTE: package scoped for use by EntryCategoryIBatisImpl

    AbstractDAOiBatisImpl.ParamMap prepareParamMapForSelectEntries(Date updatedMin, Date updatedMax,
                                                                   int startIndex, int endIndex,
                                                                   int pageSize, String locale, FeedDescriptor feed) {

        if (updatedMin != null && updatedMin.equals(ZERO_DATE)) {
            updatedMin = null;
        }

        AbstractDAOiBatisImpl.ParamMap paramMap = paramMap()
                .param("workspace", feed.getWorkspace())
                .param("updatedMin", updatedMin)
                .param("updatedMax", updatedMax)
                .param("startIndex", (long) startIndex)
                .param("endIndex", (long) endIndex)
                .param("pageSize", pageSize)
                .param("collection", feed.getCollection());

        if (locale != null) {
            paramMap.param("undefinedCountry", "**").addLocaleInfo(LocaleUtils.toLocale(locale));
        }

        if (log.isDebugEnabled()) {
            log.debug("EntriesDAOiBatisImpl prepareParamMapForSelectEntries:: paramMap= " + paramMap);
        }
        return paramMap;
    }

}
