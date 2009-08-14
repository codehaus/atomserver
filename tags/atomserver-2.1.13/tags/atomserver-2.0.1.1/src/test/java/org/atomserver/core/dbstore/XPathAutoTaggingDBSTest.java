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

import org.atomserver.core.CRUDAtomServerTestCase;
import org.atomserver.uri.EntryTarget;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.testutils.client.MockRequestContext;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.io.IOUtils;

import java.util.*;

public class XPathAutoTaggingDBSTest extends CRUDAtomServerTestCase {
    protected String getStoreName() { return "org.atomserver-atomService"; }

    protected String getURLPath() { return "dummy/acme/167370.xml"; }

    /*
    <colors>
        <color>green</color>
        <color>yellow</color>
        <color>purple</color>
        <color isDefault="true">blue</color>
    </colors>
    */
    public void testXPathAutoTagging() throws Exception {
         try {
            String urlPath = "dummy/acme/167370.xml";
            String editUri = publishAndTestVersion(
                    "/testwidget.xml", urlPath, 0,
                    Arrays.asList("green", "yellow", "purple", "blue", "DEFAULT:blue"),
                    true,"acmeALEX", "167370");

            editUri = publishAndTestVersion(
                    "/testwidget1.xml", urlPath, 0,
                    Arrays.asList("red", "orange", "black", "brown", "DEFAULT:brown"),
                    false,"acmeCHRIS", "2222");

            editUri = publishAndTestVersion(
                    "/testwidget2.xml", urlPath, 1,
                    Arrays.asList("pink", "red", "DEFAULT:red"),
                    false, "acmeBRYON", "23450");

            delete(editUri);
        } finally {
            EntriesDAO entriesDAO = (EntriesDAO) getSpringFactory().getBean("org.atomserver-entriesDAO");

            IRI entryIRI = IRI.create(
                    "http://localhost:8080/"
                    + widgetURIHelper.constructURIString("dummy", "acme", "167370", null));

            EntryTarget entryTarget =
                    widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);
        }
    }

    /*
    SCHEME TERM = [urn:foo.colors green]
    SCHEME TERM = [urn:foo.colors purple]
    SCHEME TERM = [urn:foo.systems acmeALEX]
    SCHEME TERM = [urn:foo.colors blue]
    SCHEME TERM = [urn:foo.colors yellow]
    SCHEME TERM = [urn:foo.colors DEFAULT:blue]
    */

    private String publishAndTestVersion(String dataFileName, String urlPath, int rev,
                                         List<String> colors, boolean insert,
                                         String system, String id) throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream(dataFileName));
        String editUri =
                insert ?
                insert(urlPath, getServerURL() + urlPath + "/" + rev, xml) :
                update(urlPath, getServerURL() + urlPath + "/" + rev, xml);

        ClientResponse response = clientGetWithFullURL(getServerURL() + urlPath, 200);
        Document<Entry> doc = response.getDocument();
        List<Category> categories = doc.getRoot().getCategories();
        log.debug("******** " + categories);

        assertEquals(colors.size() + 3, categories.size());

        for (Category category : categories) {
            log.debug( "SCHEME TERM = [" + category.getScheme().toString() + " "
                        + category.getTerm() + "]");

            if ("urn:foo.systems".equals(category.getScheme().toString())) {
                assertEquals(system, category.getTerm());
            } else if ("urn:sys.acme".equals(category.getScheme().toString())) {
                assertEquals(id, category.getTerm());
            } else if ("urn:composite".equals(category.getScheme().toString())) {
                assertEquals("acme-" + id, category.getTerm());
            } else {
                assertEquals("urn:foo.colors", category.getScheme().toString());
                assertTrue(colors.contains(category.getTerm()));
            }
        }
        return editUri;
    }

 }
