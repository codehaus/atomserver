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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.ServiceContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomService;
import org.atomserver.AtomWorkspace;
import org.atomserver.CategoriesHandler;
import org.atomserver.core.etc.AtomServerPerformanceLog;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.uri.ServiceTarget;
import org.atomserver.uri.URIHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

/**
 * The abstract, base AtomService implementation. Subclasses must implement newAtomWorkspace().
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
abstract public class AbstractAtomService implements AtomService {

    /**
     * Create an appropriate AtomWorkspace for this AtomService, delegating to the actual AtomService implementation.
     * The AtomWorkspace is created "empty" by the setWorkspaces method, which then delegates to the AtomWorkspace's
     * setOptions() and bootstrap() methods.
     *
     * @param parentService This AtomService.
     * @param name          The name associated with this AtomWorkspace
     * @return The newly created AtoWorkspace.
     */
    abstract public AtomWorkspace newAtomWorkspace(AtomService parentService, String name);

    protected AtomWorkspace getJoinWorkspace(List<String> joinWorkspaces) {
        throw new UnsupportedOperationException("this service does not support join feeds.");
    }

    static private final Log log = LogFactory.getLog(AbstractAtomService.class);
    static public final String DEFAULT_CATEGORIES_WORKSPACE_PREFIX = "tags:";


    protected URIHandler uriHandler = null;
    protected java.util.Map<String, AtomWorkspace> workspaces = null;
    protected AtomServerPerformanceLog perflog;

    protected CategoriesHandler categoriesHandler;
    protected ServiceContext serviceContext;

    /**
     * {@inheritDoc}
     */
    public AtomWorkspace getAtomWorkspace(String workspace) {
        Matcher matcher = URIHandler.JOIN_WORKSPACE_PATTERN.matcher(workspace);
        return matcher.matches() ?
               getJoinWorkspace(joinWorkspaces(matcher.group(1))) :
               workspaces.get(workspace);
    }

    private List<String> joinWorkspaces(String commaSeparatedWorkspaceList) {
        return (List<String>) (commaSeparatedWorkspaceList == null ?
                               Collections.EMPTY_LIST :
                               Arrays.asList(commaSeparatedWorkspaceList.split("\\s*,\\s*")));
    }

    public void initialize() {}

    /**
     * Set the optional Performance log. If present a logger will log performance statistics
     * (i.e. start time, and elapsed time in milliseconds, along with some identifier) at
     * several pertinent points within this AtomService, including all methods in this class,
     * as well as wrapping database access methods, etc. This method is meant to
     * inject an PerformanceLog which has been configured externally in an IOC container like Spring.
     * It is in this external configuration that you specify such details as the logger name, which, in turn,
     * will define the actual name of the performance log file.
     *
     * @param perflog The AtomServerPerformanceLog
     */
    public void setPerformanceLog(AtomServerPerformanceLog perflog) {
        if (log.isTraceEnabled()) {
            log.trace("setPerformanceLog: perflog= " + perflog);
        }
        this.perflog = perflog;
    }

    /**
     * Returns the optional Performance log.     *
     * @return The AtomServerPerformanceLog
     */
    public AtomServerPerformanceLog getPerformanceLog() {
        return perflog;
    }

    /**
     * {@inheritDoc}
     */
    public URIHandler getURIHandler() {
        return this.uriHandler;
    }

    /**
     * {@inheritDoc}
     */
    public void setUriHandler(URIHandler uriHandler) {
        this.uriHandler = uriHandler;
        this.uriHandler.setAtomService(this);
    }

    /**
     * {@inheritDoc}
     */
    public String getServiceBaseUri() {
        return this.uriHandler.getServiceBaseUri();
    }


    // FIXME : javadoc
    public CategoriesHandler getCategoriesHandler() {
        return categoriesHandler;
    }

    public void setCategoriesHandler(CategoriesHandler categoriesHandler) {
        this.categoriesHandler = categoriesHandler;
    }

    public ServiceContext getServiceContext() {
        return serviceContext;
    }

    public void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    /**
     * Set the AtomWorkspaces for this AtomService. Probably this method is called from an IOC container like Spring.
     * An AtomWorkspace is created for each WorkspaceOptions object provided. the actual creation of the AtomWorkspace
     * is delgated to the newAtomWorkspace method, which created an "empty" AtomWorkspace. And then subsequently,
     * the AtomWorkspace is provisioned by calling first the setOptions method, and then the bootstrap method on
     * the newly created AtomWorkspace.
     *
     * @param workspaceOptionsSet The Set of WorkspaceOptions
     */
    public void setWorkspaces(java.util.Set<WorkspaceOptions> workspaceOptionsSet) {
        this.workspaces = new java.util.HashMap<String, AtomWorkspace>();
        for (WorkspaceOptions options : workspaceOptionsSet) {
            String workspaceName = options.getName();
            AtomWorkspace workspace = newAtomWorkspace(this, workspaceName);
            workspace.setOptions(options);
            workspace.bootstrap();
            this.workspaces.put(workspaceName, workspace);
        }
    }

    /**
     * Returns the actual number of Workspaces associated with this AtomService, including "invisible" Workspaces
     *
     * @return the number of workspaces
     */
    public int getNumberOfWorkspaces() {
        return (this.workspaces != null) ? this.workspaces.size() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumberOfVisibleWorkspaces() {
        int count = 0;
        for (AtomWorkspace wspace : workspaces.values()) {
            if (wspace.getOptions().isVisible()) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public java.util.Collection<String> listWorkspaces(RequestContext request) {
        IRI iri = request.getUri();
        if (log.isDebugEnabled()) {
            log.debug("listWorkspaces:: iri= " + iri);
        }
        ServiceTarget serviceTarget = uriHandler.getServiceTarget(request);

        String workspace = serviceTarget.getWorkspace();
        java.util.Collection<String> workspaceList = null;

        if (!StringUtils.isEmpty(workspace)) {
            // A workspace-specific Service doc was requested
            AtomWorkspace wspace = getAtomWorkspace(workspace);
            if (wspace != null) {
                workspaceList = java.util.Collections.singleton(wspace.getVisibleName());
            }
        } else {
            // A list of all workspaces was requested
            workspaceList = new java.util.ArrayList<String>();
            for (AtomWorkspace wspace : workspaces.values()) {
                if (wspace.getOptions().isVisible()) {
                    workspaceList.add(wspace.getName());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("listWorkspaces:: workspaceList= " + workspaceList);
        }
        return workspaceList;
    }


    /**
     * {@inheritDoc}
     */
    public void verifyURIMatchesStorage(String workspace, String collection, IRI iri, boolean checkIfCollectionExists) {

        if (workspace == null) {
            String msg = "The URL (" + iri + ") has a NULL workspace";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        // FIXME this needs to be cleaned up....
        String wspace = workspace;
        if (workspace.startsWith(AbstractAtomService.DEFAULT_CATEGORIES_WORKSPACE_PREFIX)) {
            wspace = wspace.replaceFirst(AbstractAtomService.DEFAULT_CATEGORIES_WORKSPACE_PREFIX, "");
        }

        if (!workspaces.keySet().contains(wspace)) {
            String msg = "The URL (" + iri + ") does not indicate a recognized Atom workspace (" + workspace + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (collection == null) {
            String msg = "The URL (" + iri + ") has a NULL collection";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        if (!getAtomWorkspace(workspace).collectionExists(collection) && checkIfCollectionExists) {
            String msg = "The URL (" + iri + ") does not indicate an existing " +
                         "Atom collection (" + collection + ")";
            log.error(msg);
            throw new BadRequestException(msg);
        }
    }

    private int maxLinkAggregateEntriesPerPage = WorkspaceOptions.DEFAULT_MAX_LINK_ENTRIES_PER_PAGE;
    private int maxFullAggregateEntriesPerPage = WorkspaceOptions.DEFAULT_MAX_FULL_ENTRIES_PER_PAGE;

    public int getMaxLinkAggregateEntriesPerPage() {
        return maxLinkAggregateEntriesPerPage;
    }

    public int getMaxFullAggregateEntriesPerPage() {
        return maxFullAggregateEntriesPerPage;
    }

    public void setMaxLinkAggregateEntriesPerPage(int maxLinkAggregateEntriesPerPage) {
        this.maxLinkAggregateEntriesPerPage = maxLinkAggregateEntriesPerPage;
    }

    public void setMaxFullAggregateEntriesPerPage(int maxFullAggregateEntriesPerPage) {
        this.maxFullAggregateEntriesPerPage = maxFullAggregateEntriesPerPage;
    }
}
