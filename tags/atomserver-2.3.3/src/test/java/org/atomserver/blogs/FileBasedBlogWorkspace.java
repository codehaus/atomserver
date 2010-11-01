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


package org.atomserver.blogs;

import org.atomserver.AtomWorkspace;
import org.atomserver.AtomService;
import org.atomserver.AtomCollection;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.exceptions.AtomServerException;
import org.apache.abdera.model.Collection;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.Abdera;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 */
public class FileBasedBlogWorkspace implements AtomWorkspace {
    private String name = null;
    private AtomService parentService = null ;
    private java.util.Map<String, AtomCollection> collections = new HashMap<String, AtomCollection>();
    private WorkspaceOptions options = new WorkspaceOptions();

    public FileBasedBlogWorkspace( AtomService parentService, String name ) {
        this.name = name ;
        this.parentService = parentService;
    }

    public String getName() {
        return name;
    }

    public String getVisibleName() {
        return name;
    }

    public AtomService getParentAtomService() {
        return parentService;
    }

    public File getRootDir() {
       return ((FileBasedBlogService)parentService).getRootDir();
    }

    public AtomCollection getAtomCollection(String collectionName ) {
        AtomCollection collection = collections.get( collectionName );
        if ( collection == null ) {
            collection = new FileBasedBlogCollection( this, collectionName );
            collections.put( collectionName, collection );
        }
        return collection;
    }

    public java.util.Collection<Collection> listCollections(RequestContext request) throws AtomServerException {
        return listCollections( request, name );
    }

    public java.util.Collection<Collection> listCollections(RequestContext request, String workspace) {
        Abdera abdera = request.getServiceContext().getAbdera();
        File[] files = getRootDir().listFiles();
        List<Collection> colls = new ArrayList<Collection>(files.length);
        for (int i = 0 ; i < files.length ; ++i) {
            if (files[i].isDirectory() && ! files[i].isHidden()) {
                Collection sc = abdera.getFactory().newCollection();
                sc.setTitle(files[i].getName());
                colls.add(sc);
            }
        }
        return colls;
    }


    public WorkspaceOptions getOptions() {
        return options;
    }

    public void setOptions(WorkspaceOptions options) {
    }

    public void bootstrap() {
    }

    public List<String> listCollections() {
        return new ArrayList<String>(collections.keySet());
    }

    public void createCollection(String collection) {
        getAtomCollection(collection);
    }

    public boolean collectionExists(String collection) {
        return true;
    }
}
