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

import org.apache.abdera.ext.history.FeedPagingHelper;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Document;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.BaseFeedDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;

import java.util.Date;
import java.util.List;

/**
 */
public class FeedDBSTestCase extends DBSTestCase {
 
    static protected final int NUM_RECS = 10;
    static private final int CREATE_WIDGETS_SLEEP = 300 ;

    public void setUp() throws Exception {
        super.setUp();

        validateStartCount();
    }

    protected void validateStartCount() {
        if ( startCount == 0 ) {
            startCount = NUM_RECS;
        }

        // the tests below assume an even count at the start of the DB
        assertTrue((startCount % 2) == 0);
    }

    public void tearDown() throws Exception { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    public void createRunAllTestsDelete( String wspace, String collection, int propId, String locale ) throws Exception {
        int start = createWidgets( wspace, propId, collection, locale );
        runAllTests( wspace, collection, propId, locale );
        deleteWidgets( wspace, start, propId, collection, locale );
    }

    // --------------------
    public void runAllTests( String wspace, String collection, int propId, String locale ) throws Exception {
        runAllTests( startCount, wspace, collection, propId, locale );
    }

    public void runAllTests( int startKnt, String wspace, String collection, int propId, String locale )
            throws Exception {

        startCount = startKnt;

        // test1
        readEntireFeedAtOnce( wspace, collection );

        // test2
        loopThruPagesUsingNextLink( wspace, (startCount / 3 + 1), collection );

        // test3 
        loopThruPagesUsingNextLink( wspace, (startCount / 2), collection );

        // test4 
        readFeedWithMaxNoStart( wspace, collection );

        // test5
        maxGreaterThanTotal( wspace, collection );
    }

    //-------------------------------
    protected int createWidgets( String wspace, int propIdSeed, String collection, String locale ) throws Exception {
        return createWidgets( wspace, propIdSeed, collection, locale, true );
    }

    protected int createWidgets( String wspace, int propIdSeed, String collection, String locale, boolean checkCount )
        throws Exception {
        return createWidgets( NUM_RECS, wspace, propIdSeed, collection, locale, true );
    }

    protected int createWidgets( int numRecs, String wspace, int propIdSeed, String collection, String locale, boolean checkCount )
            throws Exception {
        // COUNT
        BaseFeedDescriptor feedDescriptor = new BaseFeedDescriptor(wspace, collection);
        int start = entriesDao.getTotalCount(feedDescriptor);
        log.debug( "start = " + start );

        // INSERT
        // These will all be created with successive modified times...
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId =  "" + ( propIdSeed + ii );
            log.debug( "\n\n CREATING ["  + collection + " " + propId + " " + locale + "]" );
            // because mysql has bad Date presicion, let's sleep between inserts
            Thread.sleep( CREATE_WIDGETS_SLEEP );
            createWidget( wspace, collection, propId, locale, createWidgetXMLFileString(propId) );
        }

        // COUNT
        int count = entriesDao.getTotalCount(feedDescriptor);
        if ( checkCount )
            assertEquals( (start + numRecs), count );
        return start;
    }

    protected void deleteWidgets( String wspace, int start, int propIdSeed, String collection, String locale ) throws Exception {
        deleteWidgets( wspace, start, propIdSeed, collection, locale, true );
    }

    protected void deleteWidgets( String wspace, int start, int propIdSeed, String collection, String locale, boolean checkCount )
        throws Exception {
        deleteWidgets( NUM_RECS, wspace, start, propIdSeed, collection, locale, true );
    }

