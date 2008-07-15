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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomService;
import org.atomserver.AtomWorkspace;
import org.atomserver.VirtualWorkspaceHandler;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.uri.URIHandler;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FileBasedBlogService implements AtomService {

    private Log log = LogFactory.getLog(FileBasedBlogService.class);
    protected java.util.Map<String, AtomWorkspace> workspaces = new HashMap<String, AtomWorkspace>();
    protected URIHandler uriHandler = null;

    private File rootDir = null;

    public File getRootDir() {
        return rootDir;
    }
    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    public AtomWorkspace getAtomWorkspace(String workspaceName) {
        AtomWorkspace workspace = workspaces.get( workspaceName );
        if ( workspace == null ) {
            workspace = new FileBasedBlogWorkspace( this, workspaceName );
            workspaces.put( workspaceName, workspace );
        }
        return workspace;
    }

    public URIHandler getURIHandler() {
        return this.uriHandler;
    }

    public void setUriHandler(URIHandler uriHandler) {
        this.uriHandler = uriHandler;
        this.uriHandler.setAtomService(this);
    }

    public String getServiceBaseUri() {
        return this.uriHandler.getServiceBaseUri();
    }

    public VirtualWorkspaceHandler getVirtualWorkspaceHandler( String id ){
        return null;
    }    

    public int getNumberOfWorkspaces()
    { return 1; }

    public int getNumberOfVisibleWorkspaces()
    { return 1; }

    public java.util.Collection<String> listWorkspaces( RequestContext request ) {
        return Arrays.asList("blogs");
    }

    public List<String> listWorkspaces() {
        return Arrays.asList("blogs");
    }

    public List<String> listCollections(String workspace) {
        List<String> collections = new ArrayList<String>();
        for (File file : getRootDir().listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.exists() &&
                       pathname.isDirectory() &&
                       pathname.canRead() &&
                       pathname.canWrite() &&
                       !pathname.isHidden();
            }
        })) {
            collections.add(file.getName());
        }
        return collections;
    }

    public boolean workspaceExists( String workspace ) {
        boolean create = false;
        File workspaceDir = getRootDir();
        return (workspaceDir.exists() && workspaceDir.isDirectory());
    }

    public boolean collectionExists(String workspace, String collection) {
        File collectionDir = new File( getRootDir(), "/" + collection );
        return (collectionDir.exists() && collectionDir.isDirectory());
    }

    public void createCollection( String workspace, String collection ) {
        if ( collectionExists( workspace, collection ) )
            return;
        File collectionDir = new File( getRootDir(), "/" + collection );
        try {
            collectionDir.mkdirs() ;
        } catch (SecurityException e) {
            String msg = "collection " + workspace + "/" + collection +
                " does not exist and could not be created.";
            log.error(msg, e);
            throw new AtomServerException( msg, e );
        }
    }

    public void verifyURIMatchesStorage(String workspace,
                                        String collection,
                                        IRI iri,
                                        boolean checkIfCollectionExists)
            throws BadRequestException {

        if (workspace == null) {
            String msg = "The URL (" + iri + ") has a NULL workspace";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (getAtomWorkspace(workspace) == null ) {
            String msg = "The URL (" + iri + ") does not indicate a recognized Atom workspace (" + workspace + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (collection == null) {
            String msg = "The URL (" + iri + ") has a NULL collection";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (!collectionExists(workspace, collection) && checkIfCollectionExists) {
            String msg = "The URL (" + iri + ") does not indicate an existing " +
                         "Atom collection (" + collection + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }
    }

    public int getMaxLinkAggregateEntriesPerPage() {
        return 100;
    }

    public int getMaxFullAggregateEntriesPerPage() {
        return 15;
    }

}
