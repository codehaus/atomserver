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


package org.atomserver.testutils.mt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.atomserver.testutils.log.Log4jHelper;


public class MultiThreadedTestCase extends TestCase {

    static private Log log = LogFactory.getLog( MultiThreadedTestCase.class );

    private static final boolean APP_LOG_LEVEL_VERBOSE =
            (System.getProperty("atomserver.test.APP_LOG_LEVEL_VERBOSE") != null);
    private static final boolean ROOT_LOG_LEVEL_VERBOSE =
            (System.getProperty("atomserver.test.ROOT_LOG_LEVEL_VERBOSE") != null);

    // -------------------------------------------------------
    public static Test suite() 
    { return new TestSuite( MultiThreadedTestCase.class ); }
    
    // -------------------------------------------------------
    public void setUp() throws Exception { 
        if (!APP_LOG_LEVEL_VERBOSE)
            Log4jHelper.setAppLogLevelToWarn();
        if (!ROOT_LOG_LEVEL_VERBOSE)
            Log4jHelper.setRootLogLevelToWarn();

        super.setUp();
    }

    // -------------------------------------------------------
    public void tearDown() throws Exception { 
        super.tearDown(); 

        if (!APP_LOG_LEVEL_VERBOSE)
            Log4jHelper.resetAppLogLevel();
        if (!ROOT_LOG_LEVEL_VERBOSE)
            Log4jHelper.resetRootLogLevel();
    }

    // -------------------------------------------------------
	public final void startAndWait( MultiThreadedTestThread[] threads, int secondsToWait ) {
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();		
		}
        try {
            log.warn( "Firing off " +  threads.length + " threads, running them for " + secondsToWait
                      + " seconds, and then stopping them all" );
            Thread.sleep(secondsToWait * 1000);

			for (int i = 0; i < threads.length; i++) {
				threads[i].setFinished();
			}

			for (int i = 0; i < threads.length; i++) {
				threads[i].join();
			}

            for ( int ii=0; ii < threads.length; ii++ ) {
                assertTrue( ! threads[ii].hasFailed() );
            }

        } catch ( InterruptedException ex ) {
        	fail("Unexpected interrupted exception");
        }		
	}

    
}
