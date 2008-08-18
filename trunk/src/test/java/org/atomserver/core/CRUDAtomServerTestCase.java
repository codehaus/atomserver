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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Link;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.utils.PartitionPathGenerator;

import javax.xml.namespace.QName;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 */
abstract public class CRUDAtomServerTestCase extends AtomServerTestCase {

    // FIXME:: these must exist somewhere in Abdera`
    private static final String NS = "http://www.w3.org/2005/Atom";
    private static final String PFX = "";
    private static final QName LINK = new QName(NS, "link", PFX);


    private String currentEntryId = null;

    // -------------------------------------------------------
    public void setUp() throws Exception {
        super.setUp();
    }

    // -------------------------------------------------------
    public void tearDown() throws Exception {
        super.tearDown();
        cleanUp();
    }

    protected void cleanUp() throws Exception{
        if ( contentStorage instanceof FileBasedContentStorage) {
            // the test; runTest() can create revs 0 thru 5
            for ( int revision = 0 ; revision < 6; revision++ ) {
                File pFile = getEntryFile(revision);
                if (pFile != null && pFile.exists())
                   pFile.delete();
            }
         }
    }

    protected String getCurrentEntryId() {
        return currentEntryId;
    }

    protected void setCurrentEntryId(String currentEntryId) {
        this.currentEntryId = currentEntryId;
    }

    protected File getPropfile() throws Exception { return null; }

    protected File getEntryFile(int revision) throws Exception {
        return null;
    }

    protected File getEntryFile(String workspace,
                                String collection,
                                String entryId,
                                Locale locale,
                                boolean gzipped,
                                int revision) 
            throws Exception {
        FileBasedContentStorage contentStorage = (FileBasedContentStorage)this.contentStorage;
        PartitionPathGenerator pathGenerator = contentStorage.getPartitionPathGenerators().get(0);
        log.debug("PATH GENERATOR CLASS : " + pathGenerator.getClass());
        return contentStorage.generateEntryFilePath(
                new BaseEntryDescriptor(workspace, collection, entryId, locale),
                pathGenerator,
                gzipped,
                revision);
    }

    protected String getFileXMLInsert() {
        String fileXMLInsert =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"12345\" inNetwork=\"false\">\n"
            + "<colors>"
            + "<color isDefault=\"true\">teal</color>"
            + "</colors>"
            + "<contact>"
            + "<contactId>1638</contactId>"
            + "<displayName>This is an insert</displayName>"
            + "<hasEmail>true</hasEmail>"
            + "</contact>"
            + "</property>";
        return fileXMLInsert;
    }

    protected String getFileXMLUpdate() {
        String fileXMLUpdate =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"12345\" inNetwork=\"false\">\n"
            + "<colors>"
            + "<color isDefault=\"true\">teal</color>"
            + "</colors>"
            + "<contact>"
            + "<contactId>1638</contactId>"
            + "<displayName>This is an update</displayName>"
            + "<hasEmail>true</hasEmail>"
            + "</contact>"
            + "</property>";
        return fileXMLUpdate;
    }

    abstract protected String getURLPath();

    protected String getSelfUriFromEditUri( String editUri ) {
        int rev = extractRevisionFromURI(editUri) - 1;
        int last = editUri.lastIndexOf("/");
        String selfUri = editUri.substring(0, last);
        selfUri = selfUri + "/" + rev;
        log.debug("editUri= " + editUri + "  selfUri= " + selfUri);
        return selfUri;
    }

    //=========================
    protected String runCRUDTest() throws Exception {
        return runCRUDTest( true );
    }

    protected String runCRUDTest( boolean shouldCheckFile ) throws Exception {
        return runCRUDTest( shouldCheckFile, getURLPath() );
    }

    protected String runCRUDTest( boolean shouldCheckFile, String urlPath ) throws Exception {
        return runCRUDTest( shouldCheckFile, urlPath, true );
    }

    protected String runCRUDTest( boolean shouldCheckFile, String urlPath, boolean shouldCheckOptConc )
            throws Exception {
        return runCRUDTest( shouldCheckFile, urlPath, shouldCheckOptConc, true );
    }

    protected String runCRUDTest( boolean shouldCheckFile, String urlPath,
                                  boolean shouldCheckOptConc, boolean expects201 )
            throws Exception {
        return runCRUDTest( shouldCheckFile, urlPath, shouldCheckOptConc, expects201, false );
    }

    protected String runCRUDTest( boolean shouldCheckFile, String urlPath,
                                  boolean shouldCheckOptConc, boolean expects201, boolean allowsAny )
            throws Exception {
        return runCRUDTest( shouldCheckFile, urlPath, shouldCheckOptConc, expects201, allowsAny, false, null );
    }

