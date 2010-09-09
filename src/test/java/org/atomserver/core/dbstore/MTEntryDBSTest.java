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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.atomserver.core.dbstore.dao.impl.EntriesDAOiBatisImpl;
import org.atomserver.testutils.mt.MultiThreadedTestCase;
import org.atomserver.testutils.mt.MultiThreadedTestThread;
import org.atomserver.utils.conf.ConfigurationAwareClassLoader;

import java.util.HashSet;
import java.util.Random;

/**
 */
public class MTEntryDBSTest extends BaseCRUDDBSTestCase {

    public static Test suite() { return new TestSuite(MTEntryDBSTest.class); }

    private MultiThreadedTestCase mtTestCase = new MultiThreadedTestCase();

    protected String getURLPath() { return null; }

    public void setUp() throws Exception { 
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            log.debug("skipping test - doesn't work in HSQL");
            return;
        }
        mtTestCase.setUp();
        super.setUp();
        ((EntriesDAOiBatisImpl)entriesDAO).setLatencySeconds(0);

        deleteList = new HashSet();
    }

    public void tearDown() throws Exception { 
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        super.tearDown();
        mtTestCase.tearDown();
    }

    static private HashSet<Deleter> deleteList = null;

    static class Deleter {
        String wspace = null;
        String collection = null;
        int propId = 0;
        String locale = null;
        MTEntryDBSTest parent = null;
        
        Deleter( String wspace, String collection, int propId, String locale, MTEntryDBSTest parent ) {
            this.wspace = wspace;
            this.collection = collection;
            this.propId = propId;
            this.locale = locale;
            this.parent = parent;
        }
  
        void delete() throws Exception {
            parent.destroyEntry( wspace, collection, ("" + propId), locale, true );
        }

        public boolean equals(Object o) {
             if (o == null || !o.getClass().equals(getClass())) {
                 return false;
             }
             Deleter other = (Deleter) o;
             return new EqualsBuilder()
                     .append(wspace, other.wspace)
                     .append(collection, other.collection)
                     .append(propId, other.propId)
                     .append(locale, other.locale)
                     .isEquals();
         }

         public int hashCode() {
             return new HashCodeBuilder(2132309, 19961)
                     .append(wspace)
                     .append(collection)
                     .append(propId)
                     .append(locale)
                    .toHashCode();
         }

    }

    public static class CRUDTestThread extends MultiThreadedTestThread {
        String wspace = null;
        String collection = null;
        int propId = 0;
        String locale = null;
        MTEntryDBSTest parent = null;
        boolean shouldCheckOptConc = true;
        boolean expects201 = true;
        boolean allowsAny = false;
        boolean addOne = true;

        java.util.Random random = new java.util.Random();

        public CRUDTestThread( String wspace, String collection, int propId,
                               String locale, MTEntryDBSTest parent,
                               boolean shouldCheckOptConc, boolean expects201,
                               boolean allowsAny, boolean addOne ) {
            this.wspace = wspace;
            this.collection = collection;
            this.propId = propId;
            this.locale = locale;
            this.parent = parent;
            this.shouldCheckOptConc = shouldCheckOptConc;
            this.expects201 = expects201;
            this.allowsAny = allowsAny;
            this.addOne = addOne;
        }

        public void runTest() throws Exception {
            if ( addOne )
               propId++;
            String urlPath = wspace + "/" + collection + "/" + propId + "." + locale + ".xml";
            parent.runCRUDTest( false, urlPath, shouldCheckOptConc, expects201, allowsAny );
            
            deleteList.add( new Deleter( wspace, collection, propId, locale, parent ) );
        }
    }
    
    public void testHittingAllDifferentOnes() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        String wspace1 = "widgets";
        String wspace2 = "dummy";

        String collection = "mttest";
        int propIdSeed = 880000;
        String locale = "es";

        // Create the seqNum rows in the DB up-front -- else the Threads will stack up and block there...
        String urlPath = wspace1 + "/" + collection + "/" +  (propIdSeed - 1000) + "." + locale + ".xml";
        runCRUDTest( false, urlPath );
        deleteList.add( new Deleter( wspace1, collection, (propIdSeed - 1000), locale, this ) );

        urlPath = wspace2 + "/" + collection + "/" +  (propIdSeed - 1000) + "." + locale + ".xml";
        runCRUDTest( false, urlPath );
        deleteList.add( new Deleter( wspace2, collection, (propIdSeed - 1000), locale, this ) );


        int numThreads = 8;
        CRUDTestThread[] threads = new CRUDTestThread[numThreads];
        String[] wspaces = { wspace1, wspace2 };
        for ( int ii=0; ii < numThreads; ii++ ) {
            CRUDTestThread thread = new CRUDTestThread( wspaces[ ii % 2 ], collection,
                                                        (propIdSeed + ii*1000 ), locale,
                                                        this, true, true, false, true );
            threads[ii] = thread;
        }

        mtTestCase.startAndWait( threads, 6 );

        System.out.println( "Destroying the Entries created... " );
        for ( Deleter deleter: deleteList ) {
            deleter.delete();
        }
    }

    // Create a psuedo-random number generator
    static private Random random = new Random();

    public void testHittingTheSameOne() throws Exception {
        if ("hsql".equals(ConfigurationAwareClassLoader.getENV().get("db.type"))) {
            return;
        }
        String wspace = "dummy";

        String collection = "mttest";
        int propId = 454679283 + random.nextInt(1000000);
        String locale = "es";

        // Create the seqNum rows in the DB up-front -- else the Threads will stack up and block there...
        int propId1 = propId - 1;
        String urlPath = wspace + "/" + collection + "/" + propId1 + "." + locale + ".xml";
        runCRUDTest(false, urlPath, false, false);

        int numThreads = 8;
        CRUDTestThread[] threads = new CRUDTestThread[numThreads];
        for (int ii = 0; ii < numThreads; ii++) {
            CRUDTestThread thread = new CRUDTestThread(wspace, collection, propId, locale,
                                                       this, false, false, false, false);
            threads[ii] = thread;
        }

        mtTestCase.startAndWait(threads, 6);

        System.out.println("Destroying the Entries created... ");
        destroyEntry(wspace, collection, ("" + propId1), locale, true);
        destroyEntry(wspace, collection, ("" + propId), locale, true);

    }
}
