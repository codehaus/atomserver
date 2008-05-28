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
import org.atomserver.core.filestore.FileBasedContentStorage;

import java.io.File;

/**
 */
public class FileRetryDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( FileRetryDBSTest.class ); }

    public void setUp() throws Exception { 
        super.setUp(); 
    }

    public void tearDown() throws Exception { 
        super.tearDown();
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadException( false );
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadNull( false );
        destroyEntry( wspace, coll, id, null, false );
    }

    protected String getStoreName() 
    { return "org.atomserver-atomService"; }

    protected boolean requiresDBSeeding() 
    { return false; }

    private String wspace = "dummy"; 
    private String coll = "dumber"; 
    private String id = "54321"; 
    

    protected String getURLPath() 
    { return (wspace + "/" + coll + "/" + id + ".xml"); }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                                  + widgetURIHelper.constructURIString(wspace, coll, id, null ));
        return entryIRI;
    }

    protected String getPropfileBase() {
        return (userdir + "/var/" + wspace + "/" + coll + "/54/54321/54321.xml");
    }

    protected File getPropfile() {
        File propFile = new File( getPropfileBase() + ".r0");
        return propFile;
    }

    // --------------------
    //       tests
    //---------------------

    public void testFileRetry() throws Exception {
        // Set to pass this time
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadException( true );
        int maxRetries = FileBasedContentStorage.getMaxRetries(); 
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadExceptionPassCount( maxRetries );

        insertThenSelect( 200 );
    }

    public void testFileRetry2() throws Exception {
        // Set to FAIL this time
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadException( true );
        int maxRetries = FileBasedContentStorage.getMaxRetries(); 
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadExceptionPassCount( maxRetries+1 );

        insertThenSelect( 500 );
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadExceptionPassCount( maxRetries );
    }

    public void testFileRetry3() throws Exception {
        // Set to pass this time
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadNull( true );
        int maxRetries = FileBasedContentStorage.getMaxRetries(); 
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadNullPassCount( maxRetries );

        insertThenSelect( 200 );
    }

    public void testFileRetry4() throws Exception {
        // Set to FAIL this time
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadNull( true );
        int maxRetries = FileBasedContentStorage.getMaxRetries(); 
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadNullPassCount( maxRetries+1 );

        insertThenSelect( 500 );
        FileBasedContentStorage.setTestingAlternatelyFailOnFileReadNullPassCount( maxRetries );
    }
    
    void insertThenSelect( int expectedResult ) throws Exception { 
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
        
        // Note: it is the GET that should see failures!!!
        // SELECT
        editURI = select(fullURL, true, expectedResult);
        log.debug( "########################################## editURI = " + editURI );
        rev = extractRevisionFromURI(editURI);
        assertEquals( 0, rev );
    }
}
