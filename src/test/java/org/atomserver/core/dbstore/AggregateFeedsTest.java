package org.atomserver.core.dbstore;

import org.apache.abdera.model.Base;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.utils.AtomDate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AggregateFeedsTest extends DBSTestCase {

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates");
        super.setUp();

        categoriesDAO.deleteAllEntryCategories("lalas");
        categoriesDAO.deleteAllEntryCategories("cuckoos");
        categoriesDAO.deleteAllEntryCategories("aloos");
        
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("lalas"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("cuckoos"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("aloos"));

        for (int i = 3006; i < 3018; i++) {
            String entryId = "" + i;
            modifyEntry("lalas", "my", entryId, Locale.US.toString(), lalaXml(i,0), true, "0");
            if (i % 2 == 0) {
                modifyEntry("cuckoos", "my", entryId, null, cuckooXml(i,0), true, "0");
            }
            if (i % 3 == 0) {
                modifyEntry("aloos", "my", entryId, null, alooXml(i,0), true, "0");
            }
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testAggregateFeeds() throws Exception {
        
        // TODO: Aggregate Feeds do not currently work in HSQLDB
        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("lalas/my");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("cuckoos/my");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("aloos/my");
        assertEquals(4, feed.getEntries().size());

        // get the aggregate feed, and mark the end index
        feed = getPage("$join/urn:link");

        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);


        // getting the next page should return a 304 NOT MODIFIED
        getPage("$join/urn:link?start-index=" + endIndex, 304);

        // changing one lala should result in a single entry in our aggregate feed
        modifyEntry("lalas", "my", "3015", Locale.US.toString(), lalaXml(3015,1), false, "1");
        feed = getPage("$join/urn:link?start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(fullEntry.getContent().contains("<id>3015</id>"));
            assertTrue(fullEntry.getContent().contains("<lala>3015</lala>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one cuckoo should result in two entries in our aggregate feed
        modifyEntry("cuckoos", "my", "3010", null, cuckooXml(3010,1), false, "1");
        feed = getPage("$join/urn:link?start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(
                    fullEntry.getContent().contains("<id>3010</id>") ||
                    fullEntry.getContent().contains("<id>3011</id>"));
            assertTrue(
                    fullEntry.getContent().contains("<lala>3010</lala>") &&
                    fullEntry.getContent().contains("<lala>3011</lala>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one aloo should result in three entries in our aggregate feed
        modifyEntry("aloos", "my", "3009", null, alooXml(3009,1), false, "1");
        feed = getPage("$join/urn:link?start-index=" + endIndex);
        assertEquals(3, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(
                    fullEntry.getContent().contains("<id>3009</id>") ||
                    fullEntry.getContent().contains("<id>3010</id>") ||
                    fullEntry.getContent().contains("<id>3011</id>"));
            assertTrue(
                    fullEntry.getContent().contains("<lala>3009</lala>") &&
                    fullEntry.getContent().contains("<lala>3010</lala>") &&
                    fullEntry.getContent().contains("<lala>3011</lala>"));
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
        modifyEntry("aloos", "my", "3009", null, alooXml(3009,2), false, "2");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012,1), false, "1");

        feed = getPage("$join/urn:link?max-results=4&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("3009", "3010", "3011"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        feed = getPage("$join/urn:link?max-results=4&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("3012", "3013", "3014"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // here, we have to double from the requested 2 up to 4, which is then reduced as above to
        // 3 before we return.
        modifyEntry("aloos", "my", "3009", null, alooXml(3009,3), false, "3");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012,2), false, "2");

        feed = getPage("$join/urn:link?max-results=2&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("3009", "3010", "3011"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        feed = getPage("$join/urn:link?max-results=2&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("3012", "3013", "3014"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // here, we have to double TWICE from the requested 1 to 2 and then to 4, which is then
        // reduced as above to 3 before we return.
        modifyEntry("aloos", "my", "3009", null, alooXml(3009,4), false, "4");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012,3), false, "3");

        feed = getPage("$join/urn:link?max-results=1&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("3009", "3010", "3011"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        feed = getPage("$join/urn:link?max-results=1&start-index=" + endIndex);
        checkPageContainsExpectedEntries(feed, Arrays.asList("3012", "3013", "3014"));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing these "overlapping" objects should result in five entries in our aggregate feed
        modifyEntry("lalas", "my", "3017", Locale.US.toString(), lalaXml(3017,1), false, "1");
        modifyEntry("cuckoos", "my", "3014", null, cuckooXml(3014,1), false, "1");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012,4), false, "4");
        feed = getPage("$join/urn:link?start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            log.debug("ENTRY-URL:: " + entryUrl);
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(
                    fullEntry.getContent().contains("<id>3012</id>") ||
                    fullEntry.getContent().contains("<id>3013</id>") ||
                    fullEntry.getContent().contains("<id>3014</id>") ||
                    fullEntry.getContent().contains("<id>3015</id>") ||
                    fullEntry.getContent().contains("<id>3017</id>"));
        }
        feed = getPage("$join/urn:link?entry-type=full&start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            assertTrue(entry.getContent().startsWith("<aggregate"));
            assertTrue(entry.getContent().contains("<lala"));
            assertTrue(entry.getContent().contains("<cuckoo"));
            assertTrue(entry.getContent().contains("<aloo"));
            assertTrue(
                    entry.getContent().contains("<id>3012</id>") ||
                    entry.getContent().contains("<id>3013</id>") ||
                    entry.getContent().contains("<id>3014</id>") ||
                    entry.getContent().contains("<id>3015</id>") ||
                    entry.getContent().contains("<id>3017</id>"));
        }
        // by adding some "search" categories to the feed, we should be able to limit the set of results
        feed = getPage("$join/urn:link/-/(urn:group)even?start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/(urn:group)odd?start-index=" + endIndex);
        assertEquals(3, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/(urn:group)even/(urn:group)blue?start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/(urn:group)odd/(urn:group)red?start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());


        // getting the same feed as a localized feed should return the same results
        feed = getPage("$join/urn:link?locale=en_US");
        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one lala should result in a single entry in our aggregate feed
        modifyEntry("lalas", "my", "3015", Locale.US.toString(), lalaXml(3015,2), false, "2");
        feed = getPage("$join/urn:link?locale=en_US&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(fullEntry.getContent().contains("<id>3015</id>"));
            assertTrue(fullEntry.getContent().contains("<lala>3015</lala>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one cuckoo should result in two entries in our aggregate feed
        modifyEntry("cuckoos", "my", "3010", null, cuckooXml(3010,2), false, "2");
        feed = getPage("$join/urn:link?locale=en_US&start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(
                    fullEntry.getContent().contains("<id>3010</id>") ||
                    fullEntry.getContent().contains("<id>3011</id>"));
            assertTrue(
                    fullEntry.getContent().contains("<lala>3010</lala>") &&
                    fullEntry.getContent().contains("<lala>3011</lala>"));
        }
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);


        // changing these "overlapping" objects should result in five entries in our aggregate feed
        modifyEntry("lalas", "my", "3017", Locale.US.toString(), lalaXml(3017,2), false, "2");
        modifyEntry("cuckoos", "my", "3014", null, cuckooXml(3014,2), false, "2");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012,5), false, "5");
        feed = getPage("$join/urn:link?locale=en_US&start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            log.debug("ENTRY-URL:: " + entryUrl);
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
            assertTrue(
                    fullEntry.getContent().contains("<id>3012</id>") ||
                    fullEntry.getContent().contains("<id>3013</id>") ||
                    fullEntry.getContent().contains("<id>3014</id>") ||
                    fullEntry.getContent().contains("<id>3015</id>") ||
                    fullEntry.getContent().contains("<id>3017</id>"));
        }
        feed = getPage("$join/urn:link?locale=en_US&entry-type=full&start-index=" + endIndex);
        assertEquals(5, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            assertTrue(entry.getContent().startsWith("<aggregate"));
            assertTrue(entry.getContent().contains("<lala"));
            assertTrue(entry.getContent().contains("<cuckoo"));
            assertTrue(entry.getContent().contains("<aloo"));
            assertTrue(
                    entry.getContent().contains("<id>3012</id>") ||
                    entry.getContent().contains("<id>3013</id>") ||
                    entry.getContent().contains("<id>3014</id>") ||
                    entry.getContent().contains("<id>3015</id>") ||
                    entry.getContent().contains("<id>3017</id>"));
        }
        // by adding some "search" categories to the feed, we should be able to limit the set of results
        feed = getPage("$join/urn:link/-/(urn:group)even?locale=en_US&start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/(urn:group)odd?locale=en_US&start-index=" + endIndex);
        assertEquals(3, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/(urn:group)even/(urn:group)blue?locale=en_US&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/(urn:group)odd/(urn:group)red?locale=en_US&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        feed = getPage("$join/urn:link/-/OR" +
                       "/AND/(urn:group)even/(urn:group)blue" +
                       "/AND/(urn:group)odd/(urn:group)red" +
                       "?locale=en_US&start-index=" + endIndex);
        assertEquals(2, feed.getEntries().size());

        // Test that we can limit the set of workspaces included in the join.
        feed = getPage("$join(lalas,cuckoos)/urn:link");
        assertEquals(12, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertFalse(fullEntry.getContent().contains("<aloo"));
        }
        feed = getPage("$join(lalas,cuckoos)/urn:link?entry-type=full&max-results=10");
        assertEquals(10, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {            
            assertTrue(entry.getContent().startsWith("<aggregate"));
            assertTrue(entry.getContent().contains("<lala"));
            assertTrue(entry.getContent().contains("<cuckoo"));
            assertFalse(entry.getContent().contains("<aloo"));
        }

        // The following is a regression test against the bug we discovered where with a previous
        // incarnation of the SQL, we would sometimes not return all of the aggregate entries in a
        // feed because we were paginating the queries at the wrong level, and the intersection of
        // the entries that matched the join with the entries that matched the category query was
        // empty.

        // move to the end of the "even" feed
        feed = getPage("$join/urn:link/-/(urn:group)even");
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // modify all of the "odd" lalas
        for (int i = 3007; i < 3018; i+=2) {
            String entryId = "" + i;
            modifyEntry("lalas", "my", entryId, Locale.US.toString(), lalaXml(i,1000), false, "*");
        }

        // modify ONE "even" lala
        modifyEntry("lalas", "my", "3008", Locale.US.toString(), lalaXml(3008,1000), false, "*");

        // we should see the one entry in the "even" feed
        feed = getPage("$join/urn:link/-/(urn:group)even?max-results=2&start-index=" + endIndex);
        assertEquals(1, feed.getEntries().size());
        assertEquals("3008", feed.getEntries().get(0).getSimpleExtension(AtomServerConstants.ENTRY_ID));

        // the following is a regression test against the bug we discovered where entries in a
        // locale that consisted of a language only (i.e. no country) were not retrievable.  This
        // ensures that we can reference such aggregates correctly.

        // set up a single aggregate entry in the locale=de feed
        modifyEntry("lalas", "my", "90210", Locale.GERMAN.toString(), lalaXml(90210,0), true, "0");
        modifyEntry("cuckoos", "my", "90210", null, cuckooXml(90210,0), true, "0");
        modifyEntry("aloos", "my", "90210", null, alooXml(90210,0), true, "0");
        // pull the feed - should get one result
        feed = getPage("$join/urn:link?locale=de");
        assertEquals(1, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            // pull down the one and only full entry - make sure we get it and that it is complete
            String entryUrl = getServerRoot() + entry.getLink("self").getHref();
            Entry fullEntry = getEntry(entryUrl);
            assertTrue(fullEntry.getContent().startsWith("<aggregate"));
            assertTrue(fullEntry.getContent().contains("<lala"));
            assertTrue(fullEntry.getContent().contains("<cuckoo"));
            assertTrue(fullEntry.getContent().contains("<aloo"));
        }

    }

    public void testStartEndIndex() throws Exception {

        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("lalas/my");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("cuckoos/my");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("aloos/my");
        assertEquals(4, feed.getEntries().size());

        // get the aggregate feed, and mark the end index
        feed = getPage("$join/urn:link");

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
        feed = getPage("$join/urn:link?start-index=" + indexes[2] + "&end-index=" + indexes[7], 200);
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 5, feed.getEntries().size());

        List<Entry> entriesCheck = feed.getEntries();
        int lastIndex = 0;
        for (Entry  entry : entriesCheck) {
            int thisIndex = Integer.parseInt( entry.getSimpleExtension(AtomServerConstants.UPDATE_INDEX) );
            assertTrue( thisIndex > indexes[2] );
            assertTrue( thisIndex > lastIndex );
            lastIndex = thisIndex;
        }
        assertEquals( indexes[7], lastIndex);

        // end-index past the actual last index
        int biggerIndex = indexes[11] + 100000;
        feed = getPage("$join/urn:link?start-index=" + indexes[2] + "&end-index=" + biggerIndex, 200);
        log.debug( "SIZE=" + feed.getEntries().size() );
        assertEquals("Testing feed length", 9, feed.getEntries().size());

        // same one for start and end, MUST return 304 cuz can't be both exclusive and inclusive
        getPage( "$join/urn:link?start-index=" + indexes[2] + "&end-index=" + indexes[2], 304 );

        // end is before start
        getPage( "$join/urn:link?start-index=" + indexes[2] + "&end-index=" + indexes[0], 400 );
    }

    public void testUpdateMaxMin() throws Exception {

        if ( "hsql".equals(entriesDao.getDatabaseType()) ) {
            log.warn( "Aggregate Feeds do NOT currently work in HSQLDB");
            return;
        }

        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("lalas/my");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("cuckoos/my");
        assertEquals(6, feed.getEntries().size());
        feed = getPage("aloos/my");
        assertEquals(4, feed.getEntries().size());

        // get the aggregate feed, and mark the end index
        feed = getPage("$join/urn:link");

        assertEquals(12, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);
        log.debug( "endIndex= " + endIndex );

        // get max earlier than any we added
        long lnow = (entriesDao.selectSysDate()).getTime();
        String earlier = AtomDate.format( new Date( lnow - 100000 ) );

        getPage( "$join/urn:link?updated-max=" + earlier, 304 );

        // Sleep a couple of seconds, then add modify an entry
        Thread.sleep( 2000 );

        lnow = entriesDao.selectSysDate().getTime();
        String beforeLast = AtomDate.format( new Date( lnow - 50 ) );

        // changing one lala should result in a single entry in our aggregate feed
        modifyEntry("lalas", "my", "3015", Locale.US.toString(), lalaXml(3015,1), false, "1");

        // get all but the one we just modified
        feed = getPage( "$join/urn:link?updated-max=" + beforeLast, 200 );
        assertEquals("Testing feed length", 11, feed.getEntries().size());

        // get just the one we just added
        lnow = entriesDao.selectSysDate().getTime();
        String afterLast = AtomDate.format( new Date( lnow ) );

        feed = getPage( "$join/urn:link?updated-min=" + beforeLast + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 1, feed.getEntries().size());

        // get all of them
        String beforeAll = AtomDate.format( new Date( lnow - 100000 ) );
        feed = getPage( "$join/urn:link?updated-min=" + beforeAll + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 12, feed.getEntries().size());

        // changing one cuckoo should result in two entries in our aggregate feed
        lnow = entriesDao.selectSysDate().getTime();
        String beforeAnother2 = AtomDate.format( new Date( lnow ) );

        modifyEntry("cuckoos", "my", "3010", null, cuckooXml(3010,1), false, "1");

        // get just the first one we modified again
        feed = getPage( "$join/urn:link?updated-min=" + beforeLast + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 1, feed.getEntries().size());

        // get everything but the last two modified
        feed = getPage( "$join/urn:link?updated-min=" + beforeAll + "&updated-max=" + afterLast, 200 );
        assertEquals("Testing feed length", 10, feed.getEntries().size());

        // get only the last two modified
        lnow = entriesDao.selectSysDate().getTime();
        String afterAnother2 = AtomDate.format( new Date( lnow ) );

        feed = getPage( "$join/urn:link?updated-min=" + beforeAnother2 + "&updated-max=" + afterAnother2, 200 );
        assertEquals("Testing feed length", 2, feed.getEntries().size());
    }

    private void checkPageContainsExpectedEntries(Feed feed, List<String> expected) {
        assertEquals(expected.size(), feed.getEntries().size());
        for (int i = 0; i < expected.size(); i++) {
            String entryId = feed.getEntries().get(i).getSimpleExtension(AtomServerConstants.ENTRY_ID);
            assertTrue(expected.contains(entryId));
        }
    }

    @SuppressWarnings("unused")
	private static void dumpToFile(Base object) throws IOException {
        FileWriter fileWriter = new FileWriter("buff.xml");
        object.writeTo(fileWriter);
        fileWriter.close();
    }

    private static String lalaXml(int id, int revNo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<lala xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        stringBuilder.append("<rev>").append(revNo).append("</rev>");
        stringBuilder.append("<group>").append(id % 2 == 0 ? "even" : "odd").append("</group>");
        stringBuilder.append("</lala>");
        return stringBuilder.toString();
    }

    private static String cuckooXml(int id, int revNo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<cuckoo xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<lala>").append(id).append("</lala>");
        stringBuilder.append("<lala>").append(id + 1).append("</lala>");
        stringBuilder.append("<rev>").append(revNo).append("</rev>");
        stringBuilder.append("<group>").append(id % 3 == 0 ? "red" : "blue").append("</group>");
        stringBuilder.append("</cuckoo>");
        return stringBuilder.toString();
    }

    private static String alooXml(int id, int revNo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<aloo xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<lala>").append(id).append("</lala>");
        stringBuilder.append("<lala>").append(id + 1).append("</lala>");
        stringBuilder.append("<lala>").append(id + 2).append("</lala>");
        stringBuilder.append("<rev>").append(revNo).append("</rev>");
        stringBuilder.append("<group>").append(id % 5 == 0 ? "heavy" : "light").append("</group>");
        stringBuilder.append("</aloo>");
        return stringBuilder.toString();
    }
}
