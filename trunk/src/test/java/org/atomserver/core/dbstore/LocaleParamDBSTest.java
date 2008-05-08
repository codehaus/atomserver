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
import org.apache.abdera.ext.history.FeedPagingHelper;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.AtomDate;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.ClientResponse;

import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.etc.AtomServerConstants;

import java.util.Date;

/**
 */
public class LocaleParamDBSTest extends ParamDBSTestCase {
    static private final int SLEEP_TIME = 1500;
    protected static final String workspace = "widgets";

    public static Test suite()
    { return new TestSuite( LocaleParamDBSTest.class ); }

     public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------

    public void testSeveralScenarios() throws Exception {

        //---------------------
        // test with bad locale
        String locale = "xxxxx_XX";
        ClientResponse response= clientGet( "widgets/acme/4?locale=" + locale, null, 400 );

        //-----------------------
        // Test with default locale
        response = clientGet( "widgets/acme/4?locale=en" );
        log.debug( "location= " + response.getLocation() );

        Entry entry = verifyEntry( response, "widgets", "acme", "4");
        String resolvedEditLink = entry.getEditLinkResolvedHref().toString();
        String resolvedSelfLink = entry.getSelfLinkResolvedHref().toString();
        response.release();

        // test the edit link
        log.debug( "edit link= " + resolvedEditLink );
        response = clientGetWithFullURL( resolvedEditLink );
        entry = verifyEntry( response, "widgets", "acme", "4");
        response.release();

        // test the self link
        log.debug( "edit link= " + resolvedSelfLink );
        response = clientGetWithFullURL( resolvedSelfLink );
        entry = verifyEntry( response, "widgets", "acme", "4");
        response.release();

        //------------------------
        // grab the entire feed in one page
        response = clientGet( "widgets/acme?locale=en" );
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", (startCount + 1), feed.getEntries().size());

        assertNull( feed.getSimpleExtension( OpenSearchConstants.TOTAL_RESULTS ) );
        assertNull( feed.getSimpleExtension( OpenSearchConstants.START_INDEX ) );
        assertNull( feed.getSimpleExtension( OpenSearchConstants.ITEMS_PER_PAGE ) );

        assertNotNull( feed.getSimpleExtension( AtomServerConstants.END_INDEX ) );

        IRI next = FeedPagingHelper.getNext( feed );
        assertNull( next );

        IRI self = feed.getSelfLinkResolvedHref();
        assertNotNull( self );
        response.release();

        //--------------------------
        // Now get the feed using the query param
        Thread.sleep( 2000 );

        Date now = entriesDao.selectSysDate();
        long lnow = now.getTime();
        String date = AtomDate.format( new Date( lnow - 50 ) );

        createWidget("widgets", "acme", "23456", "en", createWidgetXMLFileString( "23456"));

        feed = getPageByLastMod( date, "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List = \n" + feed );
        assertEquals( 1,  feed.getEntries().size());

        //----------------------------
        // Test locale paging

        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "startCount = " + startCount );

        String sysId =  "acme" ;
        int propIdSeed = 77700 ;
        String dirId = "77";

        String[] locales = { "de", "de_DE", "de_CH" };
        lnow= (entriesDao.selectSysDate()).getTime();

        // INSERT
        //int numRecs = 12 ;
        int numRecs = 6 ;

        int knt = 0;
        int jj = 0;
        for ( int ii=0; ii < numRecs; ii++ ) {
            locale = locales[(ii % locales.length)];
            String propId =  "" + (propIdSeed + knt);

            log.debug( "\n\n CREATING ["  + dirId + " " + propId + " " + locale + "]" );
            createWidget("widgets", "acme", propId, locale, createWidgetXMLFileString(propId));

            jj++;
            if ( jj == 2 ) {
                jj = 0;
                knt++;
            }
        }

