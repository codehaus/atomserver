package org.atomserver.core.dbstore;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.i18n.iri.IRI;

import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.testutils.latency.LatencyUtil;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.cache.AggregateFeedCacheManager;
import org.atomserver.ext.batch.Operation;
import org.atomserver.ext.batch.Results;
import org.atomserver.ext.batch.Status;
import org.atomserver.uri.EntryTarget;
import org.atomserver.AtomService;
import org.springframework.context.ApplicationContext;
import org.atomserver.uri.URIHandler;
import java.util.*;
import java.text.MessageFormat;


/**
 * Test AggregateFeeds using Cached timestamps. The test perform batch inserts, updates and deletes.
 * Aggregate feed queries are made after each operation to validate that the cache is accordingly
 * updated.
 *
 */
public class CachedAggregateFeedsBatchTest extends DBSTestCase {

    AggregateFeedCacheManager cacheManager = null;
    List<String> cachedFeedIds = null;
    URIHandler entryURIHelper = null;

    public void setUp() throws Exception {
        TestConfUtil.preSetup("aggregates3");
        super.setUp();

        ApplicationContext appCtx = super.getSpringFactory();
        cacheManager = (AggregateFeedCacheManager) appCtx.getBean("org.atomserver-aggregatefeedcachemanager");
        entryURIHelper = ((AtomService) appCtx.getBean("org.atomserver-atomService")).getURIHandler();

        // aggregate feeds of interest
        List<String> feedList = new ArrayList<String>();
        feedList.add("$join(reds,greens), urn:hue, en_US");
        feedList.add("$join(reds,greens), urn:hue");
        feedList.add("$join,urn:hue,en_US");
        feedList.add("$join,urn:hue");
        feedList.add("$join(reds,purples), urn:hue");
        feedList.add("$join(reds,purples), urn:rgbcode");

        // clear cache from previous state
        cacheManager.removeCachedAggregateFeeds(feedList);

        entryCategoriesDAO.deleteAllEntryCategories("reds");
        entryCategoriesDAO.deleteAllEntryCategories("greens");
        entryCategoriesDAO.deleteAllEntryCategories("purples");
        
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("reds"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("greens"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("purples"));

         // workspace blues is not used in this test but needs to be cleaned up.
        entryCategoriesDAO.deleteAllEntryCategories("blues");
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("blues"));

        // add feeds to cache
        cachedFeedIds = cacheManager.cacheAggregateFeed(feedList);

        for (int i = 6006; i < 6018; i++) {
            String entryId = "" + i;
            modifyEntry("reds", "shades", entryId, Locale.US.toString(), redXml(i), true, "0");
            if (i % 2 == 0) {
                modifyEntry("greens", "shades", entryId, null, greenXml(i), true, "0");
            }
        }
    }

