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
 * Tests that the SamplePerSecond class works as expected
 */
public class SamplePerSecondTest extends TestCase {
    /**
     * Tests the sample by:
     * <pre>
     *   1. waiting until the very start of a secoond
     *   2. making a sampler call and verify it returns null (because it is still sampling the current second)
     *   3. waiting one second so we know that second will roll over
     *   4. making another sampler call and verifying that it rolled over and contains the previous two samples and
     *      a one second time difference
     *   5. wait two seconds so we have a time difference
     *   6. do another sample, validate that the returned struct properly shows the time difference
     * </pre>
     *
     * @throws Exception something bad happened
     */
    public void testSampler() throws Exception {
        SamplePerSecond sampler = new SamplePerSecond();

        //this ensures that the rest of the test runs at the beginning of a second
        //by calling the sampler until it hits a reset point and starts counting again
        while (sampler.sample() != null) {
        }

        assertEquals("should be null since the second just rolled over", null, sampler.sample());
        Thread.sleep(1000);

        long[] results = sampler.sample();
        assertEquals("now should be 2", 2, results[0]);
        assertEquals("time since last should be 0", 1, results.length);

        Thread.sleep(2000);
        results = sampler.sample();
        assertEquals("should see 1 samples", 1, results[0]);
        assertEquals("should see 1 empty sample", 0, results[1]);
        assertEquals("should see 1 second difference (2 element array)", 2, results.length);
    }

    /**
     * Tests that when the maxSamples < time difference, that the array is truncated and all 0's
     * are returned
     *
     * @throws Exception something bad happened
     */
    public void testSamplerMax() throws Exception {
        SamplePerSecond sampler = new SamplePerSecond(2);

        //this ensures that the rest of the test runs at the beginning of a second
        //by calling the sampler until it hits a reset point and starts counting again
        while (sampler.sample() != null) {
        }

        assertEquals("should be null since the second just rolled over", null, sampler.sample());

        //test that case where difference == max
        Thread.sleep(2000);
        long[] results = sampler.sample();
        assertEquals("should all be zero", 0, results[0]);
        assertEquals("should all be zero", 0, results[1]);
        assertEquals("should see 2 element array", 2, results.length);

        //test case where difference < max
        Thread.sleep(3000);
        results = sampler.sample();
        assertEquals("should all be zero", 0, results[0]);
        assertEquals("should all be zero", 0, results[1]);
        assertEquals("should see 2 element array", 2, results.length);
    }
}