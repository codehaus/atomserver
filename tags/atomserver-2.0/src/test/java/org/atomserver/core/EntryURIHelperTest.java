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

import org.atomserver.*;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.FeedTarget;
import org.atomserver.uri.URIHandler;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.utils.AtomDate;
import org.atomserver.testutils.client.MockRequestContext;
import junit.framework.TestCase;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.abdera.Abdera;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;
import java.util.Locale;

/**
 * unit testutils the URIHelper.
 */
public class EntryURIHelperTest extends TestCase {

    private static final Log log = LogFactory.getLog(EntryURIHelperTest.class);


    protected URIHandler handler = null;
    protected ServiceContext serviceContext = null;
    static private final String CONTEXT_NAME = "org.apache.abdera.protocol.server.ServiceContext";

    protected String baseURI;

    protected void setUp() throws Exception {
        String[] configs = {"/org/atomserver/spring/propertyConfigurerBeans.xml",
                            "/org/atomserver/spring/databaseBeans.xml",
                            "/org/atomserver/spring/storageBeans.xml",
                            "/org/atomserver/spring/logBeans.xml",
                            "/org/atomserver/spring/abderaBeans.xml"};
        ApplicationContext springContext = new ClassPathXmlApplicationContext(configs);
        handler = ((AbstractAtomService) springContext.getBean("org.atomserver-atomService")).getURIHandler();
        serviceContext = (ServiceContext) springContext.getBean(CONTEXT_NAME);
        if (serviceContext.getAbdera() == null) {
            serviceContext.init(new Abdera(), null );
        }
        baseURI = handler.getServiceBaseUri();
    }

    public void testDate() {
        try { 
            String ddd = "12012007";
            Date date = AtomDate.parse( ddd );
            fail( "this should fail" );
        } catch ( IllegalArgumentException ee ) {
            log.error( ee );
        } 
       
        try { 
            String ddd = "2007-12-01";
            Date date = AtomDate.parse( ddd );
            log.debug( "********************* date= " + date );         
        } catch ( IllegalArgumentException ee ) {
            log.error( ee );
            fail( "this should NOT fail" );
        }  
    }

    public void testDate2() {
        try { 
            String iri = "http://foobar:7890/" + baseURI + "/widgets/acme/?updated-min=2007-11-30T23:59:59:000Z";
            handler.getFeedTarget(new MockRequestContext(serviceContext, "GET", iri));
        } catch ( IllegalArgumentException ee ) {
            log.error( ee );
        }

        try { 
            // This date is WRONG -- should be 59.000Z !!!
            //  NOTE: it should fail -- but it does NOT
            String ddd = "2007-11-30T23:59:59:000Z";
            Date upmin = AtomDate.parse( ddd );
            log.debug( "*********2007-11-30T23:59:59:000Z************ upmin= " + upmin );
        } catch ( IllegalArgumentException ee ) {
            log.error( ee );
        }

        String ddd2 = "2007-11-30T23:59:59.000Z";
        Date upmin2 = AtomDate.parse( ddd2 );
        log.debug( "********2007-11-30T23:59:59.000Z************* upmin2= " + upmin2 );         
        log.debug( "********2007-11-30T23:59:59.000Z************* upmin2 GMT= " + upmin2.toGMTString() );

        String iri2 = "http://stgbrss:7890/" + baseURI + "/widgets/acme?updated-min=2007-11-30T23:59:59+06:00";
        FeedTarget entryURIData = handler.getFeedTarget(new MockRequestContext(serviceContext, "GET", iri2));
        Date upmin = entryURIData.getUpdatedMinParam();
        log.debug( "********2007-11-30T23:59:59+06:00************* upmin= " + upmin );
        log.debug( "********2007-11-30T23:59:59+06:00************* upmin GMT= " + upmin.toGMTString() );
    }

    public void testSuccess() throws Exception {
        checkUriData(testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.en.xml", false),
                     "widgets", "acme", "1234", Locale.ENGLISH);
        checkUriData(testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234?locale=en_GB", false),
                     "widgets", "acme", "1234", Locale.UK);

        assertEquals(42,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?start-index=42", false)
                             .getPageDelimParam());

        assertEquals(8675309,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?max-results=8675309", false)
                             .getPageSizeParam());

        assertEquals(0,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml", false).getRevision());

        assertEquals(4,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml/4?max-results=8675309", false).getRevision());

        assertEquals(URIHandler.REVISION_OVERRIDE,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml/*", false).getRevision());

        assertEquals(URIHandler.REVISION_OVERRIDE,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml/*?start-index=15", false).getRevision());

        Date now = new Date();
        assertEquals(now,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?updated-min=" + AtomDate.format(now), false)
                             .getUpdatedMinParam());

        assertEquals(Locale.US,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234?locale=en_US", false)
                             .getLocaleParam());

        assertEquals(EntryType.full,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?entry-type=full", false)
                             .getEntryTypeParam());
 
        assertEquals(EntryType.link,
                     testDecodeEntryIRI(
                             "http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?entry-type=link", false)
                             .getEntryTypeParam());
 
    }

    public void testError() throws Exception {
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.xml", true);
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?start-index=pi", true);
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?max-results=pi", true);
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?updated-min=NEVER", true);
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?locale=aldsfkjasldfksdf", true);
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/1234.en.xml?entry-type=blah", true);
    }

    //>>>
    public void testMissingId() throws Exception {
        testDecodeEntryIRI("http://whatever/" + baseURI + "/widgets/acme/.en.xml", true);
    }


    private EntryTarget testDecodeEntryIRI(String iri,
                                            boolean expectBadUrlException) {
        log.debug("testing URI [" + iri + "] expecting " + (expectBadUrlException ? "failure" : "success"));
        try {
            EntryTarget entryURIData = handler.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri), true);
            // get all of the parameters - we don't care what the values are, but these should not throw
            // execptions (unless we EXPECT a BadUrlException, in which case we need to know that, too!)
            entryURIData.getLocaleParam();
            entryURIData.getPageDelimParam();
            entryURIData.getPageSizeParam();
            entryURIData.getUpdatedMinParam();
            entryURIData.getEntryTypeParam();
            assertFalse(expectBadUrlException);
            return entryURIData;
        } catch (BadRequestException e) {
            assertTrue(expectBadUrlException);
            return null;
        }
    }

    private void checkUriData(EntryTarget entryURIData, String workspace, String collection, String entryId, Locale locale) {
        assertEquals(workspace, entryURIData.getWorkspace());
        assertEquals(collection, entryURIData.getCollection());
        assertEquals(entryId, entryURIData.getEntryId());
        assertEquals(locale, entryURIData.getLocale());
    }

}
