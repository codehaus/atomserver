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

/**
 * Keeps a count of samples for each second time period that it is used.  If the current second changes,
 * then it returns the total count from the last second and starts over.  The return value also accounts
 * for any periods of inactivity that may occur in between samples.  This can be used in
 * conjunction with RunningAverage to track average pages per second.
 * <p/>
 * Never keeps more than one second of tracking in memory at a time.
 * <p/>
 * This class is thread safe per instance.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class SamplePerSecond {

    /**
     * a constant that hold the value that needs to be used to convert <code>System.currentTimeMillis</code>
     * into seconds.  This could potentially be changed to do other time units as well.
     */
    protected static final long TIME_FACTOR = 1000;

    /**
     * the number of samples for the current second
     */
    protected long numberOfSamplesForCurrentSecond = 0;

    /**
     * a long representing the current second being tracked
     */
    protected long currentSecond;

    /**
     * the maximum number of samples to track during periods of inactivity
     */
    protected int maximumTimeDifference = 25;

    /**
     * Initializes to start tracking for the current second.
     *
     * @param maximumTimeDifference the maximum number of empty samples to track when there hasn't been
     *                              any activity
     */
    public SamplePerSecond(int maximumTimeDifference) {
        currentSecond = now();
        this.maximumTimeDifference = maximumTimeDifference;
    }

    /**
     * Initializes with a default <code>maximumTimeDifference</code> of 25.
     */
    public SamplePerSecond() {
        this(25);
    }

    /**
     * Gets the current time in seconds
     *
     * @return <code>System.currentTimeMillis</code> factored into seconds.
     */
    protected long now() {
        return System.currentTimeMillis() / TIME_FACTOR;
    }

    /**
     * Generates a sample event in the tracker.  If the current second being tracked is now, then
     * will simply increment the counter for the current second and return null.  If the
     * second has rolled over since the last time <code>sample()</code> was called, then an array is
     * returned containing the last tracked sample value and empty elements for each second of inactivity
     * since the last call.  The array is bounded by the <code>maximumTimeDifference</code> value.
     *
     * @return if incrementing the null, if rolling over, a long array containing the last sample value and one 0
     *         for every second of inactivity since the last sample (no larger than <code>maximumTimeDifference</code>
     */
    public long[] sample() {
        long now = now();

        //the second has changed, so we need to return the previous count and reset everything
        synchronized (this) {

            // there is a very rare case where we get a NegativeArraySizeException below...
            // Note: this actually happened in PLT for Haystack
            //if (now != currentSecond) {
            if (now > currentSecond) {
                
                int timeDifference = (int) (now - currentSecond);
                //create a results array containing n elements where n <= maximumTimeDifference
                long[] results = new long[Math.min(timeDifference, maximumTimeDifference)];

                if (timeDifference < maximumTimeDifference) {
                    //set the first element to be the current sample
                    results[0] = numberOfSamplesForCurrentSecond;
                }

                //reset
                currentSecond = now;
                numberOfSamplesForCurrentSecond = 1;
                return results;
            }

            //otherwise, we're sampling for the second that is already being tracked, then
            //increment the counter and return -1.
            numberOfSamplesForCurrentSecond++;
            return null;
        }
    }
}