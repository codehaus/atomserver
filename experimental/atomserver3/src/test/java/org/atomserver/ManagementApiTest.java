package org.atomserver;

import com.sun.jersey.api.client.ClientResponse;
import static junit.framework.Assert.*;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Date;

public class ManagementApiTest extends BaseAtomServerTestCase {
    private static final IRI ROOT_IRI = new IRI("http://localhost:8000/app/");

    @Test
    public void testAppRootUrl() throws Exception {
        // check the empty feed, before anything's been added.
        checkServiceFeed();

        // test error conditions for posting Service Documents
        testPostingEmptyServiceDocToRoot();
        testPostingWrongContentTypeToRoot();
        testPostingInvalidXmlToRoot();
        testPostingInvalidServiceXmlToRoot();
        testPostingNamelessServiceToRoot();
        testPostingBadlyNamedServiceToRoot();
        testPostingServiceWithBadWorkspaceToRoot();

        // post a simple service definition, and check the Service Feed to make sure it is as we
        // expect
        String test01 = postServiceDefinition("org/atomserver/service-test01.xml")
                .getSimpleExtension(AtomServerConstants.NAME);
        checkServiceFeed(test01);

        // try to post the same Service Document again, and make sure it fails as expected
        testPostingServiceAgainToRoot("org/atomserver/service-test01.xml", "test01");

        // post another Service Document, and check the Service Feed to make sure it is as we
        // expect
        String test02 = postServiceDefinition("org/atomserver/service-test02.xml")
                .getSimpleExtension(AtomServerConstants.NAME);
        checkServiceFeed(test02, test01);
    }

    @Test
    public void testServiceUrl() throws Exception {

        // check that everythings works as expected against a non-existent service
        testRetrievingNonexistentService("nosuchservice");
        testPostingToNonexistentService("nosuchservice");
        testDeletingNonexistentService("nosuchservice");

        // post a simple service definition with a couple of workspaces
        String test03 = postServiceDefinition("org/atomserver/service-test03.xml")
                .getSimpleExtension(AtomServerConstants.NAME);
        checkServiceDocument(test03, "birds", "cars");

        // test error conditions for posting workspaces to the service
        testPostingNoEntityToService(test03);
        testPostingWrongContentTypeToService(test03);
        testPostingInvalidXmlToService(test03);
        testPostingInvalidWorkspaceDefsToService(test03);
        testPostingBadlyNamedWorkspaceToService(test03);

        // post a couple of workspaces successfully
        postWorkspaceDefinitionToService(test03, "org/atomserver/workspace-liquors.xml");
        postWorkspaceDefinitionToService(test03, "org/atomserver/workspace-countries.xml");
        postWorkspaceDefinitionToService(test03, "org/atomserver/workspace-knights.xml");

        // check that the service document looks as expected
        checkServiceDocument(test03,
                             "Countries",
                             "King_Arthur_and_the_Knights_of_t",
                             "birds", "cars", "liquors");

        // try posting a duplicate to verify that the right thing happens
        postDuplicateWorkspaceDefinitionToService(test03, "org/atomserver/workspace-cars.xml");
        postDuplicateWorkspaceDefinitionToService(test03, "org/atomserver/workspace-birds.xml");

        testDeletingService(test03);
    }

    // ---------------------------------------------------------------

    private void testDeletingService(String test03) {
        ClientResponse response = root().path(test03).delete(ClientResponse.class);
        assertEquals("req 2.3.c - successful DELETE on a Service should return an HTTP 200",
                     200,
                     response.getStatus());
        assertEquals("req 2.3.c - successful DELETE on a Service should return an appropriate " +
                     "status message",
                     "/" + test03 + "/ was deleted successfully.",
                     response.getEntity(String.class));
        assertEquals("req 2.3.b - after a DELETE, the service should be unavailable",
                     404,
                     root().path(test03).get(ClientResponse.class).getStatus());
    }

    private void postWorkspaceDefinitionToService(String serviceName, String location) {
        Workspace workspaceDocument = parse(location);
        ClientResponse response =
                root().path(serviceName).type(AtomServerConstants.APPLICATION_APP_XML)
                        .entity(workspaceDocument).post(ClientResponse.class);
        assertEquals("req 2.2.i - Successful POST of workspace should return HTTP 201",
                     201,
                     response.getStatus());
        assertNotNull("req 2.2.j - Successful POST of workspace should return a document with " +
                      "an <app:workspace> element describing the newly added workspace",
                      response.getEntity(Workspace.class));
        String workspaceName = workspaceDocument.getSimpleExtension(AtomServerConstants.NAME);
        if (workspaceName == null) {
            workspaceName = StringUtils.left(
                    workspaceDocument.getTitle().replaceAll("\\s", "_")
                            .replaceAll("[^a-zA-Z0-9-_]", ""), 32);
        }
        assertEquals("req 2.2.k - The response should contain a Location header with the " +
                     "Workspace URL",
                     ROOT_URL + "/" + serviceName + "/" + workspaceName,
                     response.getMetadata().get("Location").get(0));

        response = root().path(serviceName).path(workspaceName)
                .type(MediaType.APPLICATION_ATOM_XML).get(ClientResponse.class);
        assertEquals("req 2.2.g - The workspace should be created",
                     200,
                     response.getStatus());
        assertEquals("req 2.2.e - The name of the service is not what was expected",
                     workspaceName,
                     response.getEntity(Service.class).getWorkspaces().get(0)
                             .getSimpleExtension(AtomServerConstants.NAME));
    }

