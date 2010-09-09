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
import org.apache.commons.io.IOUtils;
import org.apache.abdera.i18n.iri.IRI;

public class RelaxNGValidatingDBSTest extends CRUDAtomServerTestCase {
    protected String getStoreName() { return "org.atomserver-atomService"; }

    protected String getURLPath() { return "dummy/acme/167370.xml"; }

    public void testListing() throws Exception {

        String xml = IOUtils.toString(getClass().getResourceAsStream("/testwidget.xml"));

        String editUri = insert(getURLPath(), getServerURL() + getURLPath(), xml);
        delete(editUri);

        EntriesDAO entriesDAO = (EntriesDAO) getSpringFactory().getBean("org.atomserver-entriesDAO");

        IRI entryIRI = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString( "dummy", "acme", "167370", null));

        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }
}
