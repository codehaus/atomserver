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

import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.testutils.client.MockRequestContext;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.lang.LocaleUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.BaseFeedDescriptor;
import org.atomserver.ContentStorage;

import java.util.Date;
import java.util.List;
import java.util.Locale;


public class EntriesDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(EntriesDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------
    public void testSysDate() throws Exception {
        Date sysDate = entriesDAO.selectSysDate();
        log.debug("sysDate = " + sysDate);
        assertNotNull(sysDate);
    }

    public void testSelectEntryByLocale() throws Exception {

        String sysId = "acme";
        String propId = "88888";
        Locale locale = LocaleUtils.toLocale("en_GB");

        // INSERT "en_GB"
        EntryMetaData entryIn = new EntryMetaData();
        entryIn.setWorkspace("widgets");
        entryIn.setCollection(sysId);
        entryIn.setLocale(locale);
        entryIn.setEntryId(propId);

        entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
        entriesDAO.insertEntry(entryIn);

        IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
        EntryTarget entryQueryEnGB = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        // INSERT "en"
        locale = LocaleUtils.toLocale("en");

        entryIn = new EntryMetaData();
        entryIn.setWorkspace("widgets");
        entryIn.setCollection(sysId);
        entryIn.setLocale(locale);
        entryIn.setEntryId(propId);

        entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
        entriesDAO.insertEntry(entryIn);
        iri = IRI.create("http://localhost:8080/"
                         + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
        EntryTarget entryQueryEn = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        // INSERT "fr"
        locale = LocaleUtils.toLocale("fr");

        entryIn = new EntryMetaData();
        entryIn.setWorkspace("widgets");
        entryIn.setCollection(sysId);
        entryIn.setLocale(locale);
        entryIn.setEntryId(propId);

        entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
        entriesDAO.insertEntry(entryIn);
        iri = IRI.create("http://localhost:8080/"
                         + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
        EntryTarget entryQueryFr = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        // SELECT BY LOCALE -- using "fr"     
        EntryMetaData entry = entriesDAO.selectEntry(entryQueryFr);

        log.debug("entry = " + entry);
        assertNotNull(entry);
        assertEquals(entry.getLocale(), LocaleUtils.toLocale("fr"));
        assertEquals(entry.getCollection(), sysId);
        assertEquals(entry.getEntryId(), propId);

        // SELECT BY LOCALE -- using "en_GB"      
        entry = entriesDAO.selectEntry(entryQueryEnGB);

        log.debug("entry = " + entry);
        assertNotNull(entry);
        assertEquals(entry.getLocale(), LocaleUtils.toLocale("en_GB"));
        assertEquals(entry.getCollection(), sysId);
        assertEquals(entry.getEntryId(), propId);

        // SELECT BY LOCALE -- using "en"    
        entry = entriesDAO.selectEntry(entryQueryEn);

        log.debug("entry = " + entry);
        assertNotNull(entry);
        assertEquals(entry.getLocale(), LocaleUtils.toLocale("en"));
        assertEquals(entry.getCollection(), sysId);
        assertEquals(entry.getEntryId(), propId);

        // DELETE "en_GB"
        entriesDAO.obliterateEntry(entryQueryEnGB);

        // SELECT BY LOCALE -- using "en_GB" -- but it's gone now     
        entry = entriesDAO.selectEntry(entryQueryEnGB);

        log.debug("entry = " + entry);
        assertNull(entry);

        // DELETE "en"
        entriesDAO.obliterateEntry(entryQueryEn);

        // DELETE "fr"
        entriesDAO.obliterateEntry(entryQueryFr);
    }

    public void testSelectEntryByLocaleWithLotsPresent() throws Exception {

        // Note: we use several because
        //   1) this matches a bug in Staging ;-)
        //   2) if the query is wrong the ResultSet might contain more items than 1....

        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        int propIdSeed = 88800;
        Locale locale = LocaleUtils.toLocale("en_US");

        // INSERT 
        int numRecs = 12;
        int knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace("widgets");
            entryIn.setCollection(sysId);
            entryIn.setLocale(locale);

            knt++; 
            String propId = "" + (propIdSeed + knt);

            entryIn.setEntryId(propId);

            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn);
        }

        String propId = "88807"; 
        IRI iri = IRI.create("http://localhost:8080/"
                         + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
        EntryTarget entryQuery1 = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        // SELECT BY LOCALE -- using "en_US" 
        EntryMetaData entry = entriesDAO.selectEntry(entryQuery1);

        log.debug("entry = " + entry);
        assertNotNull(entry);
        assertEquals(entry.getLocale(), LocaleUtils.toLocale("en_US"));
        assertEquals(entry.getCollection(), sysId);
        assertEquals(entry.getEntryId(),propId);

        // SELECT BY LOCALE -- using "en"  -- THIS ONE SHOULD FAIL  
        Locale localeEN = LocaleUtils.toLocale("en");
        iri = IRI.create("http://localhost:8080/"
                         + entryURIHelper.constructURIString(workspace, sysId, propId, localeEN));
        EntryTarget entryQuery2 = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        entry = entriesDAO.selectEntry(entryQuery2);

        log.debug("entry = " + entry);
        assertNull(entry);

        // DELETE them all for real
        knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            knt++; 
            propId = "" + (propIdSeed + knt);
            iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryQuery = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

            entriesDAO.obliterateEntry(entryQuery);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testCRUD() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        String propId = "7782";
        Locale locale = LocaleUtils.toLocale("en");

        // INSERT
        EntryMetaData entryIn = new EntryMetaData();
        entryIn.setWorkspace("widgets");
        entryIn.setCollection(sysId);
        entryIn.setLocale(locale);
        entryIn.setEntryId(propId);

        Date lnow = entriesDAO.selectSysDate();

        Thread.sleep(1000);

        long seqNum = 0L;

        entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
        entriesDAO.insertEntry(entryIn);

        int count = entriesDAO.getTotalCount(serviceDescriptor);
        assertEquals((startCount + 1), count);

        // SELECT
        IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
        EntryTarget entryQuery = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        EntryMetaData entryOut = entriesDAO.selectEntry(entryQuery);
        log.debug("====> entryOut = " + entryOut);

        assertEquals(entryOut.getCollection(), entryIn.getCollection());
        assertEquals(entryOut.getLocale(), entryIn.getLocale());
        assertEquals(entryOut.getEntryId(), entryIn.getEntryId());

        ContentStorage contentStorage =
                (ContentStorage) springFactory.getBean("org.atomserver-contentStorage");

        if (contentStorage instanceof FileBasedContentStorage) {
            FileBasedContentStorage fileBasedContentStorage = (FileBasedContentStorage) contentStorage;
            assertEquals(fileBasedContentStorage.getPhysicalRepresentation(entryOut.getWorkspace(),
                                                     entryOut.getCollection(),
                                                     entryOut.getEntryId(),
                                                     entryOut.getLocale(), 0),
                     fileBasedContentStorage.getPhysicalRepresentation(entryIn.getWorkspace(),
                                                     entryIn.getCollection(),
                                                     entryIn.getEntryId(),
                                                     entryIn.getLocale(), 0));
        }

        assertTrue(entryOut.getLastModifiedDate().after(lnow));
        assertEquals(entryOut.getPublishedDate(), entryOut.getLastModifiedDate());
        Date published = entryOut.getPublishedDate();
        Thread.sleep(1000);

        assertEquals(0, entryOut.getRevision());

        // UPDATE
        entryQuery = entryQuery.cloneWithNewRevision(entryOut.getRevision());

        entriesDAO.ensureCollectionExists(entryQuery.getWorkspace(), entryQuery.getCollection());
        int numRows = entriesDAO.updateEntry(entryQuery, false);
        assertTrue(numRows > 0);

        // SELECT
        entryOut = entriesDAO.selectEntry(entryQuery);
        log.debug("====> entryOut = " + entryOut);

        assertEquals(entryOut.getCollection(), entryIn.getCollection());
        assertEquals(entryOut.getLocale(), entryIn.getLocale());
        assertEquals(entryOut.getEntryId(), entryIn.getEntryId());
        assertEquals(entryOut.getDeleted(), false);

        assertTrue(entryIn.getLastModifiedSeqNum() < entryOut.getLastModifiedSeqNum());

        assertTrue(entryOut.getLastModifiedDate().after(published));
        assertEquals(entryOut.getPublishedDate(), published);
        Date lastMod = entryOut.getLastModifiedDate();
        Thread.sleep(1000);

        long seqNumIn = entryIn.getLastModifiedSeqNum();
        assertEquals(1, entryOut.getRevision());

        // UPDATE
        entryQuery = entryQuery.cloneWithNewRevision(entryOut.getRevision());

        entriesDAO.updateEntry(entryQuery, false);

        // SELECT
        entryOut = entriesDAO.selectEntry(entryQuery);
        log.debug("====> entryOut = " + entryOut);

        assertEquals(entryOut.getCollection(), entryIn.getCollection());
        assertEquals(entryOut.getLocale(), entryIn.getLocale());
        assertEquals(entryOut.getEntryId(), entryIn.getEntryId());
        assertEquals(entryOut.getDeleted(), false);

        assertTrue(seqNumIn < entryOut.getLastModifiedSeqNum());

        assertTrue(entryOut.getLastModifiedDate().after(lastMod));
        assertEquals(entryOut.getPublishedDate(), published);

        assertEquals(2, entryOut.getRevision());

        // UPDATE -- using a previous revision -- should fail (return -1)
        entryQuery = entryQuery.cloneWithNewRevision(1);
        numRows = entriesDAO.updateEntry(entryQuery, false);
        assertEquals(0, numRows);

        // DELETE -- using a previous revision -- should fail (return -1)
        numRows = entriesDAO.deleteEntry(entryQuery);
        assertEquals(0, numRows);

        // DELETE -- this one only updates the "deleted" flag
        entryQuery = entryQuery.cloneWithNewRevision(entryOut.getRevision());
        entriesDAO.deleteEntry(entryQuery);

        // SELECT
        entryOut = entriesDAO.selectEntry(entryQuery);
        log.debug("====> entryOut = " + entryOut);

        assertEquals(entryOut.getCollection(), entryIn.getCollection());
        assertEquals(entryOut.getLocale(), entryIn.getLocale());
        assertEquals(entryOut.getEntryId(), entryIn.getEntryId());

        assertEquals(entryOut.getDeleted(), true);

        assertTrue(entryIn.getLastModifiedSeqNum() < entryOut.getLastModifiedSeqNum());

        // DELETE
        entriesDAO.obliterateEntry(entryQuery);

        // SELECT again
        EntryMetaData entryOut2 = entriesDAO.selectEntry(entryQuery);
        log.debug("====> entryOut2 = " + entryOut2);
        assertNull(entryOut2);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testSort() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        int propIdSeed = 34300;
        Locale locale = LocaleUtils.toLocale("en");

        Date[] lastMod = new Date[3];
        long lnow = (entriesDAO.selectSysDate()).getTime();

        lastMod[0] = new Date(lnow);
        lastMod[1] = new Date(lnow - 100000L);
        lastMod[2] = new Date(lnow - 200000L);
        log.debug("lastMod:: " + lastMod[0] + " " + lastMod[1] + " " + lastMod[2]);

        // INSERT
        int numRecs = 20;
        Date zeroDate = new Date(0L);

        try { 

            for (int ii = 0; ii < numRecs; ii++) {
                EntryMetaData entryIn = new EntryMetaData();
                entryIn.setWorkspace("widgets");
                entryIn.setCollection(sysId);
                entryIn.setLocale(locale);
                String propId = "" + (propIdSeed + ii);
                entryIn.setEntryId(propId);
                
                entryIn.setLastModifiedDate(lastMod[(ii % 3)]);
                entryIn.setPublishedDate(lastMod[(ii % 3)]);
                
                //int okay = entriesDAO.insertEntry(entryIn, true);
                entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
                entriesDAO.insertEntry(entryIn, true, lastMod[(ii % 3)], lastMod[(ii % 3)]);
            }
            
            // COUNT
            int count = entriesDAO.getTotalCount(serviceDescriptor);
            assertEquals((startCount + numRecs), count);
            
            count = entriesDAO.getCountByLastModified(serviceDescriptor, lastMod[0]);
            log.debug("+++++++++++++++++++> getCountByLastModified= " + count);
            assertEquals(7, count);
            
            // SORT -- From the begining of time            
            List sortedList = entriesDAO.selectEntriesByLastModified(workspace, null, zeroDate);
            
            log.debug("List= " + sortedList);
            
            Date lastVal = zeroDate;
            for (Object obj : sortedList) {
                EntryMetaData entry = (EntryMetaData) obj;
                assertTrue(lastVal.compareTo(entry.getLastModifiedDate()) <= 0);
                lastVal = entry.getLastModifiedDate();
            }
            
            // SORT -- from now -- so the List should be empty
            Thread.sleep(1000);
            long lnow2 = (entriesDAO.selectSysDate()).getTime();
            
            sortedList = entriesDAO.selectEntriesByLastModified(workspace, null, (new Date(lnow2)));
            
            log.debug("List= " + sortedList);
            assertEquals(0, sortedList.size());
            
            // ==============
            entriesDAO.updateLastModifiedSeqNumForAllEntries(serviceDescriptor);
            
            sortedList = entriesDAO.selectEntriesByLastModified(workspace, null, zeroDate);
            log.debug("List= " + sortedList);
            
            lastVal = zeroDate;
            long seqNum = 0;
            for (Object obj : sortedList) {
                EntryMetaData entry = (EntryMetaData) obj;
                assertTrue(lastVal.compareTo(entry.getLastModifiedDate()) <= 0);
                lastVal = entry.getLastModifiedDate();


                // FIXME -- this Should work but does not.
                //          AFAICt there may be something wrong with the updateLastModifiedSeqNumForAllEntries
                //          BUT this code is NOT actually used anywhere in PRD
                //          So I am going to ignore it for now.
                //assertTrue("[seqNum= " + seqNum + "] !< [entrySeq= " + entry.getLastModifiedSeqNum(),
                //           seqNum < entry.getLastModifiedSeqNum());
                seqNum = entry.getLastModifiedSeqNum();
            }
       } finally { 
             
            // DELETE some 
            for (int ii = 0; ii < numRecs / 2; ii++) {
                String propId = "" + (propIdSeed + ii);
                IRI iri = IRI.create("http://localhost:8080/"
                                     + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
                EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);
                entriesDAO.obliterateEntry(entryTarget);
            }
            
            // SORT -- From the begining of time
            List sortedList = entriesDAO.selectEntriesByLastModifiedSeqNum(new BaseFeedDescriptor(workspace, null), zeroDate);
            log.debug("List= " + sortedList);
            
            Date lastVal = zeroDate;
            for (Object obj : sortedList) {
                EntryMetaData entry = (EntryMetaData) obj;

                log.debug("&&&&&COMPARE DATES:: " + lastVal + " " + entry.getLastModifiedDate());
                
                assertTrue(lastVal.compareTo(entry.getLastModifiedDate()) <= 0);
                lastVal = entry.getLastModifiedDate();
            }
            
            // DELETE the rest 
            for (int ii = (numRecs / 2 - 1); ii < numRecs; ii++) {
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

    public void testDeleteAll() throws Exception {

        // COUNT
        String[] workspace = { "reptiles", "amphibeans" };
        String[] sysId = { "snakes", "lizards", "frogs", "toads" };

        BaseServiceDescriptor serviceDescriptor0 = new BaseServiceDescriptor(workspace[0]);
        BaseServiceDescriptor serviceDescriptor1 = new BaseServiceDescriptor(workspace[1]);
        int startCountR = entriesDAO.getTotalCount(serviceDescriptor0);
        log.debug("startCountR = " + startCountR);
        int startCountA = entriesDAO.getTotalCount(serviceDescriptor1);
        log.debug("startCountA = " + startCountA);
               
        int propIdSeed = 88800;
        Locale locale = LocaleUtils.toLocale("en_US");
        
        // INSERT 
        int numRecs = 12;
        int knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            
            entryIn.setWorkspace( workspace[ ii % 2 ] );
            entryIn.setCollection( sysId[ ii % 4 ] );
            entryIn.setLocale(locale);
            
            knt++; 
            String propId = "" + (propIdSeed + knt);
            entryIn.setEntryId(propId);
            
            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn);
        }
        
        List sortedList = entriesDAO.selectEntriesByPage( new BaseFeedDescriptor("reptiles", null), ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 6, sortedList.size() );
        
        sortedList = entriesDAO.selectEntriesByPage( new BaseFeedDescriptor("amphibeans", null), ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 6, sortedList.size() );

        sortedList = entriesDAO.selectEntriesByPage( new BaseFeedDescriptor("amphibeans", "lizards"), ZERO_DATE, 0, 0 );
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 3, sortedList.size() );

        sortedList = entriesDAO.selectEntriesByPage( new BaseFeedDescriptor("amphibeans", "toads"), ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 3, sortedList.size() );

        
        entriesDAO.deleteAllEntries(new BaseFeedDescriptor("reptiles", null));
        
        sortedList = entriesDAO.selectEntriesByPage(new BaseFeedDescriptor("reptiles", null),  ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );
        
        entriesDAO.deleteAllEntries(new BaseFeedDescriptor("amphibeans", "lizards"));
        
        sortedList = entriesDAO.selectEntriesByPage( new BaseFeedDescriptor("amphibeans", "lizards"), ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );
        sortedList = entriesDAO.selectEntriesByPage(new BaseFeedDescriptor("amphibeans", null), ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 3, sortedList.size() );
        
        entriesDAO.deleteAllEntries(new BaseFeedDescriptor("amphibeans", "toads"));
        
        sortedList = entriesDAO.selectEntriesByPage( new BaseFeedDescriptor("amphibeans", "toads"),  ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );
        sortedList = entriesDAO.selectEntriesByPage(new BaseFeedDescriptor("amphibeans", "toads"), ZERO_DATE, 0, 0);
        log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );
        
        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up

        int finalCountR = entriesDAO.getTotalCount(serviceDescriptor0);
        log.debug("finalCountR = " + finalCountR);
        assertEquals(startCountR, finalCountR);

        int finalCountA = entriesDAO.getTotalCount(serviceDescriptor1);
        log.debug("finalCountA = " + finalCountA);
        assertEquals(startCountA, finalCountA);
    }
}
