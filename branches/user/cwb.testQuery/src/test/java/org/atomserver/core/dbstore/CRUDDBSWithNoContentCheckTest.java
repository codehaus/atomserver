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
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.core.AbstractAtomService;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.AtomWorkspace;

import java.io.File;

/**
 */
public class CRUDDBSWithNoContentCheckTest extends CRUDDBSTestCase {

    AtomWorkspace workspace = null;

    public static Test suite() { return new TestSuite(CRUDDBSWithNoContentCheckTest.class); }

    public void setUp() throws Exception {
        super.setUp();
        // Turn on alwaysUpdateEntry flag on the workspace.
        workspace = ((AbstractAtomService) getSpringFactory().getBean("org.atomserver-atomService")).getAtomWorkspace("widgets");
        workspace.getOptions().setAlwaysUpdateEntry(true);
    }

    public void tearDown() throws Exception {
        workspace.getOptions().setAlwaysUpdateEntry(false);
        super.tearDown();
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    // Overridded method to return same content for inserts and updates.
    protected String getFileXMLUpdate(int revno) {
        return super.getFileXMLUpdate(0);
    }

    // --------------------
    //       tests
    //---------------------

    public void testCRUDWithNoContentCompare() throws Exception {

        // run the tests up to some point
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        String finalEditLink = runCRUDTest();
        String url = getSelfUriFromEditUri(finalEditLink);
        int rev = extractRevisionFromURI(finalEditLink) - 1;

        // SELECT against the just deleted entry
        ClientResponse response = clientGet(url, null, 200, true);

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();
        log.debug("CONTENT= " + entryOut.getContent());
        assertEquals("<deletion xmlns=\"http://schemas.atomserver.org/atomserver/v1/rev0\" collection=\"acme\" id=\"12345\" locale=\"en\" workspace=\"widgets\"><property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"12345\" inNetwork=\"false\">\n"
                     + "<colors>"
                     + "<color isDefault=\"true\">teal</color>"
                     + "</colors>"
                     + "<contact>"
                     + "<contactId>1638</contactId>"
                     + "<displayName>This is an update 0</displayName>"
                     + "<hasEmail>true</hasEmail>"
                     + "</contact>"
                     + "</property></deletion>",
                     entryOut.getContent());

        response.release();
        if (contentStorage instanceof FileBasedContentStorage) {
            File pFile = getEntryFile(rev);
            assertTrue(pFile != null && pFile.exists());
        }
    }
}