    public void tearDown() throws Exception {
        cacheManager.removeCachedAggregateFeedsByFeedIds(cachedFeedIds);
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testCachedCRUDBatch() throws Exception {

        String [] expectedEntryList = new String [] {
            "/atomserver/v1/$join(reds,purples)/urn:hue/6006.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6007.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6008.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6009.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6010.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6011.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6012.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6013.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6014.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6015.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6016.xml",
            "/atomserver/v1/$join(reds,purples)/urn:hue/6017.xml"
        };
        Set<String> expectedSet = new HashSet<String>(Arrays.asList(expectedEntryList));

        // 1. Test Batch Inserts
        runInsertsOnly();

        // Get an aggregate join to exercise the cache
        Feed feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index=0");
        List<Entry> entries = feed.getEntries();
        assertEquals(12, entries.size());

        Set<String> actualEntries = new HashSet<String>();
        for(Entry e: entries) {
            actualEntries.add(e.getId().getPath());
        }
        assertTrue(expectedSet.equals(actualEntries));
        String endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // 2. Test Batch Updates
        runUpdatesOnly();

        // Test cache by doing an aggregate query
        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index="+ endIndex);
        assertEquals(3, feed.getEntries().size());

        // Test cache by doing an aggregate query
        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index=0");
        entries = feed.getEntries();
        assertEquals(12, entries.size());

        actualEntries.clear();
        for(Entry e: entries) {
            actualEntries.add(e.getId().getPath());
        }
        assertTrue(expectedSet.equals(actualEntries));
        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // 3. Batch InsertsAndUpdates
        runInsertsAndUpdates();

        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index="+ endIndex);
        assertEquals(5, feed.getEntries().size());

        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index=0");
        entries = feed.getEntries();
        assertEquals(14,entries.size());

        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        // 4. Batch Deletes
        runDeletesOnly();

        // Do an aggregate query and validate
        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index=" + endIndex);
        entries = feed.getEntries();
        assertEquals(2, entries.size());

        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index=0");
        entries = feed.getEntries();
        assertEquals(14,entries.size());

        // 5. Test Obliterate with cached aggregates.
        feed = doGetPage("$join(reds,purples)/urn:rgbcode?entry-type=full&start-index=0");
        assertEquals(5, feed.getEntries().size());
        removeEntriesFromWorkspace();
        feed = doGetPage("$join(reds,purples)/urn:rgbcode?entry-type=full&start-index=0", 304);
        assertNull(feed);
        feed = doGetPage("$join(reds,purples)/urn:hue?entry-type=full&start-index=0");
        assertTrue(feed.getEntries().size() > 0);
    }

    private Feed doGetPage(String aggQuery) throws Exception {
        LatencyUtil.accountForLatency();
        return getPage(aggQuery);
    }

    private Feed doGetPage(String aggQuery, int statusCode) throws Exception {
        LatencyUtil.accountForLatency();
        return getPage(aggQuery, statusCode);
    }

    private void runInsertsOnly() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "purples/shades/$batch";
        Feed batch = getFactory().newFeed();

        // create entries for workspace "purples"
        int inserts = 0;
        for (int i = 6006; i < 6018; i++) {
            if (i % 4 == 0) {
                Entry entry = createInsertEntry(i, "inserted entry " + i);
                batch.addEntry(entry);
                inserts++;
            }
        }
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, inserts, 0, 0, 0);
        LatencyUtil.updateLastWrote();
    }

    public void runUpdatesOnly() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "purples/shades/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testUpdatesOnly()";
        
        batch.addEntry(createOplessEntry(6008, updateText, 1));
        batch.addEntry(createOplessEntry(6012, updateText, 1));
        batch.addEntry(createOplessEntry(6016, updateText, 1));

        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 0, 3, 0, 0);

        clientResponse.release();
        LatencyUtil.updateLastWrote();
    }

    private void  runInsertsAndUpdates() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "purples/shades/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testUpdatesOnly()";

        batch.addEntry(createOplessEntry(6012, updateText, 2));
        batch.addEntry(createOplessEntry(6016, updateText, 2));
        batch.addEntry(createInsertEntry(6020, updateText));
        batch.addEntry(createOplessEntry(6008, updateText, 2));
        batch.addEntry(createInsertEntry(6024, updateText));

        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 2, 3, 0, 0);
        assertEquals(batch.getEntries().size(), response.getEntries().size());
        clientResponse.release();

        LatencyUtil.updateLastWrote();
    }

        // Deletes
    private void runDeletesOnly() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "purples/shades/$batch";
        Feed batch = getFactory().newFeed();

        batch.addEntry(createDeleteEntry(6008, 3));
        batch.addEntry(createDeleteEntry(6012, 3));

        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 0, 0, 2, 0);
        assertEquals(batch.getEntries().size(), response.getEntries().size());
        clientResponse.release();

        LatencyUtil.updateLastWrote();
    }

    private void removeEntriesFromWorkspace() {
        // Destroy all from "purples" workspace 
        String [] entryIds = new String [] { "6008", "6012", "6016", "6020", "6024"};
        for (String e: entryIds) {
            String propId = e;
            IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString("purples", "shades", propId, null));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET",
                                                                                           iri.toString()), true);
            EntryMetaData md = entriesDao.selectEntry(entryTarget);
