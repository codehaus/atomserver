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

import org.atomserver.core.WorkspaceOptions;

/**
 * VirtualWorkspaceHandler -- handles all aspect of a given Virtual Workspaces,
 * including ContentStorage and any specifics for that workspace.
 * <p/>
 * A Virtual Workspace is a hidden, parallel
 * workspace within AtomServer. For example, imagine that you have a real workspace named "foo"
 * which you would access with the URL; /foo. AtomServer creates a hidden, parallel workspace
 * named /tags:foo - through which you can manage Categories (tags) on the /foo workspace.
 * <p/>
 * A Virtual Workspace (e.g. /tags:foo) does not actually store any specific metadata for itself
 * (i.e. there is not a corresponding row in the EntryStore table). Instead, it manipulates
 * some aspect of the metadata for it's actual, related workspace.
 * <p/>
 * So continuing with our example, if you want to manipulate the Categories
 * of /foo/bar/xyz.xml, then you would PUT a Categories document to /tags:foo/bar/xyz.xml.
 * This is both more efficient -- we are able to surgically affect certain metadata without
 * touching the actual content -- and it works around a semantic discrepancy in the AtomPub spec.
 * AtomPub allows you to directly manipulate Categories through the Entry document, but the semantics
 * are unclear -- does a missing Categories document indicate that Categories are to be deleted,
 * or left as is? Are Categories additive (the union), or are they exclusive? By providing an
 * explicit mechanism -- Virtual Workspaces -- for manipulating metadata, we can be clear
 * and intentional, and avoid side effects.
 * <p/>
 * Typically, the ContentStorage for the Virtual Workspace will operate on the content of the PUT,
 * (e.g. a list of Categories to apply)
 * and store it into the corresponding tables for the actual workspace. It should also kick up the
 * revision number for the actual workspace.
 */
public interface VirtualWorkspaceHandler {
    static public final String CATEGORIES = "CATEGORIES";

    /**
     * Create a new Virtual Workpace for this AtomService and this actual Workspace,
     * as represented by it's WorkspaceOptions. This method is meant to entirely wire
     * up the Virtual Workspace however it so chooses -- i.e. set up the ContentStorage, etc.
     * within a newly created, internal WorkspaceOptions for the Virtual Workspace.
     * @param parentService the parent AtomService
     * @param options  the WorkspaceOptions for the actual workspace. This is required so that
     * you can set various items in the Virtual Workspace (e.g. localized, etc.)
     * @return   the new VirtualWorkspace
     */
    AtomWorkspace newVirtualWorkspace(AtomService parentService, WorkspaceOptions options);    
}
