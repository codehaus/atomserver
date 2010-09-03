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


package org.atomserver.core.dbstore;

import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.dbstore.dao.impl.EntriesDAOiBatisImpl;
import org.atomserver.core.dbstore.dao.ContentDAO;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.ext.history.FeedPagingHelper;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;

import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.sql.Connection;

/**
 */
public class TagsFeedQueriesDBSTest extends DBSTestCase {
    protected static final String workspace = "widgets";

    public static Test suite()
    { return new TestSuite( TagsFeedQueriesDBSTest.class ); }

    public void setUp() throws Exception { 
        super.setUp();         
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();

        entriesDao.deleteAllEntries( new BaseServiceDescriptor( "widgets" ) );
    }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected boolean requiresDBSeeding() { return false; }    

    // --------------------
    //       tests
    //---------------------
    public void testFeedWithOneCategory() throws Exception {
        runFeedWithOneCategory();
    }

    public void testFeedWithMultipleCategories() throws Exception {
        runFeedWithMultipleCategories();
    }

    public void XXXtestSetOpsFeedWithOneCategory() throws Exception {
        try {
            ((EntriesDAOiBatisImpl)entriesDao).setUsingSetOpsFeedPage(true);
            runFeedWithOneCategory();
        } finally {
            ((EntriesDAOiBatisImpl)entriesDao).setUsingSetOpsFeedPage(false);
        }
    }

    public void XXXtestSetOpsFeedWithMultipleCategories() throws Exception {
        try {
            ((EntriesDAOiBatisImpl)entriesDao).setUsingSetOpsFeedPage(true);
            runFeedWithMultipleCategories();
         } finally {
            ((EntriesDAOiBatisImpl)entriesDao).setUsingSetOpsFeedPage(false);
        }
    }

