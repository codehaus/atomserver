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


package org.atomserver.core;

import org.atomserver.FeedDescriptor;

/**
 * Describes the fundamental things that identify a Feed in an AtomServer - the
 * (workspace, collection, entryId, locale) tuple.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BaseFeedDescriptor implements FeedDescriptor {
    private String workspace = null;
    private String collection = null;

    public BaseFeedDescriptor() {}

    public BaseFeedDescriptor(String workspace, String collection) {
        this.workspace = workspace;
        this.collection = collection;
    }

    public String getWorkspace() { return workspace; }
    public void setWorkspace(String workspace) { this.workspace = workspace; }

    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("[ ").append(workspace).append(", ").append(collection).append("]");
        return buff.toString();
    }
}
