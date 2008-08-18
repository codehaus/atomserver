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

import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.uri.EntryTarget;
import org.atomserver.testutils.client.MockRequestContext;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.lang.LocaleUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class BatchEntriesDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(BatchEntriesDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------
    public void testSelectBatch() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String workspace = "widgets";
        String sysId = "acme";
        int propIdSeed = 72910;
        Locale locale = LocaleUtils.toLocale("en_US");

        // INSERT 
        List<EntryDescriptor> entryQueriesAll = new ArrayList<EntryDescriptor>();
        List<EntryDescriptor> entryQueriesSome = new ArrayList<EntryDescriptor>();
        int numRecs = 12;
        int knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace(workspace);
            entryIn.setCollection(sysId);
            entryIn.setLocale(locale);

            knt++;
            String propId = "" + (propIdSeed + knt);
            entryIn.setEntryId(propId);

            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);

            entryQueriesAll.add(entryTarget);
            if ( (ii % 2) == 0 )
                entryQueriesSome.add(entryTarget);

            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn);
        }

        // Batch SELECTs
        List selectList = entriesDAO.selectEntryBatch( entryQueriesAll );
        log.debug("selectList = " + selectList);
        assertNotNull( selectList );
        assertEquals( numRecs, selectList.size() );

        selectList = entriesDAO.selectEntryBatch( entryQueriesSome );
        log.debug("selectList = " + selectList);
        assertNotNull( selectList );
        assertEquals( entryQueriesSome.size(), selectList.size() );

        // Let's add some unknown entries (as if they were INSERT candidates)        
        knt = 0;
        List<EntryDescriptor> entryQueriesWithMissing = new ArrayList<EntryDescriptor>();
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace(workspace);
            entryIn.setCollection(sysId);
            entryIn.setLocale(locale);

            knt++;
            int ipropId = propIdSeed + knt;
            if ( (ii % 2) == 0 )
                ipropId += 100;
            String propId = "" +  ipropId;

            entryIn.setEntryId(propId);

            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);

            entryQueriesWithMissing.add(entryTarget);
        }
        selectList = entriesDAO.selectEntryBatch( entryQueriesWithMissing );
        log.debug("selectList = " + selectList);
        assertNotNull( selectList );
        assertEquals( numRecs/2, selectList.size() );

        // DELETE them all for real
        knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            knt++;
            String propId = "" + (propIdSeed + knt);
            IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testCRUDBatch() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String workspace = "widgets";
        String sysId = "acme";
        int propIdSeed = 12540;
        Locale locale = LocaleUtils.toLocale("en_US");

        // INSERT 
        List<EntryDescriptor> entryURIDatas = new ArrayList<EntryDescriptor>();
        int numRecs = 12;
        int knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            knt++;
            String propId = "" + (propIdSeed + knt);

            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entryURIDatas.add(entryTarget);
        }

        // Batch INSERT
        int numOpOn = entriesDAO.insertEntryBatch( workspace, entryURIDatas );
        assertEquals( numRecs, numOpOn );

        // Batch SELECT
        List selectList = entriesDAO.selectEntryBatch( entryURIDatas );
        log.debug("selectList = " + selectList);
        assertNotNull( selectList );
        assertEquals( numRecs, selectList.size() );

        Date lastModified = null;
        Date published = null;
        long seqNum = 0L;
        entryURIDatas = new ArrayList<EntryDescriptor>();
        for( Object obj : selectList ) {
            EntryMetaData entryOut = (EntryMetaData)obj;
            assertEquals(entryOut.getWorkspace(), workspace);
            assertEquals(entryOut.getCollection(), sysId);
            assertEquals(entryOut.getLocale(), locale);
            int id = Integer.valueOf( entryOut.getEntryId() );
            assertTrue( id >= (propIdSeed + 1) && id <= (propIdSeed + numRecs) );
            assertEquals(entryOut.getRevision(), 0);
            assertEquals(entryOut.getDeleted(), false);

            if ( lastModified != null ) {
                assertTrue( lastModified.compareTo( entryOut.getLastModifiedDate() ) <= 0  );
            }
            lastModified = entryOut.getLastModifiedDate();           

            if ( published != null ) {
                assertTrue( published.compareTo( entryOut.getPublishedDate() ) <= 0  );
            }
            published = entryOut.getPublishedDate();

            log.debug( "seqNum= "+ seqNum +
                       " entryOut.getLastModifiedSeqNum()= " + entryOut.getLastModifiedSeqNum());

            assertTrue( seqNum < entryOut.getLastModifiedSeqNum() );
            seqNum = entryOut.getLastModifiedSeqNum();

            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, entryOut.getEntryId(), locale,
                                                                     (entryOut.getRevision() + 1) ));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entryURIDatas.add(entryTarget);
        }

        // Batch UPDATE
        numOpOn = entriesDAO.updateEntryBatch( workspace, entryURIDatas );
        assertEquals( numRecs, numOpOn );

        // Batch SELECT
        selectList = entriesDAO.selectEntryBatch( entryURIDatas );
        log.debug("selectList = " + selectList);
        assertNotNull( selectList );
        assertEquals( numRecs, selectList.size() );

        seqNum = 0L;
        lastModified = null;
        published = null;
        entryURIDatas = new ArrayList<EntryDescriptor>();
        for( Object obj : selectList ) {
            EntryMetaData entryOut = (EntryMetaData)obj;
            assertEquals(entryOut.getWorkspace(), workspace);
            assertEquals(entryOut.getCollection(), sysId);
            assertEquals(entryOut.getLocale(), locale);
            int id = Integer.valueOf( entryOut.getEntryId() );
            assertTrue( id >= (propIdSeed + 1) && id <= (propIdSeed + numRecs) );
            assertEquals(entryOut.getRevision(), 1);
            assertEquals(entryOut.getDeleted(), false);

            if ( lastModified != null ) {
                assertTrue( lastModified.compareTo( entryOut.getLastModifiedDate() ) <= 0  );
            }
            lastModified = entryOut.getLastModifiedDate();           

            if ( published != null ) {
                assertTrue( published.compareTo( entryOut.getPublishedDate() ) <= 0  );
            }
            published = entryOut.getPublishedDate();

            assertTrue( seqNum < entryOut.getLastModifiedSeqNum() );
            seqNum = entryOut.getLastModifiedSeqNum();

            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, entryOut.getEntryId(),
                                                                     locale, (entryOut.getRevision() + 1) ));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entryURIDatas.add(entryTarget);
         }

        // Batch DELETE
        numOpOn = entriesDAO.deleteEntryBatch( workspace, entryURIDatas );
        assertEquals( numRecs, numOpOn );

        // Batch SELECT
        selectList = entriesDAO.selectEntryBatch( entryURIDatas );
        log.debug("selectList = " + selectList);
        assertNotNull( selectList );
        assertEquals( numRecs, selectList.size() );

        seqNum = 0L;
        lastModified = null;
        published = null;
        for( Object obj : selectList ) {
            EntryMetaData entryOut = (EntryMetaData)obj;
            assertEquals(entryOut.getWorkspace(), workspace);
            assertEquals(entryOut.getCollection(), sysId);
            assertEquals(entryOut.getLocale(), locale);
            int id = Integer.valueOf( entryOut.getEntryId() );
            assertTrue( id >= (propIdSeed + 1) && id <= (propIdSeed + numRecs) );
            assertEquals(entryOut.getRevision(), 2);
            assertEquals(entryOut.getDeleted(), true);

            if ( lastModified != null ) {
                assertTrue( lastModified.compareTo( entryOut.getLastModifiedDate() ) <= 0  );
            }
            lastModified = entryOut.getLastModifiedDate();           

            if ( published != null ) {
                assertTrue( published.compareTo( entryOut.getPublishedDate() ) <= 0  );
            }
            published = entryOut.getPublishedDate();

            assertTrue( seqNum < entryOut.getLastModifiedSeqNum() );
            seqNum = entryOut.getLastModifiedSeqNum();
          }

        // DELETE them all for real
        knt = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            knt++;
            String propId = "" + (propIdSeed + knt);
            IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

}
