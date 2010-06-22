package org.atomserver;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.*;
import org.apache.log4j.Logger;
import org.atomserver.app.jaxrs.AbderaMarshaller;
import org.atomserver.domain.Widget;
import org.junit.*;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.simpleframework.xml.core.Persister;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

@Ignore
public class BaseAtomServerTestCase {
    private static final Logger log = Logger.getLogger(BaseAtomServerTestCase.class);
    protected static final Persister PERSISTER = new Persister();

    private static AtomServer server;
    private static WebResource serverRoot;
    private static WebResource root;
    // TODO: this isn't quite the right name for this constant, with the /app - fix it.
    public static final String ROOT_URL =
            System.getProperty("atomserver.test.url", "http://localhost:8000");
    private static Server contentServer;

    @BeforeClass
    public static void setUp() throws Exception {
        log.debug("creating an in-memory server at " + ROOT_URL);
        server = AtomServer.create();
        server.start();

        Client client = Client.create(new DefaultClientConfig() {
            public Set getClasses() {
                return Collections.singleton(AbderaMarshaller.class);
            }
        });

        contentServer = new Server(9999);
        contentServer.addHandler(new AbstractHandler() {
            public void handle(String target,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               int dispatch) throws IOException, ServletException {
                int id = Integer.valueOf(target.substring(target.lastIndexOf("/") + 1));
                try {
                    PERSISTER.write(new Widget(id, "violet", "out of line content"),
                            response.getOutputStream());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        contentServer.start();

        serverRoot = client.resource(ROOT_URL);
        root = serverRoot.path("app");
    }

    @After
    public void clearAllServices() {
        Feed serviceFeed = root.get(Feed.class);
        for (Entry serviceEntry : serviceFeed.getEntries()) {
            IRI serviceIri = serviceEntry.getLink("alternate").getHref();
            log.debug(String.format("Deleting Service %s", serviceIri));
            serverRoot.path(new IRI(ROOT_URL).relativize(serviceIri).toString()).delete();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        log.debug("stopping in-memory server at " + ROOT_URL);
        server.stop();
        contentServer.stop();
    }

    /**
     * return the root WebResource for the server - all RESTful calls can be scoped within this.
     *
     * @return the root WebResource
     */
    protected static WebResource root() {
        return root;
    }

    /**
     * return the root WebResource for the host server
     *
     * @return the host root WebResource
     */
    protected static WebResource serverRoot() {
        return serverRoot;
    }
    
    /**
     * parse the XML resource at the given location into an Abdera Element.
     *
     * @param location the location in the classpath of the XML resource
     * @return the parsed object
     */
    protected <T extends Element> T parse(String location) {
        return (T) AbderaMarshaller.ABDERA.getParser().parse(
                getClass().getClassLoader().getResourceAsStream(location)).getRoot();
    }

    public static final Category CATEGORY = testCategory();

    protected static Entry createWidgetEntry(int id, String color, String name) throws Exception {
        return createWidgetEntry(id, color, name, false);
    }

    protected static Entry createWidgetEntry(int id, String color, String name,
                                             boolean inlineContent) throws Exception {
        Entry entry = AbderaMarshaller.ABDERA.getFactory().newEntry();
        StringWriter stringWriter = new StringWriter();
        PERSISTER.write(new Widget(id, color, name), stringWriter);
        if (inlineContent) {
            entry.setContent(
                    new IRI(String.format("http://localhost:9999/%s", id)),
                    "application/xml"); 
        } else {
            entry.setContent(stringWriter.toString(), Content.Type.XML);
        }
        entry.addCategory(testCategory());
        System.out.println(entry.getContent());
        return entry;
    }

    private static Category testCategory() {
        Category category = AbderaMarshaller.ABDERA.getFactory().newCategory();
        category.setScheme("urn:test.scheme");
        category.setTerm("test.term");
        category.setLabel("test.label");
        return category;
    }

    protected static void assertCategoriesEqual(String message,
                                                Category expected,
                                                Category category,
                                                boolean ignoreNullLabel) {
        Assert.assertEquals(message, expected.getScheme(), category.getScheme());
        Assert.assertEquals(message, expected.getTerm(), category.getTerm());
        Assert.assertTrue(message,
                          expected.getLabel() == null ? category.getLabel() == null :
                          ((ignoreNullLabel && category.getLabel() == null) ||
                           expected.getLabel().equals(category.getLabel())));
    }

}

