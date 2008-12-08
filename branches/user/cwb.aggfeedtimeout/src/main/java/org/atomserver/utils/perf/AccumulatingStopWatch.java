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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;

/**
 * AccumulatingStopWatch - This is an accumulating implementation of StopWatch.
 * <p/>
 * It is started and stopped. You ask it for elapsed time.  This version accumulates time as it is
 * started and stopped. Elapsed time is the total amount of time for all intervals timed.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AccumulatingStopWatch
        implements StopWatch {
    static private Log log = LogFactory.getLog(AccumulatingStopWatch.class);

    //-------------- Data
    private SimpleStopWatch mCurrentStopWatch = null;
    private long mElapsed = 0L;
    private int mIntervals = 0;
    private long mLastInterval = 0L;

    /**
     * {@inheritDoc}
     */
    public void start() {
        if (mCurrentStopWatch != null) {
            return;
        }
        mCurrentStopWatch = new SimpleStopWatch();
        mCurrentStopWatch.start();
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        if (mCurrentStopWatch == null) {
            return;
        }
        mCurrentStopWatch.stop();
        mLastInterval = mCurrentStopWatch.getElapsedInMillis();
        mElapsed += mLastInterval;
        mIntervals++;
        mCurrentStopWatch = null;
    }

    /**
     * {@inheritDoc}
     */
    public double getElapsed() {
        if (mCurrentStopWatch != null) {
            stop();
        }

        double secs = (double) (mElapsed) / 1000.0;
        if (log.isDebugEnabled()) {
            log.debug("getElapsed:: elapsed= " + mElapsed + " secs = " + secs);
        }
        return secs;
    }

    /**
     * {@inheritDoc}
     */
    public long getElapsedInMillis() {
        return mElapsed;
    }

    /**
     * Getter for property 'numIntervals'.
     *
     * @return Value for property 'numIntervals'.
     */
    public int getNumIntervals() {
        if (log.isDebugEnabled()) {
            log.debug("getIntervals= " + mIntervals);
        }
        return mIntervals;
    }

    /**
     * Getter for property 'averageForIntervals'.
     *
     * @return Value for property 'averageForIntervals'.
     */
    public double getAverageForIntervals() {
        if (mIntervals == 0) {
            return 0.0;
        }

        double elapsed = getElapsed();
        double avg = elapsed / ((double) mIntervals);

        if (log.isDebugEnabled()) {
            log.debug("getAverageTime= " + avg);
        }
        return avg;
    }

    /**
     * Getter for property 'lastInterval'.
     *
     * @return Value for property 'lastInterval'.
     */
    public double getLastInterval() { return (((double) mLastInterval) / 1000.0); }

    public int compareTo(Object obj) {
        AccumulatingStopWatch other = (AccumulatingStopWatch) obj;
        if (mElapsed < other.mElapsed) {
            return -1;
        } else if (mElapsed == other.mElapsed) {
            return 0;
        }
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return (AccumulatingStopWatch.class.equals(obj.getClass()) && compareTo(obj) == 0);
    }

    public boolean isGreaterThan(Object obj) { return (compareTo(obj) > 0); }

    public boolean isLessThan(Object obj) { return (compareTo(obj) < 0); }

    /**
     * {@inheritDoc}
     */
    public int hashCode() { return (int) mElapsed; }

    /**
     * {@inheritDoc}
     */
    public Object clone() throws CloneNotSupportedException {
        try {
            AccumulatingStopWatch obj = (AccumulatingStopWatch) (super.clone());
            obj.mCurrentStopWatch = mCurrentStopWatch;
            obj.mElapsed = mElapsed;
            obj.mIntervals = mIntervals;
            obj.mLastInterval = mLastInterval;
            return obj;
        }
        catch (CloneNotSupportedException ex) {
            log.error("Exception in AccumulatingStopWatch.clone():: " +
                      ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Date getStartTime() { throw new UnsupportedOperationException("NOT APPLICABLE"); }

    /**
     * {@inheritDoc}
     */
    public Date getStopTime() { throw new UnsupportedOperationException("NOT APPLICABLE"); }

    /**
     * {@inheritDoc}
     */
    public long getStartTimeAsLong() { throw new UnsupportedOperationException("NOT APPLICABLE"); }

    /**
     * {@inheritDoc}
     */
    public long getStopTimeAsLong() { throw new UnsupportedOperationException("NOT APPLICABLE"); }

}
