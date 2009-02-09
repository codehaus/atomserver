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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

/**
 * RunningAverage is a utility class that can be used to keep a running average over a finite sample size. This class
 * is intended to be used to keep an average of the time it takes a particular operation to execute for performance
 * monitoring purposes
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class RunningAverage {
    /**
     * The default run length is 25 if the no-arg constructor is used.
     */
    public static final int DEFAULT_RUN_LENGTH = 25;

    /**
     * This array stores the values that are added in the sample method. When the array fills up, we start back at
     * index of 0 and refill
     */
    protected long[] values;
    /**
     * The index into values where the next sampled value will be placed. Actually, nextIndex can be greater than
     * the size of values - we mod nextIndex with the runLength to get the actual index value to use.
     */
    protected AtomicInteger nextIndex;
    /**
     * The size of the running average (that is, how many of the past sampled values will be stored to calculate the
     * average).
     */
    protected int runLength;
    /**
     * This is the maximum value that we let nextIndex get to before we reset it back to 0. This is necessary
     * because we just keep adding to nextIndex eventually we'll overflow.
     */
    protected int maxIndexValueBeforeResetting;

    /**
     * Creates a new RunningAverage with the default run length.
     */
    public RunningAverage() {
        this(DEFAULT_RUN_LENGTH);
    }

    /**
     * Creates a RunningAverage that will look at the past runLength number of sampled values when calculating the
     * average.
     *
     * @param runLength the number of values to store to calculate the average - must be greater than 0
     */
    public RunningAverage(int runLength) {
        assert runLength > 0;

        this.runLength = runLength;
        values = new long[runLength];
        Arrays.fill(values, Long.MIN_VALUE); //min value means no value has yet been recorded in that spot
        nextIndex = new AtomicInteger(0);
        maxIndexValueBeforeResetting = runLength * 10;
    }

    /**
     * Adds a new value to be considered when calculating the average.
     *
     * @param valueToSample The value to sample. May not be equal to Long.MIN_VALUE
     */
    public void sample(long valueToSample) {
        //increment the index value
        int next = nextIndex.getAndIncrement();

        //once we reach a certain point, reset the index value so we don't overflow.
        //SYNCHRONIZATION NOTE: Note that if this method is called by 2 threads simultaneously, it's possible that
        //we'll insert one value in values that will be quickly overwritten by the next call to this method. That's
        //really not that big a deal, though, because the concept of the running average is we don't care that it's
        //100% exact, we just want to see how it changes over time
        if (next == maxIndexValueBeforeResetting) {
            nextIndex.set(1);
        }
        values[next % runLength] = valueToSample;
    }

    /**
     * Gets the calculated average.
     * @return The average of the values entered in the last runLength calls to sample (or as many times as sampled
     *         has been called if it is less than runLength). If sample has never been called, this method returns 0.
     */
    public long getAverage() {
        int numPopulatedValues = 0;
        long sum = 0;
        for (long value : values) {
            if (value != Long.MIN_VALUE) {
                sum += value;
                numPopulatedValues++;
            }
        }
        return (numPopulatedValues == 0) ? 0 : sum / numPopulatedValues;
    }
}
