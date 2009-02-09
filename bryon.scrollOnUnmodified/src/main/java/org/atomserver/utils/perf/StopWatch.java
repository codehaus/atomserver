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

import java.io.Serializable;
import java.util.Date;

/**
 * StopWatch - provides an API for a simple StopWatch, used to time events for performance
 * reporting.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface StopWatch
        extends Serializable, Cloneable {
    /**
     * Start tracking time.
     * <p/>
     * Implementations of this class may or may not support multiple start/stop cycles - if one
     * do not, the instance can only be start()ed once, followed by a subsequent stop(), after
     * which the times can be measured.  If an implementation DOES support multiple cycles, then
     * each start() must be followed by a stop(), before start() is called again.
     */
    public void start();

    /**
     * Stop tracking time.
     */
    public void stop();

    /**
     * Get the time elapsed between start() and stop() calls (in seconds).
     *
     * @return elapsed time in seconds.
     */
    public double getElapsed();

    /**
     * Get the time elapsed between start() and stop() calls (in milliseconds).
     *
     * @return elapsed time in milliseconds.
     */
    public long getElapsedInMillis();

    /**
     * Get the point in time that start() was (first) called.
     *
     * @return the point in time that start() was (first) called.
     */
    public Date getStartTime();

    /**
     * Get the point in time that start() was (first) called.
     *
     * @return the point in time that start() was (first) called.
     */
    public long getStartTimeAsLong();

    /**
     * Get the point in time that stop() was (first) called.
     *
     * @return the point in time that stop() was (first) called.
     */
    public Date getStopTime();

    /**
     * Get the point in time that stop() was (first) called.
     *
     * @return the point in time that stop() was (first) called.
     */
    public long getStopTimeAsLong();
}
