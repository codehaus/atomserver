package org.atomserver.core.dbstore;

import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Base;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.etc.AtomServerConstants;

import java.util.Locale;
import java.io.FileWriter;
import java.io.IOException;

public class AggregateFeedsTest extends DBSTestCase {

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates");
        super.setUp();

        entryCategoriesDAO.deleteAllEntryCategories("lalas");
        entryCategoriesDAO.deleteAllEntryCategories("cuckoos");
        entryCategoriesDAO.deleteAllEntryCategories("aloos");
        
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("lalas"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("cuckoos"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("aloos"));

        for (int i = 3000; i < 3024; i++) {
            String entryId = "" + i;
            modifyEntry("lalas", "my", entryId, Locale.US.toString(), lalaXml(i), true, "0");
            if (i % 2 == 0) {
                modifyEntry("cuckoos", "my", entryId, null, cuckooXml(i), true, "0");
            }
            if (i % 3 == 0) {
                modifyEntry("aloos", "my", entryId, null, alooXml(i), true, "0");
            }
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testAggregateFeeds() throws Exception {
        Feed feed;
        String endIndex;

        // first, check that the individual entry feeds are the size we expect:
        feed = getPage("lalas/my");
        assertEquals(24, feed.getEntries().size());
        feed = getPage("cuckoos/my");
        assertEquals(12, feed.getEntries().size());
        feed = getPage("aloos/my");
        assertEquals(8, feed.getEntries().size());

        // get the aggregate feed, and mark the end index
        feed = getPage("$join/urn:link");
        assertEquals(24, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // getting the next page should return a 304 NOT MODIFIED
        getPage("$join/urn:link?start-index=" + endIndex, 304);

        // changing one lala should result in a single entry in our aggregate feed
        modifyEntry("lalas", "my", "3015", Locale.US.toString(), lalaXml(3015), false, "0");
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
        modifyEntry("cuckoos", "my", "3010", null, cuckooXml(3010), false, "0");
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
        modifyEntry("aloos", "my", "3009", null, alooXml(3009), false, "0");
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

        // changing these "overlapping" objects should result in five entries in our aggregate feed
        modifyEntry("lalas", "my", "3017", Locale.US.toString(), lalaXml(3017), false, "0");
        modifyEntry("cuckoos", "my", "3014", null, cuckooXml(3014), false, "0");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012), false, "0");
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
        assertEquals(24, feed.getEntries().size());
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // changing one lala should result in a single entry in our aggregate feed
        modifyEntry("lalas", "my", "3015", Locale.US.toString(), lalaXml(3015), false, "1");
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
        modifyEntry("cuckoos", "my", "3010", null, cuckooXml(3010), false, "1");
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
        modifyEntry("lalas", "my", "3017", Locale.US.toString(), lalaXml(3017), false, "1");
        modifyEntry("cuckoos", "my", "3014", null, cuckooXml(3014), false, "1");
        modifyEntry("aloos", "my", "3012", null, alooXml(3012), false, "1");
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


        feed = getPage("$join(lalas,cuckoos)/urn:link");
        assertEquals(24, feed.getEntries().size());
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
    }

    private static void dumpToFile(Base object) throws IOException {
        FileWriter fileWriter = new FileWriter("buff.xml");
        object.writeTo(fileWriter);
        fileWriter.close();
    }

    private static String lalaXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<lala xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        stringBuilder.append("<group>").append(id % 2 == 0 ? "even" : "odd").append("</group>");
        stringBuilder.append("</lala>");
        return stringBuilder.toString();
    }

    private static String cuckooXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<cuckoo xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<lala>").append(id).append("</lala>");
        stringBuilder.append("<lala>").append(id + 1).append("</lala>");
        stringBuilder.append("<group>").append(id % 3 == 0 ? "red" : "blue").append("</group>");
        stringBuilder.append("</cuckoo>");
        return stringBuilder.toString();
    }

    private static String alooXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<aloo xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<lala>").append(id).append("</lala>");
        stringBuilder.append("<lala>").append(id + 1).append("</lala>");
        stringBuilder.append("<lala>").append(id + 2).append("</lala>");
        stringBuilder.append("<group>").append(id % 5 == 0 ? "heavy" : "light").append("</group>");
        stringBuilder.append("</aloo>");
        return stringBuilder.toString();
    }
}
