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

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.protocol.server.RequestContext;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.uri.URIHandler;

/**
 * AtomService - This is the root of an AtomServer implementation. There is a direct correllation
 * between an AtomService and the corresponding concept of a Service in the Atom protocol.
 * It is the container for Service information associated with one or more Workspaces.
 * An AtomService MUST contain one or more AtomWorkspaces.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface AtomService {
    /**
     * Return the number of visible workspaces. A visible workspace is one that is visible to a client
     * when they ask for a service document. In some cases, AtomServer maintains virtual workspaces,
     * which are not presented to the end user.
     * @return the number of visible workspaces
     */
    int getNumberOfVisibleWorkspaces();

    /**
     * Return a list of all workspace names associated with this AtomService.
     * This method is expected to behave differently, depending on the request URL
     * <p/>If the request URL is for the "base Service URL" (e.g. /), then all <b>visible</b> Workspaces will be returned
     * <p/>If the request URL is for a specific Workspace  (e.g. /foo), then only that Workspace is returned.
     * @param request The Abdera RequestContext
     * @return List of all Workspaces.
     * @throws AtomServerException
     */
    java.util.Collection<String> listWorkspaces(RequestContext request)
            throws AtomServerException;

    /**
     * Returns the base URI for this AtomService
     * @return The base URI for this AtomService
     */
    String getServiceBaseUri();

    /**
     * Returns the AtomWorkspace associated with the name; workspace
     * @param workspace The name of the workspace
     * @return The AtomWorkspace associated with the name; workspace
     */
    AtomWorkspace getAtomWorkspace(String workspace);

    /**
     * The URIHandler associated with this AtomService. The UriHandler is wired up in the IOC container.
     * @return The URIHandler associated with this AtomService
     */
    URIHandler getURIHandler();

    /**
     * Set the URIHandler for this AtomService. Probably, the UriHandler is wired up in the IOC container.
     * @param uriHandler The URIHandler associated with this AtomService
     */
    void setUriHandler(URIHandler uriHandler);

    /**
     * Verify that the workspace and collection that have been decoded from the IRI are valid. Workspaces
     * must already be defined. Collections may be created on the fly.
     * @param workspace The workspace name. May not be null
     * @param collection The Collection name. May not be null.
     * @param iri The decoded IRI.
     * @param checkIfCollectionExists Check if the Collection actually exists?
     */
    void verifyURIMatchesStorage(String workspace,
                                 String collection,
                                 IRI iri,
                                 boolean checkIfCollectionExists);
}
