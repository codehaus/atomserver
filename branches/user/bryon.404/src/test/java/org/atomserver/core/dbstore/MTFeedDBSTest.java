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

import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.dbstore.dao.EntriesDAOiBatisImpl;
import org.atomserver.testutils.mt.MultiThreadedTestCase;
import org.atomserver.testutils.mt.MultiThreadedTestThread;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class MTFeedDBSTest extends FeedDBSTestCase {

    public static Test suite() { return new TestSuite(MTFeedDBSTest.class); }

    private MultiThreadedTestCase mtTestCase = new MultiThreadedTestCase();

    private static int NUM_RECS = 4;

    public void setUp() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            log.debug("skipping test - doesn't work in HSQL");
            return;
        }
        mtTestCase.setUp();
        super.setUp();
        ((EntriesDAOiBatisImpl)entriesDao).setLatencySeconds(0);
    }

    public void tearDown() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        super.tearDown();
        mtTestCase.tearDown();
    }

    public static class FeedReadTestThread extends MultiThreadedTestThread {
        String wspace = null;
        String collection = null;
        int propId = 0;
        String locale = null;
        MTFeedDBSTest parent = null;

        public FeedReadTestThread( String wspace, String collection, int propId, String locale, MTFeedDBSTest parent ) {
            this.wspace = wspace;
            this.collection = collection;
            this.propId = propId;
            this.locale = locale;
            this.parent = parent;
        }

        public void runTest() throws Exception {
            parent.runAllTests( NUM_RECS, wspace, collection, propId, locale );
        }
    }

    // --------------------
    //       tests
    //---------------------
    public void testMultiThreadedFeedReads() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        String wspace1 = "widgets";
        String wspace2 = "dummy";

        String collection = "mttest2";
        int propIdSeed = 75310;
        String locale = "es";

        log.warn( "Creating Entries for Multi-Threaded Test......." );
        createWidgets( NUM_RECS, wspace1, propIdSeed, collection, locale, false );
        createWidgets( NUM_RECS, wspace2, propIdSeed, collection, locale, false );

        int numThreads = 8;
        int numSecs = 8;

        FeedReadTestThread[] threads = new FeedReadTestThread[numThreads];
        String[] wspaces = { wspace1, wspace2 };
        for ( int ii=0; ii < numThreads; ii++ ) {
            FeedReadTestThread thread = new FeedReadTestThread( wspaces[ ii % 2 ], collection, propIdSeed, locale, this );
            threads[ii] = thread;
        }

        mtTestCase.startAndWait( threads, numSecs );

        deleteWidgets( NUM_RECS, wspace1, 0, propIdSeed, collection, locale, true );
        deleteWidgets( NUM_RECS, wspace2, 0, propIdSeed, collection, locale, true );
    }

    public void testWriteContention() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        final String workspace = "widgets";
        final String collection = "dummy";
        final String entryId = "8675309";
        final String locale = "pl";

        int numThreads = 8;
        int numSecs = 8;

        List<MultiThreadedTestThread> threadList = new ArrayList<MultiThreadedTestThread>();
        for (int i = 0 ; i < numThreads; i++) {
            threadList.add(new MultiThreadedTestThread() {
                public void run() {
                    try {
                        runTest();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                public void runTest() throws Exception {
                    log.debug("\n%%%%%%%%%%%%%% CREATING:: [" + workspace + ", " + collection + ", " + entryId + ", " + locale);

                    int startCount1 = entriesDao.getTotalCount(new BaseServiceDescriptor(workspace));
                    log.debug("startCount = " + startCount1);

                    AbderaClient client = new AbderaClient();
                    RequestOptions options = client.getDefaultRequestOptions();
                    options.setHeader("Connection", "close");

                    Entry entry = getFactory().newEntry();
                    entry.setId(getURLPath(workspace, collection, entryId, locale, "*"));
                    entry.setContentAsXhtml(
                            createWidgetXMLFileString(entryId).replaceFirst("<\\?[^\\?]*\\?>", ""));

                    String putUrl = getServerURL() + getURLPath(workspace, collection, entryId, locale, "*");
                    log.debug("PUTting to URL : " + putUrl);
                    ClientResponse response = client.put(putUrl, entry, options);

                    Document<Entry> doc = response.getDocument();
                    Entry entryOut = doc.getRoot();

                    IRI editLink = entryOut.getEditLinkResolvedHref();
                    assertNotNull("link rel='edit' must not be null", editLink);

                    assertTrue(Arrays.asList(201, 200, 409).contains(response.getStatus()));
                    response.release();
                }
            });
        }
        mtTestCase.startAndWait(threadList.toArray(new MultiThreadedTestThread[threadList.size()]), numSecs);
        deleteEntry(workspace, collection, entryId, locale);
    }


}
