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

import org.atomserver.uri.EntryTarget;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.utils.locale.LocaleUtils;
import org.apache.abdera.i18n.iri.IRI;

import java.io.File;
import java.util.Locale;

/**
 */
public class OptConcDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( OptConcDBSTest.class ); }

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

    protected final String pId = "33445566";

    protected String getURLPath() {
        return "widgets/acme/" + pId + ".en.xml";
    }

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString("widgets", "acme", pId,
                                                                   LocaleUtils.toLocale("en")));
        return entryIRI;
    }

    protected File getPropfile() throws Exception {
        return getEntryFile(0);
    }

    protected File getEntryFile(int revision) throws Exception {
        return getEntryFile("widgets", "acme", pId, Locale.ENGLISH, true, revision);
    }

    protected boolean requiresDBSeeding() { return false; }


    // --------------------
    //       tests
    //---------------------

    public void testInsertNonZeroRev() throws Exception {
        log.debug( "########################################## testInsertNonZeroRev " );
        // Insert with as widgets/acme/33445566.en.xml/100
        //  This one should fail.
        insertRev( "/100", false );
    }

    public void testInsertZeroRev() throws Exception {
        log.debug( "########################################## testInsertZeroRev " );
        insertRev( "/0", true );

        // make sure that we can't insert it again at /0
        insertRev( "/0", false, true );        
    }

    public void testInsertNoRev() throws Exception {
        log.debug( "########################################## testInsertNoRev " );
        insertRev( "", true );
    }

    public void testInsertAnyRev() throws Exception {
        log.debug( "########################################## testInsertAnyRev " );
        insertRev( "/*", true );
    }
    
    private void insertRev( String rev, boolean expects201 ) throws Exception {
        boolean fileShouldExist = (expects201) ? true : false;
        insertRev( rev, expects201, fileShouldExist);
    }

    private void insertRev( String rev, boolean expects201, boolean fileShouldExist ) throws Exception {
        String urlPath = getURLPath();
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        // Insert with as widgets/acme/33445566.en.xml/*
        //  This one should pass.
        String insertURL = fullURL + rev;
        int expectedStatus = (expects201) ? 201 : 409;
        String editURI = insert(id, insertURL, getFileXMLInsert(), false, expectedStatus, expects201 );

        log.debug( "########################################## editURI = " + editURI );
        File propFile = getEntryFile(0);
        log.debug("propFile " + propFile);
        if ( fileShouldExist )
            assertTrue(propFile.exists());
        else
            assertFalse(propFile.exists());

        /* FIXME
        int irev = extractRevisionFromURI(editURI);
        log.debug( "irev= " + irev );
        assertEquals( 0, irev );
        */
    }
}
