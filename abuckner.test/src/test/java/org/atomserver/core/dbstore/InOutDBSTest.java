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
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * testutils DB in/out.
 */
public class InOutDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( InOutDBSTest.class ); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------

    // Test selecting a widget, then resubmitting it...
    //   (this testutils showed up the UTF-8 bug -- and allowed me to fix it ;-)
    public void testInOut() throws Exception {
        String urlPath = "widgets/acme/2797.en.xml";
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        File pFile = new File( TEST_DATA_DIR + "/widgets/acme/27/2797/en/2797.xml.r0" );
        assertNotNull( pFile );
        assertTrue( pFile.exists() );

        // SELECT (this one we know is there -- in /var/widgets)
        ClientResponse response = clientGetWithFullURL( fullURL, 200 );
        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();

        if (response.getStatus() != 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(response.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.err.println(line);
            }
        }
        assertEquals(200, response.getStatus());
        IRI editLink = entryOut.getEditLinkResolvedHref();
        String editURI = editLink.toString();

        String xmlContent = entryOut.getContent();
        assertTrue( xmlContent.indexOf( "id=\"2797\"" ) != -1 );

        log.debug( "CONTENT= [" + xmlContent );
        response.release();

        // UPDATE
        editURI = update( id, editURI, xmlContent );

        // SELECT
        editURI = select( fullURL, false, 200, "id=\"2797\"" );
    }}
