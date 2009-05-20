package org.atomserver;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.*;
import org.apache.log4j.Logger;
import org.atomserver.app.AbderaMarshaller;
import org.atomserver.domain.Widget;
import org.atomserver.ext.Aggregate;
import org.junit.*;
import org.simpleframework.xml.core.Persister;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

@Ignore
public class BaseAtomServerTestCase {
    private static final Logger log = Logger.getLogger(BaseAtomServerTestCase.class);
    protected static final Persister PERSISTER = new Persister();

    private static AtomServer server;
    private static WebResource root;
    // TODO: this isn't quite the right name for this constant, with the /app - fix it.
    public static final String ROOT_URL =
            System.getProperty("atomserver.test.url", "http://localhost:8000/app");
    public static final boolean TC =
            Boolean.valueOf(System.getProperty("atomserver.test.tc", "false"));

    @BeforeClass
    public static void setUp() throws Exception {
        if (TC) {
            log.debug("running tests against a terracotta-enabled server at " + ROOT_URL);
            int retries = 30;
            boolean connected = false;
            while (!connected && retries-- > 0) {
                Thread.sleep(1000);
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(ROOT_URL).openConnection();
                    connected = conn.getResponseCode() < 300;
                } catch (Exception e) {
                    log.debug("server not responding...");
                    // okay - retrying again...
                }
            }
            if (!connected) {
                throw new IllegalStateException("server not available at " + ROOT_URL);
            }
        } else {
            log.debug("creating an in-memory server at " + ROOT_URL);
            server = AtomServer.create();
            server.start();
        }

        Client client = Client.create(new DefaultClientConfig() {
            public Set getClasses() {
                return Collections.singleton(AbderaMarshaller.class);
            }
        });

        root = client.resource(ROOT_URL);
    }

    @After
    public void clearAllServices() {
        Feed serviceFeed = root.get(Feed.class);
        for (Entry serviceEntry : serviceFeed.getEntries()) {
            IRI serviceIri = serviceEntry.getLink("alternate").getHref();
            log.debug(String.format("Deleting Service %s", serviceIri));
            root.path(new IRI(ROOT_URL).relativize(serviceIri).toString()).delete();
        }
    }

    @AfterClass
    public static void tearDown() {
        if (TC) {
            // nothing to do - the TC server will be stopped externally
        } else {
            log.debug("stopping in-memory server at " + ROOT_URL);
            server.stop();
        }
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
     * parse the XML resource at the given location into an Abdera Element.
     *
     * @param location the location in the classpath of the XML resource
     * @return the parsed object
     */
    protected <T extends Element> T parse(String location) {
        return (T) AbderaMarshaller.parser().parse(
                getClass().getClassLoader().getResourceAsStream(location)).getRoot();
    }

    public static final Category CATEGORY = AbderaMarshaller.factory().newCategory();
    public static final Aggregate AGGREGATE = AbderaMarshaller.factory().newExtensionElement(AtomServerConstants.AGGREGATE);

    static {
        CATEGORY.setScheme("urn:test.scheme");
        CATEGORY.setTerm("test.term");
        CATEGORY.setLabel("test.label");

        AGGREGATE.setCollection("test.agg");
        AGGREGATE.setEntryId("myagg");
    }

    protected static Entry createWidgetEntry(int id, String color, String name) throws Exception {
        Entry entry = AbderaMarshaller.factory().newEntry();
        StringWriter stringWriter = new StringWriter();
        PERSISTER.write(new Widget(id, color, name), stringWriter);
        entry.setContent(stringWriter.toString(), Content.Type.XML);
        entry.addCategory(CATEGORY);
        entry.addExtension(AGGREGATE);
        return entry;
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

