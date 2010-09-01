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
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomServer;
import org.atomserver.AtomServerWrapper;
import org.atomserver.ContentStorage;
import org.atomserver.DelegatingProvider;
import org.atomserver.server.servlet.AtomServerUserInfo;
import org.atomserver.testutils.client.JettyWebAppTestCase;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.testutils.latency.LatencyUtil;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.URIHandler;
import org.atomserver.utils.locale.LocaleUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
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

    protected DelegatingProvider provider;

    public static final File TEST_DATA_DIR = new File(
            new File(System.getProperty("user.dir")),
            System.getProperty("atomserver.data.dir").replaceFirst("^file\\:", ""));

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

    protected EntryTarget getEntryTarget( String workspace, String collection, String entryId, String locale ){
        IRI iri = IRI.create("http://localhost:8080/"
                             + widgetURIHelper.constructURIString(workspace, collection, entryId,
                                                                  LocaleUtils.toLocale(locale)));
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);
        return entryTarget;
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
        provider = (DelegatingProvider)(springContext.getBean("org.atomserver-atomServer"));
        if (provider.getCurrentProvider() instanceof AtomServerWrapper) {
            ((AtomServerWrapper)provider.getCurrentProvider()).getAtomServer().setService(store);
        } else {
            ((AtomServer)provider.getCurrentProvider()).setService( store );
        }

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
        LatencyUtil.accountForLatency();

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        log.debug( "Accept-Charset= " + options.getHeader("Accept-Charset") );

        if ( ifModifiedDate != null ) {
            log.debug( "ifModifiedDate = " + ifModifiedDate );
            options.setIfModifiedSince( ifModifiedDate );

            Date ddd = options.getIfModifiedSince();
            log.debug( "IfModifiedSince = " + ddd );
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

    protected void printFeed( Feed feed ) throws Exception {
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        feed.writeTo( abdera.getWriterFactory().getWriter("PrettyXML"), stringWriter );
        log.debug( "FEED = \n" + stringWriter.toString() );
    }

    protected void printEntry( Entry entry ) throws Exception {
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        entry.writeTo( abdera.getWriterFactory().getWriter("PrettyXML"), stringWriter );
        log.debug( "FEED = \n" + stringWriter.toString() );
    }


}
