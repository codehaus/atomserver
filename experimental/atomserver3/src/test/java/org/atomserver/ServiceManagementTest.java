package org.atomserver;

import com.sun.jersey.api.client.ClientResponse;
import static junit.framework.Assert.*;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Service;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.Date;

public class ServiceManagementTest extends BaseAtomServerTestCase {
    private static final IRI ROOT_IRI = new IRI("http://localhost:8000/app/");

    @Test
    public void testCreatingSimpleService() throws Exception {
        // check the empty feed, before anything's been added.
        checkServiceFeed();

        // test error conditions for posting Service Documents
        testPostingEmptyServiceDoc();
        testPostingWrongContentType();
        testPostingInvalidXML();
        testPostingInvalidService();
        testPostingNamelessService();
        testPostingBadlyNamedService();
        testPostingServiceWithBadWorkspace();

        // post a simple service definition, and check the Service Feed to make sure it is as we
        // expect
        String test01 = postServiceDefinition("org/atomserver/service-test01.xml");
        checkServiceFeed(test01);

        // try to post the same Service Document again, and make sure it fails as expected
        testPostingServiceAgain("org/atomserver/service-test01.xml", "test01");

        // post another Service Document, and check the Service Feed to make sure it is as we
        // expect
        String test02 = postServiceDefinition("org/atomserver/service-test02.xml");
        checkServiceFeed(test02, test01);
    }

    private void testPostingEmptyServiceDoc() throws Exception {
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity("").post(ClientResponse.class);
        assertEquals("req 1.2.a - Empty body should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 1.2.a - Empty body should produce an appropriate error message",
                     "Empty request body is not valid for this request.",
                     response.getEntity(String.class));
    }

    private void testPostingWrongContentType() throws Exception {
        ClientResponse response = root().type(MediaType.TEXT_PLAIN)
                .entity(
                        "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<service xmlns=\"http://www.w3.org/2007/app\" " +
                        "xmlns:as=\"http://atomserver.org/3\">\n" +
                        "    <as:name>test01</as:name>\n" +
                        "</service>")
                .post(ClientResponse.class);
        assertEquals("req 1.2.b - Empty body should produce a HTTP 400",
                     415,
                     response.getStatus());
    }

