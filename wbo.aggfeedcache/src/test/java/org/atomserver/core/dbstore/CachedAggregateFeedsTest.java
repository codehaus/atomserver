package org.atomserver.core.dbstore;

import org.apache.abdera.model.Base;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.lang.LocaleUtils;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.utils.AtomDate;
import org.atomserver.cache.AggregateFeedCacheManager;
import org.atomserver.EntryDescriptor;
import org.atomserver.AtomService;
import org.atomserver.ext.batch.Operation;
import org.atomserver.ext.batch.Results;
import org.atomserver.ext.batch.Status;
import org.atomserver.uri.URIHandler;
import org.atomserver.uri.EntryTarget;
import org.springframework.context.ApplicationContext;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.text.MessageFormat;

/**
 *  Test AggregateFeeds using Cached timestamps. This test is the similar to
 * AggregateFeedsTest except cached is turned on and different data set is used.
 */
public class CachedAggregateFeedsTest extends DBSTestCase {

    AggregateFeedCacheManager cacheManager = null;
    List<String> cachedFeedIds = null;
    protected URIHandler entryURIHelper = null;

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates3");
        super.setUp();

        ApplicationContext appCtx = super.getSpringFactory();
        cacheManager = (AggregateFeedCacheManager) appCtx.getBean("org.atomserver-aggregatefeedcachemanager");
        entryURIHelper = ((AtomService) appCtx.getBean("org.atomserver-atomService")).getURIHandler();

        // aggregate feeds of interest
        List<String> feedList = new ArrayList<String>();
        feedList.add("$join(reds,greens,blues), urn:hue, en_US");
        feedList.add("$join(reds,greens), urn:hue, en_US");
        feedList.add("$join(reds,greens), urn:hue");
        feedList.add("$join,urn:hue,en_US");
        feedList.add("$join,urn:hue");

        // clear cache from previous state
        cacheManager.removeCachedAggregateFeeds(feedList);

        entryCategoriesDAO.deleteAllEntryCategories("reds");
        entryCategoriesDAO.deleteAllEntryCategories("greens");
        entryCategoriesDAO.deleteAllEntryCategories("blues");

