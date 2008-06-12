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

import org.apache.abdera.model.Entry;
import org.apache.abdera.i18n.iri.IRI;

import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.RequestOptions;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.core.filestore.TestingContentStorage;
import org.atomserver.core.AtomServerTestCase;

import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.testutils.conf.TestConfUtil;

import java.io.File;

/**
 */
public class FailDBSTest extends DBSTestCase {

    private String propId = "34567";
    private IRI entryIRI = null;
    private String urlPath = null;
    private TestingContentStorage testingContentStorage = null;

    public static Test suite()
    { return new TestSuite( FailDBSTest.class ); }

    public void setUp() throws Exception { 
        TestConfUtil.preSetup("fileErrorsConf");
        super.setUp();

        testingContentStorage =
                (TestingContentStorage) getSpringFactory().getBean("org.atomserver-contentStorage");

        urlPath = "widgets/acme/" + propId + ".en.xml";
        entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString( "widgets", "acme", propId,
                                                                    LocaleUtils.toLocale("en")) );
    }

    public void tearDown() throws Exception { 
        super.tearDown();
        TestConfUtil.postTearDown();
        EntryTarget entryTarget = widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
        entriesDao.obliterateEntry(entryTarget);
    }

    // --------------------
    //       tests
    //---------------------

    public void testNullXML() throws Exception {
        badXMLTest( null );
    }

    public void testEmptyXML() throws Exception {
        badXMLTest( "" );
    }

    public void test500ErrorLogs() throws Exception {
        testingContentStorage.setTestingFailOnGet( true );
        badXMLTest( createWidgetXMLFileString( propId ), 500 );
        testingContentStorage.setTestingFailOnGet( false );        
    }
    

    public void badXMLTest( String fileXML ) throws Exception {
        badXMLTest( fileXML, 422 );
    }

    public void badXMLTest( String fileXML, int code ) throws Exception {
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = AtomServerTestCase.getFactory().newEntry();
        entry.setId(id);

        entry.setContentAsXhtml(fileXML);

        log.debug("full URL = " + fullURL);
        log.debug("entry    = " + entry);
        ClientResponse response = client.put(fullURL, entry, options);

        assertEquals(code, response.getStatus());
    }
}