        /* So we end up with this::
           77700,de        77702,de_DE      77704,de_CH
           77700,de_DE     77702,de_CH      77704,de
           77701,de_CH     77703,de         77705,de_DE
           77701,de        77703,de_DE      77705,de_CH
        */

        // COUNT
        int count = entriesDao.getTotalCount(serviceDescriptor);
        assertEquals( (startCount + numRecs), count );

        int pageSize = count + 1;

        // get page (for "de")
        // should get:: 77700,de  77701,de  77703,de  77704,de
        feed = getPageByLocale( "de", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (de) = \n" + feed );
        //assertEquals( 4,  feed.getEntries().size());
        assertEquals( (numRecs/3),  feed.getEntries().size());

        // get page (for "de_DE")
        // should get:: 77700,de_DE  77702,de_DE  77703,de_DE  77704,de_DE
        feed = getPageByLocale( "de_DE", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (de_DE) = \n" + feed );
        //assertEquals( 4,  feed.getEntries().size());
        assertEquals( (numRecs/3),  feed.getEntries().size());

        // get page (for "de_CH")
        // should get:: 77701,de_CH  77702,de_CH  77704,de_CH  77704,de_CH
        feed = getPageByLocale( "de_CH", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (de_CH) = \n" + feed );
        //assertEquals( 4,  feed.getEntries().size());
        assertEquals( (numRecs/3),  feed.getEntries().size());

        // get page (for "de_AT")
       // should get:: 304 T MODIFIED
        feed = getPageByLocale( "de_AT", 304, "widgets", "acme" );
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (de_AT) = \n" + feed );

        // Verify the next link 
        feed = getPageByLocaleAndMaxResults( "de", "widgets", "acme", 2 );
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (de) PAGE 1 = \n" + feed );
        assertEquals( 2,  feed.getEntries().size());

        next = FeedPagingHelper.getNext( feed );
        if ( numRecs == 12 ) {
            assertNotNull( next );
            log.debug( "next= " + next.toString() );
            assertTrue( next.toString().indexOf( "locale=de" ) != -1 );


            feed = getPage( next.toString(), 200, true ) ;
            log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
            log.debug( "List (de) PAGE 2  = \n" + feed );
            assertEquals( 2, feed.getEntries().size());
        }

        next = FeedPagingHelper.getNext( feed );
        assertNull( next );

         // DELETE them all for real
        knt = 0;
        jj = 0;
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId =  "" + (propIdSeed + knt);
            locale = locales[(ii % locales.length)];

            deleteEntry("widgets", "acme", propId, locale);

            jj++;
            if ( jj == 2 ) {
                jj = 0;
                knt++;
            }
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = entriesDao.getTotalCount(serviceDescriptor);
        log.debug( "finalCount = " + finalCount );
        assertEquals( startCount, finalCount );

        //--------------------------
        // test Paging With Locales And LastModified()

        // COUNT
        BaseServiceDescriptor servicefrscriptor = new BaseServiceDescriptor(workspace);
        startCount = entriesDao.getTotalCount(servicefrscriptor);
        log.debug( "startCount = " + startCount );

        propIdSeed = 55500 ;
        long startTime = (entriesDao.selectSysDate()).getTime();

        // let's create a few entries
        createWidget("widgets", "acme", "" + (propIdSeed+1), "fr", createWidgetXMLFileString("" + (propIdSeed+1)));
        createWidget("widgets", "acme", "" + (propIdSeed+2), "fr", createWidgetXMLFileString("" + (propIdSeed+2)));
        createWidget("widgets", "acme", "" + (propIdSeed+3), "fr_FR", createWidgetXMLFileString("" + (propIdSeed+3)));
        createWidget("widgets", "acme", "" + (propIdSeed+2), "fr_CA", createWidgetXMLFileString("" + (propIdSeed+2)));

        // let's wait 5 secs and create some more
        Thread.sleep( SLEEP_TIME );
        long batch2Time = (entriesDao.selectSysDate()).getTime();

        createWidget("widgets", "acme", "" + (propIdSeed+4), "fr", createWidgetXMLFileString("" + (propIdSeed+4)));
        createWidget("widgets", "acme", "" + (propIdSeed+5), "fr", createWidgetXMLFileString("" + (propIdSeed+5)));
        createWidget("widgets", "acme", "" + (propIdSeed+6), "fr_FR", createWidgetXMLFileString("" + (propIdSeed+6)));
        createWidget("widgets", "acme", "" + (propIdSeed+5), "fr_CA", createWidgetXMLFileString("" + (propIdSeed+5)));

        // let's wait 5 secs more and create yet some more
        Thread.sleep( SLEEP_TIME );
        long batch3Time = (entriesDao.selectSysDate()).getTime();

        createWidget("widgets", "acme", "" + (propIdSeed+7), "fr", createWidgetXMLFileString("" + (propIdSeed+7)));
        createWidget("widgets", "acme", "" + (propIdSeed+8), "fr", createWidgetXMLFileString("" + (propIdSeed+8)));
        createWidget("widgets", "acme", "" + (propIdSeed+9), "fr_FR", createWidgetXMLFileString("" + (propIdSeed+9)));
        createWidget("widgets", "acme", "" + (propIdSeed+8), "fr_CA", createWidgetXMLFileString("" + (propIdSeed+8)));

        Thread.sleep( SLEEP_TIME );

        // batch 3
        date = AtomDate.format( new Date( batch3Time ) );
        feed = getPageByLastModAndLocale( date, "fr_FR", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 1,  feed.getEntries().size());

        feed = getPageByLastModAndLocale( date, "fr_CA", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 1,  feed.getEntries().size());

        feed = getPageByLastModAndLocale( date, "fr", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 2,  feed.getEntries().size());

        // batch 2
        date = AtomDate.format( new Date( batch2Time ) );
        feed = getPageByLastModAndLocale( date, "fr_FR", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 2,  feed.getEntries().size());

        feed = getPageByLastModAndLocale( date, "fr_CA", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 2,  feed.getEntries().size());

        feed = getPageByLastModAndLocale( date, "fr", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 4,  feed.getEntries().size());

        // batch 1
        date = AtomDate.format( new Date( startTime ) );
        feed = getPageByLastModAndLocale( date, "fr_FR", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 3,  feed.getEntries().size());

        feed = getPageByLastModAndLocale( date, "fr_CA", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 3,  feed.getEntries().size());

        feed = getPageByLastModAndLocale( date, "fr", "widgets", "acme");
        log.debug ("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" );
        log.debug( "List (fr_FR) batch1 = \n" + feed );
        assertEquals( 6,  feed.getEntries().size());

        // let's frlete what we created
        deleteEntry("widgets", "acme", "" + (propIdSeed+1), "fr");
        deleteEntry("widgets", "acme", "" + (propIdSeed+2), "fr");
        deleteEntry("widgets", "acme", "" + (propIdSeed+2), "fr_CA");
        deleteEntry("widgets", "acme", "" + (propIdSeed+3), "fr_FR");
        deleteEntry("widgets", "acme", "" + (propIdSeed+4), "fr");
        deleteEntry("widgets", "acme", "" + (propIdSeed+5), "fr");
        deleteEntry("widgets", "acme", "" + (propIdSeed+5), "fr_CA");
        deleteEntry("widgets", "acme", "" + (propIdSeed+6), "fr_FR");
        deleteEntry("widgets", "acme", "" + (propIdSeed+7), "fr");
        deleteEntry("widgets", "acme", "" + (propIdSeed+8), "fr");
        deleteEntry("widgets", "acme", "" + (propIdSeed+8), "fr_CA");
        deleteEntry("widgets", "acme", "" + (propIdSeed+9), "fr_FR");

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        finalCount = entriesDao.getTotalCount(servicefrscriptor);
        log.debug( "finalCount = " + finalCount );
        assertEquals( startCount, finalCount );

    }


}