    private void testPostingInvalidXML() throws Exception {
        String invalidXml = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                "org/atomserver/service-invalid-invalidXml.xml"));
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(invalidXml)
                .post(ClientResponse.class);
        assertEquals("req 1.2.c - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 1.2.c - Invalid XML should produce an appropriate error message",
                     "Unable to parse a valid object from request entity.",
                     response.getEntity(String.class));
    }

    private void testPostingServiceAgain(String location, String serviceName) throws Exception {
        Service test01 = parse(location);
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(test01)
                .post(ClientResponse.class);
        assertEquals("req 1.2.e - Posting a duplicate service should produce a HTTP 409",
                     409,
                     response.getStatus());
        assertEquals("req 1.2.e - Posting a duplicate service should produce an appropriate " +
                     "error message",
                     "Duplicate error - " + serviceName + " already exists in /.",
                     response.getEntity(String.class));
    }

    private void testPostingServiceWithBadWorkspace() throws Exception {
        Service test01 = parse("org/atomserver/service-invalid-badWorkspace.xml");
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(test01)
                .post(ClientResponse.class);
        assertEquals("req 1.2.g - Posting a service with an invalid workspace should fail " +
                     "with HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 1.2.g - Posting a service with an invalid workspace should fail " +
                     "with an appropriate error message",
                     "Invalid name [$invalid-name] in <as:name> element.",
                     response.getEntity(String.class));
    }

    private void testPostingInvalidService() throws Exception {
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(
                        IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                                "org/atomserver/service-invalid-noWorkspace.xml")))
                .post(ClientResponse.class);
        assertEquals("req 1.2.c - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 1.2.c - Invalid XML should produce an appropriate error message",
                     "Invalid Service Document - services must contain at least one workspace.",
                     response.getEntity(String.class));
    }

    private void testPostingNamelessService() throws Exception {
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(
                        IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                                "org/atomserver/service-invalid-noName.xml")))
                .post(ClientResponse.class);
        assertEquals("req 1.2.d - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 1.2.d - Invalid XML should produce an appropriate error message",
                     "You must provide an <as:name> element.",
                     response.getEntity(String.class));
    }

    private void testPostingBadlyNamedService() throws Exception {
            ClientResponse response = root().type(MediaType.APPLICATION_XML)
                    .entity(IOUtils.toString(
                            getClass().getClassLoader().getResourceAsStream(
                                    "org/atomserver/service-invalid-invalidName.xml")))
                    .post(ClientResponse.class);
            assertEquals("req 1.2.d - Invalid XML should produce a HTTP 400",
                         400,
                         response.getStatus());
            assertEquals("req 1.2.d - Invalid XML should produce an appropriate error message",
                         "Invalid name [$invalid-name] in <as:name> element.",
                         response.getEntity(String.class));

            response = root().type(MediaType.APPLICATION_XML)
                    .entity(IOUtils.toString(
                            getClass().getClassLoader().getResourceAsStream(
                                    "org/atomserver/service-invalid-emptyName.xml")))
                    .post(ClientResponse.class);
            assertEquals("req 1.2.d - Invalid XML should produce a HTTP 400",
                         400,
                         response.getStatus());
            assertEquals("req 1.2.d - Invalid XML should produce an appropriate error message",
                         "Invalid name [] in <as:name> element.",
                         response.getEntity(String.class));
    }


    private String postServiceDefinition(String location) {
        Service service = parse(location);
        String name = service.getSimpleExtension(AtomServerConstants.NAME);
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(service).post(ClientResponse.class);

        assertEquals("req 1.2.h - Successful creation of Service should return HTTP 201",
                     201, response.getStatus());
        assertEquals("req 1.2.i - The response body should contain the Service Document",
                     name,
                     response.getEntity(Service.class)
                             .getSimpleExtension(AtomServerConstants.NAME));
        assertEquals("req 1.2.j - The response should contain a Location header with the " +
                     "Service URL",
                     ROOT_URL + "/" + name + "/",
                     response.getMetadata().get("Location").get(0));
        return name;
    }

    private void checkServiceFeed(String... expectedServiceNames) {
        ClientResponse response =
                root().type(MediaType.APPLICATION_ATOM_XML).get(ClientResponse.class);

        assertEquals("req 1.1.a - GET on Service Root URL should return 200 OK",
                     200, response.getStatus());
        assertEquals("req 1.1.b - GET on Service Root URL should have Content-type of " +
                     "application/atom+xml",
                     MediaType.APPLICATION_ATOM_XML,
                     response.getMetadata().getFirst("Content-type"));
        Feed serviceFeed = response.getEntity(Feed.class);
        assertTrue("req 1.1.c - GET on Service root URL returns valid XML that represents an " +
                   "ATOM FEED",
                   serviceFeed != null);

        assertEquals("req 1.1.d - Author element is wrong",
                     "AtomServer v3", serviceFeed.getAuthor().getName());
        assertEquals("req 1.1.e - ID element is wrong",
                     ROOT_IRI, serviceFeed.getId());
        assertEquals("req 1.1.f - Title element is wrong",
                     "AtomServer v3 Service Documents Feed", serviceFeed.getTitle());
        assertNotNull("req 1.1.g - Updated element is null",
                      serviceFeed.getUpdated());
        assertNotNull("req 1.1.h - Self Link is null",
                      serviceFeed.getLink("self"));
        assertEquals("req 1.1.h - Self Link is wrong",
                     ROOT_IRI, serviceFeed.getLink("self").getHref());
        assertEquals("req 1.1.i - Different number of Services than expected in Service " +
                     "Documents Feed",
                     expectedServiceNames.length, serviceFeed.getEntries().size());

        Date updated = serviceFeed.getUpdated();
        for (int i = 0; i < expectedServiceNames.length; i++) {
            String name = expectedServiceNames[i];
            Entry serviceEntry = serviceFeed.getEntries().get(i);
            IRI serviceIri = ROOT_IRI.resolve(name).trailingSlash();
            assertEquals("req 1.1.j - ID element is wrong",
                         serviceIri, serviceEntry.getId());
            assertEquals("req 1.1.k - Title element is wrong",
                         name, serviceEntry.getTitle());
            assertNotNull("req 1.1.l - Updated element is null",
                          serviceEntry.getUpdated());
            assertNotNull("req 1.1.m - Alternate Link is null",
                          serviceEntry.getLink("alternate"));
            assertEquals("req 1.1.m - Alternate Link is wrong",
                         serviceIri, serviceEntry.getLink("alternate").getHref());
            assertEquals("req 1.1.n - Summary is wrong",
                         String.format("Service Document for the %s Service", name),
                         serviceEntry.getSummary());
            if (i == 0) {
                assertEquals("req 1.1.g- Service Feed Updated element does not contain the " +
                             "same value as most recently updated Service",
                             updated, serviceEntry.getUpdated());
            } else {
                assertFalse("req 1.1.o - Services are not sorted by Updated with most " +
                            "recent first",
                            serviceEntry.getUpdated().after(updated));
                updated = serviceEntry.getUpdated();
            }
        }
    }

}
