package org.atomserver;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.abdera.model.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.atomserver.domain.Widget;
import org.atomserver.test.EntryChecker;
import org.atomserver.test.FeedFollower;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.atomserver.AtomServerConstants.OPTIMISTIC_CONCURRENCY_OVERRIDE;
import static org.junit.Assert.*;

public class BasicTest extends BaseAtomServerTestCase {
    private static final Logger log = Logger.getLogger(BasicTest.class);
    private static String SERVICE;
    private static String WORKSPACE;
    private static String COLLECTION;

    private static final Set<String> names = new HashSet<String>();

    @Before
    public void setUpTestService() throws Exception {
        log.debug("BasicTest.setUpTestService");
        InputStream stream = BasicTest.class.getClassLoader().getResourceAsStream(
                "org/atomserver/BasicTest.xml");
        Service service = root().path("test").type(MediaType.APPLICATION_XML)
                .entity(IOUtils.toString(stream)).put(Service.class);
//        Service service = root().type(MediaType.APPLICATION_XML)
//                .entity(IOUtils.toString(stream)).post(Service.class);
        SERVICE = service.getSimpleExtension(AtomServerConstants.NAME);
        Workspace workspace = service.getWorkspaces().get(0);
        WORKSPACE = workspace.getSimpleExtension(AtomServerConstants.NAME);
        Collection collection = workspace.getCollections().get(0);
        COLLECTION = collection.getSimpleExtension(AtomServerConstants.NAME);

        String[] colors = {"red", "green", "blue", "cyan", "periwinkle"};
        for (int id = 0; id < 100; id++) {
            String name = UUID.randomUUID().toString();
            names.add(name);
            Entry widgetEntry = createWidgetEntry(id, colors[id % colors.length], name);
            widgetEntry.addCategory("urn:div5", id % 5 == 0 ? "true" : "false", null);
            Entry entry =
                    root().path(collectionPath()).path(String.valueOf(id))
                            .type(MediaType.APPLICATION_XML)
                            .entity(widgetEntry)
                            .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                            .put(Entry.class);
            StringWriter stringWriter = new StringWriter();
            entry.writeTo(stringWriter);
            log.debug(String.format("wrote entry : %s", stringWriter.toString()));
        }
    }

    @Test
    public void testBasicFunctionality() throws Exception {
        testFullFeed();
        testCategorizedFeeds();
        testNonExistentCategory();
        testEntryAccess();
        testMissingEntry();
        testFeedPullAfterMorePublishes();
    }

    @Test
    public void testPullingFeedFromEmptyCollection() throws Exception {
        Feed feed = root().path(SERVICE).path(WORKSPACE).path("empty")
                .accept("application/atom+xml").get(Feed.class);
        assertTrue(feed.getEntries().isEmpty());
        assertEquals("0", feed.getSimpleExtension(AtomServerConstants.END_INDEX));
    }
    
    public void testNonExistentCategory() throws Exception {
        testEmptyFeed("(urn:fake)cat");
        testEmptyFeed("(urn:fake)cat/(urn:parity)even");
        testEmptyFeed("AND/(urn:fake)cat/(urn:parity)even");
        testEvenParityFeed("OR/(urn:fake)cat/(urn:parity)even");
        testFullFeed("NOT/(urn:fake)cat");
        testEvenParityFeed("AND/(urn:parity)even/NOT/(urn:fake)cat");
    }

    public void testEmptyFeed(String... query) throws Exception {

        FeedFollower follower = new FeedFollower(
                root(),
                String.format("%s/%s/%s/%s",
                        SERVICE, WORKSPACE, COLLECTION,
                        query.length != 0 ? "-/"+query[0] : "" ), 10, 0);
        assertEquals("Empty Feed Expected", 0, follower.follow());
    }