//            entryCategoriesDAO.deleteEntryCategories(entriesDao.selectEntry(entryTarget));
            contentStorage.deleteContent(null, entryTarget);
            entriesDao.obliterateEntry(entryTarget);
        }
        LatencyUtil.updateLastWrote();
    }

    private ClientResponse runBatch(AbderaClient client,
                                    String batchURI,
                                    Feed batch,
                                    int expectedStatus) throws Exception {
        ClientResponse clientResponse = client.put(batchURI, batch);
        assertEquals(expectedStatus, clientResponse.getStatus());
        return clientResponse;
    }

    private Entry createInsertEntry(int intId, String text) {
        String id = Integer.toString(intId);
        Entry entry = getFactory().newEntry();
        entry.setContentAsXhtml(purpleXml(intId, text));
        ((Operation) entry.addExtension(AtomServerConstants.OPERATION)).setType("update");
        entry.addLink(MessageFormat.format("/" + getBaseURI() + "/purples/shades/{0}.xml/{1}", id, 0), "edit");
        entry.setId(""+ id);
        return entry;
    }

    private Entry createOplessEntry(int intId, String text, int revision) {
        String id = Integer.toString(intId);
        Entry entry = getFactory().newEntry();
        if (text != null) {
            entry.setContentAsXhtml(purpleXml(intId, text));
        }
        entry.addLink(MessageFormat.format("/" + getBaseURI() + "/purples/shades/{0}.xml/{1}", id, revision), "edit");
        return entry;
    }

    private Entry createDeleteEntry(int intId, int revision) {
        String id = Integer.toString(intId);
        Entry entry = getFactory().newEntry();
        ((Operation) entry.addExtension(AtomServerConstants.OPERATION)).setType("delete");
        entry.addLink(MessageFormat.format("/" + getBaseURI() + "/purples/shades/{0}.xml/{1}", id, revision), "edit");
        return entry;
    }

    private void checkFeedResults(Feed feed, int inserts, int updates, int deletes, int errors) {
        Results results = feed.getExtension(AtomServerConstants.RESULTS);
        assertEquals(inserts, results.getInserts());
        assertEquals(updates, results.getUpdates());
        assertEquals(deletes, results.getDeletes());
        assertEquals(errors, results.getErrors());
   }

    private static String redXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<red xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<id>").append(id).append("</id>");
        stringBuilder.append("<group>").append(id % 2 == 0 ? "even" : "odd").append("</group>");
        stringBuilder.append("</red>");
        return stringBuilder.toString();
    }

    private static String greenXml(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<green xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<red>").append(id).append("</red>");
        stringBuilder.append("<red>").append(id + 1).append("</red>");
        stringBuilder.append("<group>").append(id % 3 == 0 ? "reds" : "purples").append("</group>");
        stringBuilder.append("</green>");
        return stringBuilder.toString();
    }

    private static String purpleXml(int intId, String text) {
        String id = Integer.toString(intId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<purple xmlns='http://schemas.atomserver.org/aggregate-tests'>");
        stringBuilder.append("<red>").append(id).append("</red>");
        stringBuilder.append("<group>").append( intId % 8 == 0 ? "pale" : "bright").append("</group>");
        stringBuilder.append("<rgb>").append(hexRGB(Integer.toHexString(intId))).append("</rgb>");
        stringBuilder.append("<displayName>").append(text).append("</displayName>");
        stringBuilder.append("</purple>");
        return stringBuilder.toString();
    }

    private static String hexRGB(String hex) {
        return  "0x" + ("000000" + hex).substring(hex.length());
    }

}