        entriesDao.deleteAllEntries(new BaseServiceDescriptor("reds"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("greens"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("blues"));

        // workspace purples is not used in this test but needs to be cleaned up.
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("purples"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("purples"));

        // add feeds to cache
        cachedFeedIds = cacheManager.cacheAggregateFeed(feedList);

        for (int i = 6006; i < 6018; i++) {
            String entryId = "" + i;
            modifyEntry("reds", "shades", entryId, Locale.US.toString(), redXml(i,0), true, "0");
            if (i % 2 == 0) {
                modifyEntry("greens", "shades", entryId, null, greenXml(i,0), true, "0");
            }
            if (i % 3 == 0) {
                modifyEntry("blues", "shades", entryId, null, blueXml(i,0), true, "0");
            }
        }
    }

    public void tearDown() throws Exception {
//        cacheManager.removeCachedAggregateFeedsByFeedIds(cachedFeedIds);
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testAggregateFeeds3() throws Exception {
        
        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }
        
        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("reds/shades");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("greens/shades");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("blues/shades");
        assertEquals(4, feed.getEntries().size());

        // Note: $join/urn:hue is a cached feed.
        feed = getPage("$join/urn:hue");

        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // getting the next page should return a 304 NOT MODIFIED
        getPage("$join/urn:hue?start-index=" + endIndex, 304);

        // changing one red should result in a single entry in our aggregate feed
        modifyEntry("reds", "shades", "6015", Locale.US.toString(), redXml(6015,1), false, "1");
        feed = getPage("$join/urn:hue?start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(fullEntry.getContent().contains("<id>6015</id>"));
            assertTrue(fullEntry.getContent().contains("<red>6015</red>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one green should result in two entries in our aggregate feed
        modifyEntry("greens", "shades", "6010", null, greenXml(6010,1), false, "1");
        feed = getPage("$join/urn:hue?start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(
                    fullEntry.getContent().contains("<id>6010</id>") ||
                    fullEntry.getContent().contains("<id>6011</id>"));
            assertTrue(
                    fullEntry.getContent().contains("<red>6010</red>") &&
                    fullEntry.getContent().contains("<red>6011</red>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one blue should result in three entries in our aggregate feed
        modifyEntry("blues", "shades", "6009", null, blueXml(6009,1), false, "1");
        feed = getPage("$join/urn:hue?start-index=" + endIndex);
        assertEquals(3, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(
                    fullEntry.getContent().contains("<id>6009</id>") ||
                    fullEntry.getContent().contains("<id>6010</id>") ||
                    fullEntry.getContent().contains("<id>6011</id>"));
            assertTrue(
                    fullEntry.getContent().contains("<red>6009</red>") &&
                    fullEntry.getContent().contains("<red>6010</red>") &&
                    fullEntry.getContent().contains("<red>6011</red>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // The next three sections test what happens when there are several aggregate entries with
        // the same seqnum, and those would be split across a page boundary.  We cannot allow this,
        // because that would break the clients' ability to page through the results.  When this
        // happens, we always try to return a SMALLER page than requested, forcing ALL of the
        // entries with the overlapping seqnum on to the next page.
        //
        // The only situation where this solution does not work is when ALL of the results on a
        // page have the same seqnum.  In this case, there may be more, so we need to INCREASE
        // the page size to return all of them on one page.  This should be an extremely rare case,
        // and we solve it with a simple doubling algorithm -- we re-select the same page again,
        // doubling the page size recursively until we either reach the end of the feed or find a
        // heterogeneous set.

        // in this case, we can solve the problem by REDUCING the page size from the requested 4
        // down to 3 - this is the usual case.
        modifyEntry("blues", "shades", "6009", null, blueXml(6009,2), false, "2");
        modifyEntry("blues", "shades", "6012", null, blueXml(6012,1), false, "1");

        feed = getPage("$join/urn:hue?max-results=4&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("6009", "6010", "6011"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        feed = getPage("$join/urn:hue?max-results=4&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("6012", "6013", "6014"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // here, we have to double from the requested 2 up to 4, which is then reduced as above to
        // 3 before we return.
        modifyEntry("blues", "shades", "6009", null, blueXml(6009,3), false, "3");
        modifyEntry("blues", "shades", "6012", null, blueXml(6012,2), false, "2");

        feed = getPage("$join/urn:hue?max-results=2&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("6009", "6010", "6011"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        feed = getPage("$join/urn:hue?max-results=2&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("6012", "6013", "6014"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // here, we have to double TWICE from the requested 1 to 2 and then to 4, which is then
        // reduced as above to 3 before we return.
        modifyEntry("blues", "shades", "6009", null, blueXml(6009,4), false, "4");
        modifyEntry("blues", "shades", "6012", null, blueXml(6012,3), false, "3");

        feed = getPage("$join/urn:hue?max-results=1&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("6009", "6010", "6011"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        feed = getPage("$join/urn:hue?max-results=1&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("6012", "6013", "6014"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing these "overlapping" objects should result in five entries in our aggregate feed
        modifyEntry("reds", "shades", "6017", Locale.US.toString(), redXml(6017,1), false, "1");
        modifyEntry("greens", "shades", "6014", null, greenXml(6014,1), false, "1");
        modifyEntry("blues", "shades", "6012", null, blueXml(6012,4), false, "4");
        feed = getPage("$join/urn:hue?start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            log.debug("ENTRY-URL:: " + entryUrl);
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(
                    fullEntry.getContent().contains("<id>6012</id>") ||
                    fullEntry.getContent().contains("<id>6013</id>") ||
                    fullEntry.getContent().contains("<id>6014</id>") ||
                    fullEntry.getContent().contains("<id>6015</id>") ||
                    fullEntry.getContent().contains("<id>6017</id>"));
        }
        feed = getPage("$join/urn:hue?entry-type=full&start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            assertTrue(entry.getContent().startsWith("<aggregate"));
            assertTrue(entry.getContent().contains("<red"));
            assertTrue(entry.getContent().contains("<green"));
            assertTrue(entry.getContent().contains("<blue"));
            assertTrue(
                    entry.getContent().contains("<id>6012</id>") ||
                    entry.getContent().contains("<id>6013</id>") ||
                    entry.getContent().contains("<id>6014</id>") ||
                    entry.getContent().contains("<id>6015</id>") ||
                    entry.getContent().contains("<id>6017</id>"));
        }
        // by adding some "search" categories to the feed, we should be able to limit the set of results
        feed = getPage("$join/urn:hue/-/(urn:tint)even?start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/(urn:tint)odd?start-index=" + endIndex);
        assertEquals(3, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/(urn:tint)even/(urn:tint)blues?start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/(urn:tint)odd/(urn:tint)reds?start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());


        // getting the same feed as a localized feed should return the same results
        feed = getPage("$join/urn:hue?locale=en_US");
        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one red should result in a single entry in our aggregate feed
        modifyEntry("reds", "shades", "6015", Locale.US.toString(), redXml(6015,2), false, "2");
        feed = getPage("$join/urn:hue?locale=en_US&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(fullEntry.getContent().contains("<id>6015</id>"));
            assertTrue(fullEntry.getContent().contains("<red>6015</red>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one green should result in two entries in our aggregate feed
        modifyEntry("greens", "shades", "6010", null, greenXml(6010,2), false, "2");
        feed = getPage("$join/urn:hue?locale=en_US&start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(
                    fullEntry.getContent().contains("<id>6010</id>") ||
                    fullEntry.getContent().contains("<id>6011</id>"));
            assertTrue(
                    fullEntry.getContent().contains("<red>6010</red>") &&
                    fullEntry.getContent().contains("<red>6011</red>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing these "overlapping" objects should result in five entries in our aggregate feed
        modifyEntry("reds", "shades", "6017", Locale.US.toString(), redXml(6017,2), false, "2");
        modifyEntry("greens", "shades", "6014", null, greenXml(6014,2), false, "2");
        modifyEntry("blues", "shades", "6012", null, blueXml(6012,5), false, "5");
        feed = getPage("$join/urn:hue?locale=en_US&start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            log.debug("ENTRY-URL:: " + entryUrl);
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
            assertTrue(
                    fullEntry.getContent().contains("<id>6012</id>") ||
                    fullEntry.getContent().contains("<id>6013</id>") ||
                    fullEntry.getContent().contains("<id>6014</id>") ||
                    fullEntry.getContent().contains("<id>6015</id>") ||
                    fullEntry.getContent().contains("<id>6017</id>"));
        }
        feed = getPage("$join/urn:hue?locale=en_US&entry-type=full&start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            assertTrue(entry.getContent().startsWith("<aggregate"));
            assertTrue(entry.getContent().contains("<red"));
            assertTrue(entry.getContent().contains("<green"));
            assertTrue(entry.getContent().contains("<blue"));
            assertTrue(
                    entry.getContent().contains("<id>6012</id>") ||
                    entry.getContent().contains("<id>6013</id>") ||
                    entry.getContent().contains("<id>6014</id>") ||
                    entry.getContent().contains("<id>6015</id>") ||
                    entry.getContent().contains("<id>6017</id>"));
        }
        // by adding some "search" categories to the feed, we should be able to limit the set of results
        feed = getPage("$join/urn:hue/-/(urn:tint)even?locale=en_US&start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/(urn:tint)odd?locale=en_US&start-index=" + endIndex);
        assertEquals(3, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/(urn:tint)even/(urn:tint)blues?locale=en_US&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/(urn:tint)odd/(urn:tint)reds?locale=en_US&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        feed = getPage("$join/urn:hue/-/OR" +
                       "/AND/(urn:tint)even/(urn:tint)blues" +
                       "/AND/(urn:tint)odd/(urn:tint)reds" +
                       "?locale=en_US&start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());

        // Test that we can limit the set of workspaces included in the join. $join(reds,greens)/urn:hue is a cached feed
        feed = getPage("$join(reds,greens)/urn:hue");
        assertEquals(12, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertFalse(fullEntry.getContent().contains("<blue"));
        }
        feed = getPage("$join(reds,greens)/urn:hue?entry-type=full&max-results=10");
        assertEquals(10, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {            
            assertTrue(entry.getContent().startsWith("<aggregate"));
            assertTrue(entry.getContent().contains("<red"));
            assertTrue(entry.getContent().contains("<green"));
            assertFalse(entry.getContent().contains("<blue"));
        }

        // The following is a regression test against the bug we discovered where with a previous
        // incarnation of the SQL, we would sometimes not return all of the aggregate entries in a
        // feed because we were paginating the queries at the wrong level, and the intersection of
        // the entries that matched the join with the entries that matched the category query was
        // empty.

        // move to the end of the "even" feed
        feed = getPage("$join/urn:hue/-/(urn:tint)even");
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // modify all of the "odd" reds
        for (int i = 6007; i < 6018; i+=2) {
            String entryId = "" + i;
            modifyEntry("reds", "shades", entryId, Locale.US.toString(), redXml(i,1000), false, "*");
        }

        // modify ONE "even" red
        modifyEntry("reds", "shades", "6008", Locale.US.toString(), redXml(6008,1000), false, "*");

        // we should see the one entry in the "even" feed
        feed = getPage("$join/urn:hue/-/(urn:tint)even?max-results=2&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        assertEquals("6008", feed.getEntries().get(0).getSimpleExtension(AtomServerConstants.ENTRY_ID));

        // the following is a regression test against the bug we discovered where entries in a
        // locale that consisted of a language only (i.e. no country) were not retrievable.  This
        // ensures that we can reference such aggregates correctly.

        // set up a single aggregate entry in the locale=de feed
        modifyEntry("reds", "shades", "90210", Locale.GERMAN.toString(), redXml(90210,0), true, "0");
        modifyEntry("greens", "shades", "90210", null, greenXml(90210,0), true, "0");
        modifyEntry("blues", "shades", "90210", null, blueXml(90210,0), true, "0");
        // pull the feed - should get one result
        feed = getPage("$join/urn:hue?locale=de");
        assertEquals(1, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            // pull down the one and only full entry - make sure we get it and that it is complete
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<red"));
            assertTrue(fullEntry.getContent().contains("<green"));
            assertTrue(fullEntry.getContent().contains("<blue"));
        }

        // test oblite entries
        feed = getPage("$join/urn:hue?locale=en_US");           
        assertEquals(12, feed.getEntries().size());
        feed = getPage("$join/urn:hue");
        assertEquals(15, feed.getEntries().size());
        
        deleteEntry("reds","shades", "6006", "en_US");
        deleteEntry("greens","shades", "6006", null);
        deleteEntry("blues", "shades", "6006", null);
        feed = getPage("$join/urn:hue?locale=en_US");
        assertEquals(11, feed.getEntries().size());

        feed = getPage("$join(reds,greens)/urn:hue");
        assertEquals(13,feed.getEntries().size());

    }

    public void testStartEndIndex3() throws Exception {

        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("reds/shades");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("greens/shades");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("blues/shades");
        assertEquals(4, feed.getEntries().size());

        // get the aggregate feed, and mark the end index
        feed = getPage("$join/urn:hue");

        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        log.debug( "endIndex= " + endIndex );

        int ii = 0;
        int[] indexes = new int[12];
        List<Entry> entries = feed.getEntries();
        for (Entry entry : entries) {
            indexes[ii] = Integer.parseInt( entry.getSimpleExtension(AtomServerConstants.UPDATE_INDEX) );
            log.debug( "index[" + ii + "] = " + indexes[ii] );
            ii++;
        }
        
        // NOTE: start-index is exclusive, end-index is inclusive
        feed = getPage("$join/urn:hue?start-index=" + indexes[2] + "&end-index=" + indexes[7], 200);
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 5, feed.getEntries().size());

        List<Entry> entriesCheck = feed.getEntries();
        int lastIndex = 0;
        for (Entry entry : entriesCheck) {
            int thisIndex = Integer.parseInt( entry.getSimpleExtension(AtomServerConstants.UPDATE_INDEX) );
            assertTrue( thisIndex > indexes[2] );
            assertTrue( thisIndex > lastIndex );
            lastIndex = thisIndex;
        }
        assertEquals( indexes[7], lastIndex);

        // end-index past the actual last index
        int biggerIndex = indexes[11] + 100000;
        feed = getPage("$join/urn:hue?start-index=" + indexes[2] + "&end-index=" + biggerIndex, 200);
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 9, feed.getEntries().size());

        // same one for start and end, MUST return 304 cuz can't be both exclusive and inclusive
        getPage( "$join/urn:hue?start-index=" + indexes[2] + "&end-index=" + indexes[2], 304 );

        // end is before start
        getPage( "$join/urn:hue?start-index=" + indexes[2] + "&end-index=" + indexes[0], 400 );
    }

    public void testUpdateMaxMin3() throws Exception {

        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("reds/shades");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("greens/shades");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("blues/shades");
        assertEquals(4, feed.getEntries().size());

        // get the aggregate feed, and mark the end index
        feed = getPage("$join/urn:hue");

        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        log.debug( "endIndex= " + endIndex );

        // get max earlier than any we added
        long lnow = (entriesDao.selectSysDate()).getTime();
        String earlier = AtomDate.format( new Date( lnow - 100000 ) );

        getPage( "$join/urn:hue?updated-max=" + earlier, 304 );

        // Sleep a couple of seconds, then add modify an entry
        Thread.sleep( 2000 );

        lnow = entriesDao.selectSysDate().getTime();
        String beforeLast = AtomDate.format( new Date( lnow - 50 ) );

        // changing one red should result in a single entry in our aggregate feed
        modifyEntry("reds", "shades", "6015", Locale.US.toString(), redXml(6015,1), false, "1");

        // get all but the one we just modified
        feed = getPage( "$join/urn:hue?updated-max=" + beforeLast, 200 );
        assertEquals("Testing feed length", 11, feed.getEntries().size());

        // get just the one we just added
        lnow = entriesDao.selectSysDate().getTime();
        String afterLast = AtomDate.format( new Date( lnow ) );

        feed = getPage( "$join/urn:hue?updated-min=" + beforeLast + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 1, feed.getEntries().size());

        // get all of them
        String beforeAll = AtomDate.format( new Date( lnow - 100000 ) );
        feed = getPage( "$join/urn:hue?updated-min=" + beforeAll + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 12, feed.getEntries().size());

        // changing one green should result in two entries in our aggregate feed
        lnow = entriesDao.selectSysDate().getTime();
        String beforeAnother2 = AtomDate.format( new Date( lnow ) );

        modifyEntry("greens", "shades", "6010", null, greenXml(6010,1), false, "1");

        // get just the first one we modified again
        feed = getPage( "$join/urn:hue?updated-min=" + beforeLast + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 1, feed.getEntries().size());

        // get everything but the last two modified
        feed = getPage( "$join/urn:hue?updated-min=" + beforeAll + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 10, feed.getEntries().size());

        // get only the last two modified
        lnow = entriesDao.selectSysDate().getTime();
        String afterAnother2 = AtomDate.format( new Date( lnow ) );

        feed = getPage( "$join/urn:hue?updated-min=" + beforeAnother2 + "&updated-max=" + afterAnother2, 200 );
        assertEquals("Testing feed length", 2, feed.getEntries().size());
    }

    private void checkPageContainsExpectedEntries(Feed feed, List<String> expected) {
        assertEquals(expected.size(), feed.getEntries().size());
        for (int i = 0; i < expected.size(); i++) {
            String entryId = feed.getEntries().get(i).getSimpleExtension(AtomServerConstants.ENTRY_ID);
            assertTrue(expected.contains(entryId));
        }
    }

    private static String redXml(int id, int revNo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<red xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        stringBuilder.append("<rev>").append(revNo).append("</rev>");
        stringBuilder.append("<group>").append(id % 2 == 0 ? "even" : "odd").append("</group>");
        stringBuilder.append("</red>");
        return stringBuilder.toString();
    }

    private static String greenXml(int id, int revNo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<green xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<red>").append(id).append("</red>");
        stringBuilder.append("<red>").append(id + 1).append("</red>");
        stringBuilder.append("<rev>").append(revNo).append("</rev>");
        stringBuilder.append("<group>").append(id % 3 == 0 ? "reds" : "blues").append("</group>");
        stringBuilder.append("</green>");
        return stringBuilder.toString();
    }

    private static String blueXml(int id, int revNo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<blue xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<red>").append(id).append("</red>");
        stringBuilder.append("<red>").append(id + 1).append("</red>");
        stringBuilder.append("<red>").append(id + 2).append("</red>");
        stringBuilder.append("<rev>").append(revNo).append("</rev>");
        stringBuilder.append("<group>").append(id % 5 == 0 ? "heavy" : "light").append("</group>");
        stringBuilder.append("</blue>");
        return stringBuilder.toString();
    }
}
