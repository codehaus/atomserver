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
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.error.Error;

import java.util.Date;

/**
 */
public class EntryDBSTest extends DBSTestCase {
    protected static final String workspace = "widgets";

    public static Test suite()
    { return new TestSuite( EntryDBSTest.class ); }

     public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------
    public void testVarious() throws Exception {

        // test basics
        ClientResponse response = clientGet( "widgets/acme/4.en.xml" );
        log.debug( "location= " + response.getLocation() );

        Entry entry = verifyProperty4( response );
        String resolvedEditLink = entry.getEditLinkResolvedHref().toString();
        String resolvedSelfLink = entry.getSelfLinkResolvedHref().toString();
        response.release();

        // test the edit link
        log.debug( "edit link= " + resolvedEditLink );
        response = clientGetWithFullURL( resolvedEditLink );
        entry = verifyProperty4( response );
        response.release();

        // test the self link
        log.debug( "edit link= " + resolvedSelfLink );
        response = clientGetWithFullURL( resolvedSelfLink );
        entry = verifyProperty4( response );
        response.release();

        //--------------------------------
        // This will FAIL because we've stored "en" NOT "en_GB"
        // We should get a NOT FOUND
        response = clientGet( "widgets/acme/4.en_GB.xml", null, 404 );
        log.debug( "location= " + response.getLocation() );

        Document<Error> errdoc = response.getDocument();
        org.apache.abdera.protocol.error.Error error = errdoc.getRoot();
        assertNotNull( error );
        response.release();

        //---------------------------
        // testGetEntrySincePasses

        Thread.sleep( 1000 );

        String propId = "9993";

        // get the original
        response = clientGet( "widgets/acme/" + propId +".en.xml");
        Document<Entry> doc = response.getDocument();
        entry = doc.getRoot();
        assertTrue( entry.getContent().indexOf( "id=\"" + propId + "\"" ) != -1 );
        response.release();
        Date updated = entry.getUpdated();
        Date published = entry.getPublished();

        // we cannot say (updated == published), because these tests all reuse the same set of entries...
        assertTrue( entry.getUpdated().getTime() >= entry.getPublished().getTime() );

        String xmlContent = getContentString( "acme", "9993", "eeeek" );
        updateWidget("widgets","acme", "9993", "en", xmlContent, "0" );

        // lookup 100 secs ago
        long lnow = (entriesDao.selectSysDate()).getTime();

        response= clientGet( "widgets/acme/" + propId + ".en.xml", new Date( lnow - 100000 ), 200 );

        doc = response.getDocument();
        entry = doc.getRoot();
        assertTrue( entry.getContent().indexOf( "id=\"" + propId + "\"" ) != -1 );

        assertEquals( published, entry.getPublished() );
        assertTrue( entry.getUpdated().getTime() > entry.getPublished().getTime() );
        response.release();

        //-------------------------
        // testGetEntrySinceFails
        Thread.sleep( 1000 );

        lnow = (entriesDao.selectSysDate()).getTime();

        // should get 304 NOT MODIFIED status
        response= clientGet( "widgets/acme/9999.en.xml", new Date( lnow ), 304 );
        response.release();
    }

    private Entry verifyProperty4( ClientResponse response ) {
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        assertTrue( entry.getContent().indexOf( "id=\"4\"" ) != -1 );

        assertTrue( entry.getUpdated().getTime() > 0L );
        assertEquals( entry.getUpdated(), entry.getPublished() );

        assertTrue( entry.getId().toString().indexOf( "widgets/acme/4" ) != -1  );

        // alternate link should be null when there is content element
        assertNull( entry.getAlternateLink() );

        assertNotNull( entry.getEditLink() );
        assertNotNull( entry.getSelfLink() );
        return entry;
    }
}
