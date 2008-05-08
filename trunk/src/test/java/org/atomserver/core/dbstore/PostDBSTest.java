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
import org.apache.commons.lang.StringUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.utils.locale.LocaleUtils;

import java.io.File;

/**
 */
public class PostDBSTest extends CRUDDBSTestCase {

    private String currentLocale;
    private String currentWorkspace;

    public static Test suite()
    { return new TestSuite( PostDBSTest.class ); }

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    {
        super.tearDown();
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString( getCurrentWorkspace(), "acme",
                                                                   getCurrentEntryId(),
                                                                   LocaleUtils.toLocale(getCurrentLocale())) );
        return entryIRI;
    }

    protected String getPropfileBase() {
        if ( currentLocale == null ) {
            return userdir + "/var/" + getCurrentWorkspace() + "/acme/" + getCurrentEntryId().substring(0,2) +
                   "/" + getCurrentEntryId() + "/" + getCurrentEntryId() + ".xml";
        } else {
            return userdir + "/var/" + getCurrentWorkspace() + "/acme/" + getCurrentEntryId().substring(0,2) +
                   "/" + getCurrentEntryId() + "/" + getCurrentLocale() + "/" +
                   getCurrentEntryId() + ".xml";            
        }
    }

    protected String getCurrentLocale() {
        return currentLocale;
    }

    protected void setCurrentLocale( String locale ) {
        this.currentLocale = locale;
    }

    protected String getCurrentWorkspace() {
        return currentWorkspace;
    }

    protected void setCurrentWorkspace( String workspace ) {
        this.currentWorkspace = workspace;
    }


    // --------------------
    //       tests
    //---------------------

    public void testCRUDNoLocale() throws Exception {

        setCurrentWorkspace( "dummy" );

        // run the tests up to some point
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        String finalEditLink = runCRUDTest( true, "dummy/acme", true, true, false, true, null );

        // SELECT against the just deleted entry
        ClientResponse response = clientGet( finalEditLink, null, 200, true );

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();
        log.debug( "CONTENT= "+ entryOut.getContent() );
        assertTrue( entryOut.getContent().indexOf("<deletion") != -1);

        response.release();
        if (contentStorage instanceof FileBasedContentStorage) {
            int rev = extractRevisionFromURI( finalEditLink );
            File pFile = new File( getPropfileBase() + ".r" + rev);
            assertTrue( pFile != null && pFile.exists() );
        }
    }

    public void testCRUDWithLocale() throws Exception {

        setCurrentLocale( "en" );
        setCurrentWorkspace( "widgets" );

        // run the tests up to some point
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        String finalEditLink = runCRUDTest( true, "widgets/acme", true, true, false, true, "en" );

        // SELECT against the just deleted entry
        ClientResponse response = clientGet( finalEditLink, null, 200, true );

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();
        log.debug( "CONTENT= "+ entryOut.getContent() );
        assertTrue( entryOut.getContent().indexOf("<deletion") != -1);

        response.release();
        if (contentStorage instanceof FileBasedContentStorage) {
            int rev = extractRevisionFromURI( finalEditLink );
            File pFile = new File( getPropfileBase() + ".r" + rev);
            assertTrue( pFile != null && pFile.exists() );
        }
    }
}
