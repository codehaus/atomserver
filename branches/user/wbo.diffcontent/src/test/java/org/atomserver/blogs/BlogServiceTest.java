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

package org.atomserver.blogs;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.io.FileUtils;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.atomserver.core.AtomServerTestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;

/**
 * This test was inspired from original work by Ugo Cei
 */
public class BlogServiceTest extends JettyWebAppTestCase {

    static protected Abdera abdera = new Abdera();
    static protected final String userdir = System.getProperty("user.dir");

    static public Test suite() { return new TestSuite(BlogServiceTest.class); }

    static protected String[] BLOG_CONFIGS = {"/org/atomserver/spring/propertyConfigurerBeans.xml",
                                              "blogsJettyBeans.xml"};

    public BlogServiceTest() {
        super(true, BLOG_CONFIGS);
    }

    public void setUp() throws Exception {

        File file = new File(getClass().getResource("/testentries/var").toURI());
        FileUtils.copyDirectory(file, AtomServerTestCase.TEST_DATA_DIR);

        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();

//        FileUtils.deleteDirectory(AtomServerTestCase.TEST_DATA_DIR);

        // have to give the file system a chance to catch up
        System.out.println("SLEEPING FOR 1 sec to give linux a chance to delete some testutils files");
        Thread.sleep(1000);
    }

    protected static Factory getFactory() { return abdera.getFactory(); }

    protected String getServerURL() {
        return "http://localhost:" + getPort() + "/atom/";
    }

    public void testGetServiceDocument() throws Exception {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        // do the introspection step
        ClientResponse response = client.get(getServerURL(), options);
        assertEquals(200, response.getStatus());
        Document<Service> service_doc = response.getDocument();
        assertNotNull(service_doc);
        assertEquals(1, service_doc.getRoot().getWorkspaces().size());
        Workspace workspace = service_doc.getRoot().getWorkspace("blogs");
        assertNotNull(workspace);
        for (Collection c : workspace.getCollections()) {
            assertNotNull(c.getTitle());
            assertNotNull(c.getHref());
            assertTrue(c.getHref().toString().startsWith(workspace.getTitle() + '/'));
        }
        response.release();
    }

    public void testReadFeed() throws Exception {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        ClientResponse response = client.get(getServerURL() + "blogs/bbc", options);
        assertEquals(200, response.getStatus());

        Feed feed = (Feed) response.getDocument().getRoot();
        assertEquals("Testing feed length", 2, feed.getEntries().size());
        response.release();
    }

    public void testReadUpdatedFeed() throws Exception {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // do the introspection step
        ClientResponse response = client.get(getServerURL(), options);
        Document<Service> service_doc = response.getDocument();

        Collection coll = service_doc.getRoot().getWorkspace("blogs").getCollection("abc");

        IRI collIri = coll.getResolvedHref();

        response.release();

        response = client.get(collIri.toString(), options);
        Feed feed = (Feed) response.getDocument().getRoot();
        int n = feed.getEntries().size();
        response.release();

        response = createBasicEntry(client);
        response.release();

        response = client.get(collIri.toString(), options);
        feed = (Feed) response.getDocument().getRoot();
        assertEquals("Testing feed length", n + 1, feed.getEntries().size());

        response.release();
    }

    public void testCreateEntry() throws Exception {

        AbderaClient client = new AbderaClient();
        ClientResponse response = createBasicEntry(client);

        assertEquals(201, response.getStatus());
        assertNotNull(response.getLocation());
        response.release();
    }

    public void testCreateAndReadEntry() throws Exception {

        AbderaClient client = new AbderaClient();
        ClientResponse response = createBasicEntry(client);

        assertEquals(201, response.getStatus());
        assertNotNull(response.getLocation());
        response.release();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        response = client.get(response.getLocation().toString(), options);
        assertEquals(200, response.getStatus());
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();
        assertEquals("Some text", entry.getContent());
        response.release();
    }

    public void testDeleteEntry() throws Exception {

        AbderaClient client = new AbderaClient();
        ClientResponse response = createBasicEntry(client);

        assertEquals(201, response.getStatus());
        assertNotNull(response.getLocation());
        String location = response.getLocation().toString();
        response.release();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        response = client.get(location, options);
        assertEquals(200, response.getStatus());
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();
        assertNotNull("link rel='edit' must not be null", entry.getEditLinkResolvedHref());
        response.release();

        response = client.delete(entry.getEditLinkResolvedHref().toString(), options);
        assertEquals(200, response.getStatus());
        response.release();

        response = client.get(location, options);
        assertEquals(404, response.getStatus());
        response.release();
    }

    public void testUpdateEntry() throws Exception {

        AbderaClient client = new AbderaClient();
        ClientResponse response = createBasicEntry(client);

        assertEquals(201, response.getStatus());
        assertNotNull(response.getLocation());
        response.release();

        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        response = client.get(response.getLocation().toString(), options);
        assertEquals(200, response.getStatus());
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();
        assertNotNull("link rel='edit' must not be null", entry.getEditLinkResolvedHref());
        response.release();

        entry.setContent("Modified text");
        response = client.put(entry.getEditLinkResolvedHref().toString(), entry, options);
        assertEquals(200, response.getStatus());
        doc = response.getDocument();
        Entry entry2 = doc.getRoot();
        assertEquals("Modified text", entry2.getContent());
        response.release();
    }

    private ClientResponse createBasicEntry(AbderaClient client) throws Exception {
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // do the introspection step
        ClientResponse response = client.get(getServerURL(), options);
        Document<Service> service_doc = response.getDocument();
        Workspace workspace = service_doc.getRoot().getWorkspace("blogs");

        Collection coll = workspace.getCollection("abc");

        IRI iri = coll.getResolvedHref();

        response.release();

        // post a new entry
        Entry entry = getFactory().newEntry();
        entry.setTitle("A test post");
        entry.setContent("Some text");
        entry.setId("test" + System.currentTimeMillis());
        entry.setUpdated(new Date());
        options.setSlug("test" + System.currentTimeMillis());

        response = client.post(iri.toString(), entry, options);

        if (response.getStatus() != 201) {
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.err.println(line);
            }
        }
        return response;
    }
}
