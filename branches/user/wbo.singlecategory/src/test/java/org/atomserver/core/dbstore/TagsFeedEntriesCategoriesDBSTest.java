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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.atomserver.core.BaseServiceDescriptor;

import java.io.StringWriter;
import java.util.List;

/**
 */
public class TagsFeedEntriesCategoriesDBSTest extends DBSTestCase {
    protected static final String workspace = "widgets";

    public static Test suite()
    { return new TestSuite( TagsFeedEntriesCategoriesDBSTest.class ); }

    public void setUp() throws Exception { 
        super.setUp();         
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
    }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected boolean requiresDBSeeding() { return false; }

    // --------------------
    //       tests
    //---------------------
    public void NOtestNothing() {}

    public void testFeedWithOneCategory() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "startCount = " + startCount );

        String sysId =  "whizbang" ;
        int propIdSeed = 34500 ;

        String locale = "fr_CA";

        long lnow= (entriesDao.selectSysDate()).getTime();

        // INSERT
        int numRecs = 6 ;

        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            createWidget("widgets", "whizbang", propId, locale, createWidgetXMLFileString(propId));
        }

        // create one entry that conflicts with the first one, but is in a different locale
        // this case was causing writing a tag doc to fail, because the queries were ambiguous.
        createWidget("widgets", "whizbang", ("" + propIdSeed), "pl",
                     createWidgetXMLFileString("" + propIdSeed));

        // COUNT
        int count = entriesDao.getTotalCount(serviceDescriptor);
        assertEquals( (startCount + numRecs + 1), count );

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
            modifyEntry("tags:widgets", "whizbang", propId, locale, categoriesXML, false, "*", false);
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

        // The Entries should be in ascending order (they are ordered by seqNum)
        List<Entry> entries = feed.getEntries();
        String[] expectedEntries = { "34500", "34503", "34506", "34509" };
        int knt = 0; 
        for ( Entry entry : entries ) {
            assertTrue( entry.getId().toString().indexOf( expectedEntries[knt] ) != -1 );
            knt++;

            java.util.List<Category> catList = entry.getCategories("urn:widgets.foo");
            assertNotNull( catList );
            for (Category c : catList) {
                System.out.println("BRYON:: c.getScheme() = " + c.getScheme());
                System.out.println("BRYON:: c.getTerm() = " + c.getTerm());
            }
            assertEquals( 1, catList.size() );
            assertEquals( catList.get( 0 ).getTerm(), "test0" );                
        }

        log.debug("\n\n\n ==========================");
        // SELECT
        url = workspace + "/" + sysId ;
        feed = getPage( url, 200 );

        //assertEquals( 13, feed.getEntries().size() );
        assertEquals( (numRecs +1), feed.getEntries().size() );

        url = workspace + "/" + sysId + "?locale=" + locale ;
        feed = getPage( url, 200 );

        //assertEquals( 12, feed.getEntries().size() );
        assertEquals( numRecs, feed.getEntries().size() );

        // The Entries should be in ascending order (they are ordered by seqNum)
        entries = feed.getEntries();
        knt = 0; 
        for ( Entry entry : entries ) {
            String propId = "" + (propIdSeed + knt);
            assertTrue( entry.getId().toString().indexOf( propId ) != -1 );
 
            java.util.List<Category> catList = entry.getCategories("urn:widgets.foo");
            assertNotNull( catList );
            assertEquals( 1, catList.size() );
            assertEquals( catList.get( 0 ).getTerm(), "test" + knt % 3 );
            knt++;
         }

 
        //====================
        //numRecs = 12 ;
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId = "" + (propIdSeed + ii);
            Entry entry = getEntry( "widgets", "whizbang", propId, locale );

            IRI editLink = entry.getEditLinkResolvedHref();
            deleteEntry2( editLink.toString() );

            // Now the widgets Entry should be "marked" deleted
            entry = getEntry( "widgets", "whizbang", propId, locale );
            String content = entry.getContent();
            log.debug( "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ content= " + content );
            assertTrue( content.indexOf( "deletion" ) != -1 );

            // The tags:widgets Entry should NOT be deleted, so we ensure that it contains a
            // category element.
            entry = getEntry( "tags:widgets", "whizbang", propId, locale );
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
            deleteEntry("widgets", "whizbang", propId, locale);
        }
        deleteEntry("widgets", "whizbang", "" + propIdSeed, "pl");

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "finalCount = " + finalCount );
        assertEquals( startCount, finalCount );
    }

}
