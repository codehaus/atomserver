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
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.core.AbstractAtomService;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.locale.LocaleUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

/**
 */
public class RawClientDBSTest extends JettyWebAppTestCase {

    static private Log log = LogFactory.getLog( RawClientDBSTest.class );
    static private final String userDir = System.getProperty( "user.dir" );

    private String propId = "24560";
    private IRI entryIRI;
    private String baseURI;

    private URIHandler uriHandler;
    private ApplicationContext springContext;
    private EntriesDAO widgetsDao;


    public static Test suite() 
    { return new TestSuite(RawClientDBSTest.class); }

    public void setUp() throws Exception {  
        super.setUp(); 
        springContext = getSpringFactory();

        uriHandler = ((AbstractAtomService) springContext.getBean("org.atomserver-atomService")).getURIHandler();
        baseURI = uriHandler.getServiceBaseUri();

        widgetsDao = (EntriesDAO) springContext.getBean("org.atomserver-entriesDAO");

        entryIRI = IRI.create("http://localhost:8080/"
                              + uriHandler.constructURIString( "widgets", "foobar", propId,
                                                               LocaleUtils.toLocale("en")) );
    }

    public void tearDown() throws Exception { 
        super.tearDown(); 

        // sometimes we comment out some tests in here and this may fail -- so just protect against that...
        try {
            ServiceContext serviceContext =
                    (ServiceContext) springContext.getBean("org.apache.abdera.protocol.server.ServiceContext");
            if (serviceContext.getAbdera() == null) {
                serviceContext.init(new Abdera(), null );
            }
            EntryTarget entryTarget = uriHandler.getEntryTarget(
                    new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
            widgetsDao.obliterateEntry(entryTarget);
        } catch ( Exception ee ) {
            log.error( "%%%%%%%%%%%%%%%%%%%%%%%%%% Could not find " + entryIRI + " to delete" );
        }

        deleteTestFile( userDir + "/target/var/widgets/foobar",
                        userDir + "/target/var/widgets/foobar/24/24560/en/24560.xml" );
    }

    private String getServerRoot()
    { return "http://localhost:" + getPort(); }

    private String getServerURL()
    { return getServerRoot() + "/" + baseURI + "/";}

    // --------------------
    //       tests
    //---------------------

    private void deleteTestFile( String parentDir, String fullPath ) throws Exception {
        File ff = new File( fullPath ); 
        if ( ff.exists() ) 
            ff.delete();
        
        File dd = new File( parentDir ); 
        if ( dd.exists() ) 
            FileUtils.deleteDirectory( dd );        
    }

    // No content element at all
    @SuppressWarnings("deprecation")
	public void testMissingEntryContent() throws Exception {

        String urlToCall = getServerURL() + "widgets/foobar/24560.en.xml";

        HttpClient client = new HttpClient();
        PutMethod put = new PutMethod( urlToCall );
        put.setRequestHeader("Content-type", "text/xml; charset=UTF-8");

        URL url = this.getClass().getResource( "/missingEntryContent.xml" );
        File file = new File( url.toURI() ); 

        FileInputStream fis = new FileInputStream( file );
        put.setRequestBody( fis );
        
        // Execute the method.
        int statusCode = client.executeMethod( put );    
        
        // 422 -- unprocessable Entity
        log.debug( "STATUS CODE= " + statusCode );
        assertTrue( statusCode == 422 );
    }

    // No content within the <div>
    @SuppressWarnings("deprecation")
	public void testMissingEntryContent2() throws Exception {

        String urlToCall = getServerURL() + "widgets/foobar/24560.en.xml";

        HttpClient client = new HttpClient();
        PutMethod put = new PutMethod( urlToCall );
        put.setRequestHeader("Content-type", "text/xml; charset=UTF-8");

        URL url = this.getClass().getResource( "/missingEntryContent2.xml" );
        File file = new File( url.toURI() ); 

        FileInputStream fis = new FileInputStream( file );
        put.setRequestBody( fis );
        
        // Execute the method.
        int statusCode = client.executeMethod( put );    
        
        // 422 -- unprocessable Entity
        log.debug( "STATUS CODE= " + statusCode );
        assertTrue( statusCode == 422 );
    }

    // Empty request 
    @SuppressWarnings("deprecation")
	public void testEmptyRequest() throws Exception {

        String urlToCall = getServerURL() + "widgets/foobar/24560.en.xml";

        HttpClient client = new HttpClient();
        PutMethod put = new PutMethod( urlToCall );
        put.setRequestHeader("Content-type", "text/xml; charset=UTF-8");

        URL url = this.getClass().getResource( "/empty.xml" );
        File file = new File( url.toURI() ); 

        FileInputStream fis = new FileInputStream( file );
        put.setRequestBody( fis );
        
        // Execute the method.
        int statusCode = client.executeMethod( put );    
        
        // 422 -- unprocessable Entity
        log.debug( "STATUS CODE= " + statusCode );
        assertEquals( 422, statusCode );
    }

    // No namespace on the entry element
    @SuppressWarnings("deprecation")
	public void testNoNamespace() throws Exception {

        String urlToCall = getServerURL() + "widgets/foobar/24560.en.xml";

        HttpClient client = new HttpClient();
        PutMethod put = new PutMethod( urlToCall );
        put.setRequestHeader("Content-type", "text/xml; charset=UTF-8");

        URL url = this.getClass().getResource( "/noNamespaceEntry.xml" );
        File file = new File( url.toURI() ); 

        FileInputStream fis = new FileInputStream( file );
        put.setRequestBody( fis );
        
        // Execute the method.
        int statusCode = client.executeMethod( put );    
        
        // 422 -- unprocessable Entity
        log.debug( "STATUS CODE= " + statusCode );
        assertTrue( statusCode == 422 );
    }

}
