package org.atomserver;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;

public class PublishTest extends BaseAtomServerTestCase {
    private static final Logger log = Logger.getLogger(PublishTest.class);

    @Before
    public void setupTestService() throws Exception {
        // before each test, set up the test service
        root().type(MediaType.APPLICATION_XML)
                .entity(parse("org/atomserver/atomserver-test-service.xml"))
                .post(ClientResponse.class);
    }




    @Test
    public void testEntryPublishingBasics() throws Exception {
        ClientResponse response;
        Entry responseEntry;


        Entry widgetEntry = createWidgetEntry(1234, "red", "red widget");

        WebResource acme = root().path("atomserver-test").path("widgets").path("acme");
        WebResource categorized = acme.path("-").path("(urn:test.scheme)test.term");


        // TEST PUTTING AN ENTRY
        response =
                acme.path("1234")
                        .type(MediaType.APPLICATION_ATOM_XML)
                        .accept(MediaType.APPLICATION_ATOM_XML)
                        .entity(widgetEntry)
                        .put(ClientResponse.class);
        assertEquals("initial publish of an entry should return an HTTP 201 (CREATED)",
                     201,
                     response.getStatus());
        responseEntry = response.getEntity(Entry.class);
        assertEquals("expected to get the correct ID back",
                     "atomserver-test/widgets/acme/1234",
                     responseEntry.getId().toString());
        assertEquals("expected to get our content back",
                     widgetEntry.getContent(),
                     responseEntry.getContent());
        assertEquals("expected to get the requested Content-Type",
                     MediaType.APPLICATION_ATOM_XML,
                     response.getMetadata().getFirst("Content-Type"));
        assertTrue("expected MD5 ETag Header",
                   response.getEntityTag().getValue().matches("[a-f0-9]{32}"));
        assertEquals("expected MD5 Etag Element",
                     response.getEntityTag().getValue(),
                     responseEntry.getSimpleExtension(AtomServerConstants.ETAG));

        checkCategoryPresent(responseEntry, false);
        checkAggregatePresent(responseEntry);


        checkForEntriesInFeed(acme, "atomserver-test/widgets/acme/1234");
        checkForEntriesInFeed(categorized, "atomserver-test/widgets/acme/1234");
        checkForEntriesInFeed(root().path("atomserver-test").path("$join").path("test.agg"),
                              "atomserver-test/$join/test.agg/myagg");

        // TEST RE-PUBLISHING THE ENTRY
        response =
                acme.path("1234")
                        .accept(MediaType.APPLICATION_ATOM_XML)
                        .type(MediaType.APPLICATION_ATOM_XML)
                        .entity(widgetEntry)
                        .put(ClientResponse.class);
        assertEquals("overwrite of an entry should return an HTTP 200 (OK)",
                     200,
                     response.getStatus());
        responseEntry = response.getEntity(Entry.class);
        assertEquals("expected to get the correct ID back",
                     "atomserver-test/widgets/acme/1234",
                     responseEntry.getId().toString());
        assertEquals("expected to get our content back",
                     widgetEntry.getContent(),
                     responseEntry.getContent());
        assertEquals("expected to get the requested Content-Type",
                     MediaType.APPLICATION_ATOM_XML,
                     response.getMetadata().getFirst("Content-Type"));
        assertTrue("expected MD5 ETag Header",
                   response.getEntityTag().getValue().matches("[a-f0-9]{32}"));
        assertEquals("expected MD5 Etag Element",
                     response.getEntityTag().getValue(),
                     responseEntry.getSimpleExtension(AtomServerConstants.ETAG));
        checkCategoryPresent(responseEntry, false);
        checkAggregatePresent(responseEntry);
        checkForEntriesInFeed(acme, "atomserver-test/widgets/acme/1234");
        checkForEntriesInFeed(categorized, "atomserver-test/widgets/acme/1234");

        // TEST POSTING AN ENTRY
        response =
                acme
                        .accept(MediaType.APPLICATION_ATOM_XML)
                        .type(MediaType.APPLICATION_ATOM_XML)
                        .entity(widgetEntry)
                        .post(ClientResponse.class);
        assertEquals("POSTing an entry to a collection should return HTTP 201 (CREATED)",
                     201,
                     response.getStatus());
        responseEntry = response.getEntity(Entry.class);
        assertTrue("expected to get a generated ID back",
                   responseEntry.getId().toString()
                           .matches("atomserver-test/widgets/acme/\\w{32}"));
        assertEquals("expected to get our content back",
                     widgetEntry.getContent(),
                     responseEntry.getContent());
        assertEquals("expected to get the requested Content-Type",
                     MediaType.APPLICATION_ATOM_XML,
                     response.getMetadata().getFirst("Content-Type"));
        assertTrue("expected MD5 ETag Header",
                   response.getEntityTag().getValue().matches("[a-f0-9]{32}"));
        assertEquals("expected MD5 Etag Element",
                     response.getEntityTag().getValue(),
                     responseEntry.getSimpleExtension(AtomServerConstants.ETAG));

        checkCategoryPresent(responseEntry, false);
        checkAggregatePresent(responseEntry);
        checkForEntriesInFeed(acme,
                              "atomserver-test/widgets/acme/1234",
                              responseEntry.getId().toString());
        checkForEntriesInFeed(categorized,
                              "atomserver-test/widgets/acme/1234",
                              responseEntry.getId().toString());
    }

//    @Test
    public void testOptimisticConcurrency() throws Exception {
/*
        ClientResponse response;

        WebResource acme = root().path("atomserver-test").path("widgets").path("acme");

        Entry initialVersion = createWidgetEntry(1234, "red", "red widget");

        // POST THE ENTRY TO GET STARTED
        response = acme
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity(initialVersion)
                .post(ClientResponse.class);

        assertEquals("POSTing an entry to a collection should return HTTP 201 (CREATED)",
                     201,
                     response.getStatus());
        Entry responseEntry = response.getEntity(Entry.class);
        String entryId = responseEntry.getSimpleExtension(AtomServerConstants.ENTRY_ID);
        System.out.println("entryId = " + entryId);
        String etag = responseEntry.getSimpleExtension(AtomServerConstants.ETAG);
        System.out.println("etag = " + etag);

        Entry updatedVersion = createWidgetEntry(1234, "blue", "blue widget");

        response = acme.path(entryId)
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity(updatedVersion)
                .put(ClientResponse.class);

        assertEquals(200, response.getStatus());

        responseEntry = response.getEntity(Entry.class);
        entryId = responseEntry.getSimpleExtension(AtomServerConstants.ENTRY_ID);
        System.out.println("entryId = " + entryId);
        etag = responseEntry.getSimpleExtension(AtomServerConstants.ETAG);
        System.out.println("etag = " + etag);
 */


        // This test should test out the various Optimistic Concurrency cases:
        // -  ETags can be provided in either a header or an <as:etag/> element
        // -  If BOTH methods of providing ETags are provided, they MUST match!
        // -  If the ETag is omitted, the write fails
        // -  If the Etag does not match, the write fails
        // -  If the ETag is *, then Optimistic Concurrency is ignored for the request, and the
        //    write succeeds
        // -  If the Etag matches, then the write succeeds
        //
        // given all that, we need to check:
        //
        // PUBLISH WITH NO ETAG
        //      publish should fail (409)
        // PUBLISH WITH WRONG ETAG IN HEADER ONLY
        //      publish should fail (409)
        // PUBLISH WITH WRONG ETAG IN XML ONLY
        //      publish should fail (409)
        // PUBLISH WITH WRONG ETAG IN BOTH
        //      publish should fail (409)
        // PUBLISH WITH ONE RIGHT AND ONE WRONG (HEADER AND XML)
        //      publish should fail (400)
        // PUBLISH WITH RIGHT ETAG IN HEADER ONLY
        //      publish should succeed
        // PUBLISH WITH RIGHT ETAG IN XML ONLY
        //      publish should succeed
        // PUBLISH WITH RIGHT ETAG IN BOTH
        //      publish should succeed
        // PUBLISH WITH OVERRIDE IN HEADER ONLY
        //      publish should succeed
        // PUBLISH WITH OVERRIDE IN XML ONLY
        //      publish should succeed
        // PUBLISH WITH OVERRIDE IN BOTH (HEADER AND XML)
        //      publish should succeed
        // PUBLISH WITH OVERRIDE/ETAG MISMATCH
        //      publish should fail (400).
    }


