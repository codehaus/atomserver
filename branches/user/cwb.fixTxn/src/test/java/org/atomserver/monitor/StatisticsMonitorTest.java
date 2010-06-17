/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.monitor;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.core.AtomServerTestCase;
import org.atomserver.core.dbstore.dao.EntriesDAO;

import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Test for StatisticsMonitor methods.
 */
public class StatisticsMonitorTest extends AtomServerTestCase {

    private String workspaceCollection = "widgets/acme";

    protected String getStoreName() { return "org.atomserver-atomService"; }

    public static Test suite()
    { return new TestSuite( StatisticsMonitorTest.class ); }

     public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    { super.tearDown(); }

    // --------------------
    //       tests
    //---------------------
    public void testStatisticsMonitor() throws Exception {
        StatisticsMonitor statsMonitor = (StatisticsMonitor) getSpringFactory().getBean("org.atomserver-statsMonitor");

        EntriesDAO entriesDAO = (EntriesDAO) getSpringFactory().getBean("org.atomserver-entriesDAO");
        List<String> wkspaces = entriesDAO.listWorkspaces();
        Set<String> existingWorkspaceColletions = new HashSet<String>();
        for(String wksp: wkspaces) {
            List<String> collections = entriesDAO.listCollections(wksp);
            for(String col: collections) {
                existingWorkspaceColletions.add(wksp + "/" + col);
            }
        }

        if(existingWorkspaceColletions.size() != 0) {
            // Can test only when there are existing workspaces, collections and entries in EntryStore
            Map<String, Long> indexMap = statsMonitor.getLastIndexByWorkspaceCollection();
            assertNotNull(indexMap);
            assertTrue(indexMap.entrySet() != null);
            Set<String> diffSet = new HashSet<String>(indexMap.keySet());
            diffSet.removeAll(existingWorkspaceColletions);
            assertTrue(diffSet.isEmpty());

            Map<String, Integer> docCountMap = statsMonitor.getDocumentCountPerWorkspaceCollection();
            assertNotNull(docCountMap);
            assertTrue(docCountMap.entrySet() != null);
            diffSet = new HashSet<String>(docCountMap.keySet());

            diffSet.removeAll(existingWorkspaceColletions);
            assertTrue(diffSet.isEmpty());

        }
        assertTrue(statsMonitor.getLatency()>0);
        statsMonitor.setLatency(60000);
        assertEquals(statsMonitor.getLatency(),60000);
    }
}