    private void postDuplicateWorkspaceDefinitionToService(String serviceName, String location) {
        Workspace workspaceDocument = parse(location);
        ClientResponse response =
                root().path(serviceName).type(AtomServerConstants.APPLICATION_APP_XML)
                        .entity(workspaceDocument).post(ClientResponse.class);
        assertEquals("req 2.2.f - Post of duplicate should produce an HTTP 409 CONFLICT",
                     409,
                     response.getStatus());
        String workspaceName = workspaceDocument.getSimpleExtension(AtomServerConstants.NAME);
        if (workspaceName == null) {
            workspaceName = StringUtils.left(
                    workspaceDocument.getTitle().replaceAll("\\s", "_")
                            .replaceAll("[^a-zA-Z0-9-_]", ""), 32);
        }
        assertEquals("req 2.2.f - Posting a duplicate workspace should produce an appropriate " +
                     "error message",
                     "Duplicate error - " + workspaceName + " already exists in /" +
                     serviceName +  "/.",
                     response.getEntity(String.class));
    }

    private void checkServiceDocument(String serviceName, String... expectedWorkspaceNames) {
        ClientResponse response =
                root().path(serviceName).type(MediaType.APPLICATION_ATOM_XML).get(ClientResponse.class);
        assertEquals("req 2.1.b - Request for valid service should return HTTP 200 OK",
                     200,
                     response.getStatus());
        assertEquals("req 2.1.c - Request for valid service should have a Content-Type of " +
                     "application/atomsvc+xml",
                     "application/atomsvc+xml",
                     response.getMetadata().getFirst("Content-Type"));
        Service service = response.getEntity(Service.class);
        assertNotNull("req 2.1.d - Request for valid service should return a Service Document",
                      service);

        String name = service.getSimpleExtension(AtomServerConstants.NAME);
        assertNotNull("req 2.1.e - Service document should have an <as:name> element",
                      name);
        assertEquals("req 2.1.e - Service document's <as:name> element should match the name of " +
                     "the requested service",
                     serviceName,
                     name);

        Arrays.sort(expectedWorkspaceNames);
        assertEquals("req 2.1.f - There should be a workspace element in the Service Document" +
                     "for each workspace in the service",
                     expectedWorkspaceNames.length,
                     service.getWorkspaces().size());

        for (int i = 0; i < expectedWorkspaceNames.length; i++) {
            String expectedWorkspaceName = expectedWorkspaceNames[i];
            Workspace workspace = service.getWorkspaces().get(i);

            assertEquals("req 2.1.f - There should be a workspace element in the Service Document" +
                         "for each workspace in the service",
                         expectedWorkspaceName,
                         workspace.getSimpleExtension(AtomServerConstants.NAME));
        }
    }

