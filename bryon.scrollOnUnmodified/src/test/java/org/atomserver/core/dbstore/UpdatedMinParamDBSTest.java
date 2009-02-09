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
import org.apache.abdera.model.AtomDate;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Document;
import org.apache.abdera.protocol.client.ClientResponse;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * test the updated-min parameter.
 */
public class UpdatedMinParamDBSTest extends ParamDBSTestCase {

    public static Test suite()
    { return new TestSuite( UpdatedMinParamDBSTest.class ); }

    // -------------------------------------------------------
    public void setUp() throws Exception
    { super.setUp(); }

    // -------------------------------------------------------
    public void tearDown() throws Exception
    { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------
    public void testBadDate() throws Exception {
        String now = "BadDate";
        ClientResponse response= clientGet( "widgets/acme?updated-min=" + now, null, 400 );
        response.release();
    }

    public void testReadFeedSinceEmpty() throws Exception {
        Thread.sleep( 1000 );
        long lnow = (entriesDao.selectSysDate()).getTime();
        String now = AtomDate.format( new Date( lnow ) );
        ClientResponse response= clientGet( "widgets/acme?updated-min=" + now, null, 304 );
        response.release();
    }

    public void testWithTimeZonePlus() throws Exception {
        // we want to make sure that the "+/-" goes thru the URL 2003-12-13T18:30:02+01:00
        long lnow = (entriesDao.selectSysDate()).getTime();
        AtomDate adate = new AtomDate( new Date( lnow - 2000 ) );
        Date date = adate.getDate();
        log.debug( "+++++++++++++++ date= " + date );
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );

        TimeZone tz = sdf.getTimeZone();
        int offset = tz.getOffset( date.getTime() );

        // we don't run this test if timezone is > -01:00
        int hrs = offset/ (1000 * 60 * 60);
        log.debug( "offset= " + offset + " hrs= " + hrs );
        if ( hrs > -1 )
           return ;

        String ddd = sdf.format( date );

        String ddd2 = ddd + "-01:00"; 
        log.debug( "+++++++++++++++ ddd2= " + ddd2 );
        ClientResponse response= clientGet( "widgets/acme?updated-min=" + ddd2, null, 200 );
        response.release();

        ddd2 = ddd + "+06:00"; 
        log.debug( "+++++++++++++++ ddd2= " + ddd2 );
        response= clientGet( "widgets/acme?updated-min=" + ddd2, null, 200 );
        response.release();
    }

    public void testWithFutureTime() throws Exception {
        ClientResponse response= clientGet( "widgets/acme?updated-min=2027-12-02T23:59:59.000Z", null, 304 );
        response.release();
    }

    public void testReadFeedSince() throws Exception {
        // Now get the feed using the query param
        Thread.sleep( 2000 );

        Date nowdate = entriesDao.selectSysDate();
        long lnow = nowdate.getTime();
        String sdate = AtomDate.format( new Date( lnow - 50 ) );

        createWidget("widgets", "acme", "23456", "en", createWidgetXMLFileString( "23456"));

        ClientResponse response= clientGet( "widgets/acme?updated-min=" + sdate, null, 200 );

        Feed feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 1, feed.getEntries().size());
        response.release();

        // Make sure it works with a couple of params
        response= clientGet( "widgets/acme?max-results=5&updated-min=" + sdate, null, 200 );

        feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 1, feed.getEntries().size());
        response.release();
    }

    public void testGetEntrySincePasses() throws Exception {
        long lnow = (entriesDao.selectSysDate()).getTime();
        String sdate = AtomDate.format( new Date( lnow - 10000 ) );

        ClientResponse response= clientGet( "widgets/acme/" + stdPropId + ".en.xml?updated-min=" + sdate, null, 200 );

        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();
        assertTrue( entry.getContent().indexOf( "id=\"" + stdPropId + "\"" ) != -1 );
        response.release();
    }

    public void testGetEntrySinceFails() throws Exception {
        Thread.sleep( 1000 );
        long lnow = (entriesDao.selectSysDate()).getTime();
        String sdate = AtomDate.format( new Date( lnow ) );

        ClientResponse response= clientGet( "widgets/acme/" + stdPropId + ".en.xml?updated-min=" + sdate, null, 304 );
        response.release();
    }

}
