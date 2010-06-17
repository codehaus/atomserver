package org.atomserver;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.core.dbstore.BaseCRUDDBSTestCase;
import org.atomserver.testutils.latency.LatencyUtil;
import org.atomserver.utils.thread.ManagedThreadPoolTaskExecutor;

import java.text.MessageFormat;

/**
 * Tests different aspects of the ThrottledAtomServer
 */
public class ThrottledAtomServerTest extends BaseCRUDDBSTestCase {

    protected Log log = LogFactory.getLog(ThrottledAtomServerTest.class);

    public static Test suite() {
        return new TestSuite(ThrottledAtomServerTest.class);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        destroyEntry("widgets", "acme", "990", "en_US", true);
        destroyEntry("widgets", "acme", "991", "en_US", true);
    }

    protected String getURLPath() {
        return null;
    }

    /**
     * This test proves that the atomserver used in testing (and therefore real life as well) is handling PUTs
     * with the ThrottledAtomServer.  It determines this fact by inspecting the underlying pool used in the
     * ThrottledAtomServer.
     */
    public void testThrottleEnabled() throws Exception {
        ManagedThreadPoolTaskExecutor executor = (ManagedThreadPoolTaskExecutor) getSpringFactory().getBean("org.atomserver-taskExecutor");
        long startingNumberOfCompletedTasks = executor.getCompletedTaskCount();

        for (int i = 0; i < 2; i++) {
            String url = getServerURL() + "widgets/acme/99" + i + ".en_US.xml";
            log.info("Putting " + url);
            AbderaClient client = new AbderaClient();
            RequestOptions options = client.getDefaultRequestOptions();
            options.setHeader("Connection", "close");

            Entry entry = getFactory().newEntry();
            entry.setId(url);
            entry.setContentAsXhtml(getContentString("throttle", "99" + i));

            ClientResponse response = client.put(url, entry);
            assertEquals(201, response.getStatus());
        }
        LatencyUtil.accountForLatency();
        Thread.currentThread().sleep(50); // Give some time to finish tasks.

        //assert that 2 more tasks were completed
        long endingNumberOfCompletedTasks = executor.getCompletedTaskCount();
        log.debug("Started with " + startingNumberOfCompletedTasks + " and ended with " + endingNumberOfCompletedTasks + " complete tasks in pool");
        assertEquals("throttle should show that 20 tasks have been completed", startingNumberOfCompletedTasks + 2, endingNumberOfCompletedTasks);
    }

    protected String getContentString(String collection, String entryId) {
        return MessageFormat.format(
                "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" " +
                        "systemId=\"{0}\" id=\"{1}\" inNetwork=\"false\"> \n" +
                        "    <colors>\n" +
                        "       <color isDefault=\"true\">teal</color>\n" +
                        "    </colors>\n" +
                        "    <contact> \n" +
                        "        <contactId>929292</contactId>\n" +
                        "        <displayName>Chris</displayName> \n" +
                        "        <hasEmail>true</hasEmail> \n" +
                        "    </contact>\n" +
                        "</property>\n",
                collection,
                entryId);
    }
}