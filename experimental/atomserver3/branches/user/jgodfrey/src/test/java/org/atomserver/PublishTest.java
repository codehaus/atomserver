package org.atomserver;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.log4j.Logger;
import static org.atomserver.AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.UUID;

public class PublishTest extends BaseAtomServerTestCase {
    private static final Logger log = Logger.getLogger(PublishTest.class);
    private static final String WRONG_ETAG = UUID.randomUUID().toString().replaceAll("\\W", "");

    @Before
    public void setupTestService() throws Exception {
        // before each test, set up the test service
        root().path("atomserver-test").type(MediaType.APPLICATION_XML)
                .entity(parse("org/atomserver/atomserver-test-service.xml"))
                .put(ClientResponse.class);
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
                        .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
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

        checkCategoryPresent(responseEntry);

        checkForEntriesInFeed(acme, "atomserver-test/widgets/acme/1234");
        checkForEntriesInFeed(categorized, "atomserver-test/widgets/acme/1234");

        // TEST RE-PUBLISHING THE ENTRY
        response =
                acme.path("1234")
                        .accept(MediaType.APPLICATION_ATOM_XML)
                        .type(MediaType.APPLICATION_ATOM_XML)
                        .entity(widgetEntry)
                        .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
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
        checkCategoryPresent(responseEntry);
        checkForEntriesInFeed(acme, "atomserver-test/widgets/acme/1234");
        checkForEntriesInFeed(categorized, "atomserver-test/widgets/acme/1234");

        // TEST PUTTING AN ENTRY WITH INLINE CONTENT
        Entry inlineWidgetEntry = createWidgetEntry(2345, "red", "red widget", true);
        response =
                acme.path("2345")
                        .type(MediaType.APPLICATION_ATOM_XML)
                        .accept(MediaType.APPLICATION_ATOM_XML)
                        .entity(inlineWidgetEntry)
                        .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                        .put(ClientResponse.class);
        assertEquals("initial publish of an entry should return an HTTP 201 (CREATED)",
                     201,
                     response.getStatus());
        responseEntry = response.getEntity(Entry.class);
        assertEquals("expected to get the correct ID back",
                     "atomserver-test/widgets/acme/2345",
                     responseEntry.getId().toString());
        assertEquals("expected to get our content back",
                     inlineWidgetEntry.getContentSrc(),
                     responseEntry.getContentSrc());
        assertEquals("expected to get the requested Content-Type",
                     MediaType.APPLICATION_ATOM_XML,
                     response.getMetadata().getFirst("Content-Type"));
//        assertTrue("expected MD5 ETag Header",
//                   response.getEntityTag().getValue().matches("[a-f0-9]{32}"));
//        assertEquals("expected MD5 Etag Element",
//                     response.getEntityTag().getValue(),
//                     responseEntry.getSimpleExtension(AtomServerConstants.ETAG));

        checkCategoryPresent(responseEntry);

        checkForEntriesInFeed(acme,
                "atomserver-test/widgets/acme/1234", "atomserver-test/widgets/acme/2345");
        checkForEntriesInFeed(categorized,
                "atomserver-test/widgets/acme/1234", "atomserver-test/widgets/acme/2345");

        // TEST POSTING AN ENTRY
        response =
                acme
                        .accept(MediaType.APPLICATION_ATOM_XML)
                        .type(MediaType.APPLICATION_ATOM_XML)
                        .entity(widgetEntry)
                        .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
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

        checkCategoryPresent(responseEntry);
        checkForEntriesInFeed(acme,
                              "atomserver-test/widgets/acme/1234",
                              "atomserver-test/widgets/acme/2345",
                              responseEntry.getId().toString());
        checkForEntriesInFeed(categorized,
                              "atomserver-test/widgets/acme/1234",
                              "atomserver-test/widgets/acme/2345",
                              responseEntry.getId().toString());

        assertEquals(1, widgetEntry.getCategories().size());
    }

    @Test
    public void testErrorConditions() throws Exception {

        ClientResponse response;

        WebResource acme = root().path("atomserver-test").path("widgets").path("acme");

        response = acme
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity("")
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .post(ClientResponse.class);

        assertEquals("expected a 400 BAD REQUEST when POSTing empty request body",
                     400,
                     response.getStatus());
        assertEquals("expected an appropriate error message",
                     "Empty request body is not valid for this request.",
                     response.getEntity(String.class));

        response = acme.path("1234")
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity("")
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .put(ClientResponse.class);

        assertEquals("expected a 400 BAD REQUEST when PUTting empty request body",
                     400,
                     response.getStatus());
        assertEquals("expected an appropriate error message",
                     "Empty request body is not valid for this request.",
                     response.getEntity(String.class));

        response = acme
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity("<entry xmlns='http://www.w3.org/2005/Atom'></ent>")
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .post(ClientResponse.class);

        assertEquals("expected a 400 BAD REQUEST when POSTing empty request body",
                     400,
                     response.getStatus());
        assertEquals("expected an appropriate error message",
                     "Unable to parse a valid object from request entity.",
                     response.getEntity(String.class));

        response = acme.path("1234")
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity("<entry xmlns='http://www.w3.org/2005/Atom'></ent>")
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .put(ClientResponse.class);

        assertEquals("expected a 400 BAD REQUEST when PUTting empty request body",
                     400,
                     response.getStatus());
        assertEquals("expected an appropriate error message",
                     "Unable to parse a valid object from request entity.",
                     response.getEntity(String.class));

        response = acme
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity("<entry xmlns='http://www.w3.org/2005/Atom'>" +
                        "<content type='text/plain'>INVALID</content>" +
                        "</entry>")
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .post(ClientResponse.class);

        assertEquals("expected a 400 BAD REQUEST when POSTing empty request body",
                     400,
                     response.getStatus());
        assertEquals("expected an appropriate error message",
                     "Invalid content!",
                     response.getEntity(String.class));

        response = acme.path("1234")
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity("<entry xmlns='http://www.w3.org/2005/Atom'>" +
                        "<content type='text/plain'>INVALID</content>" +
                        "</entry>")
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .put(ClientResponse.class);

        assertEquals("expected a 400 BAD REQUEST when PUTting empty request body",
                     400,
                     response.getStatus());
        assertEquals("expected an appropriate error message",
                     "Invalid content!",
                     response.getEntity(String.class));
    }

