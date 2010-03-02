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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.io.IOUtils;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.CRUDAtomServerTestCase;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.utils.ShardedPathGenerator;

import java.util.Arrays;
import java.util.List;

public class AutoTaggingDBSTest extends CRUDAtomServerTestCase {
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
            publishAndTestVersion(
                    "/testwidget.xml", "dummy/acme/167370.xml", 0,
                    Arrays.asList("green", "yellow", "purple", "blue", "DEFAULT:blue"),
                    true, "acmeALEX", "167370");

            publishAndTestVersion(
                    "/testwidget1.xml", "dummy/acme/2222.xml", 0,
                    Arrays.asList("red", "orange", "black", "brown", "DEFAULT:brown"),
                    true, "acmeCHRIS", "2222");

            publishAndTestVersion(
                    "/testwidget2.xml", "dummy/acme/23450.xml", 0,
                    Arrays.asList("pink", "red", "DEFAULT:red"),
                    true, "acmeBRYON", "23450");

            // test for case difference in terms.
            publishAndTestVersionWithCaseDifference(
                    "/testwidget2.xml", "dummy/acme/23450.xml", 1,
                    Arrays.asList("Pink", "red", "DEFAULT:red"),
                    false, "acmeBRYON", "23450");

        } finally {
            obliterateTestEntry();
        }
    }
   
    public void testTooLarge() throws Exception {
        try {
            // insert the Entry
            String urlPath = "dummy/acme/167370.xml";
            String fileName = "/testwidget3.xml";
            String xml = IOUtils.toString(getClass().getResourceAsStream(fileName));
            insert(urlPath, getServerURL() + urlPath, xml, false, 400, false);
        } finally {
            obliterateTestEntry();
        }
    }

    public void testEmptyTerm() throws Exception {
        try {
            String urlPath = "dummy/acme/875421.xml";
            String fileName = "/testwidget4.xml";
            String xml = IOUtils.toString(getClass().getResourceAsStream(fileName));
            insert(urlPath, getServerURL() + urlPath, xml, false, 400, false);
        } finally {
            obliterateTestEntry();
        }
    }

    public void testDeleteEmptyTerm() throws Exception {
        try {
            // First insert this entry and get it auto-tagged
            String urlPath = "dummy/acme/23450.xml";
            String fileName = "/testwidget2.xml";
            String xml = IOUtils.toString(getClass().getResourceAsStream(fileName));
            insert(urlPath, getServerURL() + urlPath, xml, false, 201, false);

            // Now insert an Category with an empty term in the DB
            //  Note: we cannot get one in any other way
            EntriesDAO entriesDAO = (EntriesDAO)getSpringFactory().getBean("org.atomserver-entriesDAO");
            EntryCategoriesDAO entryCategoriesDAO =
                    (EntryCategoriesDAO)getSpringFactory().getBean("org.atomserver-entryCategoriesDAO");

            EntryDescriptor descriptor  = new BaseEntryDescriptor("dummy", "acme", "23450", null, 0);
            EntryMetaData metaData = entriesDAO.selectEntry(descriptor);

            EntryCategory entryIn = new EntryCategory();
            entryIn.setEntryStoreId(metaData.getEntryStoreId());
            entryIn.setScheme( "urn:foo.colors" );
            entryIn.setTerm( "" );

            int numRows = entryCategoriesDAO.insertEntryCategory(entryIn);
            assertTrue(numRows > 0);

            // Now publish again. This will auto-tag again, and prove that we can delete when empty terms are present
            publishAndTestVersion(
                    "/testwidget2.xml", "dummy/acme/23450.xml", 1,
                    Arrays.asList("pink", "red", "DEFAULT:red"),
                    false, "acmeBRYON", "23450");
            
        } finally {
            obliterateTestEntry();
        }
    }


    private void obliterateTestEntry() throws Exception {
        EntriesDAO entriesDAO = (EntriesDAO) getSpringFactory().getBean("org.atomserver-entriesDAO");

        for (String id : Arrays.asList("167370", "2222", "23450", "875421")) {
            IRI entryIRI = IRI.create(
                    "http://localhost:8080/"
                    + widgetURIHelper.constructURIString("dummy", "acme", id, null));

            EntryTarget entryTarget =
                    widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext,
                                                                          "GET", entryIRI.toString()), true);
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

    private void publishAndTestVersion(String dataFileName, String urlPath, int rev,
                                       List<String> colors, boolean insert,
                                       String system, String id) throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream(dataFileName));
        publishAndTestVersionXml(xml, urlPath, rev, colors, insert, system, id);
    }

    private void publishAndTestVersionWithCaseDifference(String dataFileName, String urlPath, int rev,
                                       List<String> colors, boolean insert,
                                       String system, String id) throws Exception {
        String xml = IOUtils.toString(getClass().getResourceAsStream(dataFileName));
        xml = xml.replace("pink", "Pink");
        publishAndTestVersionXml(xml, urlPath, rev, colors, insert, system, id);
    }


    private void publishAndTestVersionXml(String xml, String urlPath, int rev,
                                       List<String> colors, boolean insert,
                                       String system, String id) throws Exception {
        if (insert) {
            insert(urlPath, getServerURL() + urlPath + "/" + rev, xml);
        } else {
            update(urlPath, getServerURL() + urlPath + "/" + rev, xml);
        }

        ClientResponse response = clientGetWithFullURL(getServerURL() + urlPath, 200);
        Document<Entry> doc = response.getDocument();
        List<Category> categories = doc.getRoot().getCategories();
        log.debug("******** " + categories);

        assertEquals(colors.size() + 4, categories.size());

        for (Category category : categories) {
            log.debug("SCHEME TERM = [" + category.getScheme().toString() + " "
                      + category.getTerm() + "]");

            if ("urn:foo.systems".equals(category.getScheme().toString())) {
                assertEquals(system, category.getTerm());
            } else if ("urn:sys.acme".equals(category.getScheme().toString())) {
                assertEquals(id, category.getTerm());
            } else if ("urn:composite".equals(category.getScheme().toString())) {
                assertEquals("acme-" + id, category.getTerm());
            } else if ("urn:test-stripes".equals(category.getScheme().toString())) {
                String expected = "" + ShardedPathGenerator.computeShard(id, 4, 2);
                assertEquals("expected [" + expected + "] was [" + category.getTerm() + "]",
                             expected, category.getTerm());
            } else {
                assertEquals("urn:foo.colors", category.getScheme().toString());
                assertTrue(colors.contains(category.getTerm()));
            }
        }
    }

}
