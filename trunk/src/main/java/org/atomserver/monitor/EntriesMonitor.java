/* Copyright Homeaway, Inc 2005-2008. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.monitor;

import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * JMX enabled monitor to track how much of PUT entrys are of same content or of different content.
 *
 */

@ManagedResource(description = "Atomserver Entries Monitor")
public class EntriesMonitor {

    private long numberOfEntriesToUpdate = 0;
    private long numberOfEntriesActuallyUpdated = 0;
    private long numberOfEntriesWitheSameContent = 0;

    // Number of getEntries requests to server
    private long numberOfgetEntriesRequests = 0;
    // Number of getEntries requests which returns zero count .
    private long entriesReturningZeroCount = 0;

    @ManagedAttribute(description = "Number of Entries to Add/Update")
    public long getNumberOfEntriesToUpdate() {
        return numberOfEntriesToUpdate;
    }

    @ManagedAttribute(description = "Number of Entries Actually Added/Updated")
    public long getNumberOfEntriesActuallyUpdated() {
        return numberOfEntriesActuallyUpdated;
    }

    @ManagedAttribute(description = "Number of Entries With Same Content/Categories")
    public long getNumberOfEntriesWitheSameContent() {
        return numberOfEntriesWitheSameContent;
    }

    @ManagedAttribute(description = "Number of getEntries requests")
    public long getNumberOfgetEntriesRequests() {
        return this.numberOfgetEntriesRequests;
    }

    @ManagedAttribute(description = "Number of getEntries requests returning zero count")
    public long getNumberOfgetEntriesReturningZeroCount() {
        return this.entriesReturningZeroCount;
    }

    @ManagedAttribute(description = "Percentage of getEntries requests returning zero count")
    public double getPercentOfNonZeroEntriesRequested() {
        if((entriesReturningZeroCount == 0) && (numberOfgetEntriesRequests == 0)) {
            return -1.0;
        }
        if(entriesReturningZeroCount > 0) {
            return (entriesReturningZeroCount )/((double)numberOfgetEntriesRequests) * 100.0;
        }
        return 0.0;
    }


    // ========== Update methods ==========
    
    public synchronized void updateNumberOfEntriesToUpdate(int count) {
        numberOfEntriesToUpdate += count;
    }

    public synchronized void updateNumberOfEntriesActuallyUpdated(int count) {
        numberOfEntriesActuallyUpdated += count;
    }
    
    public synchronized void updateNumberOfEntriesNotUpdatedDueToSameContent(int count) {
        numberOfEntriesWitheSameContent += count;
    }

    public synchronized void updateNumberOfGetEntriesRequests(int count) {
        numberOfgetEntriesRequests += count;
    }

    public synchronized void updateNumberOfGetEntriesRequestsReturningNone(int count) {
        entriesReturningZeroCount += count;
    }

}
