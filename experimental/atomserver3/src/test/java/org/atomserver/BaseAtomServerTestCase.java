package org.atomserver;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Entry;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.log4j.Logger;
import org.atomserver.app.AbderaMarshaller;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.After;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

@Ignore
public class BaseAtomServerTestCase {
    private static final Logger log = Logger.getLogger(BaseAtomServerTestCase.class);

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
}

