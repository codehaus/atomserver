/* Copyright (c) 2009 HomeAway, Inc.
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

package org.atomserver.server.servlet;

import java.util.List;
import java.util.ArrayList;

import org.springframework.jmx.export.annotation.*;

/**
 * BlockingFilterSettings - settings for the BlockingFilter. This object is a JMX MBean and is managed through
 * JMX console. It is injected into <code>BlockingFilter</code> object.
 */
@ManagedResource(description = "Blocking Filter")
public class BlockingFilterSettings {

    // List of blocked users
    private final List<String> blockedUsers = new ArrayList<String>();

    // List of regular expressions for blocked paths
    private final List<String> blockedPaths = new ArrayList<String>();

    // Maximum content length
    private int maxContentLength = -1;

    private boolean writesDisabled = false;


    /**
     * Constructor for BlockingFilterSettings
     */
    public BlockingFilterSettings() {}

    /**
     * Returns the maximum content length
     * @return
     */
    @ManagedAttribute(description="Get maximum content length")
    public int getMaxContentLength() {
        return maxContentLength;
    }

    /**
     * Returns a list of blocked users
     * @return a list of blocked users
     */
    @ManagedAttribute(description="Get blocked users")
    public List<String> getBlockedUsers() {
        return blockedUsers;
    }

    /**
     * Returns a list of regular expressions for the blocked paths.
     * @return a list of blocked users
     */
    @ManagedAttribute(description="Get blocked paths")
    public List<String> getBlockedPaths() {
        return blockedPaths;
    }

    /**
     * Set the maximum content length. A value of -1 means the content will
     * not be blocked by its length.
     * @param maxContentLength
     */
    @ManagedAttribute(description="Set maximum content length")
    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    /**
     * Add a user to block.
     * @param user  user id
     */
    @ManagedOperation(description = "Add a user to block")
    @ManagedOperationParameters({
      @ManagedOperationParameter(name="user", description="User to block")
    })
    public void addBlockedUser(String user) {
        if(!this.blockedUsers.contains(user)) {
            this.blockedUsers.add(user);
        }
    }

    /**
     * Remove a blocked user.
     * @param user
     */
    @ManagedOperation(description = "Remove a blocked user")
    @ManagedOperationParameters({
      @ManagedOperationParameter(name="user", description="Blocked user to remove")
    })
    public void removeBlockedUser(String user) {
        if(this.blockedUsers.contains(user)) {
            this.blockedUsers.remove(user);
        }
    }

    /**
     * Add a regular expression of a path to block.
     * @param pathExp
     */
    @ManagedOperation(description = "Add a regular expression of a path to block")
    @ManagedOperationParameters({
      @ManagedOperationParameter(name="pathExp", description="Regular expression of the path to block")
    })
    public void addBlockedPath(String pathExp) {
        if(!this.blockedPaths.contains(pathExp)) {
            this.blockedPaths.add(pathExp);
        }
    }

    /**
     * Remove the regular expression of the path that has been blocked.
     * @param pathExp
     */
    @ManagedOperation(description = "Remove a regular expression for a blocked path")
    @ManagedOperationParameters({
      @ManagedOperationParameter(name="pathExp", description="Regular expression of the blocked path to remove")
    })
    public void removeBlockedPath(String pathExp) {
        if(this.blockedPaths.contains(pathExp)) {
            this.blockedPaths.remove(pathExp);
        }
    }

    /**
     * Check if the writes are not allowed or not.
     */
    @ManagedAttribute( description = "Block writes to the server")
    public boolean getWritesDisabled() {
        return writesDisabled;
    }

     /**
     * Enable or disable writes to this server
     * @return true if the writes are blocked, false otherwise.
     */
    @ManagedAttribute( description = "Block or Allow writes to the server", persistPolicy="OnUpdate")
    public void setWritesDisabled(boolean writesEnabled) {
        this.writesDisabled = writesEnabled;
    }
}
