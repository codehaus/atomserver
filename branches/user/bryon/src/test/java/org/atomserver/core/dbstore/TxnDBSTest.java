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
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.atomserver.core.filestore.FileBasedContentStorage;

import java.io.File;

/**
 */
public class TxnDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( TxnDBSTest.class ); }

    public void setUp() throws Exception { 
        super.setUp(); 
        //FileBasedContentStorage.setTestingDeleteBackupsOnExit( false );
        FileBasedContentStorage.setTestingAlternatelyFailOnPut( true );
    }

    public void tearDown() throws Exception { 
        super.tearDown();
        //FileBasedContentStorage.setTestingDeleteBackupsOnExit( true );
        FileBasedContentStorage.setTestingAlternatelyFailOnPut( false );
        destroyEntry( wspace, coll, id, null, false );
    }

    protected String getStoreName() 
    { return "org.atomserver-atomService"; }

    protected boolean requiresDBSeeding() 
    { return false; }

    private String wspace = "dummy"; 
    private String coll = "dumber"; 
    private String id = "12345"; 
    

    protected String getURLPath() 
    { return (wspace + "/" + coll + "/" + id + ".xml"); }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                                  + widgetURIHelper.constructURIString(wspace, coll, id, null ));
        return entryIRI;
    }

    protected String getPropfileBase() {
        return (userdir + "/var/" + wspace + "/" + coll + "/12/12345/12345.xml");
    }

    protected File getPropfile() {
        File propFile = new File( getPropfileBase() + ".r0");
        return propFile;
    }

    // --------------------
    //       tests
    //---------------------

    public void testTxn() throws Exception {
        String urlPath = getURLPath();
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        //INSERT
        String editURI = insert(id, fullURL);
        log.debug( "########################################## editURI = " + editURI );

        if (contentStorage instanceof FileBasedContentStorage) {
            File propFile = getPropfile();
            assertNotNull( propFile );
            log.debug(propFile);
            assertTrue(propFile.exists());
        }

        int rev = extractRevisionFromURI(editURI);
        assertEquals( 0, rev );
        
        // SELECT
        editURI = select(fullURL, true);
        log.debug( "########################################## editURI = " + editURI );
        rev = extractRevisionFromURI(editURI);
        assertEquals( 0, rev );

        // UPDATE
        // this one should fail cuz we set setTestingAlternatelyFailOnPut
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = getFactory().newEntry();
        entry.setId(id);
        entry.setContentAsXhtml( getFileXMLUpdate());

        ClientResponse response = client.put(fullURL, entry, options);
        assertEquals(500, response.getStatus());
     
        // SELECT
        editURI = select(fullURL, true);
        log.debug( "########################################## editURI = " + editURI );
        rev = extractRevisionFromURI(editURI);
        assertEquals( 0, rev );

        // verify that the file is rolled back
        // NOTE: the SELECT above has actually verified the content is the original content...

        File file = getPropfile();
        assertTrue( file.exists() );

        /*
        File bakFile = new File(userdir + "/var/" + wspace + "/" + coll + "/12/12345/12345.xml.r0");
        assertFalse( bakFile.exists() );
        */
        File newFile = new File(userdir + "/var/" + wspace + "/" + coll + "/12/12345/12345.xml.r1");
        assertTrue( !newFile.exists() ); 

    }
}
