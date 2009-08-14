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


package org.atomserver.core.dbstore.utils;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.dbstore.FeedDBSTestCase;

import java.io.File;

/**
 */
public class PurgeDBSTest extends FeedDBSTestCase {
    private static final String DUMMY = "dummy";
    private static final String WIDGETS = "widgets";
    private static final String OMPAH = "ompah";
    private static final String LUMPAH = "lumpah";
    private static final String ES_LOCALE = "es";
    private static final int PROP_ID_SEED = 75310;

    public static Test suite() { return new TestSuite(PurgeDBSTest.class); }

    public void setUp() throws Exception { super.setUp(); }

    public void tearDown() throws Exception { super.tearDown(); }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------
    public void testPurger() throws Exception {

        BaseServiceDescriptor widgetsServiceDesc = new BaseServiceDescriptor(WIDGETS);
        BaseServiceDescriptor dummyServiceDesc = new BaseServiceDescriptor(DUMMY);

        // capture how many total "widgets" and "dummies" there are at the beginning of the test
        int widgetsStartCount = entriesDao.getTotalCount(widgetsServiceDesc);
        log.debug( "widgetsStartCount = " + widgetsStartCount);

        log.debug("LOGPOINT#" + 1 + " there are " +
                  entriesDao.getTotalCount(widgetsServiceDesc) + "widgets and " +
                  entriesDao.getTotalCount(dummyServiceDesc) + "dummies");

        // create 2 widgets/ompah
        createWidgets( 2, WIDGETS, PROP_ID_SEED, OMPAH, ES_LOCALE, false );

        log.debug("LOGPOINT#" + 2 + " there are " +
                  entriesDao.getTotalCount(widgetsServiceDesc) + "widgets and " +
                  entriesDao.getTotalCount(dummyServiceDesc) + "dummies");

        // create 2 dummy/lumpah
        createWidgets( 2, DUMMY, PROP_ID_SEED, LUMPAH, ES_LOCALE, false );

        log.debug("LOGPOINT#" + 3 + " there are " +
                  entriesDao.getTotalCount(widgetsServiceDesc) + "widgets and " +
                  entriesDao.getTotalCount(dummyServiceDesc) + "dummies");

        // look at the directories on disk where the workspaces and collections are stored...
        String basedir = System.getProperty("atomserver.data.dir").replaceFirst("file:", userdir + "/");
        File widgetsDir = new File( basedir + "/widgets" );
        File acmeDir = new File( basedir + "/widgets/acme" );
        File ompahDir = new File( basedir + "/widgets/ompah" );
        File dummyDir = new File( basedir + "/dummy" );
        File lumpahDir = new File( basedir + "/dummy/lumpah" );

        // all of the workspace and collection dirs should exist
        assertTrue( widgetsDir.exists() );
        assertTrue( acmeDir.exists() );
        assertTrue( ompahDir.exists() );
        assertTrue( dummyDir.exists() );
        assertTrue( lumpahDir.exists() );

        // purge the widgets/ompah collection
        DBPurger.getInstance(getSpringFactory()).purge(WIDGETS, OMPAH);

        log.debug("LOGPOINT#" + 4 + " there are " +
                  entriesDao.getTotalCount(widgetsServiceDesc) + "widgets and " +
                  entriesDao.getTotalCount(dummyServiceDesc) + "dummies");

        int widgetsFinalCount = entriesDao.getTotalCount(widgetsServiceDesc);
        log.debug( "widgetsFinalCount = " + widgetsFinalCount);
        assertEquals(widgetsStartCount, widgetsFinalCount);
        
        // purge the dummy workspace
        DBPurger.getInstance(getSpringFactory()).purge(DUMMY, null);

        log.debug("LOGPOINT#" + 5 + " there are " +
                  entriesDao.getTotalCount(widgetsServiceDesc) + "widgets and " +
                  entriesDao.getTotalCount(dummyServiceDesc) + "dummies");

        int dummyFinalCount = entriesDao.getTotalCount(dummyServiceDesc);
        log.debug( "dummyFinalCount = " + dummyFinalCount);
        assertEquals(0, dummyFinalCount);

        // the workspaces and pre-existing collections should exist...
        assertTrue( widgetsDir.exists() );
        assertTrue( acmeDir.exists() );
        assertTrue( dummyDir.exists() );

        // but widgets/ompah and dummy/lumpah should not!
        assertFalse( ompahDir.exists() );
        assertFalse( lumpahDir.exists() );
    }

}
