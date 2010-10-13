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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4JLogger;

/** NOTE: Do NOT let log4j leak from here !!!
 */
public class Log4jHelper {

    public enum LogLevel { 
        TRACE (org.apache.log4j.Level.TRACE), 
            DEBUG (org.apache.log4j.Level.DEBUG),
            INFO  (org.apache.log4j.Level.INFO),
            WARN  (org.apache.log4j.Level.WARN),
            ERROR (org.apache.log4j.Level.ERROR),
            FATAL (org.apache.log4j.Level.FATAL);
        
        private final org.apache.log4j.Level log4jLevel;
        LogLevel( org.apache.log4j.Level log4jLevel ) {
            this.log4jLevel = log4jLevel;
        }

        public org.apache.log4j.Level log4jLevel() 
        { return log4jLevel; }

        static public LogLevel valueOf( org.apache.log4j.Level l4jLevel ) {
            String l4jString = l4jLevel.toString();
            return valueOf( l4jString );
        }
    }

    private Log4jHelper() {}

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    static private final String App_LOG4J_CATEGORY = "org.atomserver";

    static private org.apache.log4j.Logger AppLogger = null;
    static private org.apache.log4j.Logger rootLogger = null;

    // NOTE: you cannot just set this statically here, 
    //       because log4j won't be fully initialized and you'll get a NPE
    static private org.apache.log4j.Level originalAppLogLevel = null;
    static private org.apache.log4j.Level originalRootLogLevel = null;

    static private org.apache.log4j.Logger getLog4jLogger( Log clog ) {
        return ((Log4JLogger)( clog )).getLogger();
    }

    static private org.apache.log4j.Logger getAppLogger() {
        if ( AppLogger == null ) {
            AppLogger = getLog4jLogger( LogFactory.getLog(App_LOG4J_CATEGORY) );
            originalAppLogLevel = AppLogger.getEffectiveLevel() ;
        }
        return AppLogger;
    }

    static private org.apache.log4j.Logger getRootLogger() {
        if ( rootLogger == null ) {
            rootLogger = org.apache.log4j.Logger.getRootLogger();
            originalRootLogLevel = rootLogger.getLevel() ;
        }
        return rootLogger;
    }

    static public void setAppLogLevelToWarn() {
        System.out.println( "WARNING: setting App Log Level to WARN" );
        getAppLogger().setLevel( (org.apache.log4j.Level)org.apache.log4j.Level.WARN );
    }

    static public void resetAppLogLevel() {
        System.out.println( "WARNING: resetting App Log Level to " + originalAppLogLevel );
        getAppLogger().setLevel( originalAppLogLevel );
    }

    static public void setRootLogLevelToWarn() {
        System.out.println( "WARNING: setting ROOT Log Level to WARN" );
        getRootLogger().setLevel( (org.apache.log4j.Level)org.apache.log4j.Level.WARN );
    }

    static public void resetRootLogLevel() {
        System.out.println( "WARNING: setting ROOT Log Level to " + originalRootLogLevel );
        getRootLogger().setLevel( originalRootLogLevel );
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** returns the previous LogLevel, so you can set it back when you're finished !!
     */
    static public LogLevel setLogLevel( Log clog, LogLevel newLevel ) {
        org.apache.log4j.Logger logger = getLog4jLogger( clog );
        System.out.println( "log4jLevel = " + logger.getEffectiveLevel() );
        LogLevel originalLogLevel = LogLevel.valueOf( logger.getEffectiveLevel() );
        getLog4jLogger( clog ).setLevel( (org.apache.log4j.Level)( newLevel.log4jLevel() ));
        return originalLogLevel;
    }
    
}
