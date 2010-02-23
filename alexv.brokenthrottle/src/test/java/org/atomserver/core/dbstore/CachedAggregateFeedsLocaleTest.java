
package org.atomserver.core.dbstore;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.commons.lang.StringUtils;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.cache.AggregateFeedCacheManager;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;


public class CachedAggregateFeedsLocaleTest extends DBSTestCase {

    AggregateFeedCacheManager cacheManager = null;
    List<String> cachedFeedIds = null;


    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates2");
        super.setUp();

        cacheManager = (AggregateFeedCacheManager) super.getSpringFactory().getBean("org.atomserver-aggregatefeedcachemanager");

         // aggregate feeds of interest
        List<String> feedList = new ArrayList<String>();
        feedList.add("$join,urn:foobars.mady");
        feedList.add("$join,urn:fooboos.mady");

        // clear cache from previous state
        cacheManager.removeCachedAggregateFeeds(feedList);

        entryCategoriesDAO.deleteAllEntryCategories("foos");
        entryCategoriesDAO.deleteAllEntryCategories("boos");
        entryCategoriesDAO.deleteAllEntryCategories("bars");

        entriesDao.deleteAllEntries(new BaseServiceDescriptor("foos"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("boos"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("bars"));

         // add feeds to cache
        cachedFeedIds = cacheManager.cacheAggregateFeed(feedList);

        for (int i = 1000; i < 1003; i++) {
            String entryId = "" + i;
            modifyEntry("boos", "baz", entryId, Locale.US.toString(), createBoo(i), true, "0");
            modifyEntry("foos", "baz", entryId, Locale.US.toString(), createFoo(i), true, "0");
            modifyEntry("foos", "baz", entryId, Locale.FRANCE.toString(), createFoo(i), true, "0");
            modifyEntry("bars", "baz", entryId, null, createBar(i), true, "0");
        }
    }

    public void tearDown() throws Exception {
        cacheManager.removeCachedAggregateFeedsByFeedIds(cachedFeedIds);
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    // Localized aggregated with non-localized
    public void testLocaleInAggFeeds() throws Exception {

        // TODO: Aggregate Feeds do not currently work in HSQLDB
        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("foos/baz");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("bars/baz");
        assertEquals(3, feed.getEntries().size());

        // get the aggregate feed
        feed = getPage("$join/urn:foobars.mady?entry-type=full");
        printFeed( feed );

        assertEquals(3, feed.getEntries().size());

        for (Entry entry : feed.getEntries()) {
            String content = entry.getContent();
            assertTrue(content.startsWith("<aggregate"));

            assertEquals( 3, StringUtils.countMatches( content, "<entry" ) );
            assertEquals( 2, StringUtils.countMatches( content, "<as:locale" ) );
            assertEquals( 1, StringUtils.countMatches( content, "en_US</as:locale>" ) );
            assertEquals( 1, StringUtils.countMatches( content, "fr_FR</as:locale>" ) );
            assertEquals( 2, StringUtils.countMatches( content, "<foo" ) );
            assertEquals( 1, StringUtils.countMatches( content, "<bar" ) );
       }
    }

    // Localized aggregated with localized
    public void testLocaleInAggFeeds2() throws Exception {

        // TODO: Aggregate Feeds do not currently work in HSQLDB
        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("foos/baz");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("boos/baz");
        assertEquals(3, feed.getEntries().size());

        // get the aggregate feed
        feed = getPage("$join/urn:fooboos.mady?entry-type=full");
        printFeed( feed );

        assertEquals(3, feed.getEntries().size());

        for (Entry entry : feed.getEntries()) {
            String content = entry.getContent();
            assertTrue(content.startsWith("<aggregate"));

            assertEquals( 3, StringUtils.countMatches( content, "<entry" ) );
            assertEquals( 3, StringUtils.countMatches( content, "<as:locale" ) );
            assertEquals( 2, StringUtils.countMatches( content, "en_US</as:locale>" ) );
            assertEquals( 1, StringUtils.countMatches( content, "fr_FR</as:locale>" ) );
            assertEquals( 2, StringUtils.countMatches( content, "<foo" ) );
            assertEquals( 1, StringUtils.countMatches( content, "<boo" ) );
       }
    }

    private static String createBoo(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<boo xmlns='http://schemas.atomserver.org/aggregate-tests2'>");
        stringBuilder.append("<blah id=\"").append(id).append("\" groupId=\"mady\" />");
        stringBuilder.append("</boo>");
        return stringBuilder.toString();
    }

    private static String createFoo(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<foo xmlns='http://schemas.atomserver.org/aggregate-tests2'>");
        stringBuilder.append("<blah id=\"").append(id).append("\" groupId=\"mady\" />");
        stringBuilder.append("<info id=\"").append(id).append("\" groupId=\"mady\" />");
        stringBuilder.append("</foo>");
        return stringBuilder.toString();
    }

    private static String createBar(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<bar xmlns='http://schemas.atomserver.org/aggregate-tests2'>");
        stringBuilder.append("<info id=\"").append(id).append("\" groupId=\"mady\" />");
        stringBuilder.append("</bar>");
        return stringBuilder.toString();
    }

}