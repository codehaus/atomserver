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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.server.servlet.BlockingFilterSettings;

/**
 * This test checks the blocking of contents, url paths, and user by setting the BlockingFilterSettings.
 */
public class BlockingFilterTest extends CRUDDBSTestCase {

    static final String REGX_PATH = "^\\/atomserver\\/v1\\/widgets\\/acme(.)*";

    private String editUrl = null;
    private String propId2 = "A12345";

    public void setUp() throws Exception {
        super.setUp();
        removeEntry(propId); // make sure data is clean
    }

    public void tearDown() throws Exception {
        removeEntry(propId);
        super.tearDown();
    }

    // --------------------
    //       test
    //---------------------

    public void testBlockingFilter() throws Exception {

        checkBlockedByContentLength();
        checkBlockedPath();
        checkBlockedUser();
        checkBlockedWrites();
    }

    public void checkBlockedByContentLength() throws Exception {

        // Block by content length.
        getFilterSettings().setMaxContentLength(10);
        // This addEntry will set the servlet request's content length
        addEntry(false, 413);

        // This addEntry will not set the servlet request's content length
        addEntry(true, 413);

        // Unblock content length.
        getFilterSettings().setMaxContentLength(-1);

        // addEntry with servlet request's content length set
        editUrl = addEntry(false, 201);
        assertNotNull(editUrl);

        removeEntry(propId);

        // addEntry with servlet request's content length not being set.
        editUrl = addEntry(true, 201);

        assertNotNull(editUrl);

    }

    public void checkBlockedPath() throws Exception {

        // Blocked by Path
        ClientResponse response = clientGet(editUrl, null, 200, true);
        response.release();

        getFilterSettings().addBlockedPath(REGX_PATH);
        response = clientGet(editUrl, null, 403, true);
        response.release();

        getFilterSettings().removeBlockedPath(REGX_PATH);
        response = clientGet(editUrl, null, 200, true);
        response.release();

    }

    public void checkBlockedUser() throws Exception {
        // TODO: Jetty needs to be set up with authentication.
        // As it is now, this test does not do much to validate blocking of a user.

        // Block User
        getFilterSettings().addBlockedUser("foo");
        getFilterSettings().addBlockedUser("bar");
        //ClientResponse response = clientGet( editUrl, null, 403, true );
        ClientResponse response = clientGet(editUrl, null, 200, true); // should be 403 if working correctly.
        response.release();

        // Unblock user
        getFilterSettings().removeBlockedUser("foo");
        getFilterSettings().removeBlockedUser("bar");
        response = clientGet(editUrl, null, 200, true);
        response.release();

    }

    public void checkBlockedWrites() throws Exception {

        // Block Writes
        getFilterSettings().setWritesDisabled(true);

        // Try a write

        String urlPath =  "widgets/acme/" + propId2 + ".en.xml";
        addEntryWithURL(urlPath, 403);

        // Unblock writes
        getFilterSettings().setWritesDisabled(false);

        // Try a write
        addEntryWithURL(urlPath, 201);

        // Clean  up
        removeEntry(propId2);
    }

    private String addEntryWithURL(String urlPath, int expectedResponse) throws Exception {
        String editLink = simpleInsert( false, expectedResponse, urlPath);
        if (editLink != null) {
            return getSelfUriFromEditUri(editLink);
        }
        return null;
    }

    private String addEntry(boolean noContentLen, int expectedResponse) throws Exception {
        String editLink = simpleInsert(noContentLen, expectedResponse, getURLPath());
        if (editLink != null) {
            return getSelfUriFromEditUri(editLink);
        }
        return null;
    }

    private void removeEntry(String propertyId) {
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI(propertyId).toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    protected String simpleInsert(boolean noContentLen, int expectedResponse, String urlPath) throws Exception {

        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        if (noContentLen) {
            options.setUseChunked(true); // this will set the Content Length to -1
        }

        Entry entry = getFactory().newEntry();
        entry.setId(id);
        entry.setContentAsXhtml(getFileXMLInsert());

        log.debug("full URL = " + fullURL);
        ClientResponse response = client.put(fullURL, entry, options);

        int status = response.getStatus();

        assertEquals(expectedResponse, response.getStatus());

        String editLinkStr = null;
        if (status == 201 || status == 200) {
            Document<Entry> doc = response.getDocument();
            Entry entryOut = null;
            try {
                entryOut = doc.getRoot();
            } catch (ClassCastException e) {
                log.error("ABDERA ERROR MESSAGE : " + ((org.apache.abdera.protocol.error.Error) doc.getRoot()).getMessage());
                throw e;
            }

            IRI editLink = entryOut.getEditLinkResolvedHref();
            assertNotNull("link rel='edit' must not be null", editLink);

            editLinkStr = editLink.toString();

            response.release();
        }
        response.release();
        return editLinkStr;
    }

    private BlockingFilterSettings getFilterSettings() throws Exception {
        return (BlockingFilterSettings) getSpringFactory().getBean("org.atomserver-blockingFilter");
    }

}
