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


package org.atomserver.testutils.log;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class Log4jHelperTest extends TestCase {

    static private Log log = LogFactory.getLog( Log4jHelperTest.class );
    
    // -------------------------------------------------------
    public static Test suite() 
    { return new TestSuite( Log4jHelperTest.class ); }
    
    // -------------------------------------------------------
    protected void setUp() throws Exception 
    { super.setUp(); }
    
    // -------------------------------------------------------
    protected void tearDown() throws Exception
    { super.tearDown(); }
    
    //----------------------------
    //          Tests 
    //----------------------------
  
    // These tests are rather bogus.
    //  We really should verify it using a file...
    //  But we'd need to wire a new Appender into log4j -- or tee stdout...

    public void testSetAppLogLevel() {
        log.debug( "AppLogLevel:: This line should get printed" );
        
        Log4jHelper.setAppLogLevelToWarn();
        log.debug( "AppLogLevel:: This line should NOT get printed" );
        
        Log4jHelper.resetAppLogLevel();
        log.debug( "AppLogLevel:: This line should get printed" );
    }

    public void testSetRootLogLevel() {
        Log log2 = LogFactory.getLog( "org.apache.foobar" );
        log2.debug( "RootLogLevel:: This line should get printed" );
        
        Log4jHelper.setRootLogLevelToWarn();
        log2.debug( "RootLogLevel:: This line should NOT get printed" );

        Log4jHelper.resetRootLogLevel();
        log2.debug( "RootLogLevel:: This line should get printed" );
    }

    // This matches the common use case
    public void testSetArbitraryLogLevel() {
        Log log3 = LogFactory.getLog( "org.atomserver.foobar" );
        log3.debug( "ArbitraryLogLevel:: This line should get printed because the default level for AppLogLevel is TRACE" );

        // First let's set AppLogLevel to WARN
        Log4jHelper.setAppLogLevelToWarn();
        log3.debug( "ArbitraryLogLevel:: This line should NOT get printed" );
        
        // Now let's set log3 to DEBUG 
        Log4jHelper.LogLevel originalLogLevel = Log4jHelper.setLogLevel( log3, Log4jHelper.LogLevel.INFO );
        log3.info( "ArbitraryLogLevel:: This line should get printed (originalLogLevel = " + originalLogLevel + ")" );
        log3.debug( "ArbitraryLogLevel:: This line should NOT get printed" );
        assertTrue( originalLogLevel.equals( Log4jHelper.LogLevel.WARN ) ); 

        // Now set it back -- Don't forget to do this!!!
        Log4jHelper.LogLevel lastLogLevel = Log4jHelper.setLogLevel( log3, originalLogLevel );
        log3.debug( "ArbitraryLogLevel:: This line should get printed (lastLogLevel = " + lastLogLevel);
        assertTrue( lastLogLevel.equals( Log4jHelper.LogLevel.INFO ) ); 
    }
   
}