    protected String runCRUDTest(boolean shouldCheckFile, String urlPath,
                                 boolean shouldCheckOptConc, boolean expects201,
                                 boolean allowsAny, boolean doPost, String locale )
            throws Exception {
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        log.debug( "DOING A POST = " + doPost + " locale= " + locale);

        //INSERT
        String editURI = null;
        if ( doPost ) {
            String urlToPost = (locale == null) ? fullURL : fullURL + "?locale=" + locale ;
            editURI = post(id, urlToPost, getFileXMLInsert(), 201 );
            if (locale == null) {
                fullURL = fullURL + "/" + getCurrentEntryId() + ".xml";
            } else {
                fullURL = fullURL + "/" + getCurrentEntryId() + "." + locale + ".xml";
            }
            log.debug( "fullURL = " + fullURL );
        } else {
            String insertURL = ( shouldCheckOptConc ) ? fullURL : (fullURL + "/*") ;
            editURI = insert(id, insertURL, getFileXMLInsert(), expects201, allowsAny );
        }

        log.debug( "########################################## editURI = " + editURI );
        if (  shouldCheckFile && contentStorage instanceof FileBasedContentStorage) {
            File propFile = getEntryFile(0);
            assertNotNull( propFile );
            log.debug("propFile " + propFile);
            assertTrue(propFile.exists());
        }

        int rev = 0;
        if ( shouldCheckOptConc ) {
            rev = extractRevisionFromURI(editURI);
            assertEquals( 1, rev );
        }

        // SELECT
        String xmlTestString = ( shouldCheckOptConc ) ? "This is an insert" : null;
        editURI = select(fullURL, true, 200, xmlTestString);
        log.debug( "########################################## editURI = " + editURI );
        if ( shouldCheckOptConc ) {
            rev = extractRevisionFromURI(editURI);
            assertEquals( 1, rev );
        }

        // UPDATE
        String updateURL = ( shouldCheckOptConc ) ? editURI : (fullURL + "/*") ;
        editURI = update(id, updateURL, getFileXMLUpdate(), allowsAny);
        log.debug( "########################################## editURI = " + editURI );
        if (  shouldCheckFile && contentStorage instanceof FileBasedContentStorage) {
            File propFile = getEntryFile(1);
            assertNotNull( propFile );
            log.debug("propfile= " + propFile);
            assertTrue(propFile.exists());
        }
        if ( shouldCheckOptConc ) {
            rev = extractRevisionFromURI(editURI);
            assertEquals( 2, rev );
        }

        // SELECT
        xmlTestString = ( shouldCheckOptConc ) ? "This is an update" : null;
        editURI = select(fullURL, false, 200, xmlTestString);
        log.debug( "########################################## editURI = " + editURI );
        if ( shouldCheckOptConc ) {
            rev = extractRevisionFromURI(editURI);
            assertEquals( 2, rev );
        }

        // DELETE
        String deleteURL = ( shouldCheckOptConc ) ? editURI : (fullURL + "/*") ;
        editURI = delete(deleteURL);
        log.debug( "########################################## editURI = " + editURI );
        if (  shouldCheckFile && contentStorage instanceof FileBasedContentStorage) {
            File propFile = getEntryFile(2);
            assertNotNull( propFile );
            log.debug("propfile= " + propFile);
            assertTrue(propFile.exists());
        }
        if ( shouldCheckOptConc ) {
            rev = extractRevisionFromURI(editURI);
            assertEquals( 3, rev );
        }

        if (getStoreName().equals("org.atomserver-atomService") && shouldCheckOptConc ) {
            // figure out what the last revision from the delete was
            int revision = extractRevisionFromURI(editURI);

            // Use the /* to override, no matter the revision
            String fullURLWithRevisionOverride = fullURL + "/*";

            // UPDATE (with override)
            editURI = update(id, fullURLWithRevisionOverride);
            assertEquals(revision + 1, (revision = extractRevisionFromURI(editURI)));

            // UPDATE (with override) AGAIN
            editURI = update(id, fullURLWithRevisionOverride);
            assertEquals(revision + 1, (revision = extractRevisionFromURI(editURI)));

            // DELETE (with override)
            editURI = delete(fullURLWithRevisionOverride);
            assertEquals(revision + 1, (revision = extractRevisionFromURI(editURI)));
        }
        
        return editURI;
    }

    //=========================
    //       insert 
    //=========================
    protected String insert(String id, String fullURL) throws Exception {
        return insert(id, fullURL, getFileXMLInsert() );
    }

    protected String insert(String id, String fullURL, String fileXML) throws Exception {
        return insert(id, fullURL, fileXML, true );
    }

    protected String insert(String id, String fullURL, String fileXML, boolean expects201) throws Exception {
        return insert(id, fullURL, fileXML, expects201, false );
    }

