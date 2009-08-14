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


package org.atomserver.core.filestore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.AtomServerTestCase;
import org.atomserver.testutils.conf.TestConfUtil;

import java.io.File;
import java.util.Date;

/**
 */
public class FilestoreTest extends AtomServerTestCase {
 
   protected static final String userdir = System.getProperty( "user.dir" );

    public static Test suite()
    { return new TestSuite( FilestoreTest.class ); }

    public void setUp() throws Exception {
        TestConfUtil.preSetup("filestore-conf");
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }


    protected String getStoreName() {
        return "filestore";
    }

    public void testNothing() {}

    public void XXXtestGetServiceDocument() throws Exception {
        // when we use the service URL with the widgets workspace, we expect only that workspace
        // to come back in the service document
        runServiceDocTest("widgets/", 1);
        // when we hit the root url, we expect to get both workspaces
        runServiceDocTest("", 2);
    }

    private void runServiceDocTest(String url, int expectedWorkspaceCount) {
        ClientResponse response = clientGet(url);

        Document<Service> service_doc = response.getDocument();
        assertNotNull(service_doc);

        assertEquals(expectedWorkspaceCount, service_doc.getRoot().getWorkspaces().size());

        Workspace workspace = service_doc.getRoot().getWorkspace( "widgets" );

        assertNotNull(workspace);
        assertFalse(workspace.getCollections().isEmpty());
        for (Collection c: workspace.getCollections()) {
            assertNotNull(c.getTitle());
            assertNotNull(c.getHref());
            assertTrue(c.getHref().toString().startsWith(workspace.getTitle() + '/'));
        }
        response.release();
    }

    public void XXXtestReadFeed() throws Exception {
        ClientResponse response = clientGet( "widgets/acme/" );

        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug( "SIZE=" + feed.getEntries().size() );
        //assertEquals("Testing feed length", 10, feed.getEntries().size());

        //"1970-01-01T00:00:00.000Z" is the "O" date
        String updated = feed.getUpdatedString();
        log.debug( ">>>>>>>>>>>>>> updated = " + updated );
        assertFalse( updated.equals( "1970-01-01T00:00:00.000Z" ) );
        response.release();
    }

    public void XXXtestReadFeedSinceEmpty() throws Exception {
        ClientResponse response= clientGet( "widgets/acme", new Date(), 304 );
        response.release();
    }

    public void XXXtestReadFeedSince() throws Exception {
        Date now = new Date();
        long lnow = now.getTime();
        File touchFile = new File( TEST_DATA_DIR + "/widgets/acme/27/2788/en/2788.xml.r0" );
        long origLastModified = touchFile.lastModified();
        touchFile.setLastModified( lnow );

        log.debug( "origLastModified = " +  origLastModified + " lnow= " + lnow );

        Thread.sleep( 1000 );
        //response= clientGet( "widgets/acme", new Date( lnow - 10000 ), 200 );
        ClientResponse response= clientGet( "widgets/acme", new Date( lnow - 1000 ), 200 );

        Feed feed = (Feed) response.getDocument().getRoot();
        log.debug( "SIZE=" + feed.getEntries().size() );

        assertEquals("Testing feed length", 1, feed.getEntries().size());
        touchFile.setLastModified( origLastModified );
        response.release();
    }

    public void XXXtestGetEntry() throws Exception {
        ClientResponse response = clientGet( "widgets/acme/4.en.xml" );

        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        assertTrue( entry.getContent().indexOf( "id=\"4\"" ) != -1 );
        response.release();
    }

    public void XXXtestGetEntrySincePasses() throws Exception {
        Date now = new Date();
        long lnow = now.getTime();
        File touchFile = new File( TEST_DATA_DIR + "/widgets/acme/99/9999/en/9999.xml.r0" );
        long origLastModified = touchFile.lastModified();
        touchFile.setLastModified( lnow );

        ClientResponse response= clientGet( "widgets/acme/9999.en.xml", new Date( lnow - 10000 ), 200 );

        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        assertTrue( entry.getContent().indexOf( "id=\"9999\"" ) != -1 );
        touchFile.setLastModified( origLastModified );
        response.release();
    }

    public void XXXtestGetEntrySinceFails() throws Exception {
        Date now = new Date();
        long lnow = now.getTime();
        File touchFile = new File( TEST_DATA_DIR + "/widgets/acme/99/9999/en/9999.xml.r0" );
        long origLastModified = touchFile.lastModified();
        touchFile.setLastModified( lnow - 10000 );

        ClientResponse response= clientGet( "widgets/acme/9999.en.xml", new Date( lnow ), 304 );

        touchFile.setLastModified( origLastModified );
        response.release();
    }

}
