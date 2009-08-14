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

import junit.framework.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.utils.perf.AccumulatingStopWatch;

/**
 */
public class AccumulatingStopWatchTest
    extends TestCase 
{
    static private Log log = LogFactory.getLog( AccumulatingStopWatchTest.class );

    protected void setUp() 
        throws java.lang.Exception
    {  super.setUp();  }

    public static Test suite() 
    {  return new TestSuite( AccumulatingStopWatchTest.class ); }

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

    public void testStopStart() {
        long elapse = 0;

        //int sleepTime = 2000; 
        int sleepTime = 500; 

        AccumulatingStopWatch tt = new AccumulatingStopWatch();
        tt.start();
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}

        tt.stop();
        double et1 = tt.getElapsed();
        log.debug( "Elaspsed time= " + et1 );

        // We slept sleepTime secs so it must be bigger than that...
        elapse += sleepTime;
        double check = getElapseCheck( elapse, sleepTime ); 
        assertTrue( et1 > check );

        tt.start();
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        tt.stop();
        double et2 = tt.getElapsed();
        log.debug( "Elaspsed time= " + et2 );

        int slept = sleepTime + sleepTime;

        check = getElapseCheck( elapse, slept ); 
        assertTrue( et2 > check );

        tt.stop();
        double et3 = tt.getElapsed();
        log.debug( "Elaspsed time= " + et2 );
        assertTrue( et2 == et3 );

        tt.start();
        tt.start();
    }

    public void testCompareTo() {
        AccumulatingStopWatch t1 = new AccumulatingStopWatch();
        AccumulatingStopWatch t2 = new AccumulatingStopWatch();
        int results = t1.compareTo( t1 );
        log.debug( "Results of comparing t1 w/ t1 = " + results);
        assertTrue( results == 0 );

        int sleepTime = 500; 

        t1.start();
        try { Thread.sleep(sleepTime); }
        catch ( InterruptedException ex ) {}
        t2.start();

        // These two are still equal, haven't stopped them yet...
        results = t1.compareTo( t2 );
        log.debug( "Results of comparing t1 w/ t2 = " + results);
        assertTrue( results == 0 );

        t1.stop();
        results = t1.compareTo( t2 );
        log.debug( "Results of comparing t1 w/ t2 = " + results);
        assertTrue( results != 0 );

        t2.stop();
        results = t1.compareTo( t2 );
        log.debug( "Results of comparing t1 w/ t2 = " + results);
        assertTrue( results != 0 );
    }

    public void testClone() {
        try { 
            AccumulatingStopWatch t1 = new AccumulatingStopWatch();
            AccumulatingStopWatch sheep = (AccumulatingStopWatch)t1.clone();
            assertTrue( sheep.equals( t1 ) );
        }
        catch ( Exception ex ) {
            fail( "Bad clone..." );
        }
    }

}
