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

/**
 * Tests the RunningAverage class.
 */
public class RunningAverageTest extends TestCase {


    public void testRunningAverage() throws Exception {
        final RunningAverage average = new RunningAverage();

        assertEquals("Average should be 0 before sample() is called", 0, average.getAverage());

        //check case where sample is called less than runLength times;
        average.sample(1);
        average.sample(2);
        average.sample(3);
        assertEquals("Average not correct when sample only called a couple times", 2, average.getAverage());

        for (int i = 1; i <= 100; i++) {
            average.sample(i);
        }
        //since the default run length is 25, the average should now be the average of 76-100
        assertEquals("Expected average not correct", 88, average.getAverage());

        //call sample some more to ensure we hit the case where nextIndex needs to reset
        for (int i = 1; i <= 300; i++) {
            average.sample(i);
        }
        assertEquals("Expected average not correct after nextIndex reset", 288, average.getAverage());

        //test multi-threaded access
        Thread[] updatingThreads = new Thread[20];
        for (int i = 0; i < updatingThreads.length; i++) {
            updatingThreads[i] = new Thread() {
                public void run() {
                    for (int i = 0; i < 500; i++) {
                        average.sample(i % 3);
                    }
                }
            };
            updatingThreads[i].start();
        }

        for (Thread thread : updatingThreads) {
            thread.join();
        }

        //at this point, since we've just bombarded the sample with equal numbers of calls of 0, 1 and 2,
        //so the average should be 1
        assertEquals("Expected average after multithreaded access not correct", 1, average.getAverage());
    }
}