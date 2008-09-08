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

package org.atomserver.utils.thread;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * A JMX managed Spring ThreadPoolTaskExecutor
 */
@ManagedResource(description = "Throttled AtomServer Thread Pool")
public class ManagedThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Sets the core pool size")
    public void setCorePoolSize(int i) {
        super.setCorePoolSize(i);
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Gets the core pool size")
    public int getCorePoolSize() {
        return super.getCorePoolSize();
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Gets the maximum pool size")
    public void setMaxPoolSize(int maxPoolSize) {
        super.setMaxPoolSize(maxPoolSize);
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Sets the maximum pool size")
    public int getMaxPoolSize() {
        return super.getMaxPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Sets the thread KeepAlive time (secs)")
    public void setKeepAliveSeconds(int i) {
        super.setKeepAliveSeconds(i);
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Sets the thread KeepAlive time (secs)")
    public int getKeepAliveSeconds() {
        return super.getKeepAliveSeconds();
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Sets the work queue capacity")
    public void setQueueCapacity(int i) {
        super.setQueueCapacity(i);
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Gets the current pool size")
    public int getPoolSize() {
        return super.getPoolSize();
    }

    /**
     * {@inheritDoc}
     */
    @ManagedAttribute(description = "Gets the approx number of threads that are actively executing tasks")
    public int getActiveCount() {
        return super.getActiveCount();
    }

    /**
     * Returns the largest number of threads that have ever simultaneously been in the pool.
     * @return the number of threads
     */
    @ManagedAttribute(description = "Gets the largest number of threads that have ever simultaneously been in the pool")
    public int getLargestPoolSize() {
        return super.getThreadPoolExecutor().getLargestPoolSize();
    }

    /**
     * Returns the approximate total number of tasks that have been scheduled for execution.
     * Because the states of tasks and threads may change dynamically during computation,
     * the returned value is only an approximation, but one that does not ever decrease
     * across successive calls.
     * @return  the number of tasks
     */
    @ManagedAttribute(description = "Gets the the approx total number of tasks that have been scheduled for execution")
    public long getTaskCount() {
        return super.getThreadPoolExecutor().getTaskCount();
    }

    /**
     * Returns the approximate total number of tasks that have completed execution.
     *  Because the states of tasks and threads may change dynamically during computation,
     * the returned value is only an approximation, but one that does not ever decrease
     * across successive calls.
     * @return   the number of tasks
     */
    @ManagedAttribute(description = "Gets the approx total number of tasks that have completed execution")
    public long getCompletedTaskCount() {
        return super.getThreadPoolExecutor().getCompletedTaskCount();
    }

    /**
     * Returns the size of the work queue (if any)
     * @return the number of entries in the work queue
     */
    @ManagedAttribute(description = "Gets the size of the work queue")
    public int getQueueSize() {
        return ( super.getThreadPoolExecutor().getQueue() == null)
               ? 0
               : super.getThreadPoolExecutor().getQueue().size();
    }

    /**
     * Returns the number of elements that the work queue can ideally
     * (in the absence of memory or resource constraints) accept without blocking,
     * or Integer.MAX_VALUE if there is no intrinsic limit.
     *
     * Note that you cannot always tell if an attempt to add an element will
     * succeed by inspecting remainingCapacity because it may be the case that
     *  another thread is about to put or take an element.
     *
     * @return  the remaining queue capacity
     */
    @ManagedAttribute(description = "Gets the number of elements that the work queue can ideally accept")
    public int getRemainingQueueCapacity() {
        return ( super.getThreadPoolExecutor().getQueue() == null)
               ? 0
               : super.getThreadPoolExecutor().getQueue().remainingCapacity();
    }

    /**
     * Returns a String containing the current state of the ThreadPool. Meant for use in a logging statement.
     * @return a String containing the current state of the ThreadPool
     */
    public String getThreadStats() {
        StringBuffer buff = new StringBuffer();
        buff.append( "[Pool(core,max,curr) " );
        buff.append( getCorePoolSize() ).append(", ");
        buff.append( getMaxPoolSize() ).append(", ");
        buff.append( getPoolSize() ).append("]");

        buff.append( " [Tasks(active,max,req,done) " );
        buff.append( getActiveCount() ).append(", ");
        buff.append( getLargestPoolSize() ).append(", ");
        buff.append( getTaskCount() ).append(", ");
        buff.append( getCompletedTaskCount() ).append("]");

        buff.append( " [Queue(size,open) ");
        buff.append( getQueueSize() ).append(", ");
        buff.append( getRemainingQueueCapacity() ).append("]");

        return buff.toString();
    }
}
