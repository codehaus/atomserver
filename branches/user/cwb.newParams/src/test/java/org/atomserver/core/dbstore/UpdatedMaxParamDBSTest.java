/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.model.AtomDate;
import org.apache.abdera.model.Feed;

import java.util.Date;

public class UpdatedMaxParamDBSTest extends ParamDBSTestCase {

    public static Test suite()
    { return new TestSuite( UpdatedMaxParamDBSTest.class ); }

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
        ClientResponse response= clientGet( "widgets/acme?updated-max=" + now, null, 400 );
        response.release();
    }

    public void testMaxBeforeMin() throws Exception {
        long lnow = (entriesDao.selectSysDate()).getTime();
        String earlier = AtomDate.format( new Date( lnow - 100000 ) );
        String now = AtomDate.format( new Date( lnow ) );
        ClientResponse response= clientGet( "widgets/acme?updated-min=" + now + "&updated-max=" + earlier, null, 400 );
        response.release();
    }

    public void testReadFeed() throws Exception {
        // We start out with the DB seeded (10 entries) + 1 added in ParamDBSTestCase

        // get max earlier than any we added
        long lnow = (entriesDao.selectSysDate()).getTime();
        String earlier = AtomDate.format( new Date( lnow - 100000 ) );
        ClientResponse response= clientGet( "widgets/acme?updated-max=" + earlier, null, 304 );
        response.release();

        // Sleep a couple of seconds, then add another entry
        Thread.sleep( 2000 );

        lnow = entriesDao.selectSysDate().getTime();
        String beforeLast = AtomDate.format( new Date( lnow - 50 ) );

        createWidget("widgets", "acme", "23456", "en", createWidgetXMLFileString( "23456"));

        // get all but the one we just added
        response= clientGet( "widgets/acme?updated-max=" + beforeLast, null, 200 );

        Feed feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", getNumSeeded() + 1, feed.getEntries().size());
        response.release();

        // get just the one we just added
        lnow = entriesDao.selectSysDate().getTime();
        String afterLast = AtomDate.format( new Date( lnow ) );

        response= clientGet( "widgets/acme?updated-min=" + beforeLast + "&updated-max=" + afterLast, null, 200 );

        feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 1, feed.getEntries().size());
        response.release();

        // get all of them
        String beforeAll = AtomDate.format( new Date( lnow - 100000 ) );

        response= clientGet( "widgets/acme?updated-min=" + beforeAll + "&updated-max=" + afterLast, null, 200 );

        feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", getNumSeeded() + 2, feed.getEntries().size());
        response.release();

        // add a couple more
        lnow = entriesDao.selectSysDate().getTime();
        String beforeAnother2 = AtomDate.format( new Date( lnow ) );

        createWidget("widgets", "acme", "34567", "en", createWidgetXMLFileString( "34567"));
        createWidget("widgets", "acme", "45678", "en", createWidgetXMLFileString( "45678"));

        // get just the first one we added again
        response= clientGet( "widgets/acme?updated-min=" + beforeLast + "&updated-max=" + afterLast, null, 200 );

        feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 1, feed.getEntries().size());
        response.release();

        // get everything but the last two added
        response= clientGet( "widgets/acme?updated-min=" + beforeAll + "&updated-max=" + afterLast, null, 200 );

        feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", getNumSeeded() + 2, feed.getEntries().size());
        response.release();

        // get only the last two added
        lnow = entriesDao.selectSysDate().getTime();
        String afterAnother2 = AtomDate.format( new Date( lnow ) );

        response= clientGet( "widgets/acme?updated-min=" + beforeAnother2 + "&updated-max=" + afterAnother2, null, 200 );

        feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 2, feed.getEntries().size());
        response.release();
     }


}