    public void testEvenParityFeed(String... query) throws Exception {

        FeedFollower follower = new FeedFollower(
                root(),
                String.format("%s/%s/%s/%s",
                        SERVICE, WORKSPACE, COLLECTION,
                        query.length != 0 ? "-/"+query[0] : "" ), 10, 0);
        assertEquals((names.size()+1)/2, follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertTrue(widget.getId() % 2 == 0);
                    }
                },
                null));
    }

    public void testFullFeed(String... query) throws Exception {

        final Set<String> allNames = new HashSet<String>(names);
        FeedFollower follower = new FeedFollower(
                root(),
                String.format("%s/%s/%s/%s",
                        SERVICE, WORKSPACE, COLLECTION,
                        query.length != 0 ? "-/"+query[0] : "" ), 10, 0);
        System.err.println(collectionPath());
        assertEquals(allNames.toString(), allNames.size(), follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertTrue(widget.getName(), allNames.remove(widget.getName()));
                    }
                },
                null));
        assertTrue(allNames.toString(), allNames.isEmpty());
    }

    public void testCategorizedFeeds() throws Exception {

        FeedFollower follower = new FeedFollower(
                root(),
                String.format("%s/%s/%s/-/%s",
                        SERVICE, WORKSPACE, COLLECTION,
                        "(urn:parity)even"), 10, 0);
        assertEquals(50, follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertTrue(widget.getId() % 2 == 0);
                    }
                },
                null));

        follower = new FeedFollower(
                root(),
                String.format("%s/%s/%s/-/%s",
                        SERVICE, WORKSPACE, COLLECTION,
                        "AND/(urn:parity)even/(urn:div5)true"), 10, 0);
        assertEquals(10, follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertTrue(widget.getId() % 2 == 0);
                        assertTrue(widget.getId() % 5 == 0);
                    }
                },
                null));

        follower = new FeedFollower(
                root(),
                String.format("%s/%s/%s/-/%s",
                        SERVICE, WORKSPACE, COLLECTION,
                        "NOT/AND/(urn:parity)even/(urn:div5)true"), 10, 0);
        assertEquals(90, follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertFalse((widget.getId() % 2 == 0) && (widget.getId() % 5 == 0));
                    }
                },
                null));
    }

    public void testEntryAccess() throws Exception {
        int id = 17;
        Entry entry = root()
                .path(String.format("%s/%s/%s/%s",
                        SERVICE, WORKSPACE, COLLECTION, String.valueOf(id)))
                .accept(MediaType.APPLICATION_XML).get(Entry.class);
        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
        assertEquals(id, widget.getId());
    }

    public void testMissingEntry() throws Exception {
        int id = 175;
        String entryId =
                String.format("%s/%s/%s/%s", SERVICE, WORKSPACE, COLLECTION, String.valueOf(id));
        try {
            root().path(entryId).accept(MediaType.APPLICATION_XML).get(Entry.class);
            fail("we expected an Exception");
        } catch (UniformInterfaceException e) {
            ClientResponse response = e.getResponse();
            assertEquals(404, response.getStatus());
            assertEquals(String.format("%s NOT FOUND", entryId),
                    response.getEntity(String.class));
        }
    }

    public void testFeedPullAfterMorePublishes() throws Exception {

        final Set<String> allNames = new HashSet<String>(names);
        FeedFollower follower = new FeedFollower(
                root(), collectionPath(), 10, 0);
        assertEquals(100, follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertTrue(allNames.remove(widget.getName()));
                    }
                },
                null));
        assertTrue(allNames.isEmpty());

        String[] colors = {"red", "green", "blue", "cyan", "periwinkle"};
        for (int id = 90; id < 110; id++) {
            String name = UUID.randomUUID().toString();
            names.add(name);
            Entry widgetEntry = createWidgetEntry(id, colors[id % colors.length], name);
            widgetEntry.addCategory("urn:div5", id % 5 == 0 ? "true" : "false", null);
            ClientResponse response = root().path(collectionPath()).path(String.valueOf(id)).type(MediaType.APPLICATION_XML)
                    .entity(widgetEntry)
                    .header("ETag", OPTIMISTIC_CONCURRENCY_OVERRIDE)
                    .put(ClientResponse.class);
            assertEquals(Response.Status.Family.SUCCESSFUL, response.getResponseStatus().getFamily());
        }


        assertEquals(20, follower.follow(
                new EntryChecker() {
                    public void check(Entry entry) throws Exception {
                        Widget widget = PERSISTER.read(Widget.class, entry.getContent());
                        assertTrue(widget.getId() >= 90 && widget.getId() < 110);
                    }
                },
                null));

        follower.reset();

        assertEquals(110, follower.follow());
    }


    // TODO: add an @Test annotation here to activate this, eventually remove this.
    public void manualTest() throws Exception {
        System.out.println("begin manual testing now.");
        Thread.sleep(600000 /* ten minutes */);
    }

    private static String collectionPath() {
        return String.format("%s/%s/%s", SERVICE, WORKSPACE, COLLECTION);
    }
}
