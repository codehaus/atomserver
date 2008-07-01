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
import org.atomserver.core.filestore.TestingContentStorage;
import org.atomserver.testutils.conf.TestConfUtil;

import java.io.File;
import java.util.Locale;

/**
 */
public class FileRetryDBSTest extends CRUDDBSTestCase {

    private TestingContentStorage testingContentStorage = null;

    public static Test suite()
    { return new TestSuite( FileRetryDBSTest.class ); }

    public void setUp() throws Exception { 
        TestConfUtil.preSetup("fileErrorsConf");
        super.setUp();

        testingContentStorage =
                (TestingContentStorage) getSpringFactory().getBean("org.atomserver-contentStorage");
    }

    public void tearDown() throws Exception { 
        super.tearDown();
        TestConfUtil.postTearDown();
        testingContentStorage.setTestingAlternatelyFailOnFileReadException( false );
        testingContentStorage.setTestingAlternatelyFailOnFileReadNull( false );
        destroyEntry( wspace, coll, id, null, false );
    }

    protected String getStoreName() 
    { return "org.atomserver-atomService"; }

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
        return (TEST_DATA_DIR + "/" + wspace + "/" + coll + "/54/54321/54321.xml");
    }

    protected File getEntryFile(int revision) throws Exception {
        return getEntryFile(wspace, coll, "54321", null, true, revision);
    }

    // --------------------
    //       tests
    //---------------------

    public void testFileRetry() throws Exception {
        // Set to pass this time
        testingContentStorage.setTestingAlternatelyFailOnFileReadException( true );
        int maxRetries = FileBasedContentStorage.MAX_RETRIES;
        testingContentStorage.setTestingAlternatelyFailOnFileReadExceptionPassCount( maxRetries );

        insertThenSelect( 200 );
    }

    public void testFileRetry2() throws Exception {
        // Set to FAIL this time
        testingContentStorage.setTestingAlternatelyFailOnFileReadException( true );
        int maxRetries = FileBasedContentStorage.MAX_RETRIES;
        testingContentStorage.setTestingAlternatelyFailOnFileReadExceptionPassCount( maxRetries+1 );

        insertThenSelect( 500 );
        testingContentStorage.setTestingAlternatelyFailOnFileReadExceptionPassCount( maxRetries );
    }

    public void testFileRetry3() throws Exception {
        // Set to pass this time
        testingContentStorage.setTestingAlternatelyFailOnFileReadNull( true );
        int maxRetries = FileBasedContentStorage.MAX_RETRIES;
        testingContentStorage.setTestingAlternatelyFailOnFileReadNullPassCount( maxRetries );

        insertThenSelect( 200 );
    }

    public void testFileRetry4() throws Exception {
        // Set to FAIL this time
        testingContentStorage.setTestingAlternatelyFailOnFileReadNull( true );
        int maxRetries = FileBasedContentStorage.MAX_RETRIES;
        testingContentStorage.setTestingAlternatelyFailOnFileReadNullPassCount( maxRetries+1 );

        insertThenSelect( 500 );
        testingContentStorage.setTestingAlternatelyFailOnFileReadNullPassCount( maxRetries );
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
