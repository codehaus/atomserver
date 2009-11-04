/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.monitor;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.core.AtomServerTestCase;

import java.util.Map;

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

        Map<String, Long> indexMap = statsMonitor.getLastIndexByWorkspaceCollection();
        assertNotNull(indexMap);
        assertTrue(indexMap.entrySet() != null);
        assertTrue(indexMap.containsKey(workspaceCollection));
        assertTrue((indexMap.get(workspaceCollection))> 0 );


        Map<String, Integer> docCountMap = statsMonitor.getDocumentCountPerWorkspaceCollection();
        assertNotNull(docCountMap);
        assertTrue(docCountMap.entrySet() != null);
        assertTrue(docCountMap.containsKey(workspaceCollection));
        assertTrue((docCountMap.get(workspaceCollection))> 0 );

        assertTrue(statsMonitor.getLatency()>0);
        statsMonitor.setLatency(60000);
        assertEquals(statsMonitor.getLatency(),60000);
    }
}
