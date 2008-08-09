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
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.logic.BooleanTerm;
import org.atomserver.uri.EntryTarget;
import org.atomserver.AtomCategory;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.*;
import org.atomserver.testutils.client.MockRequestContext;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;

import java.util.*;

public class CategoryPagingEntriesDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(CategoryPagingEntriesDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { 
        super.setUp();
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        entriesDAO.deleteAllRowsFromEntries();
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------
    public void testPageWithCategories() throws Exception {

        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        //int propIdSeed = 333333300;
        int propIdSeed = 23400;

        String[] localeStrings = { "zh", "fr", "ar", "it", "pt", "pl" };
        Locale[] locales = new Locale[localeStrings.length];
        for ( int ii=0; ii < locales.length; ii++ ) {
            locales[ii] = LocaleUtils.toLocale( localeStrings[ii] );
        }

        Date[] lastMod = new Date[3];
        long lnow = (entriesDAO.selectSysDate()).getTime();

        lastMod[0] = new Date(lnow);
        lastMod[1] = new Date(lnow - 2000L);
        lastMod[2] = new Date(lnow - 4000L);
        log.debug("lastMod:: " + lastMod[0] + " " + lastMod[1] + " " + lastMod[2]);

        // INSERT -- first we have to seed the DB
        //           NOTE: no SeqNums yet...
        int numRecs = 20;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace("widgets");
            entryIn.setCollection(sysId);

            entryIn.setLocale( locales[(ii % 6 )] );

            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId(propId);

            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn, true, lastMod[(ii % 3)], lastMod[(ii % 3)]);
        }

        // COUNT
        int count = entriesDAO.getTotalCount(serviceDescriptor);
        assertEquals((startCount + numRecs), count);

        // UPDATE -- Now we put in SeqNum
        List updatedEntries = entriesDAO.updateLastModifiedSeqNumForAllEntries(serviceDescriptor);

        //------------------------
        // Now create the Categories

        int startCountEC = entryCategoriesDAO.getTotalCount(workspace);
        log.debug("startCountEC = " + startCountEC);

        String scheme = "urn:ha/widgets";
        String termSeed = "foobar";

        // INSERT
        for ( int ii = 0; ii < numRecs; ii++ ) { 
            EntryCategory entryIn = new EntryCategory();
            entryIn.setWorkspace( workspace );
            entryIn.setCollection( sysId );
            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId( propId );
            entryIn.setLocale( locales[(ii % 6 )] );
            entryIn.setScheme( scheme );
            entryIn.setTerm( termSeed + ( ii % 4 ) );
            
            int inserts = entryCategoriesDAO.insertEntryCategory(entryIn);
            assertTrue(inserts > 0);
        }

        // INSERT
        String termSeed2 = "noogie";
        for ( int ii = 0; ii < numRecs; ii++ ) { 
            EntryCategory entryIn = new EntryCategory();
            entryIn.setWorkspace( workspace );
            entryIn.setCollection( sysId );
            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId( propId );
            entryIn.setLocale( locales[(ii % 6 )] );
            entryIn.setScheme( scheme );
            entryIn.setTerm( termSeed2 + ( ii % 2 ) );
            
            int inserts = entryCategoriesDAO.insertEntryCategory(entryIn);
            assertTrue(inserts > 0);
        }

        int countEC = entryCategoriesDAO.getTotalCount(workspace);
        assertEquals((startCountEC + numRecs*2), countEC);
 
        /* so the DB should look like this now
           BUT any number propId may sort differently where the lastModified is the same
           
              -4000 (2)              -2000 (1)             Now (0)
           -------------------------------------------------------------
           23402 (1) f2 n0 ar        23401 (7)  f1 n1 fr      23400 (14) f0 n0 zh
           23405 (2) f1 n1 pl        23404 (8)  f0 n0 pt      23403 (15) f3 n1 it
           23408 (3) f0 n0 ar        23407 (9)  f3 n1 fr      23406 (16) f2 n0 zh
           23411 (4) f3 n1 pl        23410 (10) f2 n0 pt      23409 (17) f1 n1 it
           23414 (5) f2 n0 ar        23413 (11) f1 n1 fr      23412 (18) f0 n0 zh
           23417 (6) f1 n1 pl        23416 (12) f0 n0 pt      23415 (19) f3 n1 it
                                     23419 (13) f3 n1 fr      23418 (20) f2 n0 zh
        */
       
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //                selectEntriesByPagePerCategory
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        log.debug("\n\n\n**************************************");
        // Now lets get everything (no paging) 
        checkSelectEntriesByPagePerCategory( propIdSeed, workspace, sysId, scheme, "foobar0", 0 );
        checkSelectEntriesByPagePerCategory( propIdSeed, workspace, sysId, scheme, "foobar1", 1 );
        checkSelectEntriesByPagePerCategory( propIdSeed, workspace, sysId, scheme, "foobar2", 2 );
        checkSelectEntriesByPagePerCategory( propIdSeed, workspace, sysId, scheme, "foobar3", 3 );

        //============================
        log.debug("\n\n\n**************************************");
        // simulate a first page (delim=0) 
 
        Collection<BooleanExpression<AtomCategory>> categoryList =
                new HashSet<BooleanExpression<AtomCategory>>();
        AtomCategory atomCategory = new AtomCategory( scheme, "foobar0" );
        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));

        List sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 2,
                                                    null,
                                                    new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

         // this first set should all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
            String pid = entry1.getEntryId();
            assertTrue( pid.equals("23404") || pid.equals("23416") );
        }

        // get second page
        int pageDelim = getPageDelim(sortedList);

        sortedList = entriesDAO.selectFeedPage(lastMod[1], pageDelim, 2,
                                               null,
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        // this second set should all be at lastMod[0]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[0], entry1.getLastModifiedDate()));
            String pid = entry1.getEntryId();
            assertTrue( pid.equals("23400") || pid.equals("23412") );
        }

        //============================
        log.debug("\n\n\n**************************************");
        // simulate a first page (delim=0) 

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar3" );

        categoryList.add(new BooleanTerm<AtomCategory>("term1", atomCategory));

        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 2,
                                               null,
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        // this first set should all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
            String pid = entry1.getEntryId();
            assertTrue( pid.equals("23407") || pid.equals("23419") );
        }

        // get second page
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], pageDelim, 2,
                                               null,
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        // this second set should all be at lastMod[0]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[0], entry1.getLastModifiedDate()));
            String pid = entry1.getEntryId();
            assertTrue( pid.equals("23403") || pid.equals("23415") );
        }

        //============================
        // Let's check with a couple of ANDs 
        // NOTE: this means you MUST match BOTH "foobar3" AND "foobar2" !!!

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar3" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        atomCategory = new AtomCategory( scheme, "foobar2" );

        categoryList.add(new BooleanTerm<AtomCategory>("term1", atomCategory));

        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 3,
                                               null,
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );

        //~~~~~~~~~~~~~~~~

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar0" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        atomCategory = new AtomCategory( scheme, "noogie0" );

        categoryList.add(new BooleanTerm<AtomCategory>("term1", atomCategory));

        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 2,
                                               null,
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        // this first set should all be at lastMod[1]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[1], entry1.getLastModifiedDate()));
            String pid = entry1.getEntryId();
            assertTrue( pid.equals("23404") || pid.equals("23416") );
        }

        // get second page
        pageDelim = getPageDelim(sortedList);
        sortedList = entriesDAO.selectFeedPage(lastMod[1], pageDelim, 2,
                                               null,
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        // this second set should all be at lastMod[0]
        for (Object obj : sortedList) {
            EntryMetaData entry1 = (EntryMetaData) obj;
            assertTrue(datesAreEqual(lastMod[0], entry1.getLastModifiedDate()));
            String pid = entry1.getEntryId();
            assertTrue( pid.equals("23400") || pid.equals("23412") );
        }

        /* so the DB should look like this now
           BUT any number propId may sort differently where the lastModified is the same
           
              -4000 (2)                  -2000 (1)             Now (0)
           -------------------------------------------------------------
           23402 (1) f2 n0 ar        23401 (7)  f1 n1 fr      23400 (14) f0 n0 zh
           23405 (2) f1 n1 pl        23404 (8)  f0 n0 pt      23403 (15) f3 n1 it
           23408 (3) f0 n0 ar        23407 (9)  f3 n1 fr      23406 (16) f2 n0 zh
           23411 (4) f3 n1 pl        23410 (10) f2 n0 pt      23409 (17) f1 n1 it
           23414 (5) f2 n0 ar        23413 (11) f1 n1 fr      23412 (18) f0 n0 zh
           23417 (6) f1 n1 pl        23416 (12) f0 n0 pt      23415 (19) f3 n1 it
                                     23419 (13) f3 n1 fr      23418 (20) f2 n0 zh
        */
           
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //                selectEntriesByPageAndLocalePerCategory
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        log.debug("\n\n\n**************************************");

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar0" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        sortedList = entriesDAO.selectFeedPage(ZERO_DATE, 0, 100,
                                               "zh",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        sortedList = entriesDAO.selectFeedPage(ZERO_DATE, 0, 100,
                                               "fr",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );
 

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar3" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        sortedList = entriesDAO.selectFeedPage(ZERO_DATE, 0, 100,
                                               "fr",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        //============================
        log.debug("\n\n\n**************************************");

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar0" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 2,
                                               "zh",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        log.debug("\n\n\n**************************************");

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar2" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 2,
                                               "zh",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );

        //============================
        // Let's check with a couple of ANDs 
        // NOTE: this means you MUST match BOTH "foobar3" AND "foobar2" !!!
        //       So none should match here....

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar3" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        atomCategory = new AtomCategory( scheme, "foobar2" );

        categoryList.add(new BooleanTerm<AtomCategory>("term1", atomCategory));
        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 3,
                                               "ar",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 0, sortedList.size() );

        categoryList.clear();
        atomCategory = new AtomCategory( scheme, "foobar0" );

        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));
        atomCategory = new AtomCategory( scheme, "noogie0" );

        categoryList.add(new BooleanTerm<AtomCategory>("term1", atomCategory));
        sortedList = entriesDAO.selectFeedPage(lastMod[1], 0, 3,
                                               "zh",
                                               new BaseFeedDescriptor(workspace, sysId), categoryList);
        log.debug("List= " + sortedList);
        assertEquals( 2, sortedList.size() );
 
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // DELETE them all for real
        for (int ii = 0; ii < numRecs; ii++) {
            String propId = "" + (propIdSeed + ii);
            Locale locale = locales[(ii % 6)];

            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
//            URIData entryQuery = entryURIHelper.decodeEntryURI(iri);
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

            entryCategoriesDAO.deleteEntryCategories(entriesDAO.selectEntry(entryTarget));
            entriesDAO.obliterateEntry(entryTarget);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);

        // DELETE
        for ( int ii=0; ii< numRecs; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setWorkspace( workspace );
            entryIn.setCollection( sysId );
            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId( propId );
            entryIn.setLocale( locales[(ii % 6 )] );
            entryIn.setScheme( scheme );
            entryIn.setTerm( termSeed + ( ii % 4 ) );
            entryCategoriesDAO.deleteEntryCategory(entryIn);
        }

        for ( int ii=0; ii< numRecs; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setWorkspace( workspace );
            entryIn.setCollection( sysId );
            String propId = "" + (propIdSeed + ii);
            entryIn.setEntryId( propId );
            entryIn.setLocale( locales[(ii % 6 )] );
            entryIn.setScheme( scheme );
            entryIn.setTerm( termSeed2 + ( ii % 2 ) );
            entryCategoriesDAO.deleteEntryCategory(entryIn);
        }
        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCountEC = entryCategoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCountEC);
        assertEquals(startCountEC, finalCountEC);
    }

    private void checkSelectEntriesByPagePerCategory( int propIdSeed, String workspace, String sysId, String scheme, String term, int offset ) {
        Collection<BooleanExpression<AtomCategory>> categoryList =
                new HashSet<BooleanExpression<AtomCategory>>();
        AtomCategory atomCategory = new AtomCategory( scheme, term );
        categoryList.add(new BooleanTerm<AtomCategory>("term0", atomCategory));

        List sortedList = entriesDAO.selectFeedPage(ZERO_DATE, 0, 100,
                                                    null,
                                                    new BaseFeedDescriptor(workspace, sysId), categoryList);

        log.debug("List= " + sortedList);
        assertEquals( 5, sortedList.size() );
        ArrayList<String> propIds = new ArrayList();
        for ( int ii=0; ii < 5; ii++ ) { propIds.add( "" + (propIdSeed + (ii*4) + offset) );}
        for (Object obj : sortedList) {
            EntryMetaData entry = (EntryMetaData) obj;
            String propIdChk = entry.getEntryId();
            assertTrue( propIds.contains( propIdChk ));
        }
    }

    private int getPageDelim(List sortedList) {
        EntryMetaData entry = (EntryMetaData) (sortedList.get(sortedList.size() - 1));

        int pageDelim = (int) (entry.getLastModifiedSeqNum());

        log.debug("pageDelim= " + pageDelim);
        return pageDelim;
    }

}