    protected void deleteWidgets( int numRecs, String wspace, int start, int propIdSeed, String collection, String locale, boolean checkCount )
            throws Exception {
        // DELETE them all for real
        for ( int ii=0; ii < numRecs; ii++ ) {
            String propId =  "" + (propIdSeed + ii);            
            deleteEntry( wspace, collection, propId, locale);
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up

        if ( checkCount ) {
            int finalCount = entriesDao.getTotalCount( new BaseFeedDescriptor(wspace, collection ));
            log.debug( "finalCount = " + finalCount );
            assertEquals( start, finalCount );
        }
    }

    // use next link to navigate to page
    protected void nextAndSelf( String wspace, String collection ) throws Exception {
        int pgSize = startCount / 2 + 1;
        ClientResponse response = clientGet( wspace + "/" + collection + "?max-results=" + pgSize);
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug("SIZE=" + feed.getEntries().size());
        assertEquals("Testing feed length", pgSize, feed.getEntries().size());

        if ( checkTotalResults( wspace ) )
            assertEquals(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS)), startCount);
        assertTrue(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.START_INDEX)) >= 0);
        assertTrue(Integer.parseInt(feed.getSimpleExtension(AtomServerConstants.END_INDEX)) > 0);
        assertEquals(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE)), pgSize);
        response.release();

        // using the next link, move to the next page.
        IRI next = FeedPagingHelper.getNext(feed);
        assertNotNull(next);
        log.debug("========> next= " + next);

        response = clientGetWithFullURL(next.toString());

        feed = (Feed) response.getDocument().getRoot();

        log.debug("SIZE=" + feed.getEntries().size());
        int pgSize2 = startCount - pgSize;
        assertEquals("Testing feed length", pgSize2, feed.getEntries().size());
        if ( checkTotalResults( wspace ) )
            assertEquals(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS)), startCount);
        assertEquals(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE)), pgSize);
        response.release();

        // try out the self link
        IRI self = feed.getSelfLinkResolvedHref();
        log.debug("&&&&&&&&&& SELF= " + self);
        assertNotNull(self);

        response = clientGetWithFullURL(self.toString());

        feed = (Feed) response.getDocument().getRoot();
        log.debug("SIZE=" + feed.getEntries().size());
        assertEquals("Testing feed length", pgSize2, feed.getEntries().size());
        if ( checkTotalResults( wspace ) )
            assertEquals(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS)), startCount);
        assertEquals(Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE)), pgSize);
        response.release();

        // now use previous to navigate back
        IRI previous = FeedPagingHelper.getPrevious(feed);
        assertNull(previous);
    }

    protected void maxGreaterThanTotal( String wspace, String collection ) throws Exception {
        int pgSize = startCount + 2;
        int startDelim = 0;
        ClientResponse response = clientGet( wspace + "/" + collection + "?max-results=" + pgSize + "&start-index=" + startDelim);
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug("SIZE=" + feed.getEntries().size());
        assertEquals("Testing feed length", startCount, feed.getEntries().size());

        if ( checkTotalResults( wspace ) )
            assertNull(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS));
        assertNull(feed.getSimpleExtension(OpenSearchConstants.START_INDEX));
        assertNull(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE));

        verifyEndIndex( wspace, feed );

        IRI next = FeedPagingHelper.getNext(feed);
        assertNull(next);
        IRI self = feed.getSelfLinkResolvedHref();
        assertNotNull(self);
        response.release();
    }


    protected void verifyEndIndex( String wspace, Feed feed  ) throws Exception {
        String endIndexStr = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        assertNotNull( endIndexStr );
        long endIndex = Long.valueOf( endIndexStr ); 

        List<Entry> entries = feed.getEntries();
        Entry lastEntry = (Entry)(entries.get( entries.size() - 1 ));

        // get the self URL
        Link selfLink = lastEntry.getSelfLink(); 
        assertNotNull( selfLink );
        IRI lastEntryIRI = selfLink.getHref(); 
        assertNotNull( lastEntryIRI );

        MockRequestContext request = new MockRequestContext(serviceContext, "GET", lastEntryIRI.toString());
        EntryTarget lastEntryTarget = widgetURIHelper.getEntryTarget(request, true);
        EntryMetaData lastEntryMetaData = entriesDao.selectEntry(lastEntryTarget);
        long endIndexEntry = lastEntryMetaData.getUpdateTimestamp();
        
        log.debug( "&&&&&&&&&&&&&&&&&&& feedUpdated, endIndex= " + endIndex + ", " + endIndexEntry );
        assertEquals( endIndex, endIndexEntry );
    }


    protected void readFeedMaxPerPage(  String wspace, String collection ) throws Exception {
        WorkspaceOptions options =  store.getAtomWorkspace( wspace ).getOptions();

        int incomingMax = options.getDefaultMaxLinkEntriesPerPage();

        int maxPerPage = (startCount/2) - 1;

        options.setDefaultMaxLinkEntriesPerPage(maxPerPage);
        ClientResponse response = clientGet( wspace + "/" + collection +"?max-results=8");
        verifyFeed1(wspace, response, maxPerPage);
        options.setDefaultMaxLinkEntriesPerPage(incomingMax);
        response.release();
    }

    protected void readFeedWithMaxNoStart( String wspace, String collection ) throws Exception {

        int pgSize = (startCount/2) - 1;

        ClientResponse response = clientGet( wspace + "/" + collection + "?max-results=" + pgSize);
        verifyFeed1(wspace, response, pgSize);
        response.release();
    }

    protected void readEntireFeedAtOnce( String wspace, String collection ) throws Exception {
        ClientResponse response = clientGet( wspace + "/" + collection);
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug("SIZE=" + feed.getEntries().size());
        assertEquals("Testing feed length", startCount, feed.getEntries().size());

        if ( checkTotalResults( wspace ) )
            assertNull(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS));
        assertNull(feed.getSimpleExtension(OpenSearchConstants.START_INDEX));
        assertNull(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE));

        verifyEndIndex( wspace, feed );

        IRI next = FeedPagingHelper.getNext(feed);
        assertNull(next);

        IRI self = feed.getSelfLinkResolvedHref();
        assertNotNull(self);

        // verify lastModified
        Date feedUpdated = feed.getUpdated();
        List<Entry> entries = feed.getEntries();
        Entry lastEntry = (Entry)(entries.get( entries.size() - 1 ));
        Date lastEntryUpdated = lastEntry.getUpdated();
        log.debug( "&&&&&&&&&&&&&&&&&&& feedUpdated, lastEntryUpdated= " + feedUpdated  + ", " + lastEntryUpdated );
        assertEquals( feedUpdated, lastEntryUpdated );
        response.release();

    }

    protected void verifyFeed1(String wspace, ClientResponse response, int pgSize) {
        Feed feed = (Feed) response.getDocument().getRoot();

        log.debug("SIZE=" + feed.getEntries().size());
        assertEquals("Testing feed length", pgSize, feed.getEntries().size());

        int totalEntries = 0;
        if ( checkTotalResults( wspace ) )
            totalEntries = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS));
        int startIndex = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.START_INDEX));
        int endIndex = Integer.parseInt(feed.getSimpleExtension(AtomServerConstants.END_INDEX));
        int pageSize = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE));
        log.debug("(totalEntries, startIndex, endIndex, pageSize)= " + totalEntries
                  + " " + startIndex + " " + endIndex + " " + pageSize + " ");

        if ( checkTotalResults( wspace ) )
            assertEquals(totalEntries, startCount);
        assertTrue(endIndex > 0);
        assertEquals(pageSize, pgSize);

        IRI next = FeedPagingHelper.getNext(feed);
        assertNotNull(next);
        IRI self = feed.getSelfLinkResolvedHref();
        assertNotNull(self);
    }

    protected void loopThruPagesUsingEndIndex( String wspace, int pgSize, String collection) throws Exception {
        int startDelim = 0;
        int endDelim = 0;
        int numpages = startCount / pgSize;
        numpages += ((startCount % pgSize) == 0) ? 0 : 1;
        log.debug("numpages = " + numpages);

        for (int ii = 0; ii < numpages; ii++) {
            startDelim = endDelim;
            endDelim = getPageUsingEndIndex(wspace, pgSize, startDelim, endDelim, collection);
        }
    }

    protected void loopThruPagesUsingNextLink( String wspace, int pgSize, String collection ) throws Exception {
        loopThruPagesUsingNextLink( wspace, pgSize, collection, startCount, false) ;
    }

    protected void loopThruPagesUsingNextLink( String wspace, int pgSize, String collection,
                                               int totalEntries, boolean isMTtest ) throws Exception {
        int startDelim = 0;
        int endDelim = 0;
        int numpages = totalEntries / pgSize;
        numpages += ((totalEntries % pgSize) == 0) ? 0 : 1;
        log.debug("numpages = " + numpages);

        int knt = 0;
        IRI iri = new IRI(getServerURL() + wspace + "/" + collection + "?max-results=" + pgSize);
        IRI next = iri;
        while (next != null) {
            next = getPageUsingNext(wspace, pgSize, next, isMTtest );
            knt++;
        }
        // check that reading past the end gives a 304
        clientGetWithFullURL(iri.toString() + "&start-index=10000000", 304);
        // check that with the "scroll-on-unmodified" param, we get back an empty feed with the
        // appropriate paging fields set.
        ClientResponse clientResponse =
                clientGetWithFullURL(iri.toString() + "&start-index=10000000&scroll-on-unmodified", 200);
        Feed feed = clientResponse.<Feed>getDocument().getRoot();
        long endIndex = Long.parseLong(feed.getSimpleExtension(AtomServerConstants.END_INDEX));
        assertTrue(endIndex > 0);
        
        if ( !isMTtest )
            assertEquals(numpages, knt);
    }

    protected int getPageUsingEndIndex( String wspace, int pgSize, int startIndex, int endIndex, String collection )  throws Exception {

        log.debug("pgSize, startIndex, endIndex = " + pgSize + " " + startIndex + " " + endIndex);

        ClientResponse response = clientGet( wspace + "/" + collection + "?max-results=" + pgSize + "&start-index=" + startIndex);
        Feed feed = (Feed) response.getDocument().getRoot();

        int totalEntriesResp = 0;
        if ( checkTotalResults( wspace ) )
            totalEntriesResp = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS));

        int pageSizeResp = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE));
        int startIndexResp = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.START_INDEX));

        int endIndexResp = Integer.parseInt(feed.getSimpleExtension(AtomServerConstants.END_INDEX));
        log.debug("(totalEntriesResp, endIndexResp, pageSizeResp)= " + totalEntriesResp
                  + " " + endIndexResp + " " + pageSizeResp + " ");

        verifyEndIndex( wspace, feed );

        if ( checkTotalResults( wspace ) )
            assertEquals(totalEntriesResp, startCount);
        assertTrue(endIndexResp > startIndex);
        assertEquals(pageSizeResp, pgSize);
        response.release();

        return endIndexResp;
    }

    protected IRI getPageUsingNext(String wspace, int pgSize, IRI pageToGet, boolean isMTtest) throws Exception {
        log.debug("=============> getPage:: pageToGet = " + pageToGet);

        ClientResponse response = clientGetWithFullURL(pageToGet.toString());

        Feed feed = (Feed) response.getDocument().getRoot();
        response.release();

        int totalEntries = 0;
        if ( checkTotalResults( wspace ) )
            totalEntries = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.TOTAL_RESULTS));

        int pageSize = Integer.parseInt(feed.getSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE));
        log.debug("(totalEntries, pageSize)= " + totalEntries + " " + pageSize + " ");

        if ( checkTotalResults(  wspace ) )
            assertEquals(totalEntries, startCount);
        assertEquals(pageSize, pgSize);

        List<Entry> entries = feed.getEntries();
        for (Entry eee : entries) {
            log.debug("***************************************");
            log.debug( eee );

            // in the multithreaded test, we want to GET every entry....
            if ( isMTtest ) {

                //String link = eee.getSelfLinkResolvedHref().toString();
                String link = getServerRoot() + eee.getId();
                log.debug( "link= " + link );

                response = clientGetWithFullURL( link );
                Document<Entry> doc = response.getDocument();
                Entry entry = doc.getRoot();
                assertNotNull( "entry is NULL (for " + link + ")", entry );

                if ( entry.getContent() == null ) {
                    java.io.StringWriter stringWriter = new java.io.StringWriter();
                    doc.writeTo( abdera.getWriterFactory().getWriter("PrettyXML"), stringWriter );
                    log.warn( "CONTENT IS NULL FOR (" + link + ")" + "\n" + stringWriter.toString() );
                    assertNotNull( "content is NULL (for " + link + ")", entry.getContent() );
                }

                assertTrue( entry.getContent().indexOf( "id=" ) != -1 );
                response.release();
            }
        }

        // verify lastModified
        Date feedUpdated = feed.getUpdated();
        Entry lastEntry = (Entry)(entries.get( entries.size() - 1 ));
        Date lastEntryUpdated = lastEntry.getUpdated();
        log.debug( "&&&&&&&&&&&&&&&&&&& feedUpdated, lastEntryUpdated= " + feedUpdated  + ", " + lastEntryUpdated );
        assertEquals( feedUpdated, lastEntryUpdated );

        if ( ! isMTtest )
            verifyEndIndex( wspace, feed );

        // The final NextLink comes back null
        IRI next = FeedPagingHelper.getNext(feed);
        log.debug("========> next= " + next);

        return next;
    }

}
