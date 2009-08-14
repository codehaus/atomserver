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

import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a class that is meant to be exported as a JMX MBean. It keeps track of
 * various performance statistics in AtomServer.
 * It is IOC wired into a StatsTrackingPerformanceLog.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "AtomServer Statistics Tracker")
public class StatsTracker {

    private HashMap<String,Stats> statsMap = new HashMap<String,Stats>();

    static class Stats {
        private RunningAverage timeAvg = new RunningAverage();
        private RunningAverage tpsAvg = new RunningAverage();
        private SamplePerSecond tps = new SamplePerSecond();

        RunningAverage getTimeAvg() {
            return timeAvg;
        }
        RunningAverage getTpsAvg() {
            return tpsAvg;
        }
        SamplePerSecond getTps() {
            return tps;
        }
    }

    /**
     * Return the Stats Map. Foe JUnits only.
     * @return the Stats Map
     */
    HashMap<String, Stats> getStatsMap() {
        return statsMap;
    }

    /**
     * The performance statistics are delivered as a single String of repeated
     * [key:: avg-resp(ms), avg-tps] substrings, where avg-resp(ms) is the average response
     * time in millis, and avg-tps is the average TPS over the last N measurements. <br/>
     * (NOTE: average TPS can be zero if you don't get a lot of traffic) <br/>
     * Returning a single String allows us to not know apriori what we are keeping stats on.
     * @return  The performance stats concatenated into a single String
     */
    @ManagedAttribute(description = "Gets the performance statistics [key:: avg-resp(ms), avg-tps]")
    public String getPerformanceStatistics() {
        StringBuffer buff = new StringBuffer();
        for ( Map.Entry entry : statsMap.entrySet() ) {
            buff.append("[").
                    append(entry.getKey()).
                    append(":: ").
                    append( ((Stats)(entry.getValue())).getTimeAvg().getAverage()).
                    append(", ").
                    append( ((Stats)(entry.getValue())).getTpsAvg().getAverage()).
                    append("]\n");
        }
        String lastTimeCalledString =  buff.toString();
        return (StringUtils.isEmpty(lastTimeCalledString)) ? "No calls have been made" : lastTimeCalledString;
    }

    /**
     * tracks one sample for transactions per second
     */
    public void trackTps( String tag) {
        Stats stats = statsMap.get(tag );
        if ( stats == null ) {
            stats = new Stats();
            statsMap.put( tag, stats );
        }
        processTps( stats.getTps(), stats.getTpsAvg() );
    }

    /**
     * tracks one sample for the running verage time
     * @param time The elapsed time in millis
     */
    public void trackTime(String tag, long time) {
        Stats stats = statsMap.get(tag );
        if ( stats == null ) {
            stats = new Stats();
            statsMap.put( tag, stats );
        }
        stats.getTimeAvg().sample(time);
    }

    /**
     * Process a Transactions Per Second tick for the given sample and track the
     * appropriate running average if necessary
     *
     * @param tpsTracker the SamplePerSecond to tick
     * @param runningAvg the RunningAverage to track
     */
    protected void processTps(SamplePerSecond tpsTracker, RunningAverage runningAvg) {
        //tick one sample in the SamplePerSecond class
        long samples[] = tpsTracker.sample();

        //if it returns null, then it is incrementing the current second and we do
        //nothing, otherwise we have a result to process
        if (samples != null) {
            for (long sample : samples) {
                runningAvg.sample(sample);
            }
        }
    }


}
