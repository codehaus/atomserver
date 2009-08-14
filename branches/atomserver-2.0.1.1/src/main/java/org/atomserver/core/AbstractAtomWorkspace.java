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

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Collection;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomCollection;
import org.atomserver.AtomServer;
import org.atomserver.AtomService;
import org.atomserver.AtomWorkspace;
import org.atomserver.exceptions.BadRequestException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * The abstract, base AtomWorkspace implementation. Subclasses must implement newAtomCollection(), as well as
 * bootstrap() from the AtomWorkspace interface.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
abstract public class AbstractAtomWorkspace implements AtomWorkspace {

    /**
     * Create an appropriate AtomCollection for this AtomWorkspace, delegating to the actual AtomWorkspace implementation.
     * @param parentWorkspace This AtomWorkspace
     * @param collectionName The name to associate with this AtomCollection
     * @return The newly created AtomCollection.
     */
    abstract public AtomCollection newAtomCollection( AtomWorkspace parentWorkspace, String collectionName);

    static private final Log log = LogFactory.getLog(AbstractAtomWorkspace.class);

    private String name = null;
    private WorkspaceOptions options = null;
    private AtomService parentService = null;

    private java.util.Map<String, AtomCollection> collections = new HashMap<String, AtomCollection>();
    private final Object collectionCreateLock = new Object();

    public AbstractAtomWorkspace( AtomService parentService, String name ) {
        this.name = name ;
        this.parentService = parentService;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public AtomService getParentAtomService() {
        return parentService;
    }

    /**
     * {@inheritDoc}
     */
    public AtomCollection getAtomCollection(String collectionName) {
        AtomCollection collection = collections.get( collectionName );
        if ( collection == null ) {
            synchronized ( collectionCreateLock ) {
                 collection = newAtomCollection( this, collectionName );
                 collections.put( collectionName, collection );
            }
        }
        return collection;
    }

    /**
     * {@inheritDoc}
     */
    public WorkspaceOptions getOptions() {
        return options;
    }

    /**
     * {@inheritDoc}
     */
    public void setOptions( WorkspaceOptions options ) {
        this.options = options;
    }

    /**
     * {@inheritDoc}
     */
    public java.util.Collection<Collection> listCollections( RequestContext request) {
        Abdera abdera = request.getServiceContext().getAbdera();

        if (parentService.getAtomWorkspace( name ) == null) {
            String msg = "The workspace provided (" + name + ") is not available.";
            log.error(msg);
            throw new BadRequestException(msg);
        }
        List<String> collectionNames = listCollections();

        List<Collection> colls = new ArrayList<Collection>(collectionNames.size());

        Factory factory = AtomServer.getFactory( abdera );

        for (String collectionName : collectionNames) {
            Collection sc = factory.newCollection();
            // NOTE: In AtomServer, we use the fact that Collection.title IS the Collection name !!
            //       This is a bit of a hack !!
            sc.setTitle(collectionName);
            colls.add(sc);
        }
        return colls;
    }

    /**
     * {@inheritDoc}
     */
    public boolean collectionExists(String collection) {
        return listCollections().contains(collection);
    }
}