    public void runFeedWithOneCategory() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "startCount = " + startCount );

        String sysId =  "acme" ;
        int propIdSeed = 34500 ;
        String locale = "fr";

        long lnow= (entriesDao.selectSysDate()).getTime();

        // INSERT
        int numRecs = 12 ;

        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            createWidget("widgets", "acme", propId, locale, createWidgetXMLFileString(propId));
        }
        // COUNT
        int count = entriesDao.getTotalCount(serviceDescriptor);
        assertEquals( (startCount + numRecs), count );

        int pageSize = count + 1;

        //====================
        // Create a standard APP Categories doc
        //  which is the Content for this "tags:widgets" Entry
        int startCountEC = entryCategoriesDAO.getTotalCount(workspace);
        log.debug( "startCountEC = " + startCountEC );
       
        String scheme = "urn:widgets.foo";
        for ( int ii=0; ii < numRecs; ii++ ) {
            Categories categories = getFactory().newCategories();

            Category category = getFactory().newCategory();
            category.setScheme( scheme );
            category.setTerm( "test" + ii % 3 );
            categories.addCategory( category );
            
            StringWriter stringWriter = new StringWriter();
            categories.writeTo( stringWriter ); 
            String categoriesXML = stringWriter.toString();
            log.debug( "Categories= " + categoriesXML );

            //INSERT
            String propId = "" + (propIdSeed + ii);
            modifyEntry("tags:widgets", "acme", propId, locale, categoriesXML, false, "*", false);
        }

        /* So we end up with this::
           34500,fr,t0        34504,fr,t1       34508,fr,t2
           34501,fr,t1        34505,fr,t2       34509,fr,t0
           34502,fr,t2        34506,fr,t0       34510,fr,t1 
           34503,fr,t0        34507,fr,t1       34511,fr,t2  
        */

        //===================================
        //  SELECT THE FEED USING A CATEGORY
        //====================================
        log.debug("\n\n\n ==========================");
        // SELECT
        String url = workspace + "/" + sysId + "/-/(" + scheme + ")test0";
        Feed feed = getPage( url, 200 );

        assertEquals( (numRecs/3), feed.getEntries().size() );

        // The Entries should be in ascending order
        List<Entry> entries = feed.getEntries();
        String[] expectedEntries = { "34500", "34503", "34506", "34509" };
        int knt = 0; 
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries[knt] ) != -1 );
            knt++;
        }

        // SELECT
        url = workspace + "/" + sysId + "/-/(" + scheme + ")test2?locale=fr";
        feed = getPage( url, 200 );

        assertEquals( (numRecs/3), feed.getEntries().size() );

        // The Entries should be in ascending order
        entries = feed.getEntries();
        String[] expectedEntries2 = { "34502", "34505", "34508", "34511" };
        knt = 0; 
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries2[knt] ) != -1 );
            knt++;
        }

        //====================
        // DELETE the Entries -- using the real RESTful DELETE 
        //    This does NOT delete the Categories -- because we don't actually delete the Entries
        //    Thus, the Categories should NOT get deleted!!!!
        //numRecs = 12 ;
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            Entry entry = getEntry( "widgets", "acme", propId, locale );
            
            IRI editLink = entry.getEditLinkResolvedHref();
            deleteEntry2( editLink.toString() );
            
            // Now the widgets Entry should be "marked" deleted
            entry = getEntry( "widgets", "acme", propId, locale );
            String content = entry.getContent();
            log.debug( "content= " + content );
            assertTrue( content.indexOf( "deletion" ) != -1 );
            
            // The tags:widgets Entry should NOT be deleted (it doesn't really exist -- it is just the widgets Entry)
            // so we ensure that it contains a category element.
            entry = getEntry( "tags:widgets", "acme", propId, locale );
            content = entry.getContent();
            log.debug( "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ content= " + content );
            assertTrue( content.indexOf( "category" ) >= 0 );
        }
        
        int finalCountEC = entryCategoriesDAO.getTotalCount(workspace);
        log.debug( "finalCountEC = " + finalCountEC );
        // check that there was one category entered for each record
        assertEquals( numRecs, finalCountEC );

        //====================
        // DELETE them all for real
        //numRecs = 12 ;
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            deleteEntry("widgets", "acme", propId, locale);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "finalCount = " + finalCount );
        assertEquals( startCount, finalCount );
    }

    public void runFeedWithMultipleCategories() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "startCount = " + startCount );

        String sysId =  "acme" ;
        int propIdSeed = 98700 ;
        String[] locale = {"fr", "pl" };

        long lnow= (entriesDao.selectSysDate()).getTime();

        // INSERT
        // this must be 12 
        int numRecs = 12 ;
        //int numRecs = 6 ;

        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            createWidget("widgets", "acme", propId, locale[ii % 2], createWidgetXMLFileString(propId));
        }
        // COUNT
        int count = entriesDao.getTotalCount(serviceDescriptor);
        assertEquals( (startCount + numRecs), count );

        int pageSize = count + 1;

        //====================
        // Create a standard APP Categories doc
        //  which is the Content for this "tags:widgets" Entry

        int startCountEC = entryCategoriesDAO.getTotalCount(workspace);
        log.debug( "startCountEC = " + startCountEC );
       
        String scheme = "urn:widgets.foo";
        for ( int ii=0; ii < numRecs; ii++ ) {
            Categories categories = getFactory().newCategories();

            Category category = getFactory().newCategory();
            category.setScheme( scheme );
            category.setTerm( "test" + ii % 3 );
            categories.addCategory( category );
            
            category = getFactory().newCategory();
            category.setScheme( scheme );
            category.setTerm( "boo" + ii % 2 );
            categories.addCategory( category );
            
            category = getFactory().newCategory();
            category.setScheme( scheme );
            category.setTerm( "ugh" + ii % 4 );
            categories.addCategory( category );
            
            category = getFactory().newCategory();
            category.setScheme( scheme );
            category.setTerm( "sam" + ii % 5 );
            categories.addCategory( category );
            
            StringWriter stringWriter = new StringWriter();
            categories.writeTo( stringWriter ); 
            String categoriesXML = stringWriter.toString();
            log.debug( "!!!!!!!!!!!!! Categories= " + categoriesXML );

            //INSERT
            String propId = "" + (propIdSeed + ii);
            modifyEntry("tags:widgets", "acme", propId, locale[ ii % 2 ], categoriesXML, false, "*", false);
        }

        /* So we end up with this::
           98700,fr,t0,b0,u0,s0         98704,fr,t1,b0,u0,s4        98708,fr,t2,b0,u0,s3 
           98701,pl,t1,b1,u1,s1         98705,pl,t2,b1,u1,s0        98709,pl,t0,b1,u1,s4 
           98702,fr,t2,b0,u2,s2         98706,fr,t0,b0,u2,s1        98710,fr,t1,b0,u2,s0  
           98703,pl,t0,b1,u3,s3         98707,pl,t1,b1,u3,s2        98711,pl,t2,b1,u3,s1   
        */

        //===================================
        //  SELECT THE FEED USING A CATEGORY
        //====================================
        log.debug("\n\n\n ==========================");
        // SELECT
        String url = workspace + "/" + sysId + "/-/(" + scheme + ")test0/(" + scheme + ")boo0";
        Feed feed = getPage( url, 200 );

        assertEquals( (numRecs/6), feed.getEntries().size() );

        // The Entries should be in ascending order
        List<Entry> entries = feed.getEntries();
        String[] expectedEntries = { "98700", "98706" };
        int knt = 0; 
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries[knt] ) != -1 );
            knt++;
        }

        log.debug("\n\n\n ==========================");
        // SELECT
        url = workspace + "/" + sysId + "/-/(" + scheme + ")sam1?locale=pl";
        feed = getPage( url, 200 );

        assertEquals( (numRecs/6), feed.getEntries().size() );

        // The Entries should be in ascending order
        entries = feed.getEntries();
        String[] expectedEntries0 = { "98701", "98711" };
        knt = 0; 
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries0[knt] ) != -1 );
            knt++;
        }

        // SELECT
        url = workspace + "/" + sysId + "/-/AND/(" + scheme + ")test0/OR/(" + scheme + ")ugh0/(" + scheme + ")ugh1";
        feed = getPage( url, 200 );

        assertEquals( (numRecs/6), feed.getEntries().size() );

        // The Entries should be in ascending order
        entries = feed.getEntries();
        String[] expectedEntries2 = { "98700", "98709" };
        knt = 0;
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries2[knt] ) != -1 );
            knt++;
        }

        // SELECT
        url = workspace + "/" + sysId + "/-/OR/OR/OR/(" + scheme + ")sam0/(" + scheme + ")sam1/(" + scheme + ")sam2/(" + scheme + ")sam3";
        feed = getPage( url, 200 );
        assertEquals( 10, feed.getEntries().size() );

        // The Entries should be in ascending order
        entries = feed.getEntries();
        String[] expectedEntries5 = { "98700", "98701", "98702", "98703", "98705", "98706", "98707", "98708", "98710", "98711" };
        knt = 0;
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries5[knt] ) != -1 );
            knt++;
        }

        // SELECT
        url = workspace + "/" + sysId + "/-/(" + scheme + ")test2/(" + scheme + ")boo1/(" + scheme + ")ugh1";
        feed = getPage( url, 200 );
        assertEquals( 1, feed.getEntries().size() );

        // The Entries should be in ascending order
        entries = feed.getEntries();
        String[] expectedEntries3 = { "98705" };
        knt = 0;
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries3[knt] ) != -1 );
            knt++;
        }

        // SELECT
        url = workspace + "/" + sysId + "/-/(" + scheme + ")test2/(" + scheme + ")boo1?locale=pl&max-results=1";
        feed = getPage( url, 200 );
        assertEquals( 1, feed.getEntries().size() );

        // Verify the next link 
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (pl) PAGE 1 = \n" + feed );
        IRI next = FeedPagingHelper.getNext( feed );
        assertNotNull( next );
        log.debug( "next= " + next.toString() );
        assertTrue( next.toString().indexOf( "locale=pl" ) != -1 );  
        assertTrue( next.toString().indexOf( "test2" ) != -1 );  
        assertTrue( next.toString().indexOf( "boo1" ) != -1 );  


        // The Entries should be in ascending order
        entries = feed.getEntries();
        String[] expectedEntries4 = { "98705", "98711" };
        knt = 0; 
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries4[knt] ) != -1 );
            knt++;
        }

        //====================
        //numRecs = 12 ;
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            Entry entry = getEntry( "widgets", "acme", propId, locale[ ii % 2 ] );
            
            IRI editLink = entry.getEditLinkResolvedHref();
            deleteEntry2( editLink.toString() );
            
            // Now the widgets Entry should be "marked" deleted
            entry = getEntry( "widgets", "acme", propId, locale[ ii % 2 ] );
            String content = entry.getContent();
            log.debug( "content= " + content );
            assertTrue( content.indexOf( "deletion" ) != -1 );

            // The tags:widgets Entry should NOT be deleted, so we ensure that it contains a
            // category element.
            entry = getEntry( "tags:widgets", "acme", propId, locale[ ii % 2 ] );
            content = entry.getContent();
            log.debug( "content= " + content );
            assertTrue( content.indexOf( "category" ) >= 0 );
        }

        int finalCountEC = entryCategoriesDAO.getTotalCount(workspace);
        log.debug( "finalCountEC = " + finalCountEC );
        // check that there are 4 categories for each record.
        assertEquals( numRecs * 4, finalCountEC );

        //====================
        // DELETE them all for real
        //numRecs = 12 ;
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            deleteEntry("widgets", "acme", propId, locale[ ii % 2 ]);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "finalCount = " + finalCount );
        assertEquals( startCount, finalCount );
    }

    public void testRaceConditionWritingNewTagsWorkspace() throws Exception {
        // we don't always hit the race condition, so run through this three times - if we pass all
        // three, we can be fairly sure we haven't regressed.
        for (int j = 0; j < 3 ; j++) {
            // first, we need to clear out the offending workspace, since the bug only occurs when we
            // try to insert a workspace for the first time
            ContentDAO contentDAO = (ContentDAO) appSpringFactory.getBean("org.atomserver-contentDAO");
            //contentDAO.deleteAllContent();
            contentDAO.deleteAllRowsFromContent();
            entryCategoriesDAO.deleteAllRowsFromEntryCategories();
            entriesDao.deleteAllRowsFromEntries();
            Connection conn = null;
            try {
                conn = entriesDao.getWriteEntriesDAO().getDataSource().getConnection();
                conn.createStatement().execute(
                "DELETE FROM AtomCollection WHERE workspace = 'tags:widgets'");
                conn.createStatement().execute(
                "DELETE FROM AtomWorkspace WHERE workspace = 'tags:widgets'");
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }

            int propIdSeed = 94949;
            int numEntries = 7;

            for (int i = 0; i < numEntries; i++) {
                String propId = "" + (propIdSeed + i);
                createWidget("widgets", "acme", propId, "en_US", createWidgetXMLFileString(propId));
            }

            ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
            for (int i = 0; i < numEntries; i++) {
                final String propId = "" + (propIdSeed + i);

                Categories categories = getFactory().newCategories();

                Category category = getFactory().newCategory();
                category.setScheme("urn:test-scheme");
                category.setTerm("test-" + i % 3);
                categories.addCategory(category);

                category = getFactory().newCategory();
                category.setScheme("urn:test-scheme2");
                category.setTerm("test-" + i % 5);
                categories.addCategory(category);

                category = getFactory().newCategory();
                category.setScheme("urn:test-scheme3");
                category.setTerm("test-" + i % 7);
                categories.addCategory(category);

                StringWriter stringWriter = new StringWriter();
                categories.writeTo(stringWriter);
                final String categoriesXML = stringWriter.toString();

                tasks.add(new Callable<Object>() {
                    public Object call() throws Exception {
                        try {
                            modifyEntry("tags:widgets", "acme", propId, "en_US", categoriesXML, false, "1", false);
                            return null;
                        } catch (Exception e) {
                            return e;
                        }
                    }
                });
            }

            ExecutorService threadPool = Executors.newFixedThreadPool(numEntries);
            List<Future<Object>> futures = threadPool.invokeAll(tasks);
            for (Future<Object> future : futures) {
                Exception exception = (Exception) future.get();
                assertNull(exception == null ? "" : exception.getMessage(), exception);
            }
        }
    }
    
}
