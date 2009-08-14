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
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.io.FileUtils;
import org.atomserver.core.*;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.dbstore.utils.DBSeeder;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.URIHandler;
import org.springframework.context.ApplicationContext;

import java.text.MessageFormat;
import java.io.File;

/**
 */
public class DBSTestCase extends AtomServerTestCase {

    protected EntriesDAO entriesDao = null;
    protected EntryCategoriesDAO entryCategoriesDAO = null;

    protected int startCount = 0;

    protected void insertEntry(BaseEntryDescriptor entryDescriptor, boolean writeFileContent) throws Exception {
        DBSeeder.getInstance(getSpringFactory()).insertWidget(entryDescriptor);
        if (contentStorage instanceof FileBasedContentStorage && writeFileContent ) {
            String content = getContentString( entryDescriptor.getCollection(),
                                               entryDescriptor.getEntryId(),
                                               "Mr Blister" );
            contentStorage.putContent(content, entryDescriptor);
        }
    }

    protected String getContentString(String collection, String entryId, String contact) {
        return MessageFormat.format(
                "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" " +
                                    "systemId=\"{0}\" id=\"{1}\" inNetwork=\"false\"> \n" +
                                    "    <colors>\n" +
                                    "       <color isDefault=\"true\">teal</color>\n" +
                                    "    </colors>\n" +
                                    "    <contact> \n" +
                                    "        <contactId>{2}</contactId>\n" +
                                    "        <displayName>Chris</displayName> \n" +
                                    "        <hasEmail>true</hasEmail> \n" +
                                    "    </contact>\n" +
                                    "</property>\n",
                                    collection,
                                    entryId,
                                    contact);
    }

    public void setUp() throws Exception {
        super.setUp();

        ApplicationContext springContext = getSpringFactory();

        entriesDao = (EntriesDAO) springContext.getBean("org.atomserver-entriesDAO");
        entryCategoriesDAO = (EntryCategoriesDAO) springContext.getBean("org.atomserver-entryCategoriesDAO");

        // we may need something in the DB to run these tests
        if ( requiresDBSeeding() ) {

            File file = new File(getClass().getResource("/testentries/var").toURI());
            FileUtils.copyDirectory(file, TEST_DATA_DIR);

            DBSeeder.getInstance(springContext).seedEntriesClearingFirst();
        } else {
            DBSeeder.getInstance(springContext).createWidgetsDir();
        }

        startCount = entriesDao.getTotalCount( new BaseFeedDescriptor("widgets", "acme"));
        log.debug("startCount = " + startCount);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (requiresDBSeeding()) {
            FileUtils.deleteDirectory(TEST_DATA_DIR);            
        }
    }

    protected boolean requiresDBSeeding() { return false; }

    protected String getStoreName() { return "org.atomserver-atomService"; }

    public boolean checkTotalResults( String workspace ) {
        return store.getAtomWorkspace(workspace).getOptions().getDefaultProducingTotalResultsFeedElement();
    }

    //----------------------------------------------
    protected void createWidget(String workspace,
                                String collection,
                                String entryId,
                                String locale,
                                String xmlFileString,
                                boolean checkPhysicalFile) throws Exception {
        modifyEntry(workspace, collection, entryId, locale, xmlFileString, true, null, checkPhysicalFile);
    }

    protected void createWidget(String workspace,
                                String collection,
                                String entryId,
                                String locale,
                                String xmlFileString) throws Exception {
        modifyEntry(workspace, collection, entryId, locale, xmlFileString, true, null);
    }

    protected void updateWidget(String workspace,
                                String collection,
                                String entryId,
                                String locale,
                                String xmlFileString,
                                String revision) throws Exception {
        modifyEntry(workspace, collection, entryId, locale, xmlFileString, false, revision);
    }

