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

package org.atomserver.core.dbstore.dao;

import org.atomserver.utils.locale.LocaleUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.BaseFeedDescriptor;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.testutils.latency.LatencyUtil;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;

import java.util.Date;
import java.util.List;
import java.util.Locale;




public class LocaleEntriesDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(LocaleEntriesDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------
    public void testPageWithLocales() throws Exception {
        // COUNT
        BaseServiceDescriptor serviceDescriptor = new BaseServiceDescriptor(workspace);
        int startCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("startCount = " + startCount);

        String sysId = "acme";
        int propIdSeed = 44400;

        String[] locales = {"de", "de_DE", "de_CH"};

        Date lnow = entriesDAO.selectSysDate();

        // INSERT -- first we have to seed the DB 
        //           NOTE: no SeqNums yet...
        int numRecs = 12;
        int knt = 0;
        int jj = 0;
        for (int ii = 0; ii < numRecs; ii++) {
            EntryMetaData entryIn = new EntryMetaData();
            entryIn.setWorkspace("widgets");
            entryIn.setCollection(sysId);
            entryIn.setLocale(LocaleUtils.toLocale(locales[(ii % locales.length)]));

            String propId = "" + (propIdSeed + knt);

            entryIn.setEntryId(propId);

            entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
            entriesDAO.insertEntry(entryIn);

            jj++;
            if (jj == 2) {
                jj = 0;
                knt++;
            }
        }
        LatencyUtil.updateLastWrote();

        /* So we end up with this::
           44400,de        44402,de_DE      44404,de_CH
           44400,de_DE     44402,de_CH      44404,de
           44401,de_CH     44403,de         44405,de_DE
           44401,de        44403,de_DE      44405,de_CH
        */

        // COUNT
        int count = entriesDAO.getTotalCount(serviceDescriptor);
        assertEquals((startCount + numRecs), count);

        int pageSize = count + 1;

        LatencyUtil.accountForLatency();

        // get page (for "de")
        BaseFeedDescriptor feedDescriptor = new BaseFeedDescriptor(workspace, null);
        List sortedList = entriesDAO.selectFeedPage(ZERO_DATE, AtomServerConstants.FAR_FUTURE_DATE, 0, pageSize,
                                                    locales[0], feedDescriptor, null);
        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.debug("List (de) = \n" + sortedList);
        assertEquals(4, sortedList.size());

        // get page (for "de_DE") 
        sortedList = entriesDAO.selectFeedPage(ZERO_DATE, AtomServerConstants.FAR_FUTURE_DATE, 0, pageSize,
                                               locales[1], feedDescriptor, null);
        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.debug("List (de_DE) = \n" + sortedList);
        assertEquals(4, sortedList.size());

        // get page (for "de_CH") 
        sortedList = entriesDAO.selectFeedPage(ZERO_DATE, AtomServerConstants.FAR_FUTURE_DATE, 0, pageSize,
                                               locales[2], feedDescriptor, null);
        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.debug("List (de_CH) = \n" + sortedList);
        assertEquals(4, sortedList.size());

        // get page (for "de_AT") 
        sortedList = entriesDAO.selectFeedPage(ZERO_DATE, AtomServerConstants.FAR_FUTURE_DATE, 0, pageSize,
                                               "de_AT", feedDescriptor, null);
        log.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.debug("List (de_AT) = \n" + sortedList);
        assertEquals(0, sortedList.size());

        // DELETE them all for real
        knt = 0;
        jj = 0;
        for (int ii = 0; ii < numRecs; ii++) {

            String propId = "" + (propIdSeed + knt);

            Locale locale = LocaleUtils.toLocale(locales[(ii % locales.length)]);
            IRI iri = IRI.create("http://localhost:8080/"
                                 + entryURIHelper.constructURIString(workspace, sysId, propId, locale));
            EntryTarget entryTarget = entryURIHelper.getEntryTarget(
                    new MockRequestContext(serviceContext, "GET", iri.toString()), true);
            entriesDAO.obliterateEntry(entryTarget);

            jj++;
            if (jj == 2) {
                jj = 0;
                knt++;
            }
        }

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP ); // give the DB a chance to catch up
        int finalCount = entriesDAO.getTotalCount(serviceDescriptor);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

}

