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
import org.apache.commons.io.FileUtils;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;

import java.io.File;
import java.util.Locale;

/**
 */
public class NoDirDBSTest extends CRUDDBSTestCase {

    public static Test suite() { return new TestSuite(NoDirDBSTest.class); }

    public void setUp() throws Exception { super.setUp(); }

    public void tearDown() throws Exception { super.tearDown(); }

    // --------------------
    //       tests
    //---------------------
    public void testNoDir() throws Exception {

        String collection = "Foo-" + System.currentTimeMillis();

        String urlPath = "widgets/" + collection + "/12345.en.xml";
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        File blahDir = new File(TEST_DATA_DIR + "/widgets/" + collection);
        File pfile = getEntryFile("widgets", collection, "12345", Locale.ENGLISH, true, 0);
        assertFalse(blahDir.exists());
        assertFalse(pfile.exists());

        // A GET on a non-existent Collection should return a 404
        select(fullURL, true, 400, null);
        assertFalse(blahDir.exists());
        assertFalse(pfile.exists());

        // A PUT on a non-existent Collection should succeed and create the dir
        insert(id, fullURL, getFileXMLInsert() );
        assertTrue(blahDir.exists());
        assertTrue(pfile.exists());

        // A GET should now return it
        select(fullURL, true);
        assertTrue(blahDir.exists());
        assertTrue(pfile.exists());

        IRI widgetIRI = IRI.create("http://localhost:8080/" +
                                   widgetURIHelper.constructURIString("widgets", collection, "12345", Locale.ENGLISH));
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", widgetIRI.toString()), true);
        entriesDAO.obliterateEntry(entryTarget);

        FileUtils.deleteDirectory( blahDir );

        Thread.sleep(700);
        assertFalse(blahDir.exists());
        assertFalse(pfile.exists());
    }

}