    @Test
    public void testOptimisticConcurrency() throws Exception {

        ClientResponse response;

        WebResource acme = root().path("atomserver-test").path("widgets").path("acme");

        Entry initialVersion = createWidgetEntry(1234, "red", "red widget");

        // POST THE ENTRY TO GET STARTED
        response = acme
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity(initialVersion)
                .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                .post(ClientResponse.class);

        assertEquals("POSTing an entry to a collection should return HTTP 201 (CREATED)",
                     201,
                     response.getStatus());
        Entry responseEntry = response.getEntity(Entry.class);
        String entryId = responseEntry.getSimpleExtension(AtomServerConstants.ENTRY_ID);
        String etag = responseEntry.getSimpleExtension(AtomServerConstants.ETAG);

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
        etag = putNextEntry(acme, entryId, etag, null, null, 409);
        // PUBLISH WITH WRONG ETAG IN HEADER ONLY
        //      publish should fail (409)
        etag = putNextEntry(acme, entryId, etag, WRONG_ETAG, null, 409);
        // PUBLISH WITH WRONG ETAG IN XML ONLY
        //      publish should fail (409)
        etag = putNextEntry(acme, entryId, etag, null, WRONG_ETAG, 409);
        // PUBLISH WITH WRONG ETAG IN BOTH
        //      publish should fail (409)
        etag = putNextEntry(acme, entryId, etag, WRONG_ETAG, WRONG_ETAG, 409);
        // PUBLISH WITH ONE RIGHT AND ONE WRONG (HEADER AND XML)
        //      publish should fail (400)
        etag = putNextEntry(acme, entryId, etag, etag, WRONG_ETAG, 400);
        // PUBLISH WITH RIGHT ETAG IN HEADER ONLY
        //      publish should succeed
        etag = putNextEntry(acme, entryId, etag, etag, null, 200);
        // PUBLISH WITH RIGHT ETAG IN XML ONLY
        //      publish should succeed
        etag = putNextEntry(acme, entryId, etag, null, etag, 200);
        // PUBLISH WITH RIGHT ETAG IN BOTH
        //      publish should succeed
        etag = putNextEntry(acme, entryId, etag, etag, etag, 200);
        // PUBLISH WITH OVERRIDE IN HEADER ONLY
        //      publish should succeed
        etag = putNextEntry(acme, entryId, etag,
                            OPTIMISTIC_CONCURRENCY_OVERRIDE, null, 200);
        // PUBLISH WITH OVERRIDE IN XML ONLY
        //      publish should succeed
        etag = putNextEntry(acme, entryId, etag,
                            null, OPTIMISTIC_CONCURRENCY_OVERRIDE, 200);
        // PUBLISH WITH OVERRIDE IN BOTH (HEADER AND XML)
        //      publish should succeed
        etag = putNextEntry(acme, entryId, etag,
                            OPTIMISTIC_CONCURRENCY_OVERRIDE, OPTIMISTIC_CONCURRENCY_OVERRIDE, 200);
        // PUBLISH WITH OVERRIDE/ETAG MISMATCH
        //      publish should fail (400).
        etag = putNextEntry(acme, entryId, etag,
                            etag, OPTIMISTIC_CONCURRENCY_OVERRIDE, 400);

        // and finally, just make sure we can overwrite it one last time.
        putNextEntry(acme, entryId, etag, etag, null, 200);
    }

    private String putNextEntry(WebResource acme,
                                String entryId,
                                String correctEtag,
                                String headerEtag,
                                String xmlEtag,
                                int expectedStatus) throws Exception {
        ClientResponse response;
        Entry updatedVersion = createWidgetEntry(1234, "blue", UUID.randomUUID().toString());

        WebResource.Builder builder = acme.path(entryId)
                .accept(MediaType.APPLICATION_ATOM_XML)
                .type(MediaType.APPLICATION_ATOM_XML)
                .entity(updatedVersion);

        if (headerEtag != null) {
            builder.header("ETag", headerEtag);
        }

        if (xmlEtag != null) {
            updatedVersion.addSimpleExtension(AtomServerConstants.ETAG, xmlEtag);
        }

        response = builder.put(ClientResponse.class);

        assertEquals(expectedStatus, response.getStatus());

        return 200 == expectedStatus ?
               response.getEntity(Entry.class).getSimpleExtension(AtomServerConstants.ETAG) :
               correctEtag;
    }


    private void checkCategoryPresent(Entry entry) {
        assertEquals("expected our category to be preserved",
                     1,
                     entry.getCategories().size());
        assertCategoriesEqual("expected to preserve the category applied to the entry",
                              CATEGORY,
                              entry.getCategories().get(0), false);
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
            checkCategoryPresent(entry);
            assertTrue("expected MD5 ETag Header",
                       entry.getSimpleExtension(AtomServerConstants.ETAG).matches("[a-f0-9]{32}"));
        }
    }
}