    private void checkAggregatePresent(Entry entry) {
        assertEquals("expected our aggregate marker to be preserved",
                     1,
                     entry.getExtensions(AtomServerConstants.AGGREGATE).size());
        assertEquals("expected our aggregate marker to be preserved",
                     AGGREGATE,
                     entry.getExtensions(AtomServerConstants.AGGREGATE).get(0));
    }

    private void checkCategoryPresent(Entry entry, boolean ignoreNullLabel) {
        assertEquals("expected our category to be preserved",
                     1,
                     entry.getCategories().size());
        assertCategoriesEqual("expected to preserve the category applied to the entry",
                              CATEGORY,
                              entry.getCategories().get(0), ignoreNullLabel);
    }

    private void checkForEntriesInFeed(WebResource feedResource,
                                       String... expectedIds) throws Exception {
        ClientResponse response =
                feedResource.type(MediaType.APPLICATION_ATOM_XML).get(ClientResponse.class);
        Feed feed = response.getEntity(Feed.class);

        assertTrue("expected MD5 ETag Header",
                   response.getEntityTag().getValue().matches("[a-f0-9]{32}"));
        assertEquals("expected MD5 Etag Element",
                     response.getEntityTag().getValue(),
                     feed.getSimpleExtension(AtomServerConstants.ETAG));
        assertEquals("wrong number of entries returned in feed",
                     expectedIds.length,
                     feed.getEntries().size());
        for (int i = 0; i < expectedIds.length; i++) {
            Entry entry = feed.getEntries().get(i);
            assertEquals(String.format("expected to get [%s] at position [%s] in the feed",
                                       expectedIds[i], i),
                         expectedIds[i],
                         entry.getId().toString());
            boolean isAggregateFeed = feedResource.toString().contains("$join");
            checkCategoryPresent(entry, isAggregateFeed);
            if (!isAggregateFeed) {
                checkAggregatePresent(entry);
            }
            assertTrue("expected MD5 ETag Header",
                       entry.getSimpleExtension(AtomServerConstants.ETAG).matches("[a-f0-9]{32}"));
        }
    }
}
