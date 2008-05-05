/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.atomserver.core.dbstore;

import org.atomserver.ext.batch.Operation;
import org.atomserver.ext.batch.Results;
import org.atomserver.ext.batch.Status;
import org.atomserver.core.etc.AtomServerConstants;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;

import java.text.MessageFormat;

/**
 */
public class BatchDBSTest extends DBSTestCase {

    public static Test suite() { return new TestSuite(BatchDBSTest.class); }

    public void setUp() throws Exception { super.setUp(); }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected String getStoreName() {
        return "org.atomserver-atomService";
    }

    // --------------------
    //       tests
    //---------------------

    //  FIXME   FIXME   FIXME   FIXME   FIXME
    public void NOtestDisabled() throws Exception {
        AbderaClient client = new AbderaClient();

         RequestOptions options = client.getDefaultRequestOptions();
         options.setHeader("Connection", "close");

         String batchURI = getServerURL() + "widgets/acme/$batch";
         Feed batch = getFactory().newFeed();
         String updateText = "testInitialLoad()";
         batch.addEntry(createUpdateEntry("92345", updateText, 0));
         batch.addEntry(createUpdateEntry("92346", updateText, 0));
         ClientResponse clientResponse = runBatch(client, batchURI, batch, 403);
    }  

    public void testAll() throws Exception {
        runInitialLoad();
        runIntermixedInsertsAndUpdates();
        runUpdatesOnly();
        runOptimisticConcurrencyErrors();
        runBadRequests();
        runBatchTooLarge();
        runMultipleOperationsApplied();
        runNewIdNamespace();
        runDeleteNonexistent();
        runDeleteAll();

        Thread.sleep( 2000 );
    }


