/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.EntryState;
import org.apache.commons.lang.LocaleUtils;

import java.util.Locale;

public class EntryStateDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(EntryStateDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception {
        super.setUp();
        //entryStateDAO.deleteAllRowsFromEntryState();
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------

    public void testCRUD() {
        // COUNT
        //int startCount = entryCategoriesDAO.getTotalCount(workspace);
        //log.debug("startCount = " + startCount);

        String sysId = "acme";
        String propId = "78757";
        Locale locale = LocaleUtils.toLocale("en_GB");
        String message = "I am the walrus";
        String serverIp = "10.20.30.40";
        String serviceName = "theDude";
        String type = "CREATE";


        // INSERT
        EntryState entryIn = new EntryState();
        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, locale, 0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);

        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);
        entryIn.setEntryStoreId(metaData.getEntryStoreId());

        entryIn.setMessage(message);
        entryIn.setServerIp(serverIp);
        entryIn.setServiceName(serviceName);
        entryIn.setState(type);
         
        Object result = entryStateDAO.insertEntryState(entryIn);
        log.debug( result );

        /*
        int numRows = entryCategoriesDAO.insertEntryState(entryIn);
        assertTrue(numRows > 0);

        int count = entryCategoriesDAO.getTotalCount(workspace);
        assertEquals((startCount + 1), count);

        // SELECT
        EntryState entryOut = entryCategoriesDAO.selectEntryState(entryIn);
        log.debug("====> entryOut = " + entryOut);
        assertNotNull(entryOut);

        // DELETE
        entryCategoriesDAO.deleteEntryState(entryIn);
        entriesDAO.obliterateEntry(descriptor);

        // SELECT again
        EntryState entryOut2 = entryCategoriesDAO.selectEntryState(entryIn);
        log.debug("====> entryOut2 = " + entryOut2);
        assertNull(entryOut2);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entryCategoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
        */

    }
}
