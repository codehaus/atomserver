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


package org.atomserver.utils;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * An Inversion of Control Logger, where the actual logger name is configured in the IOC container
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class IOCLog
{
    static protected Log stdlog = LogFactory.getLog( IOCLog.class );

    protected String loggerName = null;
    protected Log ioclog = null;

    public Log getLog() { 
        return ioclog; 
    } 

    /** 
     * Wired from Spring 
     */
    public void setLoggerName( String loggerName ) {
        ioclog = LogFactory.getLog( loggerName );
        if ( ioclog == null || !ioclog.isInfoEnabled () ) {
            ioclog.warn( new Date () + "\nLogger is not currently ENABLED. \nEnable \""+
                          loggerName + "=INFO\" in log4j.xml to see the log messages\n");
            stdlog.warn( new Date () + "\nLogger is not currently ENABLED. \nEnable \""+
                          loggerName + "=INFO\" in log4j.xml to see the log messages\n");
        }   
        if ( stdlog.isDebugEnabled() ) 
            stdlog.debug( "IOCLog( " + loggerName + ") is ENABLED" );
    }

}