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

package org.atomserver.utils.alive;


/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface IsAliveHandler {

    /**
     * Set the initial Alive state of the server (from the IOC framework)
     * @param state must be one of "OK" or "DOWN"
     */
    void setInitialAliveState( String state );

    /** return the current state as either "OK", "DOWN", or "ERROR"
     */
    String getAliveState();

    /** toggle the current state to "OK"
     */
    void activate();

    /** Toggle the current state to "DOWN". This is meant to be used externally (via JMX). 
     * Once the system is set as "DOWN" it will remain that way until it is reset using activate(). 
     * This allows it to be removed from the load balancer, and its Request queue drained.
     * This is useful when shutting down the system. 
     */
    void deactivate();
    
    /** dynamically determine the current state (either "OK", "DOWN", or "ERROR")
     */
    AliveStatus isAlive();
}