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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.lang.StringUtils;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.server.servlet.BlockingFilterSettings;

/**
 * This test checks the blocking of contents, url paths, and user by setting the BlockingFilterSettings.
 */
public class BlockingFilterTest extends CRUDDBSTestCase {
    
    static final String REGX_PATH = "^\\/atomserver\\/v1\\/widgets\\/acme(.)*";    

    String editUrl = null;

    private BlockingFilterSettings getFilterSettings() throws Exception {
        return (BlockingFilterSettings) getSpringFactory().getBean("org.atomserver-blockingFilter");
    }

    public void setUp() throws Exception {
        super.setUp();
        removeEntry();
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    // --------------------
    //       tests
    //---------------------

    public void testBlockedByContentLength() throws Exception {

        // Block by content length.
        getFilterSettings().setMaxContentLength(10);
        String editLink = addEntry(413);

        // Unblock content length.
        getFilterSettings().setMaxContentLength(-1);
        editLink = addEntry(201);

        removeEntry();
    }

    public void testBlockedPaths() throws Exception {

        // setup entry
        String editUrl = addEntry(201);

        // Blocked by Path
        ClientResponse response = clientGet( editUrl, null, 200, true );
        response.release();

        getFilterSettings().addBlockedPath(REGX_PATH);
        response = clientGet( editUrl, null, 403, true );
        response.release();

        getFilterSettings().removeBlockedPath(REGX_PATH);
        response = clientGet( editUrl, null, 200, true );
        response.release();

        removeEntry();
    }

    public void testBlockedUser() throws Exception {
        // TODO: Jetty needs to be set up with authentication and restarted before the blocking user tests can be run.
        // Block User
//        getFilterSettings().addBlockedUser("foo");
//        response = clientGet( url, null, 403, true );
//        response.release();

        // Unblock user
//        getFilterSettings().removeBlockedUser("foo");
//        response = clientGet( url, null, 200, true );
//        response.release();
        
    }

    private String addEntry(int expectedResponse) throws Exception {
        String editLink = simpleInsert(expectedResponse);
        if(editLink != null) {
           return getSelfUriFromEditUri(editLink);
        }
        return null;
    }

    private void removeEntry() {
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    protected String simpleInsert(int expectedResponse ) throws Exception {

        String urlPath = getURLPath();
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = getFactory().newEntry();
        entry.setId(id);
        entry.setContentAsXhtml(getFileXMLInsert());

        log.debug("full URL = " + fullURL);
        ClientResponse response = client.put(fullURL, entry, options);

        int status = response.getStatus();

        assertEquals( expectedResponse, response.getStatus() );

        String editLinkStr = null;
        if(status == 201 || status == 200) {
            Document<Entry> doc = response.getDocument();
            Entry entryOut = null;
            try {
                entryOut = doc.getRoot();
            } catch (ClassCastException e) {
                log.error("ABDERA ERROR MESSAGE : " + ((org.apache.abdera.protocol.error.Error)doc.getRoot()).getMessage());
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

}
