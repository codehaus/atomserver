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
package org.atomserver.utils.test;

import junit.framework.TestCase;
import org.apache.abdera.model.*;
import org.apache.abdera.parser.stax.FOMParser;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.atomserver.testutils.latency.LatencyUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

public class TestingAtomServerTest extends TestCase {
    private static final String LOVEJOYS_CONTENT = "<bar xmlns=\"http://atomserver.org/bars\">Lovejoys</bar>";

    // A "good" bar, for testing that we can put valid content into the service
    private static final String LOVEJOYS =
            "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
            "  <id>bars/test/1234.xml</id>\n" +
            "  <content type=\"xhtml\">\n" +
            "    <div xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "      " + LOVEJOYS_CONTENT + "\n" +
            "    </div>\n" +
            "  </content>\n" +
            "</entry>";

    // A "bad" bar, for testing that we reject things that don't match the RNC
    private static final String AQUARIUM =
            "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
            "  <id>bars/test/1234.xml</id>\n" +
            "  <content type=\"xhtml\">\n" +
            "    <div xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "      <bar xmlns=\"http://atomserver.org/bars\"><name>Aquarium</name></bar>\n" +
            "    </div>\n" +
            "  </content>\n" +
            "</entry>";

    // a BAZ to POST
    private static final String BAZ =
            "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
            "  <content type=\"xhtml\">\n" +
            "    <div xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "      <baz xmlns=\"http://atomserver.org/baz\">I am a baz</baz>\n" +
            "    </div>\n" +
            "  </content>\n" +
            "</entry>";

    // test that our server works when configured through Spring
    public void testSpringConfigured() throws Exception {
        TestingAtomServer server = new TestingAtomServer();

        server.setSpringBeansLocation("org/atomserver/utils/test/workspaces.xml");

        runTests(server, "spring");
    }

    // test that our server works when configured through the API
    public void testAPIConfigured() throws Exception {
        TestingAtomServer server = new TestingAtomServer();

        server.addWorkspace("bars", false)
                .setRncLocation("classpath:org/atomserver/utils/test/bars.rnc")
                .setXPathAutotaggerRules(
                        "NAMESPACE bars = http://atomserver.org/bars;\n" +
                        "DELETE SCHEME {urn:bar.name};\n" +
                        "MATCH \"/bars:bar\" {urn:bar.name}$;");
        server.addWorkspace("baz", true);

        runTests(server, "api");
    }

    private void runTests(TestingAtomServer server, String collection) throws Exception {
        int port = server.start("atomserver", "v1");

        // create lovejoys
        putEntry("http://localhost:" + port + "/atomserver/v1/bars/" + collection + "/lovejoys.xml",
                 LOVEJOYS, 201);
        // update lovejoys
        putEntry("http://localhost:" + port + "/atomserver/v1/bars/" + collection + "/lovejoys.xml/1",
                 LOVEJOYS, 200);
        // should get an error from aquarium
        putEntry("http://localhost:" + port + "/atomserver/v1/bars/" + collection + "/aquarium.xml",
                 AQUARIUM, 422);

        LatencyUtil.updateLastWrote();

        // getting Lovejoys
        Entry entry = get(Entry.class, "http://localhost:" + port + "/atomserver/v1/bars/" + collection + "/lovejoys.xml");
        assertEquals(LOVEJOYS_CONTENT, entry.getContent());
        assertEquals("Lovejoys", entry.getCategories("urn:bar.name").get(0).getTerm());

        LatencyUtil.accountForLatency();

        // the feed should have ONE entry - LoveJoys
        Feed feed = get(Feed.class, "http://localhost:" + port + "/atomserver/v1/bars/" + collection + "?entry-type=full");
        assertEquals(1, feed.getEntries().size());
        assertEquals(LOVEJOYS_CONTENT, feed.getEntries().get(0).getContent());
        assertEquals("Lovejoys", feed.getEntries().get(0).getCategories("urn:bar.name").get(0).getTerm());

        // the service doc should have two workspaces - bars and baz
        Service service = get(Service.class, "http://localhost:" + port + "/atomserver/v1");
        assertEquals(2, service.getWorkspaces().size());
        for (Workspace ws : service.getWorkspaces()) {
            assertTrue(Arrays.asList("bars", "baz").contains(ws.getTitle()));
        }

        postEntry("http://localhost:" + port + "/atomserver/v1/baz/" + collection + "?locale=en_US",
                 BAZ, 201);

        server.stop();
    }

    private void putEntry(String url, String content, int expectedResponse) throws IOException {
        HttpClient client = new HttpClient();
        PutMethod put = new PutMethod(url);
        put.setRequestHeader("Content-type", "text/xml; charset=UTF-8");

        put.setRequestEntity(new StringRequestEntity(
                content,
                "application/xml",
                "UTF-8"));

        assertEquals(expectedResponse, client.executeMethod(put));
    }

    private void postEntry(String url, String content, int expectedResponse) throws IOException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(url);
        post.setRequestHeader("Content-type", "text/xml; charset=UTF-8");

        post.setRequestEntity(new StringRequestEntity(
                content,
                "application/xml",
                "UTF-8"));

        assertEquals(expectedResponse, client.executeMethod(post));
    }

    private <T extends Element> T get(Class<T> clazz, String url) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);

        client.executeMethod(get);
        return new FOMParser().<T>parse(new StringReader(get.getResponseBodyAsString())).getRoot();
    }
}
