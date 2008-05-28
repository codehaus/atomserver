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
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.File;

/**
 * testutils Keepalives
 */
public class KeepaliveDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( KeepaliveDBSTest.class ); }

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected String getStoreName() {
        return "org.atomserver-atomService";
    }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------
    public void testKeepalives() throws Exception {
        String urlPath = "widgets/acme/2797.en.xml";
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        File pFile = new File( userdir + "/var/widgets/acme/27/2797/en/2797.xml.r0" );
        assertNotNull( pFile );
        assertTrue( pFile.exists() );

        // NOTE: The SimpleHttpConnectionManager will always try to keep the connection open (alive) between consecutive requests.
        SimpleHttpConnectionManager connMngr = new SimpleHttpConnectionManager();
        HttpClient client = new HttpClient( connMngr );
        GetMethod get = new GetMethod( fullURL );
        get.setRequestHeader("Content-type", "text/xml; charset=UTF-8");
       
        // Execute the method.
        int statusCode = client.executeMethod( get );    
        
        log.debug( "STATUS CODE= " + statusCode );
        assertTrue( statusCode == 200 );

        // NOTE: you should release the connection here for HttpClient
        //       but we don't -- just to be extra sure that we're using the same HttpConnection...
        
        //-------------
        // This should be on the same Connection

        // Execute the method.
        statusCode = client.executeMethod( get );    
        
        log.debug( "STATUS CODE= " + statusCode );
        assertTrue( statusCode == 200 );  

       // Execute the method.
        statusCode = client.executeMethod( get );    
        
        log.debug( "STATUS CODE= " + statusCode );
        assertTrue( statusCode == 200 );  
    }
}
