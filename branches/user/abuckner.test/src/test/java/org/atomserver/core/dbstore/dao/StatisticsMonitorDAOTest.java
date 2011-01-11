/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.monitor.WorkspaceCollectionMaxIndex;
import org.atomserver.monitor.WorkspaceCollectionDocumentCount;

import java.util.List;


/**
 * Test for StatisticsMonitorDAO calls.
 */
public class StatisticsMonitorDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(StatisticsMonitorDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------
    public void testStatistcsMonitorDAO() throws Exception {
        List<WorkspaceCollectionMaxIndex> indexList = statisticsMonitorDAO.getLastIndexPerWorkspaceCollection();
        assertNotNull(indexList);
        assertTrue(indexList.size() >= 0);    // depending on the db state it could return 0.

        List<WorkspaceCollectionDocumentCount> docCountList  = statisticsMonitorDAO.getDocumentCountPerWorkspaceCollection();
        assertNotNull( docCountList);
        assertTrue(docCountList.size() >= 0); // depending on the db state it could return 0.
    }
}
