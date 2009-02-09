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

package org.atomserver.utils.stats;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.utils.perf.StopWatch;
import org.atomserver.utils.perf.AutomaticStopWatch;

import java.util.HashMap;


public class StatsTrackerTest extends TestCase {
    static private Log log = LogFactory.getLog(StatsTrackerTest.class);

    public static Test suite() { return new TestSuite(StatsTrackerTest.class); }

    protected void setUp() throws Exception { super.setUp(); }

    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests
    //----------------------------
    public void testClientCall() throws Exception {
        StatsTracker tracker = new StatsTracker();

        // this shoudl force the Stats to get created
        tracker.trackTps( "FOO" );

        HashMap<String, StatsTracker.Stats> statsMap = tracker.getStatsMap();
        StatsTracker.Stats stats = statsMap.get( "FOO" );
        assertNotNull( stats );

        //this ensures that the rest of the test runs at the beginning of a second
        //by calling the sampler until it hits a reset point and starts counting again
        SamplePerSecond sampler = stats.getTps();
        while (sampler.sample() != null) {
        }

        // This how we will use this - with an AutomaticStopWatch
        for (int ii = 0; ii < 100; ii++) {
            StopWatch stopWatch = new AutomaticStopWatch();
            Thread.sleep(10);
            tracker.trackTps( "FOO" );
            tracker.trackTime( "FOO", stopWatch.getElapsedInMillis());
        }
        Thread.sleep(1000);

        log.debug( "STATS STRING= " + tracker.getPerformanceStatistics() );

        // the best we can say is that  0 > TPS <= 100
        long avgTPS = stats.getTpsAvg().getAverage();
        assertTrue( (avgTPS > 0 ) && (avgTPS <= 100) );

        // We should be approximately 10ms, give or take 10ms
        long avgTime = stats.getTimeAvg().getAverage();
        assertTrue( Math.abs(avgTime - 10L) < 10L );
    }
}
