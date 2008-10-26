/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.etc.AtomServerConstants;

import java.util.List;

public class EndIndexParamDBSTest extends ParamDBSTestCase {

    public static Test suite()
    { return new TestSuite( EndIndexParamDBSTest.class ); }

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
    public void testEndIndex() throws Exception {
        // We start out with the DB seeded (10 entries) + 1 added in ParamDBSTestCase

        int totalNum = getNumSeeded() + 1;
        ClientResponse response= clientGet( "widgets/acme?start-index=0", null, 200 );
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", totalNum, feed.getEntries().size());

        // no startIndex on the last page (only page in this case)
        int endIndex = Integer.parseInt(feed.getSimpleExtension(AtomServerConstants.END_INDEX));
        log.debug( " endIndex= " + endIndex );

        int ii = 0;
        int[] indexes = new int[totalNum];
        List<Entry> entries = feed.getEntries();
        for (Entry entry : entries) {
            indexes[ii] = Integer.parseInt( entry.getSimpleExtension(AtomServerConstants.UPDATE_INDEX) );
            log.debug( "index[" + ii + "] = " + indexes[ii] );
            ii++;
        }
        response.release();

        // NOTE: start-index is exclusive, end-index is inclusive
        response= clientGet( "widgets/acme?start-index=" + indexes[2] + "&end-index=" + indexes[7], null, 200 );
        feed = (Feed) response.getDocument().getRoot();

        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 5, feed.getEntries().size());
        response.release();

        // end-index past the actual last index
        int biggerIndex = indexes[totalNum - 1] + 100000;
        response= clientGet( "widgets/acme?start-index=" + indexes[2] + "&end-index=" + biggerIndex, null, 200 );
        feed = (Feed) response.getDocument().getRoot();

        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", totalNum - 3, feed.getEntries().size());
        response.release();

        // same one for start and end, MUST return 304 cuz can't be both exclusive and inclusive
        response= clientGet( "widgets/acme?start-index=" + indexes[2] + "&end-index=" + indexes[2], null, 304 );
        response.release();

        // end is before start
        response= clientGet( "widgets/acme?start-index=" + indexes[2] + "&end-index=" + indexes[0], null, 400 );
        response.release();
     }
}