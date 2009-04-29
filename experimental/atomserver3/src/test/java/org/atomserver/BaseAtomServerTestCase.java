package org.atomserver;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.log4j.Logger;
import org.atomserver.app.AbderaMarshaller;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

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
    private static final String ROOT_URL =
            System.getProperty("atomserver.test.url", "http://localhost:8000/app");
    private static final boolean TC =
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

    @AfterClass
    public static void tearDown() {
        if (TC) {
        } else {
            log.debug("stopping in-memory server at " + ROOT_URL);
            server.stop();
        }
    }

    protected static WebResource root() {
        return root;
    }
}

