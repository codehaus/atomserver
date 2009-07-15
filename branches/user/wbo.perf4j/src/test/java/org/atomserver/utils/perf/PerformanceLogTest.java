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

package org.atomserver.utils.perf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.ApplicationContext;
import org.atomserver.utils.perf.PerformanceLog;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;


public class PerformanceLogTest
    extends TestCase 
{
    static private Log log = LogFactory.getLog(  PerformanceLog.class );

    private ApplicationContext springFactory = null;
    private PerformanceLog plog = null;

    public void setUp() throws Exception { 
        super.setUp(); 
        String[] configs = {"/org/atomserver/spring/logBeans.xml"};
        springFactory = new ClassPathXmlApplicationContext( configs );
        plog = (PerformanceLog) springFactory.getBean("org.atomserver-performanceLog");
    }

    public void tearDown() throws Exception
    { super.tearDown(); }
 
	public static Test suite() 
    { return new TestSuite( PerformanceLogTest.class ); }

    //----------------------------
    //          Tests 
    //----------------------------
    public void testBasics() throws Exception {
        StopWatch stopWatch = new AutomaticStopWatch();
        Thread.sleep(500);
        plog.log( "blah1", "blah2", stopWatch );        
    }
}