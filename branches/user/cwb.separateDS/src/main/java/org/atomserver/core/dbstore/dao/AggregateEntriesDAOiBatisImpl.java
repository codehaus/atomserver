/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

import java.util.*;

/**
 *
 */
public class AggregateEntriesDAOiBatisImpl
        extends BaseEntriesDAOiBatisImpl
        implements AggregateEntriesDAO {

    private static FeedQueryHeuristicsHelper heuristicsHelper = null;


    protected void initDao() throws Exception {
        super.initDao();
        heuristicsHelper = new FeedQueryHeuristicsHelper();
    }


    @ManagedAttribute(description = "Maximum index")
    public long getMaxIndex() {
        return heuristicsHelper.maxIndex;
    }

    @ManagedAttribute(description = "Minimum index")
    public long getMinIndex() {
        return heuristicsHelper.minIndex;
    }

    @ManagedAttribute(description = "Switch-over timestamp")
    public long getSwitchOverTimestamp() {
        return heuristicsHelper.switchOverTimestamp;
    }

    @ManagedAttribute(description = "Latency for updating Entry statistics (minutes).")
    public int getEntryStatisticsLatency() {
        return heuristicsHelper.getEntryStatisticsLatency();
    }

    @ManagedAttribute(description = "Latency for updating Entry statistics(minutes).")
    public void setEntryStatisticsLatency(int entryStatisticsLatency) {
        heuristicsHelper.setEntryStatisticsLatency(entryStatisticsLatency);
    }

    @ManagedAttribute(description = "Percentage of index span to switch index scan to seek in aggregate feed query. (0 is always seek)")
    public double getSwitchOverPercent() {
        return heuristicsHelper.getSwitchOverPercent();
    }

    @ManagedAttribute(description = "Percentage of index span to switch index scan to seek in aggregate feed query. (0 is always seek)")
    public void setSwitchOverPercent(double switchOverPercent) {
        heuristicsHelper.setSwitchOverPercent(switchOverPercent);
    }

    @ManagedOperation(description = "force update of entry statistics used for calculating switch over timestamp.")
    public synchronized void updateEntryStats() {
        heuristicsHelper.readStats();
    }


    public AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor, List<String> joinWorkspaces) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            AbstractDAOiBatisImpl.ParamMap paramMap = paramMap()
                    .param("collection", entryDescriptor.getCollection())
                    .param("entryId", entryDescriptor.getEntryId())
                    .param("pageSize", 1);
            if (entryDescriptor.getLocale() != null) {
                paramMap.addLocaleInfo(entryDescriptor.getLocale());
            }
            if (joinWorkspaces != null && !joinWorkspaces.isEmpty()) {
                paramMap.param("joinWorkspaces", joinWorkspaces);
            }

            paramMap.put("usequery", FeedQueryHeuristicsHelper.SEEK); // Always use seek
            Map<String, AggregateEntryMetaData> map =
                    AggregateEntryMetaData.aggregate(entryDescriptor.getWorkspace(),
                                                     entryDescriptor.getCollection(),
                                                     entryDescriptor.getLocale(),
                                                     getSqlMapClientTemplate().queryForList("selectAggregateEntries", paramMap));

            return map.get(entryDescriptor.getEntryId());
        } finally {
            stopWatch.stop("DB.selectAggregateEntry",
                           AtomServerPerfLogTagFormatter.getPerfLogEntryString(entryDescriptor));
        }
    }

    public List<AggregateEntryMetaData> selectAggregateEntriesByPage(FeedDescriptor feed,
                                                                     Date updatedMin,
                                                                     Date updatedMax,
                                                                     Locale locale,
                                                                     int startIndex,
                                                                     int endIndex,
                                                                     int pageSize,
                                                                     Collection<BooleanExpression<AtomCategory>> categoriesQuery,
                                                                     List<String> joinWorkspaces) {

        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            AbstractDAOiBatisImpl.ParamMap paramMap = prepareParamMapForSelectEntries(updatedMin, updatedMax,
                                                                                      startIndex, endIndex, pageSize,
                                                                                      locale == null ? null : locale.toString(), feed);

            if (joinWorkspaces != null && !joinWorkspaces.isEmpty()) {
                paramMap.param("joinWorkspaces", joinWorkspaces);
            }

            if (categoriesQuery != null) {
                paramMap.param("categoryFilterSql",
                               CategoryQueryGenerator.generateCategoryFilter(categoriesQuery));
                paramMap.param("categoryQuerySql",
                               CategoryQueryGenerator.generateCategorySearch(categoriesQuery));
            }

            if (getLatencySeconds() > 0) {
                paramMap.param("latencySeconds", getLatencySeconds());
            }

            heuristicsHelper.applyHeuristics(paramMap, FeedQueryHeuristicsHelper.SEEK);
            List entries = getSqlMapClientTemplate().queryForList("selectAggregateEntries", paramMap);

            Map<String, AggregateEntryMetaData> map =
                    AggregateEntryMetaData.aggregate(feed.getWorkspace(), feed.getCollection(), locale, entries);
            return new ArrayList(map.values());
        } finally {
            stopWatch.stop("DB.selectAggregateEntriesByPage",
                           AtomServerPerfLogTagFormatter.getPerfLogFeedString(locale == null ? null : locale.toString(), feed.getWorkspace(), feed.getCollection())
            );
        }
    }
    //=================================================
    //  Support to decide Aggregate Feed Seek or Scan.
    //=================================================

    /*
     * Class/Object used by iBatis to return Entry stats.
     */

    public static class EntryStats {
        long maxTimestamp;
        long minTimestamp;

        public long getMaxTimestamp() {
            return maxTimestamp;
        }

        public void setMaxTimestamp(Long maxTimestamp) {
            this.maxTimestamp = maxTimestamp;
        }

        public long getMinTimestamp() {
            return minTimestamp;
        }

        public void setMinTimestamp(Long minTimestamp) {
            this.minTimestamp = minTimestamp;
        }
    }

    /*
     * Helper class apply entry statistics to decide if index seek or index scan should be used.
     * Generally, seek is better than scan, but when the feed query start index is too
     * far away from the tip, SQL server needs to bring in a lot of matching rows and in
     * this case, scan helps.
     */

    class FeedQueryHeuristicsHelper {

        // configuration settings
        static final int DEAULT_STATS_LATENCY = 15; // minutes
        static final double DEFAULT_SWITCHOVERPERCENT = 50.0; // % of overall timestamp span to switch to index seek.
        // query mode
        static final String SEEK = "indexSeek";
        static final String SCAN = "indexScan";


        private int entryStatisticsLatency = DEAULT_STATS_LATENCY; // minutes
        private double switchOverPercent = DEFAULT_SWITCHOVERPERCENT; // percentage to switch over to indexSeek

        // statistics of interest
        long minIndex = 0;
        long maxIndex = 0;

        // local computed variables
        long switchOverTimestamp = 0;
        private long nextSyncTime = 0;


        FeedQueryHeuristicsHelper() {
            readStats();
        }

        double getSwitchOverPercent() {
            return switchOverPercent;
        }

        long getSwitchOverTimestamp() {
            return switchOverTimestamp;
        }

        int getEntryStatisticsLatency() {
            return entryStatisticsLatency;
        }

        void setEntryStatisticsLatency(int entryStatisticsLatency) {
            if (entryStatisticsLatency <= 0) {
                this.entryStatisticsLatency = DEAULT_STATS_LATENCY; // default
            }
            this.entryStatisticsLatency = entryStatisticsLatency;
            nextSyncTime = 0;
        }

        void setSwitchOverPercent(double switchOverPercent) {
            if (switchOverPercent < 0.0) {
                this.switchOverPercent = 0.0;
            } else if (switchOverPercent > 100.0) {
                this.switchOverPercent = 100.0;
            } else {
                this.switchOverPercent = switchOverPercent;
            }
            computeSwitchOverTimestamp();
        }

        synchronized void readStats() {
            long currentTime = System.currentTimeMillis();
            if (nextSyncTime < currentTime) {
                if (Double.compare(switchOverPercent, 0.0) == 0) {
                    switchOverTimestamp = 0;
                } else if (Double.compare(switchOverPercent, 100.0) == 0) {
                    switchOverTimestamp = Long.MAX_VALUE;
                } else {
                    EntryStats entryStats = getEntryStats();
                    if (entryStats != null) {
                        maxIndex = entryStats.getMaxTimestamp();
                        minIndex = entryStats.getMinTimestamp();
                    }
                    computeSwitchOverTimestamp();
                }
                nextSyncTime = currentTime + entryStatisticsLatency * 60 * 1000;
            }
        }

        void computeSwitchOverTimestamp() {
            long span = maxIndex - minIndex;
            switchOverTimestamp = minIndex + (long) (span * switchOverPercent / 100.0);
            if (log.isDebugEnabled()) {
                log.debug(" Update timestamps:");
                log.debug("  min timestamp= " + minIndex);
                log.debug("  max timestamp= " + maxIndex);
                log.debug("  switchover time stamp= " + switchOverTimestamp);
            }
        }

        // Apply heuristics for adjusting the aggregate feed query for MS SQL Server.
        // Currently, it looks at the timestamp only. It can be extended to look at
        // other parameter vaues such as updateDate as well.

        void applyHeuristics(HashMap<String, Object> paramMap, String defaultMode) {

            paramMap.put("usequery", defaultMode); // default to seek
            Long startIndex = (Long) paramMap.get("startIndex");

            // no need to handle it if there is no start index or not sql server
            if (startIndex != null && "sqlserver".equals(getDatabaseType())) {

                if (startIndex < minIndex) {
                    startIndex = minIndex;
                    paramMap.put("startIndex", startIndex);
                }

                String entryId = (String) paramMap.get("entryId");    // use seek when EntryId is not null
                if (entryId == null) {
                    readStats();
                    paramMap.put("usequery", (startIndex < switchOverTimestamp) ? SCAN : SEEK);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(" usequery = " + paramMap.get("usequery"));
            }
        }

        // database read

        EntryStats getEntryStats() {
            ParamMap paramMap = paramMap();
            List<EntryStats> list = (List<EntryStats>) getSqlMapClientTemplate().queryForList("selectEntryStats", paramMap);
            return (list.size() == 0) ? null : list.get(0);
        }
    }

}

