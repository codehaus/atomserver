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

import org.apache.abdera.model.Collection;
import org.apache.abdera.protocol.server.RequestContext;
import org.atomserver.exceptions.AtomServerException;

import org.atomserver.core.WorkspaceOptions;

import java.util.List;

/**
 * AtomWorkspace - Workspaces in AtomPub allow you to group related information (resources) into logical sets (collections).
 * An AtomWorkspace represents that concept. It is a server-defined set of AtomPub collections, which are represented
 * as zero or more AtomCollection objects, where these AtomCollections describe the AtomPub collections of
 * resources available to Clients for reading and/or writing.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface AtomWorkspace {

    /**
     * Return the name associated with this AtomWorkspace
     * @return The name associated with this AtomWorkspace
     */
    String getName();

    /**
     * Returns the AtomService to which this AtomWorkspace belongs
     * @return The AtomService to which this AtomWorkspace belongs
     */
    AtomService getParentAtomService();

    /**
     * Return the List of collection names associated with this AtomWorkspace
     * @return The List of collection names associated with this AtomWorkspace
     */
    List<String> listCollections();

    /**
     * Create a collection for this collection name.
     * @param collection The name of the collection to create.
     */
    void createCollection(String collection);

    /**
     * Return true/false if the named collection exists.
     * @param collection The name of the collection to check.
     * @return True/false if the named collection exists
     */
    boolean collectionExists( String collection );

    /**
     * Return the list of Abdera Collections associated with the AtomWorkspace.
     * @param request The Abdera RequestContext
     * @return The list of Abdera Collections associated with the AtomWorkspace
     * @throws AtomServerException
     */
    java.util.Collection<Collection> listCollections( RequestContext request )
            throws AtomServerException;

    /**
     * Return the AtomCollection associated with provided collectionName.
     * @param collectionName The collection name
     * @return The AtomCollection associated with provided name.
     */
    AtomCollection getAtomCollection(String collectionName);

    /**
     * Return the WorkspaceOptions associated with this AtomWorkspace
     * @return The WorkspaceOptions associated with this AtomWorkspace
     */
    WorkspaceOptions getOptions();

    /**
     * Set the WorkspaceOptions associated with this AtomWorkspace. Probably this method is called by
     * an IOC container like Spring.
     * @param options The WorkspaceOptions associated with this AtomWorkspace.
     */
    void setOptions( WorkspaceOptions options );

    /**
     * This method should be called as Workspaces are instantiated, so that any specific bootstrapping
     * can occur. For example, it can do tasks such as ensure Collections are created up-front.
     */
    void bootstrap();
}
