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
 * SimpleStopWatch - This is a simple one-time use implementation of StopWatch.
 * <p/>
 * NOTE: it is not cummulative. It will only operate once.
 * it is meant to be created and destroyed for a single timing.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class SimpleStopWatch implements StopWatch {
    static private Log log = LogFactory.getLog(SimpleStopWatch.class);

    static private final long NOT_SET = -1;

    //-------------- Data
    private long mStart = NOT_SET;
    private long mStop = NOT_SET;

    public SimpleStopWatch(long start, long stop) {
        this();

        if (start == 0) {
            start = NOT_SET;
        }
        mStart = start;

        if (stop == 0) {
            stop = NOT_SET;
        }
        mStop = stop;
    }

    /**
     * Constructs a new SimpleStopWatch.
     */
    public SimpleStopWatch() {}

    /**
     * {@inheritDoc}
     */
    public void start() {
        if (mStart != NOT_SET) {
            return;
        }
        mStart = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        if (mStop != NOT_SET) {
            return;
        }
        mStop = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    public double getElapsed() {
        long elapsed = getElapsedInMillis();
        double secs = (double) (elapsed) / 1000.0;

        if (log.isTraceEnabled()) {
            log.trace("getElapsed:: secs = " + secs);
        }
        return secs;
    }

    /**
     * {@inheritDoc}
     */
    public long getElapsedInMillis() {
        if (mStart == NOT_SET) {
            return 0L;
        }

        long lastTime = mStop;
        if (lastTime == NOT_SET) {
            lastTime = System.currentTimeMillis();
        }

        long elapsed = lastTime - mStart;
        if (log.isTraceEnabled()) {
            log.trace("getElapsedInMillis:: elapsed= " + elapsed +
                      " mStart= " + mStart + " lastTime= " + lastTime);
        }

        return elapsed;
    }

    /**
     * {@inheritDoc}
     */
    public Date getStartTime() {
        if (mStart != NOT_SET) {
            return new Date(mStart);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Date getStopTime() {
        if (mStop != NOT_SET) {
            return new Date(mStart);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTimeAsLong() { return mStart; }

    /**
     * {@inheritDoc}
     */
    public long getStopTimeAsLong() { return mStop; }

    public int compareTo(Object obj) {
        SimpleStopWatch other = (SimpleStopWatch) obj;
        long elapsed = getElapsedInMillis();
        long otherElapsed = other.getElapsedInMillis();
        if (elapsed < otherElapsed) {
            return -1;
        } else if (elapsed == otherElapsed) {
            return 0;
        }
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return (SimpleStopWatch.class.equals(obj.getClass()) && compareTo(obj) == 0);
    }

    public boolean isGreaterThan(Object obj) { return (compareTo(obj) > 0); }

    public boolean isLessThan(Object obj) { return (compareTo(obj) < 0); }

    /**
     * {@inheritDoc}
     */
    public int hashCode() { return (int) mStart; }

    /**
     * {@inheritDoc}
     */
    public Object clone() throws CloneNotSupportedException {
        try {
            SimpleStopWatch obj = (SimpleStopWatch) (super.clone());
            obj.mStart = mStart;
            obj.mStop = mStop;
            return obj;
        }
        catch (CloneNotSupportedException ex) {
            log.error("Exception in SimpleStopWatch.clone():: " +
                      ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

}
