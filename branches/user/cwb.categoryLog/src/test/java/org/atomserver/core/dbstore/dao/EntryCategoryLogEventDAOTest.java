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

package org.atomserver.core.dbstore.dao;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.core.EntryMetaData;

import java.util.List;


public class EntryCategoryLogEventDAOTest
        extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(EntryCategoryLogEventDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception {
        super.setUp();
        entryCategoryLogEventDAO.deleteAllRowsFromEntryCategoryLogEvent();
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------

    public void testCRUD() throws Exception {
        // COUNT
        int startCount = entryCategoryLogEventDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        //String workspace = "widgets";
        String sysId = "acme";
        String propId = "2182";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        // INSERT the Entry
        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null, 0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);

        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);

        // INSERT the EntryCategory
        EntryCategory entryIn = new EntryCategory();
        entryIn.setEntryStoreId(metaData.getEntryStoreId());
        entryIn.setScheme( scheme );
        entryIn.setTerm( term );

        int numRows = entryCategoriesDAO.insertEntryCategory(entryIn);
        assertTrue(numRows > 0);

        EntryCategory entryOut = entryCategoriesDAO.selectEntryCategory(entryIn);
        log.debug("====> entryOut = " + entryOut);
        assertNotNull(entryOut);

        // INSERT the EntryCategoryLogEvent
        numRows = entryCategoryLogEventDAO.insertEntryCategoryLogEvent(entryIn);
        assertTrue(numRows > 0);

        int count = entryCategoryLogEventDAO.getTotalCount(workspace);
        assertEquals((startCount + 1), count);

        // SELECT
        List<EntryCategoryLogEvent> logEvents = entryCategoryLogEventDAO.selectEntryCategoryLogEvent(entryIn);
        log.debug("====> %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == 1);

        EntryCategoryLogEvent logEvent = logEvents.get(0);
        assertEquals(sysId, logEvent.getCollection() );
        assertEquals(propId, logEvent.getEntryId());
        assertEquals(scheme, logEvent.getScheme());
        assertEquals(term, logEvent.getTerm());
        assertNotNull( logEvent.getCreateDate() );

        // DELETE
        entryCategoryLogEventDAO.deleteEntryCategoryLogEvent(entryIn);
        entryCategoriesDAO.deleteEntryCategory(entryIn);
        entriesDAO.obliterateEntry(descriptor);

        // SELECT again
        List<EntryCategoryLogEvent> logEvents2 = entryCategoryLogEventDAO.selectEntryCategoryLogEvent(entryIn);
        log.debug("====> ********************************** logEvents2 = " + logEvents2);
        assertTrue(logEvents2.size() == 0);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entryCategoryLogEventDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }
}