    public void runInitialLoad() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testInitialLoad()";
        batch.addEntry(createUpdateEntry("92345", updateText, 0));
        batch.addEntry(createUpdateEntry("92346", updateText, 0));
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 2, 0, 0, 0);
        assertEquals(batch.getEntries().size(), response.getEntries().size());
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            assertEquals("insert", operation.getType());
            assertEquals("201", status.getCode());
            assertEquals("CREATED", status.getReason());
            assertTrue(entry.getContent().contains(updateText));
        }
        clientResponse.release();
    }

    public void runIntermixedInsertsAndUpdates() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testIntermixedInsertsAndUpdates()";
        batch.addEntry(createUpdateEntry("92345", updateText, 0));
        batch.addEntry(createUpdateEntry("92346", updateText, 0));
        batch.addEntry(createUpdateEntry("92347", updateText, 0));
        batch.addEntry(createUpdateEntry("92348", updateText, 0));
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 2, 2, 0, 0);
        assertEquals(batch.getEntries().size(), response.getEntries().size());
        int insertCount = 0, updateCount = 0;
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            if (entry.getId().toString().contains("92347") || entry.getId().toString().contains("92348")) {
                assertTrue("insert".equals(operation.getType()));
                assertEquals("201", status.getCode());
                assertEquals("CREATED", status.getReason());
                insertCount++;
            } else if (entry.getId().toString().contains("92345") || entry.getId().toString().contains("92346")) {
                assertTrue("update".equals(operation.getType()));
                assertEquals("200", status.getCode());
                assertEquals("OK", status.getReason());
                updateCount++;
            }
            assertTrue(entry.getContent().contains(updateText));
        }
        assertEquals(2, insertCount);
        assertEquals(2, updateCount);
        clientResponse.release();
    }

    public void runUpdatesOnly() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testUpdatesOnly()";
        batch.addEntry(createOplessEntry("92345", updateText, 1));
        batch.addEntry(createOplessEntry("92346", updateText, 1));
        batch.addEntry(createOplessEntry("92347", updateText, 0));
        batch.addEntry(createOplessEntry("92348", updateText, 0));
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 0, 4, 0, 0);
        assertEquals(batch.getEntries().size(), response.getEntries().size());
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            assertEquals("update", operation.getType());
            assertEquals("200", status.getCode());
            assertEquals("OK", status.getReason());
            assertTrue(entry.getContent().contains(updateText));
        }
        clientResponse.release();
    }

    public void runOptimisticConcurrencyErrors() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testOptimisticConcurrencyErrors()";
        batch.addEntry(createUpdateEntry("92345", updateText, 1));
        batch.addEntry(createUpdateEntry("92346", updateText, 1));
        batch.addEntry(createUpdateEntry("92347", updateText, 1));
        batch.addEntry(createUpdateEntry("92348", updateText, 1));
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 0, 2, 0, 2);

        assertEquals(batch.getEntries().size(), response.getEntries().size());
        int errorCount = 0, updateCount = 0;
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            if (entry.getId().toString().contains("92345") || entry.getId().toString().contains("92346")) {
                assertEquals("insert", operation.getType());
                assertEquals("409", status.getCode());
                assertEquals("Optimisitic Concurrency Error:: /" + getBaseURI() + "/widgets/acme/$batch", status.getReason());
                assertFalse(entry.getContent().contains(updateText));
                errorCount++;
            } else if (entry.getId().toString().contains("92347") || entry.getId().toString().contains("92348")) {
                assertEquals("update", operation.getType());
                assertEquals("200", status.getCode());
                assertEquals("OK", status.getReason());
                assertTrue(entry.getContent().contains(updateText));
                updateCount++;
            }
        }
        assertEquals(2, errorCount);
        assertEquals(2, updateCount);
        clientResponse.release();
    }

    public void runBadRequests() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testBadRequests()";
        batch.addEntry(createUpdateEntry("2/3/4/5/6", updateText, 1));
        batch.addEntry(createUpdateEntry("#2323", updateText, 1));
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 0, 0, 0, 2);

        assertEquals(batch.getEntries().size(), response.getEntries().size());
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            assertEquals("update", operation.getType());
            assertEquals("400", status.getCode());
            // the error message must be one of the following...
            try {
                assertEquals("Bad Request:: /" + getBaseURI() + "/widgets/acme/$batch\n" +
                             "Reason:: Bad request URI: /widgets/acme/2/3/4/5/6.en.xml/1",
                             status.getReason());
            } catch (Throwable e) {
                assertEquals("Bad Request:: /" + getBaseURI() + "/widgets/acme/$batch\n" +
                             "Reason:: Bad request URI: /widgets/acme/#2323.en.xml/1",
                             status.getReason());
            }
        }

        clientResponse.release();
    }

    public void runBatchTooLarge() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testBatchTooLarge()";
        for (int i = 10; i < 30; i++) {
            batch.addEntry(createUpdateEntry("9900" + i, updateText, 1));
        }
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 400);
        assertEquals("Bad Request", clientResponse.getStatusText());
        org.apache.abdera.protocol.error.Error response =
                clientResponse.<org.apache.abdera.protocol.error.Error>getDocument().getRoot();

        assertEquals("Bad Request:: /" + getBaseURI() + "/widgets/acme/$batch\n" +
                     "Reason:: too many entries (20) in batch - max is 15",
                     response.getMessage());

        clientResponse.release();
    }

    public void runMultipleOperationsApplied() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        String updateText = "testMultipleOperationsApplied()";
        Entry updateEntry = createUpdateEntry("990041", updateText, 1);
        ((Operation)updateEntry.addExtension(AtomServerConstants.OPERATION)).setType("other");
        batch.addEntry(updateEntry);

        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();

        assertEquals(batch.getEntries().size(), response.getEntries().size());
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            assertEquals("update", operation.getType());
            assertEquals("400", status.getCode());
            assertEquals("Bad Request:: /" + getBaseURI() + "/widgets/acme/$batch\n" +
                         "Reason:: Multiple operations applied to one entry",
                         status.getReason());
        }

        clientResponse.release();
    }

    public void runNewIdNamespace() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // this run verifies that a URI that starts with $, but is not "$batch" will fail with a 404.
        String batchURI = getServerURL() + "widgets/acme/$other";
        Feed batch = getFactory().newFeed();
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 404);

        clientResponse.release();
    }

    public void runDeleteNonexistent() throws Exception {
        AbderaClient client = new AbderaClient();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        String batchURI = getServerURL() + "widgets/acme/$batch";
        Feed batch = getFactory().newFeed();
        batch.addEntry(createDeleteEntry("98765", 0));
        ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
        Feed response = clientResponse.<Feed>getDocument().getRoot();
        checkFeedResults(response, 0, 0, 0, 1);
        assertEquals(batch.getEntries().size(), response.getEntries().size());
        for (Entry entry : response.getEntries()) {
            Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
            Status status = entry.getExtension(AtomServerConstants.STATUS);
            assertEquals("delete", operation.getType());
            assertEquals("404", status.getCode());
            assertEquals("Unknown Entry:: /" + getBaseURI() + "/widgets/acme/$batch\n" +
                         "Reason:: Entry [widgets, acme, 98765, en] NOT FOUND", status.getReason());
        }
        clientResponse.release();
    }

    public void runDeleteAll() throws Exception {
        try {
            AbderaClient client = new AbderaClient();

            RequestOptions options = client.getDefaultRequestOptions();
            options.setHeader("Connection", "close");

            String batchURI = getServerURL() + "widgets/acme/$batch";
            Feed batch = getFactory().newFeed();
            ((Operation) batch.addExtension(AtomServerConstants.OPERATION)).setType("delete");
            batch.addEntry(createOplessEntry("92345", null, 2));
            batch.addEntry(createOplessEntry("92346", null, 2));
            batch.addEntry(createOplessEntry("92347", null, 2));
            batch.addEntry(createOplessEntry("92348", null, 2));
            ClientResponse clientResponse = runBatch(client, batchURI, batch, 200);
            Feed response = clientResponse.<Feed>getDocument().getRoot();
            checkFeedResults(response, 0, 0, 4, 0);
            assertEquals(batch.getEntries().size(), response.getEntries().size());
            for (Entry entry : response.getEntries()) {
                Operation operation = entry.getExtension(AtomServerConstants.OPERATION);
                Status status = entry.getExtension(AtomServerConstants.STATUS);
                assertEquals("delete", operation.getType());
                assertEquals("200", status.getCode());
                assertEquals("OK", status.getReason());
                assertTrue(entry.getContent().contains("<deletion"));
                if (entry.getId().toString().contains("92345") || entry.getId().toString().contains("92346")) {
                    assertTrue(entry.getContent().contains("testUpdatesOnly"));
                } else if (entry.getId().toString().contains("92347") || entry.getId().toString().contains("92348")) {
                    assertTrue(entry.getContent().contains("testOptimisticConcurrencyErrors"));
                }
            }
            clientResponse.release();
        } finally {
            deleteEntry("widgets", "acme", "92345", "en");
            deleteEntry("widgets", "acme", "92346", "en");
            deleteEntry("widgets", "acme", "92347", "en");
            deleteEntry("widgets", "acme", "92348", "en");
        }
    }



    private void checkFeedResults(Feed feed, int inserts, int updates, int deletes, int errors) {
        Results results = feed.getExtension(AtomServerConstants.RESULTS);
        log.debug("feed results: " + results);
        assertEquals(inserts, results.getInserts());
        assertEquals(updates, results.getUpdates());
        assertEquals(deletes, results.getDeletes());
        assertEquals(errors, results.getErrors());
    }

    private ClientResponse runBatch(AbderaClient client,
                                    String batchURI,
                                    Feed batch,
                                    int expectedStatus) throws Exception {
        ClientResponse clientResponse = client.put(batchURI, batch);
        assertEquals(expectedStatus, clientResponse.getStatus());
        return clientResponse;
    }

    private Entry createUpdateEntry(String id, String text, int revision) {
        Entry entry = getFactory().newEntry();
        entry.setContentAsXhtml(createPropertyXml(id, text));
        ((Operation) entry.addExtension(AtomServerConstants.OPERATION)).setType("update");
        entry.addLink(MessageFormat.format("/" + getBaseURI() + "/widgets/acme/{0}.en.xml/{1}", id, revision), "edit");
        return entry;
    }

    private Entry createDeleteEntry(String id, int revision) {
        Entry entry = getFactory().newEntry();
        ((Operation) entry.addExtension(AtomServerConstants.OPERATION)).setType("delete");
        entry.addLink(MessageFormat.format("/" + getBaseURI() + "/widgets/acme/{0}.en.xml/{1}", id, revision), "edit");
        return entry;
    }

    private Entry createOplessEntry(String id, String text, int revision) {
        Entry entry = getFactory().newEntry();
        if (text != null) {
            entry.setContentAsXhtml(createPropertyXml(id, text));
        }
        entry.addLink(MessageFormat.format("/" + getBaseURI() + "/widgets/acme/{0}.en.xml/{1}", id, revision), "edit");
        return entry;
    }


    private String createPropertyXml(String id, String text) {
        return MessageFormat.format("<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"{0}\" " +
                                    "inNetwork=\"false\">\n"
                                    + "<colors>\n"
                                    + "<color isDefault=\"true\">teal</color>\n"
                                    + "</colors>\n"
                                    + "<contact>\n"
                                    + "<contactId>1638</contactId>\n"
                                    + "<displayName>{1}</displayName>\n"
                                    + "<hasEmail>true</hasEmail>\n"
                                    + "</contact>\n"
                                    + "</property>",
                                    id,
                                    text);
    }
}