    protected void modifyEntry(String workspace,
                               String collection,
                               String entryId,
                               String locale,
                               String xmlFileString,
                               boolean creating,
                               String revision) throws Exception {
        modifyEntry(workspace, collection, entryId, locale, xmlFileString, creating, revision, true);
    }

    protected void modifyEntry(String workspace,
                               String collection,
                               String entryId,
                               String locale,
                               String xmlFileString,
                               boolean creating,
                               String revision,
                               boolean checkContent) throws Exception {
        modifyEntry(workspace, collection, entryId, locale, xmlFileString, creating, revision, checkContent, true);
     }

     protected void modifyEntry(String workspace,
                                   String collection,
                                   String entryId,
                                   String locale,
                                   String xmlFileString,
                                   boolean creating,
                                   String revision,
                                   boolean checkContent,
                                   boolean checkCount ) throws Exception {

        log.debug("\n%%%%%%%%%%%%%% CREATING:: [" + workspace + ", " + collection + ", " + entryId + ", " + locale);

        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);

        int startCount = 0;
        if ( checkCount )  {
            startCount = entriesDao.getTotalCount(serviceDescriptor);
            log.debug("startCount = " + startCount);
        }

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = AtomServerTestCase.getFactory().newEntry();
        entry.setId(getURLPath(workspace, collection, entryId, locale, revision));
        entry.setContentAsXhtml(xmlFileString.replaceFirst("<\\?[^\\?]*\\?>", ""));

        String putUrl = getServerURL() + getURLPath(workspace, collection, entryId, locale, revision);
        log.debug("PUTting to URL : " + putUrl);
        ClientResponse response = client.put(putUrl, entry, options);

        Document<Entry> doc = response.getDocument();
         Entry entryOut;
         try {
             entryOut = doc.getRoot();
         } catch (Exception e) {
             Document<org.apache.abdera.protocol.error.Error> errorDoc = response.getDocument();
             log.error(errorDoc.getRoot().getMessage());
             throw e;
         }

         IRI editLink = entryOut.getEditLinkResolvedHref();
        assertNotNull("link rel='edit' must not be null", editLink);

        assertEquals( ("entry [" + workspace + "," + collection + "," + locale + "," + entryId +"]"),
                      creating ? 201 : 200, response.getStatus());
        response.release();

        // file system needs to catch up
        if (checkContent) {
            int rev = extractRevisionFromURI(editLink.toString());

            Thread.sleep( 300 );
            assertNotNull( contentStorage.getContent(new BaseEntryDescriptor(workspace,
                                                                             collection,
                                                                             entryId,
                                                                             LocaleUtils.toLocale(locale),
                                                                             (rev - 1) )));
        }

