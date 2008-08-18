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

import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
import org.atomserver.uri.EntryTarget;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.core.filestore.FileBasedContentStorage;

/**
 */
public class RevisionDBSTest extends CRUDDBSTestCase {

    public static Test suite() { return new TestSuite(RevisionDBSTest.class); }

    public void setUp() throws Exception { super.setUp(); }

    public void tearDown() throws Exception {
        super.tearDown();
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    // --------------------
    //       tests
    //---------------------
    public void testCRUD() throws Exception {

        String id = getURLPath();
        String fullURL = getServerURL() + id;

        //INSERT
        String editURI = insert(id, fullURL);
        if (contentStorage instanceof FileBasedContentStorage) {
            assertTrue( getPropfile().exists());
        }

        // SELECT
        String fullURL0 = fullURL + "/0";
        editURI = select(fullURL0, true);
        log.debug("editURI= " + editURI);
        assertTrue(editURI.indexOf("/1") != -1);

        // SELECT
        // This should fail -- there is no 1 yet
        String fullURL1 = fullURL + "/1";
        editURI = select(fullURL1, true, 404);
        log.debug("editURI= " + editURI);
        assertNull(editURI);

        // UPDATE -- now we go to 1
        editURI = update(id, fullURL1);
        log.debug("editURI= " + editURI);
        assertTrue(editURI.indexOf("/2") != -1);

        // SELECT -- should find 1
        editURI = select(fullURL1, false);
        log.debug("editURI= " + editURI);
        assertTrue(editURI.indexOf("/2") != -1);

        // UPDATE
        // This should fail -- there is no 3 yet
        String fullURL3 = fullURL + "/3";
        editURI = update(id, fullURL3);
        log.debug("editURI= " + editURI);
        // this should return the proper edit URI
        assertTrue(editURI.indexOf("/2") != -1);

        // DELETE
        // This should fail -- there is no 3 yet
        editURI = delete(fullURL3);
        log.debug("editURI= " + editURI);
        // this should return the proper edit URI
        assertTrue(editURI.endsWith("/2"));

        // DELETE
        editURI = delete(editURI);
        log.debug("editURI= " + editURI);
        assertTrue(editURI.endsWith("/3"));

        // check that what happens when we delete a non-existent entry with the /* override is what we expect
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // try to delete an entry that never existed, verify that we get a 404
        assertEquals(404,
                     client.delete(getServerURL() + "widgets/acme/23456789.en.xml/*", options).getStatus());
        
        // try to delete the entry that we just removed, and verify that we get a 200
        //  We now allow this condition.....
        assertEquals(200, client.delete(fullURL + "/*", options).getStatus());
    }
}
