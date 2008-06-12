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
import org.apache.commons.io.FileUtils;
import org.atomserver.core.CRUDAtomServerTestCase;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.dbstore.utils.DBSeeder;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.utils.locale.LocaleUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;

/**
 */
abstract public class BaseCRUDDBSTestCase extends CRUDAtomServerTestCase {

    protected EntriesDAO entriesDAO = null;
    protected EntryCategoriesDAO entryCategoriesDAO = null;

    public void setUp() throws Exception {
        super.setUp();

        ApplicationContext springContext = getSpringFactory();
        entriesDAO = (EntriesDAO) springContext.getBean("org.atomserver-entriesDAO");
        entryCategoriesDAO = (EntryCategoriesDAO) springContext.getBean("org.atomserver-entryCategoriesDAO");

        // we need something in the DB to run these tests
        if ( requiresDBSeeding()  ) {

            File file = new File(getClass().getResource("/testentries/var").toURI());
            FileUtils.copyDirectory(file, TEST_DATA_DIR);

            DBSeeder.getInstance(springContext).seedEntriesClearingFirst();
        } else {
            DBSeeder.getInstance(springContext).createWidgetsDir();
        }      
    }

    public void tearDown() throws Exception { super.tearDown(); }

    protected boolean requiresDBSeeding() { return false; }

    protected String getStoreName() { return "org.atomserver-atomService"; }

    protected void destroyEntry(String workspace, String collection, String entryId,
                                String locale, boolean deletePhysicalFile)
            throws Exception {
        IRI iri = IRI.create("http://localhost:8080/"
                             + widgetURIHelper.constructURIString(workspace, collection, entryId,
                                                                  LocaleUtils.toLocale(locale)));
        
        log.debug("deleting IRI : " + iri);
        EntryTarget entryTarget = widgetURIHelper.getEntryTarget(
                new MockRequestContext(serviceContext, "GET", iri.toString()), true);
        contentStorage.deleteContent(null, entryTarget);
        entriesDAO.obliterateEntry(entryTarget);

        Thread.sleep(DB_CATCHUP_SLEEP);
    }
}