        // COUNT
        if ( checkCount ) {
            int exitCount = entriesDao.getTotalCount(serviceDescriptor);
           assertEquals((creating ? startCount + 1 : startCount), exitCount);
        }
    }

    // FIXME::  THESE 2 SHOULD BE destroyEntry !!!!
    protected void deleteEntry(String workspace, String collection, String entryId, String locale)
            throws Exception {
        URIHandler handler = widgetURIHelper;
        IRI iri = IRI.create("http://localhost:8080/"
                             + handler.constructURIString(workspace, collection, entryId, LocaleUtils.toLocale(locale)));

        log.debug("deleting IRI : " + iri);

        EntryTarget entryTarget = handler.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);
        entryCategoriesDAO.deleteEntryCategories(entriesDao.selectEntry(entryTarget));
        contentStorage.deleteContent(null, entryTarget);
        entriesDao.obliterateEntry(entryTarget);

        Thread.sleep( DB_CATCHUP_SLEEP );
    }

    // FIXME:: rename to deleteEntry
    protected void deleteEntry2(String fullURL) throws Exception {
        log.debug("\n%%%%%%%%%%%%%% DELETING:: [" + fullURL);

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        ClientResponse response = client.delete(fullURL, options);
        assertEquals(200, response.getStatus());
        response.release();
    }

    protected String getURLPath(String workspace, String collection, String entryId, String locale, String revision) {
        return workspace + "/" + collection + "/" + entryId +
               (locale == null ? "" : ("." + locale)) + ".xml" +
               (revision == null ? "" : "/" + revision);
    }

    protected String createWidgetXMLFileString(String entryId) {
        return "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"" + entryId + "\" inNetwork=\"false\">\n"
               + "<colors>"
               + "<color isDefault=\"true\">teal</color>"
               + "</colors>"
               + "<contact>"
               + "<contactId>1638</contactId>"
               + "<displayName>This is an insert</displayName>"
               + "<hasEmail>true</hasEmail>"
               + "</contact>"
               + "</property>";
    }

    protected Feed getPageByLastMod(String date, String workspace, String collection) {
        String url = workspace + "/" + collection + "?updated-min=" + date + "&locale=en";
        return getPage(url);
    }

    protected Feed getPageByLocale(String locale, String workspace, String collection) {
        String url = workspace + "/" + collection + "?locale=" + locale;
        return getPage(url);
    }

    protected Feed getPageByLocale(String locale, int statusCode, String workspace, String collection) {
        String url = workspace + "/" + collection + "?locale=" + locale;
        return getPage(url, statusCode);
    }

    protected Feed getPageByLocaleAndMaxResults(String locale, String workspace, String collection, int maxResults) {
        String url = workspace + "/" + collection + "?locale=" + locale + "&max-results=" + maxResults;
        return getPage(url);
    }

    protected Feed getPageByLastModAndLocale(String date, String locale, String workspace, String collection) {
        String url = workspace + "/" + collection + "?updated-min=" + date + "&locale=" + locale;
        return getPage(url);
    }

    protected Feed getPage(String url) {
        return getPage(url, 200);
    }

    protected Feed getPage(String url, int statusCode) {
        return getPage(url, statusCode, false);
    }

    protected Feed getPage(String url, int statusCode, boolean isFullURL) {
        log.debug("%%%%%%%%% url = " + url);

        ClientResponse response = clientGet(url, null, statusCode, isFullURL);

        if (response.getStatus() == 304) {
            response.release();
            return null;
        }

        Feed feed = (Feed) response.getDocument().getRoot();
        log.debug("SIZE=" + feed.getEntries().size());
        response.release();

        // every time we pull a feed in a test case, run through the update indices and assert
        // that they are in increasing order (note that it's not strictly increasing, since
        // several aggregate feeds can have the same updateIndex) 
        long updateIndex = 0L;
        for (Entry entry : feed.getEntries()) {
            Long nextIndex = Long.valueOf(entry.getSimpleExtension(AtomServerConstants.UPDATE_INDEX));
            assertTrue(nextIndex >= updateIndex);
            updateIndex = nextIndex;
        }

        return feed;
    }

    protected Entry getEntry(String workspace, String collection, String entryId, String locale) {
        String url = getServerURL() + getURLPath(workspace, collection, entryId, locale, null);
        return getEntry(url);
    }

    protected Entry getEntry(String url) {
        log.debug("%%%%%%%%% url = " + url);

        ClientResponse response = clientGet(url, null, 200, true);

        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();
        response.release();
        return entry;
    }

    protected Entry verifyEntry(ClientResponse response, String workspace, String collection, String entryId) {
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        assertTrue(entry.getContent().indexOf("id=\"" + entryId + "\"") != -1);

        assertTrue(entry.getUpdated().getTime() > 0L);
        assertEquals(entry.getUpdated(), entry.getPublished());

        assertTrue(entry.getId().toString().indexOf(workspace + "/" + collection + "/" + entryId) != -1);

        // alternate link should be null when there is content element
        assertNull(entry.getAlternateLink());

        assertNotNull(entry.getEditLink());
        assertNotNull(entry.getSelfLink());
        return entry;
    }

}