    private void testPostingInvalidXmlToService(String serviceName) throws Exception {
        String invalidXml = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                "org/atomserver/workspace-invalid-invalidXml.xml"));
        ClientResponse response = root().path(serviceName).type(MediaType.APPLICATION_XML)
                .entity(invalidXml)
                .post(ClientResponse.class);
        assertEquals("req 2.2.d - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 2.2.d - Invalid XML should produce an appropriate error message",
                     "Unable to parse a valid object from request entity.",
                     response.getEntity(String.class));
    }

    private void testPostingInvalidWorkspaceDefsToService(String serviceName) throws Exception {
        ClientResponse response = root().path(serviceName).type(MediaType.APPLICATION_XML)
                .entity(
                        IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                                "org/atomserver/workspace-invalid-empty.xml")))
                .post(ClientResponse.class);
        assertEquals("req 2.2.d - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 2.2.d - Invalid XML should produce an appropriate error message",
                     "Workspaces require an <atom:title> element.",
                     response.getEntity(String.class));
    }


    private void testRetrievingNonexistentService(String serviceName) {
        ClientResponse response =
                root().path(serviceName).type(MediaType.APPLICATION_ATOM_XML)
                        .get(ClientResponse.class);
        assertEquals("req 2.1.a - Request for nonexistent service should return HTTP 404",
                     404,
                     response.getStatus());
        assertEquals("req 2.1.a - Request for nonexistent service should produce an appropriate " +
                     "error message",
                     serviceName + " not found within /.",
                     response.getEntity(String.class));
    }

    private void testPostingToNonexistentService(String serviceName) {
        ClientResponse response =
                root().path(serviceName).type(MediaType.APPLICATION_ATOM_XML)
                        .entity(parse("org/atomserver/workspace-cars.xml"))
                        .post(ClientResponse.class);
        assertEquals("req 2.2.a - POST to nonexistent service should return HTTP 404",
                     404,
                     response.getStatus());
        assertEquals("req 2.2.a - POST to nonexistent service should produce an appropriate " +
                     "error message",
                     serviceName + " not found within /.",
                     response.getEntity(String.class));
    }

    private void testDeletingNonexistentService(String serviceName) {
        ClientResponse response =
                root().path(serviceName).delete(ClientResponse.class);
        assertEquals("req 2.3.a - DELETE to nonexistent service should return HTTP 404",
                     404,
                     response.getStatus());
        assertEquals("req 2.3.a - DELETE to nonexistent service should produce an appropriate " +
                     "error message",
                     serviceName + " not found within /.",
                     response.getEntity(String.class));
    }

    private void testPostingWrongContentTypeToService(String serviceName) throws Exception {
        ClientResponse response = root().path(serviceName).type(MediaType.TEXT_PLAIN)
                .entity(
                        "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<service xmlns=\"http://www.w3.org/2007/app\" " +
                        "xmlns:as=\"http://atomserver.org/3\">\n" +
                        "</service>")
                .post(ClientResponse.class);
        assertEquals("req 2.2.c - Empty body should produce a HTTP 400",
                     415,
                     response.getStatus());
    }

    private void testPostingNoEntityToService(String serviceName) {
        ClientResponse response =
                root().path(serviceName).type(MediaType.APPLICATION_ATOM_XML)
                        .entity("")
                        .post(ClientResponse.class);
        assertEquals("req 2.2.b - POST to service with no request entity should return HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 2.2.b - POST to service with no request entity should return " +
                     "appropriate error message",
                     "Empty request body is not valid for this request.",
                     response.getEntity(String.class));
    }

    private void testPostingEmptyServiceDocToRoot() throws Exception {
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity("").post(ClientResponse.class);
        assertEquals("req 1.2.a - Empty body should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 1.2.a - Empty body should produce an appropriate error message",
                     "Empty request body is not valid for this request.",
                     response.getEntity(String.class));
    }

    private void testPostingWrongContentTypeToRoot() throws Exception {
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

    private void testPostingInvalidXmlToRoot() throws Exception {
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

    private void testPostingServiceAgainToRoot(String location, String serviceName) throws Exception {
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

    private void testPostingServiceWithBadWorkspaceToRoot() throws Exception {
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

    private void testPostingInvalidServiceXmlToRoot() throws Exception {
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

    private void testPostingNamelessServiceToRoot() throws Exception {
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

    private void testPostingBadlyNamedServiceToRoot() throws Exception {
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

    private void testPostingBadlyNamedWorkspaceToService(String serviceName) throws Exception {
        ClientResponse response = root().path(serviceName).type(MediaType.APPLICATION_XML)
                .entity(IOUtils.toString(
                        getClass().getClassLoader().getResourceAsStream(
                                "org/atomserver/workspace-invalid-invalidName.xml")))
                .post(ClientResponse.class);
        assertEquals("req 2.2.d - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 2.2.d - Invalid XML should produce an appropriate error message",
                     "Invalid name [!invalid] in <as:name> element.",
                     response.getEntity(String.class));

        response = root().path(serviceName).type(MediaType.APPLICATION_XML)
                .entity(IOUtils.toString(
                        getClass().getClassLoader().getResourceAsStream(
                                "org/atomserver/workspace-invalid-emptyName.xml")))
                .post(ClientResponse.class);
        assertEquals("req 2.2.d - Invalid XML should produce a HTTP 400",
                     400,
                     response.getStatus());
        assertEquals("req 2.2.d - Invalid XML should produce an appropriate error message",
                     "Invalid name [] in <as:name> element.",
                     response.getEntity(String.class));
    }


    private Service postServiceDefinition(String location) {
        Service service = parse(location);
        String name = service.getSimpleExtension(AtomServerConstants.NAME);
        ClientResponse response = root().type(MediaType.APPLICATION_XML)
                .entity(service).post(ClientResponse.class);

        assertEquals("req 1.2.h - Successful creation of Service should return HTTP 201",
                     201,
                     response.getStatus());
        assertEquals("req 1.2.i - The response body should contain the Service Document",
                     name,
                     response.getEntity(Service.class)
                             .getSimpleExtension(AtomServerConstants.NAME));
        assertEquals("req 1.2.j - The response should contain a Location header with the " +
                     "Service URL",
                     ROOT_URL + "/" + name,
                     response.getMetadata().get("Location").get(0));
        return service;
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
