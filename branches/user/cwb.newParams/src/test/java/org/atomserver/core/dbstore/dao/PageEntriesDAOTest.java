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

import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.BaseFeedDescriptor;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.testutils.latency.LatencyUtil;
import org.atomserver.ContentStorage;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;

import java.util.Date;
import java.util.List;
import java.util.Locale;


public class PageEntriesDAOTest extends DAOTestCase {

    static private boolean hasSlept = false;
    static private final int SLEEP_TIME = 2500;

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(PageEntriesDAOTest.class); }

    // -------------------------------------------------------        */

    protected void setUp() throws Exception {
        super.setUp();
        // sleep long enuf for the following testutils to work ;-)
        //  we cannot be sure which testutils runs first, so do this here
        if (!hasSlept) {
            hasSlept = true;
            log.debug("PageEntriesDAOTest:: SLEEPING FOR " + SLEEP_TIME + " ms");
            Thread.sleep(SLEEP_TIME);
        }
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------

    private int getPageDelim(List sortedList) {
        EntryMetaData entry = (EntryMetaData) (sortedList.get(sortedList.size() - 1));

        int pageDelim = (int) (entry.getLastModifiedSeqNum());

        log.debug("pageDelim= " + pageDelim);
        return pageDelim;
    }

    public void testPage() throws Exception {

        // COUNT
        int startCount = entriesDAO.getTotalCount(new BaseServiceDescriptor(workspace));
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        int propIdSeed = 11100;
        Locale locale = LocaleUtils.toLocale("en_GB");

        Date[] lastMod = new Date[3];
       long lnow = (entriesDAO.selectSysDate()).getTime();

        lastMod[0] = new Date(lnow);
        lastMod[1] = new Date(lnow - 1000L);
        lastMod[2] = new Date(lnow - 2000L);
        log.debug("lastMod:: " + lastMod[0] + " " + lastMod[1] + " " + lastMod[2]);

        // INSERT -- first we have to seed the DB 
        //           NOTE: no SeqNums yet...
        int numRecs = 20;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace("widgets");
            entryIn.setCollection(sysId);
            entryIn.setLocale(locale);
            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId(propId);

            entryIn.setLastModifiedDate(lastMod[(ii % 3)]);
            entryIn.setPublishedDate(lastMod[(ii % 3)]);

            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn, true, lastMod[(ii % 3)], lastMod[(ii % 3)]);
        }

        // COUNT
        int count = entriesDAO.getTotalCount(new BaseServiceDescriptor(workspace));
        assertEquals((startCount + numRecs), count);

        // UPDATE -- Now we put in SeqNum
        List updatedEntries = entriesDAO.updateLastModifiedSeqNumForAllEntries(new BaseServiceDescriptor(workspace));
        LatencyUtil.updateLastWrote();

        /* so the DB should look like this now
           BUT any number propId may sort differently where the lastModified is the same
           
           -200000           -100000          Now
           -------------------------------------------
           22202 (1)         22201 (7)        22200 (14)
           22205 (2)         22204 (8)        22203 (15)
           22208 (3)         22207 (9)        22206 (16)
           22211 (4)         22210 (10)       22209 (17)
           22214 (5)         22213 (11)       22212 (18)
           22217 (6)         22216 (12)       22215 (19)
                             22219 (13)       22218 (20)
        */

        // SELECT w/ PAGINATION
        int startSeqNum = (int) (((EntryMetaData) (updatedEntries.get(0))).getLastModifiedSeqNum());
        int endSeqNum = (int) (((EntryMetaData) (updatedEntries.get(updatedEntries.size() - 1))).getLastModifiedSeqNum());
        log.debug("&&&&&&&&&&&&&&&&&startSeqNum= " + startSeqNum);
        log.debug("&&&&&&&&&&&&&&&&&endSeqNum= " + endSeqNum);

        // let's just jump into the middle somewhere
        int pageDelim = startSeqNum + 5 + startCount;

        log.debug("pageDelim= " + pageDelim);

        LatencyUtil.accountForLatency();

        // get page
        List sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                                    null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);

        // this first set should all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
        }

        // get page
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        log.debug("######################## 0 :: " + (EntryMetaData) (sortedList.get(0)));
        log.debug("######################## 1 :: " + (EntryMetaData) (sortedList.get(1)));
        log.debug("######################## 2 :: " + (EntryMetaData) (sortedList.get(2)));

        // the second set should have 3 at lastMod[1] 
        assertTrue(datesAreEqual(lastMod[1], ((EntryMetaData) (sortedList.get(0))).getLastModifiedDate()));
        assertTrue(datesAreEqual(lastMod[1], ((EntryMetaData) (sortedList.get(1))).getLastModifiedDate()));
        assertTrue(datesAreEqual(lastMod[1], ((EntryMetaData) (sortedList.get(2))).getLastModifiedDate()));

        // get page
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        // this set should all be 2 at lastMod[0] and the 1 at lastMod[0]
        assertTrue(datesAreEqual(lastMod[1], ((EntryMetaData) (sortedList.get(0))).getLastModifiedDate()));
        assertTrue(datesAreEqual(lastMod[0], ((EntryMetaData) (sortedList.get(1))).getLastModifiedDate()));
        assertTrue(datesAreEqual(lastMod[0], ((EntryMetaData) (sortedList.get(2))).getLastModifiedDate()));

        // DELETE some 
        for (int ii = 0; ii < numRecs / 2; ii++) {
            String propId = "" + (propIdSeed + ii);
            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale, 1));

            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);
        }

        /* so the DB should look like this now
           BUT any number propId may sort differently where the lastModified is the same
           So we CANNOT actually predict the way the seqNums will get distributed within a given time slot
           
           -200000           -100000          Now
           -------------------------------------------
           22211 (4)         22210 (10)       
           22214 (5)         22213 (11)       22212 (18)
           22217 (6)         22216 (12)       22215 (19)
                             22219 (13)       22218 (20)
        */

        // let's just jump in at the last of the first set
        pageDelim = startSeqNum + 5 + startCount;
        log.debug("pageDelim= " + pageDelim);

        // get page
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        // this first set should all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
        }

        // get page
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        // the second set should have 1 at lastMod[1] and the last 2 at lastMod[0]
        assertTrue(datesAreEqual(lastMod[1], ((EntryMetaData) (sortedList.get(0))).getLastModifiedDate()));
        assertTrue(datesAreEqual(lastMod[0], ((EntryMetaData) (sortedList.get(1))).getLastModifiedDate()));
        assertTrue(datesAreEqual(lastMod[0], ((EntryMetaData) (sortedList.get(2))).getLastModifiedDate()));

        // DELETE the rest 
        for (int ii = (numRecs / 2 - 1); ii < numRecs; ii++) {
            String propId = "" + (propIdSeed + ii);
            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(new BaseServiceDescriptor(workspace));
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }


    public void testPage2() throws Exception {

        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        int propIdSeed = 22200;
        Locale locale = LocaleUtils.toLocale("en_GB");

        Date[] lastMod = new Date[3];
        long lnow = (entriesDAO.selectSysDate()).getTime();

        lastMod[0] = new Date(lnow);
        lastMod[1] = new Date(lnow - 1000L);
        lastMod[2] = new Date(lnow - 2000L);
        log.debug("lastMod:: " + lastMod[0] + " " + lastMod[1] + " " + lastMod[2]);

        // INSERT -- first we have to seed the DB 
        //           NOTE: no SeqNums yet...
        int numRecs = 20;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace("widgets");
            entryIn.setCollection(sysId);
            entryIn.setLocale(locale);
            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId(propId);

            entryIn.setLastModifiedDate(lastMod[(ii % 3)]);
            entryIn.setPublishedDate(lastMod[(ii % 3)]);

            //int okay = entriesDAO.insertEntry(entryIn, true );
            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn, true, lastMod[(ii % 3)], lastMod[(ii % 3)]);
        }

        // COUNT
        int count = entriesDAO.getTotalCount(serviceDescriptor);
        assertEquals((startCount + numRecs), count);

        // UPDATE -- Now we put in SeqNum
        List updatedEntries = entriesDAO.updateLastModifiedSeqNumForAllEntries(serviceDescriptor);

        LatencyUtil.updateLastWrote();

        /* so the DB should look like this now
           BUT any number propId may sort differently where the lastModified is the same
           
           -200000           -100000          Now
           -------------------------------------------
           22202 (1)         22201 (7)        22200 (14)
           22205 (2)         22204 (8)        22203 (15)
           22208 (3)         22207 (9)        22206 (16)
           22211 (4)         22210 (10)       22209 (17)
           22214 (5)         22213 (11)       22212 (18)
           22217 (6)         22216 (12)       22215 (19)
                             22219 (13)       22218 (20)
        */

        LatencyUtil.accountForLatency();

        // simulate a first page (delim=0) 
        List sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, 0, 3,
                                                    null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        // this first set should all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
        }

        // get second page
        int pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        // this second set should also all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
        }

        // DELETE some -- using the "real" deleteEntry, which is really an update in disguise
        for (int ii = 0; ii < numRecs / 2; ii++) {
            String propId = "" + (propIdSeed + ii);
            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale, 1));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            int okay = entriesDAO.deleteEntry(entryTarget);
            assertTrue(okay > 0);
        }

        LatencyUtil.updateLastWrote();
        LatencyUtil.accountForLatency();


        /* so the DB should look like this now
           BUT any number propId may sort differently where the lastModified is the same
           So we CANNOT actually predict the way the seqNums will get distributed within a given time slot

           -200000           -100000            Now                  Now + t1...t10   
           -----------------------------------------------------------------------------------
           22211 (4)         22210 (10)                       22200 (21)   22202 (25)   22201 (29) 
           22214 (5)         22213 (11)       22212 (18)      22201 (22)   22201 (26)   22201 (30)
           22217 (6)         22216 (12)       22215 (19)      22202 (23)   22201 (27)   
                             22219 (13)       22218 (20)      22202 (24)   22201 (28) 

        */

        // get page -- there couold either be 1 left at lastMod[1] and 2 at lastMod[0]
        // OR it is possible that we just deleted the remaining one at lastMod[1], 
        // so we have 3 at lastMod[0]  (it arbitrarilly depends on the sort)
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        int foundL1 = 0;
        int foundL0 = 0;
        for (int ii = 0; ii < sortedList.size(); ii++) {
            if (datesAreEqual(lastMod[1], ((EntryMetaData) (sortedList.get(ii))).getLastModifiedDate())) {
                foundL1++;
            } else if (datesAreEqual(lastMod[0], ((EntryMetaData) (sortedList.get(ii))).getLastModifiedDate())) {
                foundL0++;
            } else {
                fail("it had to be either L1 or L2");
            }
        }
        assertTrue((foundL1 == 1 && foundL0 == 2) || (foundL0 == 3));

        // get page -- there should just be one left at lastMod[0]
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], AtomServerConstants.FAR_FUTURE_DATE, pageDelim, 3,
                                               null, new BaseFeedDescriptor(workspace, null), null);
        log.debug("List= " + sortedList);

        // should have 1 at lastMod[0] and the last "deleted", or all deleted

        if (foundL1 == 1) {
            assertTrue(datesAreEqual(lastMod[0], ((EntryMetaData) (sortedList.get(0))).getLastModifiedDate()));
        } else {
            assertEquals(true, ((EntryMetaData) (sortedList.get(0))).getDeleted());
        }

        assertEquals(true, ((EntryMetaData) (sortedList.get(1))).getDeleted());
        assertEquals(true, ((EntryMetaData) (sortedList.get(2))).getDeleted());

        ContentStorage physicalStorage =
                (ContentStorage) springFactory.getBean("org.atomserver-contentStorage");

        // DELETE them all for real
        for (int ii = 0; ii < numRecs; ii++) {
            String propId = "" + (propIdSeed + ii);
            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

}

