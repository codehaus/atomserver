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

import org.springframework.context.ApplicationContext;

import java.io.File;

import org.apache.abdera.i18n.iri.IRI;
import org.atomserver.core.filestore.FileBasedContentStorage;

/**
 */
public class AltClassLoaderDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( AltClassLoaderDBSTest.class ); }

    public void setUp() throws Exception { 
        super.setUp(); 
    }

    public void tearDown() throws Exception { 
        super.tearDown();
        //destroyEntry( wspace, coll, id, null, false );
    }

    protected String getStoreName() 
    { return "org.atomserver-atomService"; }

    protected boolean requiresDBSeeding() 
    { return false; }

    private String wspace = "dummy"; 
    private String coll = "dumber"; 
    private String id = "34567"; 
    

    protected String getURLPath() 
    { return (wspace + "/" + coll + "/" + id + ".xml"); }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                                  + widgetURIHelper.constructURIString(wspace, coll, id, null ));
        return entryIRI;
    }

    protected File getPropfile() {
        File propFile = new File(userdir + "/var/" + wspace + "/" + coll + "/34/34567/34567.xml");
        return propFile;
    }

    // --------------------
    //       tests
    //---------------------

    public void testDummy() throws Exception {
    }

    public void XXXtestAltClassLoader() throws Exception {
        ApplicationContext springContext = getSpringFactory();

        // FIXME
        Object testBean = springContext.getBean( "testBean" );
        assertNotNull( testBean );

        log.debug( "testBean= " + testBean );


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

    }
}
