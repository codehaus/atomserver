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
import org.atomserver.utils.locale.LocaleUtils;

import java.io.File;
import java.util.Locale;

/**
 */
abstract public class CRUDDBSTestCase extends BaseCRUDDBSTestCase {

    public void setUp() throws Exception {
        super.setUp();
        setCurrentEntryId( propId );
    }

    public void tearDown() throws Exception { super.tearDown(); }

    protected void cleanUp() throws Exception{ super.cleanUp(); }

    static protected final String propId = "12345";

    protected String getURLPath() { return "widgets/acme/" + propId + ".en.xml"; }

    protected String getFileXMLInsert() {
        String fileXMLInsert =
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"" + propId + "\" inNetwork=\"false\">\n"
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
            "<property xmlns=\"http://schemas.atomserver.org/widgets/v1/rev0\" systemId=\"acme\" id=\"" + propId + "\" inNetwork=\"false\">\n"
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

    protected IRI getEntryIRI() {
        IRI entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString("widgets", "acme", propId, LocaleUtils.toLocale("en")));
        return entryIRI;
    }

    protected File getPropfile() throws Exception {
        return getEntryFile(0);
    }

    protected File getEntryFile(int revision) throws Exception {
        return getEntryFile("widgets", "acme", "12345", Locale.ENGLISH, true, revision);
    }
}
