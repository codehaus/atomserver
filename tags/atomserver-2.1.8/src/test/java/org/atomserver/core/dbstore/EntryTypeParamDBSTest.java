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
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.WorkspaceOptions;

import java.util.List;

/**
 */
public class EntryTypeParamDBSTest extends ParamDBSTestCase {

    public static Test suite()
    { return new TestSuite( EntryTypeParamDBSTest.class ); }

     public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------

    // NOTE: we don't have to check teh default cases (e.g. Feeds w/ link and Entries w/ full)
    //       because all the other tests do that implicitly

    public void testVarious() throws Exception {

        // testReadFeedWithEntryTypeFull
        ClientResponse response = clientGet( "widgets/acme?max-results=2&entry-type=full" );
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 2, feed.getEntries().size());

        List entries = feed.getEntries();
        for (Object obj : entries) {
            Entry entry = (Entry)obj;

            assertNotNull( entry.getContent() );
             // just look for something that should be in the full XML
            assertTrue( entry.getContent().indexOf( "id=\"" ) != -1 );
            // alternate link should be null when there is content element
            assertNull( entry.getAlternateLink() );
             
            assertTrue( entry.getUpdated().getTime() > 0L );
            assertEquals( entry.getUpdated(), entry.getPublished() );
            
            assertNotNull( entry.getEditLink() );
            assertNotNull( entry.getSelfLink() );
        }
        response.release();

        //--------------------------------
        // testReadEntryWithEntryTypeLink
        response = clientGet( "widgets/acme/4.en.xml?entry-type=link" );
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        log.debug( "entry= " + entry );

        assertNull( entry.getContent() );
         // alternate link should not be null when there is no content element
        assertNotNull( entry.getAlternateLink() );
       
        assertTrue( entry.getUpdated().getTime() > 0L );
        assertNotNull( entry.getPublished() );
                
        assertNotNull( entry.getEditLink() );
        assertNotNull( entry.getSelfLink() );
        response.release();

        //--------------------------------
        // testReadFeedMaxFullPerPage
        WorkspaceOptions options =  store.getAtomWorkspace( "widgets" ).getOptions();

        int incomingMax = options.getDefaultMaxFullEntriesPerPage();
        int maxPerPage = 2;

        options.setDefaultMaxFullEntriesPerPage(maxPerPage);

        response = clientGet("widgets/acme?max-results=8&entry-type=full");
        feed = (Feed) response.getDocument().getRoot();

        log.debug("SIZE=" + feed.getEntries().size());
        assertEquals("Testing feed length", maxPerPage, feed.getEntries().size());

        options.setDefaultMaxFullEntriesPerPage(incomingMax);

        response.release();
    }

}
