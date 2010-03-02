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

package org.atomserver.monitor;

import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.atomserver.core.dbstore.dao.StatisticsMonitorDAO;

import java.util.Map;
import java.util.List;
import java.util.TreeMap;

/**
 * StatisticsMonitor - This class returns specific statistics of the Atomserver reported on
 * the JMX console.
 */

@ManagedResource(description = "Atomserver Statistics Monitor")
public class StatisticsMonitor {

    private StatisticsMonitorDAO statsMonitorDAO; // Injected by spring.

    private int latency = 30000;  // 30 secs delay between database reads
    private long lastMaxIndexTimestamp = 0;
    private long lastDocCountTimestamp = 0;


    private Map<String, Integer> documentCount;
    private Map<String, Long> maxIndex;

    public StatisticsMonitorDAO getStatsMonitorDAO() {
        return statsMonitorDAO;
    }

    public void setStatsMonitorDAO(final StatisticsMonitorDAO statsMonitorDAO) {
        this.statsMonitorDAO = statsMonitorDAO;
    }

    @ManagedAttribute(description = "Delay between database accesses (millisecond)")
    public int getLatency() {
        return this.latency;
    }

    @ManagedAttribute(description = "Delay between database accesses (millisecond)")
    public void setLatency(final int latency) {
        this.latency = latency;
    }

    @ManagedAttribute(description = "Last Index in each Workspace-Collection")
    public Map<String, Long> getLastIndexByWorkspaceCollection() {
        if (System.currentTimeMillis() > (lastMaxIndexTimestamp + latency) || maxIndex == null) {
            maxIndex = getMaxIndexMap();
            lastMaxIndexTimestamp = System.currentTimeMillis();
        }
        return maxIndex;
    }


    @ManagedAttribute(description = "Number of Documents in each Workspace-Collection")
    public Map<String, Integer> getDocumentCountPerWorkspaceCollection() {
        if (System.currentTimeMillis() > (lastDocCountTimestamp + latency) || documentCount == null) {
            documentCount = getDocumentCountMap();
            lastDocCountTimestamp = System.currentTimeMillis();
        }
        return documentCount;
    }

    /*
     * Query database for document count.
     */
    private Map<String, Integer> getDocumentCountMap() {
        List<WorkspaceCollectionDocumentCount> list = statsMonitorDAO.getDocumentCountPerWorkspaceCollection();
        Map<String, Integer> map = new TreeMap<String, Integer>();
        for (WorkspaceCollectionDocumentCount docCount : list) {
            map.put(docCount.getWorkspace() + "/" + docCount.getCollection(), docCount.getDocumentCount());
        }
        return map;
    }

    /*
     * Query database for maximum indexes.
     */
    private Map<String, Long> getMaxIndexMap() {
        List<WorkspaceCollectionMaxIndex> list = statsMonitorDAO.getLastIndexPerWorkspaceCollection();
        Map<String, Long> map = new TreeMap<String, Long>();
        for (WorkspaceCollectionMaxIndex maxIndex : list) {
            map.put(maxIndex.getWorkspace() + "/" + maxIndex.getCollection(), maxIndex.getMaxIndex());
        }
        return map;
    }
}
