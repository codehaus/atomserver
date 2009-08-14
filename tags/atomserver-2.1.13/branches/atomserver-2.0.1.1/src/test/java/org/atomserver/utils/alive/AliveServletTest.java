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


package org.atomserver.utils.alive;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.ContentStorage;
import org.atomserver.core.dbstore.dao.AbstractDAOiBatisImpl;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.springframework.context.ApplicationContext;


public class AliveServletTest extends JettyWebAppTestCase {

    static private Log log = LogFactory.getLog( AliveServletTest.class );

    protected ApplicationContext springContext = null; 
    protected IsAliveHandler isAliveHandler = null;

    public static Test suite() 
    { return new TestSuite( AliveServletTest.class ); }

    public void setUp() throws Exception { 
        super.setUp();  
        springContext = getSpringFactory();
        isAliveHandler = (IsAliveHandler)( springContext.getBean("org.atomserver-isAliveHandler"));
    }

    public void tearDown() throws Exception {  
        super.tearDown(); 
    }

    private String getServerRoot()
    { return "http://localhost:" + getPort(); }

    // --------------------
    //       tests
    //---------------------
    public void testInitialState() throws Exception {
        // we should start up OK
        assertEquals( "OK", isAliveHandler.getAliveState() );
        assertTrue( isAliveHandler.isAlive().isOkay() );

        // This method is really meant ONLY to set once....
        isAliveHandler.setInitialAliveState( "DOWN");
        assertEquals( "DOWN", isAliveHandler.getAliveState() );
        assertTrue( isAliveHandler.isAlive().isDown() );

        isAliveHandler.setInitialAliveState( "OK");
        assertEquals( "OK", isAliveHandler.getAliveState() );       
        assertTrue( isAliveHandler.isAlive().isOkay() );
    }

    public void testSuccess() throws Exception {
        callAlivePage( "OK", 200 );
        assertEquals( "OK", isAliveHandler.getAliveState() );
        assertTrue( isAliveHandler.isAlive().isOkay() );

        isAliveHandler.deactivate();
        assertEquals( "DOWN", isAliveHandler.getAliveState() );
        assertTrue( isAliveHandler.isAlive().isDown() );

        // call it a couple of times to make sure it stays down ;-) 
        callAlivePage( "DOWN", 503 );
        callAlivePage( "DOWN", 503 );
        assertEquals( "DOWN", isAliveHandler.getAliveState() );
        assertTrue( isAliveHandler.isAlive().isDown() );

        isAliveHandler.activate();
        assertEquals( "OK", isAliveHandler.getAliveState() );
        callAlivePage( "OK", 200 );
        assertTrue( isAliveHandler.isAlive().isOkay() );

        ApplicationContext springContext = getSpringFactory();
        ContentStorage contentStorage = (ContentStorage)( springContext.getBean("org.atomserver-contentStorage") );
        if (contentStorage instanceof FileBasedContentStorage) {
            assertTrue(((FileBasedContentStorage)contentStorage).testingWroteAvailabiltyFile());
        }        
    }

    public void testFileSystemFailure() throws Exception {
        ApplicationContext springContext = getSpringFactory();
        ContentStorage contentStorage = (ContentStorage)( springContext.getBean("org.atomserver-contentStorage") );
        if (contentStorage instanceof FileBasedContentStorage) {
            ((FileBasedContentStorage)contentStorage).testingSetRootDirAbsPath( "foobar" );
            callAlivePage( "ERROR", 503 );
            assertTrue( isAliveHandler.isAlive().isError() );
        }
    }

    public void testDBFailure() throws Exception {
        AbstractDAOiBatisImpl.setTestingForceFailure( true );
        callAlivePage( "ERROR", 503 );
        assertTrue( isAliveHandler.isAlive().isError() );
        AbstractDAOiBatisImpl.setTestingForceFailure( false );
    }   

    private void callAlivePage( String returnText, int statusExpected ) throws Exception {
        log.debug ( "checking for " + returnText );

        // NOTE: redirect from /alive.txt is in the Resin conf file
        String urlToCall = getServerRoot() + "/" + getServletContextName() + "/alive";

        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod( urlToCall );       
        try { 
            // Execute the method.
            int statusCode = client.executeMethod( get );    
            
            log.debug( "STATUS CODE= " + statusCode );
            
            // Read the response body.
            byte[] responseBody = get.getResponseBody();
            
            // Deal with the response.
            String body = new String(responseBody); 
            log.debug(body);    
            
            assertTrue( body.indexOf( returnText ) != -1 );
            assertEquals( statusExpected, statusCode);
        } finally {
            // Release the connection.
            get.releaseConnection();
        }  
    }

}