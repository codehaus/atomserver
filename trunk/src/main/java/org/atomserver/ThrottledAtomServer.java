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
package org.atomserver;

import org.apache.abdera.protocol.server.ResponseContext;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.Abdera;

import java.util.concurrent.*;

/**
 * ThrottledAtomServer - An AtomServer which "throttles" calls to PUTs and POSTs, while leaving GETs
 * unthrottled - at least herein. Note that they are <b>all</b> throttled at the shared DB Connection pool.
 * And by throttling the writes here, we alway leave some number of DB Connections available for reads.
 * <p/>
 * Because we effectively serialize all writes at the database (by taking an exclusive lock
 * on the EntryStore table), the database can only handle so much load before it becomes
 * overloaded. If we limit the load on the database to it's peak capacity, we will get far
 * more throughput than we would if we overwhelm it by exceeding capacity - to where it can
 * no longer keep up with demand.
 * <p/>
 * Essentially, we "throttle" the load to some optimal load, and spill any excess load to a queue
 * to be dealt with as quickly as possible. Here the throttling is done using a ThreadPoolExecutor
 * with a ThreadPool sized to some number less than the total number of DB connections,
 * and far less than the number of HTTP Connections. This ThreadPoolExecutor is configured
 * with a BlockingQueue into which any oveload is spilled.
 * <p/>
 * Note that your Servlet container - or perhaps an Apache front-end - most likely already
 * does this same sort of throttling of its HTTP Connection load. But this ThrottledAtomServer is still useful.
 * Because our GETs are all "snapshot reads", they are very fast, and this design allows us to separate
 * read load from write load, and thus, to always provide some database capacity for GETs.  
 */
public class ThrottledAtomServer extends AtomServer {

    private Executor threadPool;
    private long taskTimeout;

    public void setThreadPool(Executor threadPool) {
        this.threadPool = threadPool;
    }

    public void setTaskTimeout(long taskTimeout) {
        this.taskTimeout = taskTimeout;
    }

    public ResponseContext createEntry(final RequestContext request) {
        Callable<ResponseContext> callableTask =
                new Callable<ResponseContext>() {
                    public ResponseContext call() {
                        return ThrottledAtomServer.super.createEntry( request );
                    }
                };
        return executePooledTask( request, callableTask );
    }

    public ResponseContext updateEntry(final RequestContext request) {
        Callable<ResponseContext> callableTask =
                new Callable<ResponseContext>() {
                    public ResponseContext call() {
                        return ThrottledAtomServer.super.updateEntry( request );
                    }
                };
        return executePooledTask( request, callableTask );
    }

    public ResponseContext deleteEntry(final RequestContext request) {
        Callable<ResponseContext> callableTask =
                new Callable<ResponseContext>() {
                    public ResponseContext call() {
                        return ThrottledAtomServer.super.deleteEntry( request );
                    }
                };
        return executePooledTask( request, callableTask );
    }

    /**
     * Execute the CallableTask on a ThreadPoolTaskExecutor. <br/>
     * NOTE: the standard Exception handling of AtomServer still happens in the AtomServer class.
     * Any Exception handling done here is for Exceptions that actually are thrown this far up
     * the food chain -- Exceptions that pertain directly to the TaskExecutor --
     * for example, TimeoutException or ExecutionException.
     * @param request
     * @param callableTask
     * @return  The Abdera ResponseContext
     */
    private ResponseContext executePooledTask( final RequestContext request,
                                               final Callable<ResponseContext> callableTask ) {
        ResponseContext response = null;
        Abdera abdera = request.getServiceContext().getAbdera();

        FutureTask<ResponseContext> futureTask = new FutureTask( callableTask );
        threadPool.execute( futureTask );

        try {
            logger.debug( "starting to wait for the task to complete");
            response = futureTask.get( taskTimeout, TimeUnit.MILLISECONDS );

        } catch (InterruptedException e) {
            // InterruptedException - if the current thread was interrupted while waiting               
            // Re-assert the thread's interrupted status
            Thread.currentThread().interrupt();

            logger.error( "InterruptedException in executePooledTask: Cause= " + e.getCause() + 
                          " Message= " + e.getMessage(), e );
            return this.servererror( abdera, request, "InterruptedException occurred:: " + e.getCause(), e );
        } catch (ExecutionException e) {
            // ExecutionException - if the computation threw an exception
            // Because all Exception handling is done in the super class; AtomServer, we should never get this
            logger.error( "ExecutionException in executePooledTask: Cause= " + e.getCause() +
                          " Message= " + e.getMessage(), e );
            return this.servererror( abdera, request, "ExecutionException occurred:: " + e.getCause(), e );
        } catch (TimeoutException e) {
            //  TimeoutException - if the wait timed out
            logger.error( "TimeoutException in executePooledTask: Cause= " + e.getCause() +
                          " Message= " + e.getMessage(), e );
            return this.servererror( abdera, request, "TimeoutException occurred:: " + e.getCause(), e );
        } catch (Exception e) {
            logger.error( "Unknown Exception in executePooledTask: Cause= " + e.getCause() +
                          " Message= " + e.getMessage(), e );
            return this.servererror( abdera, request, "Unknown Exception occurred:: " + e.getCause(), e );

        } finally {
            // Best practice is to cancel tasks whose result is no longer needed
            // NOTE; task.cancel() is harmless if the task has already completed
            // Interrupt if running...
            futureTask.cancel(true);
        }

        return response;
    }

}
