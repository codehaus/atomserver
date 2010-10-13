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
import org.atomserver.core.dbstore.utils.DBSeeder;

public class ClearDBSTest extends DBSTestCase {

    static public boolean ENABLE_DB_CLEAR_ALL = false;

    static {
        String prop = System.getProperty("ENABLE_DB_CLEAR_ALL");
        if (prop != null) {
            if (prop.equals("true") || prop.equals("TRUE")) {
                ENABLE_DB_CLEAR_ALL = true;
            }
        }
    }

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(ClearDBSTest.class); }

    // -------------------------------------------------------
    public void setUp() throws Exception { super.setUp(); }

    // -------------------------------------------------------
    public void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------

    // Allows us to easily clear the DB. Useful when you don't want to go online
    // to SQLServer thru a GUI to accomplish the same...

    public void testClearDB() throws Exception {
        if (ENABLE_DB_CLEAR_ALL) {
            DBSeeder.getInstance(getSpringFactory()).clearDB();
        }
    }
}