    protected String insert(String id, String fullURL, String fileXML,
                            boolean expects201, boolean allowAny ) throws Exception {
        int expectedStatus = ( expects201 ) ? 201 : 200;
        return insert(id, fullURL, fileXML, allowAny, expectedStatus, expects201);
    }

    protected String insert(String id, String fullURL, String fileXML,
                            boolean allowAny, int expectedStatus ) throws Exception {
        return insert(id, fullURL, fileXML, allowAny, expectedStatus, false );

    }

    protected String insert(String id, String fullURL, String fileXML,
                            boolean allowAny, int expectedResponse, boolean expects201 ) throws Exception {
        assertNotNull( fileXML);

        // INSERT
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = getFactory().newEntry();
        entry.setId(id);
        entry.setContentAsXhtml(fileXML);

        log.debug("full URL = " + fullURL);
        ClientResponse response = client.put(fullURL, entry, options);

        int status = response.getStatus();
        log.debug( "********************************");
        log.debug( "response.getStatus() = " + status );

        boolean statusIs200or201 = ((status == 200) || (status == 201 ));
        if ( !statusIs200or201 && ( allowAny || expectedResponse > 201 )) {
            log.warn( "WARN::::::::: status = " + status + " for " + fullURL + " expected = " + expectedResponse );
            if ( !allowAny )
                assertEquals( expectedResponse, status );

            String editLinkStr = null;
            if ( expectedResponse == 409 ) {
                editLinkStr = get409EditLink( response );
            }
            response.release();
            return editLinkStr;
        }
        if ( expects201 ) {
            assertEquals(201, status);
        }  else {

            if ( status == 409 ) {
                String editLinkStr = get409EditLink( response );
                response.release();
                return editLinkStr;
            }
            else {
                assertTrue( "status must be either 200 or 201 (" + status + ")", statusIs200or201 );
            }
        }

        Document<Entry> doc = response.getDocument();
        Entry entryOut = null;
        try {
            entryOut = doc.getRoot();
        } catch (ClassCastException e) {
            log.error("ABDERA ERROR MESSAGE : " + ((org.apache.abdera.protocol.error.Error)doc.getRoot()).getMessage());
            throw e;
        }

        IRI editLink = entryOut.getEditLinkResolvedHref();
        assertNotNull("link rel='edit' must not be null", editLink);

        String editLinkStr = editLink.toString();

        response.release();
        return editLinkStr;
    }

    private String get409EditLink( ClientResponse response ) {
        assertEquals( 409, response.getStatus() );
        Document<ExtensibleElement> doc = response.getDocument();
        ExtensibleElement error = doc.getRoot();
        log.debug( "&&&&&&&&&&&&&& error = " + error );

        Link link = error.getExtension( LINK );
        IRI editLink = link.getResolvedHref();
        String editLinkStr = null;
        if ( editLink != null )
            editLinkStr = editLink.toString();

        return editLinkStr;
    }

