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


package org.atomserver.core.filestore;

import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.core.CRUDAtomServerTestCase;
import org.atomserver.testutils.conf.TestConfUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 */
public class CRUDFSTest extends CRUDAtomServerTestCase {

    public static Test suite() { return new TestSuite(CRUDFSTest.class); }

    public void setUp() throws Exception {
        TestConfUtil.preSetup("filestore-conf");
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    protected String getStoreName() {
        return "filestore"; 
    }

    static protected final String propId = "12345";

    protected String getURLPath() { return "widgets/acme/" + propId + ".en.xml"; }

    protected String getFileXMLInsert() {
        String fileXMLInsert =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" " +
            "systemId=\"acme\" id=\"" + propId + "\" inNetwork=\"false\">\n"
            + "<colors>\n"
            + "<color isDefault=\"true\">teal</color>\n"
            + "</colors>\n"
            + "<contact>\n"
            + "<contactId>1638</contactId>\n"
            + "<displayName>This is an insert</displayName>\n"
            + "<hasEmail>true</hasEmail>\n"
            + "</contact>\n"
            + "</property>";
        return fileXMLInsert;
    }

    protected String getFileXMLUpdate() {
        String fileXMLUpdate =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" " +
            "systemId=\"acme\" id=\"" + propId + "\" inNetwork=\"false\">\n"
            + "<colors>\n"
            + "<color isDefault=\"true\">teal</color>\n"
            + "</colors>\n"
            + "<contact>\n"
            + "<contactId>1638</contactId>\n"
            + "<displayName>This is an update</displayName>\n"
            + "<hasEmail>true</hasEmail>\n"
            + "</contact>\n"
            + "</property>";
        return fileXMLUpdate;
    }

    protected IRI getEntryIRI() {
        return IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString("widgets", "acme", propId,
                                                                   LocaleUtils.toLocale("en")));
    }

    protected File getPropfile() {
        return new File(TEST_DATA_DIR + "/widgets/acme/12/12345/en/12345.xml");
    }

    // --------------------
    //       tests
    //---------------------

    public void testNothing() {}

    public void XXXtestCRUD() throws Exception {
        String finalEditLink = runCRUDTest( true, getURLPath(), false );

        assertNull(finalEditLink);
        assertTrue(FileUtils.readFileToString(getPropfile()).contains("<deletion"));
    }
}
