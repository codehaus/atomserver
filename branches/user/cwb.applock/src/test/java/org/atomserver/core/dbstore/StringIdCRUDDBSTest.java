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
import org.apache.abdera.model.Document;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.i18n.iri.IRI;
import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.uri.EntryTarget;

import org.atomserver.testutils.client.MockRequestContext;

/**
 */
public class StringIdCRUDDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( StringIdCRUDDBSTest.class ); }

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    {
        super.tearDown();
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    protected String getURLPath() { return "dummy/ugga/bugga.en.xml"; }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString("dummy", "ugga", "bugga", LocaleUtils.toLocale("en")));
        return entryIRI;
    }

    // --------------------
    //       tests
    //---------------------

    public void testCRUD() throws Exception {

        // run the tests up to some point
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        String finalEditLink = runCRUDTest( false );
        String selfLink = getSelfUriFromEditUri(finalEditLink);

        // SELECT against the just deleted entry
        ClientResponse response = clientGet( selfLink, null, 200, true );

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();

        assertEquals("<deletion xmlns=\"http://schemas.atomserver.org/atomserver/v1/rev0\" collection=\"ugga\" id=\"bugga\" locale=\"en\" workspace=\"dummy\"><property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"12345\" inNetwork=\"false\">\n"
                     + "<colors>"
                     + "<color isDefault=\"true\">teal</color>"
                     + "</colors>"
                     + "<contact>"
                     + "<contactId>1638</contactId>"
                     + "<displayName>This is an update</displayName>"
                     + "<hasEmail>true</hasEmail>"
                     + "</contact>"
                     + "</property></deletion>",
                     entryOut.getContent());

        response.release();
    }
}
