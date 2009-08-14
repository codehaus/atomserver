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

import java.util.*;
import junit.framework.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.utils.perf.SimpleStopWatch;

public class SimpleStopWatchTest
    extends TestCase 
{
    static private Log log = LogFactory.getLog( SimpleStopWatchTest.class );

    protected void setUp() 
        throws java.lang.Exception
    {  super.setUp();  }
 
	public static Test suite() 
    { return new TestSuite( SimpleStopWatchTest.class ); }

    //----------------------------
    //          Tests 
    //----------------------------
    static private final double tol = 0.095;

    public double getElapseCheck( long elapse, long incr ) {
        double dElapse = ((double)elapse)/1000.0;
        double dIncr = ((double)incr)/1000.0;
        double check = dElapse - (dIncr * tol) ;
        log.debug( "Check Elaspsed time= " + check + 
                      " elapse= " + elapse + 
                      " (I*tol)= " + (dIncr * tol) );
        return check;
    }

    public void testOneStopOneStart() {
        long elapse = 0;

        //int sleepTime = 2000; 
        int sleepTime = 500; 

        SimpleStopWatch tt = new SimpleStopWatch();
        tt.start();
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}

        tt.stop();
        double et1 = tt.getElapsed();
        log.debug( "Elaspsed time= " + et1 );

        // We slept 2 secs so it must be bigger than that...
        elapse += sleepTime;
        double check = getElapseCheck( elapse, sleepTime ); 
        assertTrue( et1 > check );
        
        // Can't start/stop twice...
        Date dd1 = tt.getStartTime();
        log.debug( "Start time= " + dd1 );
        tt.start();
        Date dd3 = tt.getStartTime();
        log.debug( "Start time= " + dd1 );
        assertTrue( dd1.equals( dd3 ) );

        Date dd2 = tt.getStopTime();
        log.debug( "Stop time= " + dd2 );

        tt.stop();
        Date dd4 = tt.getStartTime();
        log.debug( "Start time= " + dd1 );
        assertTrue( dd2.equals( dd4 ) );

        double et2 = tt.getElapsed();
        log.debug( "Elaspsed time= " + et2 );

        assertTrue( et1 == et2 );
    }

    public void testMultiElapsed() {
        long elapse = 0;

        //int sleepTime = 1000; 
        int sleepTime = 500; 

        SimpleStopWatch tt = new SimpleStopWatch();

        tt.start();

        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        double et1 = tt.getElapsed();
        log.debug( "Elaspsed time 1= " + et1 );

        elapse += sleepTime;
        double check = getElapseCheck( elapse, sleepTime ); 
        assertTrue( et1 > check );
        
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        double et2 = tt.getElapsed();
        log.debug( "Elaspsed time 2= " + et2 );

        elapse += sleepTime;
        check = getElapseCheck( elapse, sleepTime ); 
        assertTrue( et2 > check );

        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        double et3 = tt.getElapsed();
        log.debug( "Elaspsed time 3= " + et3 );

        elapse += sleepTime;
        check = getElapseCheck( elapse, sleepTime ); 
        assertTrue( et3 > check );

        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        tt.stop();
        double et4 = tt.getElapsed();
        log.debug("Elaspsed time 4= " + et4 );

        elapse += sleepTime;
        check = getElapseCheck( elapse, sleepTime ); 
        assertTrue( et4 > check );

        // Constant now that we stopped the perf
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        double et5 = tt.getElapsed();
        log.debug("Elaspsed time 5= " + et5 );

        assertTrue( et5 == et4 );
    }

    public void testCompareTo() {
        SimpleStopWatch t1 = new SimpleStopWatch();
        SimpleStopWatch t2 = new SimpleStopWatch();
        int results = t1.compareTo( t1 );
        log.debug( "Results of comparing t1 w/ t1 = " + results);
        assertTrue( results == 0 );

        //int sleepTime = 1000; 
        int sleepTime = 500; 

        t1.start();
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        t2.start();

        results = t1.compareTo( t2 );
        log.debug( "Results of comparing t1 w/ t2 = " + results);
        assertTrue( results != 0 );
    }

    public void testClone() {
        try { 
            SimpleStopWatch t1 = new SimpleStopWatch();
            SimpleStopWatch sheep = (SimpleStopWatch)t1.clone();
            assertTrue( sheep.equals( t1 ) );
        }
        catch ( Exception ex ) {
            fail( "Bad clone..." );
        }
    }

}
