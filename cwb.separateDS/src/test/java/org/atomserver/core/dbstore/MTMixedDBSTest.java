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

import org.atomserver.core.dbstore.dao.impl.EntriesDAOiBatisImpl;
import org.atomserver.testutils.mt.MultiThreadedTestCase;
import org.atomserver.testutils.mt.MultiThreadedTestThread;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Locale;

/**
 */
public class MTMixedDBSTest extends FeedDBSTestCase {

    public static Test suite() { return new TestSuite(MTMixedDBSTest.class); }

    private MultiThreadedTestCase mtTestCase = new MultiThreadedTestCase();

    static private final boolean KEEP_ENTRIES_BETWEEN_RUNS =
            (System.getProperty("atomserver.test.KEEP_ENTRIES_BETWEEN_RUNS") != null);

    static private String testWorkspace = "widgets";
    static private String testCollection = "mtmixed";
    static private String testLocaleStr = "fr";
    static private Locale testLocale = new Locale( testLocaleStr );
    static private int entryIdSeed = 78900000;

    static private int totalEntries = 20; // 100;
    static private int pgSize = 5;        // 20;
    static private int numThreads = 10;   // 40;
    static private int secsToRun = 15;    // 40;

    public void setUp() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            log.debug("skipping test - doesn't work in HSQL");
            return;
        }
        mtTestCase.setUp();
        super.setUp();

        ((EntriesDAOiBatisImpl)entriesDao).setLatencySeconds(0);
        entriesDao.ensureCollectionExists(testWorkspace, testCollection);
    }

    protected void validateStartCount() throws Exception {
        contentDAO.deleteAllRowsFromContent();
        Thread.sleep(5000);
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        Thread.sleep(5000);
        entriesDao.deleteAllRowsFromEntries();
        startCount = NUM_RECS;
    }

    public void tearDown() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        super.tearDown();
        mtTestCase.tearDown();

        if ( !KEEP_ENTRIES_BETWEEN_RUNS ) {
            entriesDao.deleteAllEntries( new BaseServiceDescriptor( testWorkspace ) );
        }
    }

    public static class FeedReadTestThread extends MultiThreadedTestThread {
         MTMixedDBSTest parent = null;

         public FeedReadTestThread( MTMixedDBSTest parent ) {
            this.parent = parent;
         }

         public void runTest() throws Exception {
             parent.loopThruPagesUsingNextLink( testWorkspace, pgSize, testCollection, totalEntries, true );
         }
    }

    public static class PutTestThread extends MultiThreadedTestThread {
         MTMixedDBSTest parent = null;
         int entryId = 0;
         int entryIdStart = 0;
         int range = 0;

         public PutTestThread( MTMixedDBSTest parent, int entryIdStart, int range ) {
             this.parent = parent;
             this.entryIdStart = entryIdStart;
             this.entryId = entryIdStart;
             this.range = range;
         }

         public void runTest() throws Exception {
             if ( entryId >= (entryIdStart + range ) )
                entryId = entryIdStart;

             String content = parent.getContentString( testCollection, ("" + entryId), ("Mr " + entryId) );
             parent.modifyEntry(testWorkspace,
                                testCollection,
                                ("" + entryId),
                                testLocaleStr,
                                content,
                                false, "*", false, false );

             entryId++;
         }
     }

    protected boolean requiresDBSeeding() { return false; }

    public void testmixedLoad() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }

        if ( !KEEP_ENTRIES_BETWEEN_RUNS ) {
            log.warn( "Creating " + totalEntries + " Entries for Multi-Threaded Test......." );
            for ( int ii = 0; ii < totalEntries; ii++ ){
                String entryId = "" + (entryIdSeed + ii);
                log.warn( "Inserting Entry [" + testWorkspace + "," +  testCollection + ","
                          + testLocale + "," + entryId + "]");
                insertEntry( new BaseEntryDescriptor(testWorkspace, testCollection, entryId, testLocale, 0),
                             true );
            }
        }

        MultiThreadedTestThread[] threads = new MultiThreadedTestThread[numThreads];

        for ( int ii=0; ii < numThreads/2; ii++ ) {
            log.warn( "Creating Feed Thread ");
            MultiThreadedTestThread thread = new FeedReadTestThread( this );
            threads[ii] = thread;
        }

        int range = totalEntries/(numThreads/2) ;
        int entryIdStart = entryIdSeed;
        log.warn( "range = " + range );
        for ( int ii=numThreads/2; ii < numThreads; ii++ ) {
            log.warn( "PUT Thread starting at " + entryIdStart);
            MultiThreadedTestThread thread = new PutTestThread( this, entryIdStart, range );
            threads[ii] = thread;
            entryIdStart += range;
        }

        mtTestCase.startAndWait( threads, secsToRun );
   }

}