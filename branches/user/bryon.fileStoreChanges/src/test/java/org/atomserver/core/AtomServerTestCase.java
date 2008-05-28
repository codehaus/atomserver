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


package org.atomserver.core;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomServer;
import org.atomserver.ContentStorage;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.atomserver.uri.URIHandler;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
abstract public class AtomServerTestCase extends JettyWebAppTestCase {
    // ---------------------
    //   static variables
    // ---------------------
    protected static final int DB_CATCHUP_SLEEP = 300; 

    protected static final String APPLICATION_CONTEXT_CLASSPATH_LOCATION =
            "/org/atomserver/spring/atomServerApplicationContext.xml";

    protected static Abdera abdera = new Abdera();

    protected static final String userdir = System.getProperty( "user.dir" );
    
    private static final Pattern REVISION_PATTERN = Pattern.compile(".*/(\\d+)");

    static private final String CONTEXT_NAME =
            "org.apache.abdera.protocol.server.ServiceContext";

    // ---------------------
    //   instance variables
    // ---------------------
    protected Log log = LogFactory.getLog(AtomServerTestCase.class);

    protected AbstractAtomService store;

    protected ContentStorage contentStorage;

    protected URIHandler widgetURIHelper;

    protected String baseURI;

    protected ServiceContext serviceContext;

    // -------------------------------------------------------
    //                    METHODS
    // -------------------------------------------------------

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected String getServerRoot()
    { return "http://localhost:" + getPort(); }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected String getServerURL()
    { return getServerRoot() + "/" + getBaseURI() + "/";}

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public void setUp() throws Exception {
        super.setUp();

        ApplicationContext springContext = getSpringFactory();

        setStore( springContext );

        widgetURIHelper = ((AbstractAtomService) springContext
                .getBean("org.atomserver-atomService")).getURIHandler();
        baseURI = widgetURIHelper.getServiceBaseUri();
        serviceContext = (ServiceContext) springContext.getBean(CONTEXT_NAME);        
        if (serviceContext.getAbdera() == null) {
            serviceContext.init(new Abdera(), null );
        }
    }

    protected String getBaseURI() {
        return baseURI;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public void tearDown() throws Exception {
        super.tearDown();
    }

   //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // FIXME -- get from context
    static protected Factory getFactory() {
        return abdera.getFactory();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected void setStore( ApplicationContext springContext )
        throws Exception {

        String storeName = getStoreName();
        store = (AbstractAtomService)( springContext.getBean( storeName ) );
        AtomServer provider = (AtomServer)(springContext.getBean("org.atomserver-atomServer"));
        provider.setService( store );
        
        contentStorage = (ContentStorage) springContext.getBean("org.atomserver-contentStorage");
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    abstract protected String getStoreName();


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected ClientResponse clientGetWithFullURL( String url ) {
        return clientGet( url, null, 200, true );
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected ClientResponse clientGetWithFullURL( String url, int expectedResult ) {
        return clientGet( url, null, expectedResult, true );
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected ClientResponse clientGet( String url ) {
        return clientGet( url, null, 200, false );
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected ClientResponse clientGet( String url, Date ifModifiedDate, int expectedResult ) {
        return clientGet( url, ifModifiedDate, expectedResult, false );
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected ClientResponse clientGet( String url, Date ifModifiedDate, int expectedResult, boolean isFullURL ) {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        log.debug( ">>>>>>>>>>>>>>>>> " + options.getHeader("Accept-Charset") );

        if ( ifModifiedDate != null ) {
            log.debug( ">>>>>>>>>>>>>>>>> ifModifiedDate = " + ifModifiedDate );
            options.setIfModifiedSince( ifModifiedDate );

            Date ddd = options.getIfModifiedSince();
            log.debug( ">>>>>>>>>>>>>>>>> ddd = " + ddd );
        }

        log.debug( "Calling Abdera Client using GET on [" + url + "]" );
        ClientResponse response = null;
        if ( isFullURL )
            response = client.get( url, options );
        else
            response = client.get( getServerURL() + url, options );

        assertEquals( expectedResult, response.getStatus() );
        return response;
    }

    protected int extractRevisionFromURI(String uri) {
        if ( uri == null )
            return 0;
        Matcher matcher = REVISION_PATTERN.matcher(uri);
        assertTrue(matcher.matches());
        return Integer.parseInt(matcher.group(1));
    }

}