    // This method causes the Client to barf. So do NOT use it unless you are reporting an error....
    private void dumpResponse( ClientResponse response ) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(response.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            log.error(line);
        }
        br.close();
        Thread.sleep(500);
    }


    //=========================
    //      POST
    //=========================
    protected String post(String id, String fullURL, String fileXML, int expectedResponse ) throws Exception {
        assertNotNull( fileXML);

        // INSERT
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = getFactory().newEntry();
        if ( id != null )
            entry.setId(id);
        entry.setContentAsXhtml(fileXML);

        log.debug("full URL = " + fullURL);
        ClientResponse response = client.post(fullURL, entry, options);

        int status = response.getStatus();
        log.debug( "********************************");
        log.debug( "response.getStatus() = " + status );

        assertEquals( expectedResponse,response.getStatus() );

        Document<Entry> doc = response.getDocument();
        Entry entryOut = null;
        try {
            entryOut = doc.getRoot();
        } catch (ClassCastException e) {
            log.error("ABDERA ERROR MESSAGE : " + ((org.apache.abdera.protocol.error.Error)doc.getRoot()).getMessage());
            throw e;
        }

        IRI editLink = entryOut.getEditLinkResolvedHref();
        assertNotNull("link rel='edit' must not be null", editLink);

        String editLinkStr = editLink.toString();

        String entryId = entryOut.getSimpleExtension(AtomServerConstants.ENTRY_ID);
        log.debug( "entryId= " + entryId);
        setCurrentEntryId( entryId );

        response.release();
        return editLinkStr;
    }


    //=========================
    //      select
    //=========================
    protected String select(String fullURL, boolean isInsert) throws Exception {
        return select(fullURL, isInsert, 200);
    }

    protected String select(String fullURL, boolean isInsert, int expectedResult) throws Exception {
        String xmlTestString = (isInsert) ? "This is an insert" : "This is an update";
        return select(fullURL, isInsert, expectedResult, xmlTestString);
    }

    protected String select(String fullURL, boolean isInsert,
                            int expectedResult, String xmlTestString ) throws Exception {

        // SELECT
        ClientResponse response = clientGetWithFullURL(fullURL, expectedResult);

        IRI editLink = null;
        if (response.getStatus() != 200) {
            assertEquals( expectedResult, response.getStatus());
            if ( response.getStatus() == 409 ) {
                Document<ExtensibleElement> doc = response.getDocument();
                ExtensibleElement error = doc.getRoot();
                log.debug( "&&&&&&&&&&&&&& error = " + error );

                Link link = error.getExtension( LINK );
                log.debug( "&&&&&&&&&&&&&& editLink = " + editLink );
                editLink = link.getResolvedHref();
            }
            else {
                return null;
            }
        } else {
            Document<Entry> doc = response.getDocument();
            Entry entryOut = doc.getRoot();
            editLink = entryOut.getEditLinkResolvedHref();

            log.debug( "CONTENT:: " + entryOut.getContent() );
            log.debug( "xmlTestString:: " + xmlTestString );
            if ( xmlTestString != null )
                assertTrue(entryOut.getContent().indexOf(xmlTestString) != -1);
        }
        assertNotNull("link rel='edit' must not be null", editLink);
        response.release();
        return editLink.toString();
    }

    //=========================
    //    update 
    //=========================
    protected String update(String id, String fullURL) throws Exception {
        return update(id, fullURL, getFileXMLUpdate() );
    }

    protected String update(String id, String fullURL, String fileXML) throws Exception {
        return update(id, fullURL, fileXML, false );
    }

    protected String update(String id, String fullURL, String fileXML, boolean allowsAny ) throws Exception {
        return update(id, fullURL, fileXML, allowsAny, -1 );
    }

    protected String update(String id, String fullURL, String fileXML,
                            boolean allowsAny, int expectedStatus ) throws Exception {
        assertNotNull( fileXML);
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        Entry entry = getFactory().newEntry();
        entry.setId(id);
        entry.setContentAsXhtml(fileXML);

        ClientResponse response = client.put(fullURL, entry, options);

        log.debug("&&&&&&&&&&&&&& response = " + response);
        int status = response.getStatus();
        if (expectedStatus != -1)
            assertEquals(expectedStatus, status);

        IRI editLink = null;
        if (status != 200) {
            if ( allowsAny ) {
                log.error( "ERROR::::::::: status = " + status + " for " + fullURL );
                return null;
            }

            assertEquals(409, status);
            Document<ExtensibleElement> doc = response.getDocument();
            ExtensibleElement error = doc.getRoot();
            log.debug("&&&&&&&&&&&&&& error = " + error);

            Link link = error.getExtension(LINK);
            log.debug("&&&&&&&&&&&&&& editLink = " + editLink);
            editLink = link.getResolvedHref();
        } else {
            Document<Entry> doc = response.getDocument();
            Entry entryOut = doc.getRoot();
            assertEquals(200, status);
            editLink = entryOut.getEditLinkResolvedHref();
        }
        assertNotNull("link rel='edit' must not be null", editLink);
        response.release();
        return editLink.toString();
    }

    //=========================
    //      delete
    //=========================
    protected String delete(String fullURL) throws Exception {
        return delete( fullURL, 409 );
    }

    protected String delete(String fullURL, int errorStatusCode ) throws Exception {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        ClientResponse response = client.delete(fullURL, options);

        String editLinkStr = null;
        if (getStoreName().equals("org.atomserver-atomService")) {
            IRI editLink = null;
            if (response.getStatus() != 200) {
                assertEquals(errorStatusCode, response.getStatus());
                Document<ExtensibleElement> doc = response.getDocument();
                ExtensibleElement error = doc.getRoot();
                log.debug("&&&&&&&&&&&&&& error = " + error);

                Link link = error.getExtension(LINK);
                log.debug("&&&&&&&&&&&&&& editLink = " + editLink);
                if ( link != null ) {
                    editLink = link.getResolvedHref();
                }
            } else {
                Document<Entry> doc = response.getDocument();
                Entry entryOut = doc.getRoot();
                assertEquals(200, response.getStatus());
                editLink = entryOut.getEditLinkResolvedHref();
            }
            if ( response.getStatus() == 200 || response.getStatus() == 409 ) {
               assertNotNull("link rel='edit' must not be null", editLink);
               editLinkStr = editLink.toString();
            }
        } else {
            assertEquals(200, response.getStatus());
        }
        response.release();
        return editLinkStr;
    }

}
