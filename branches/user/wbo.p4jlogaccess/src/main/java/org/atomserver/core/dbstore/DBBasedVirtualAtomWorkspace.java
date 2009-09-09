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

package org.atomserver.core.dbstore;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.AtomCollection;
import org.atomserver.AtomService;
import org.atomserver.AtomWorkspace;
import org.atomserver.EntryType;
import org.atomserver.core.EntryMetaData;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.MovedPermanentlyException;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.FeedTarget;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBBasedVirtualAtomWorkspace extends DBBasedAtomWorkspace {

    private static final Log log = LogFactory.getLog(DBBasedVirtualAtomWorkspace.class);

    static String getEntriesWorkspaceName(String workspaceName) {
        String[] parts = workspaceName.split(":");
        String affliatedWorkspace = null;
        if (parts.length == 2) {
            affliatedWorkspace = parts[1];
        }
        log.debug("AFFLIATED WORKSPACE for " + workspaceName + " is " + affliatedWorkspace);
        return affliatedWorkspace;
    }

    public DBBasedVirtualAtomWorkspace(AtomService parentAtomService, String name) {
        super(parentAtomService, name);
    }

    /**
     * {@inheritDoc}
     */
    public String getVisibleName() {
        return getEntriesWorkspaceName(getName());
    }

    public AtomCollection newAtomCollection(AtomWorkspace parentWorkspace, String collectionName) {

        return new DBBasedAtomCollection(this, collectionName) {

            protected String getEntriesWorkspaceName() {
                return DBBasedVirtualAtomWorkspace.getEntriesWorkspaceName(parentAtomWorkspace.getName());
            }

            /**
             * {@inheritDoc}
             */
            public Feed getEntries(RequestContext request) throws AtomServerException {
                FeedTarget feedTarget = getURIHandler().getFeedTarget(request);
                String workspace = feedTarget.getWorkspace();

                String msg = "Cannot ask for a Feed to a tags: workspace (" + request.getResolvedUri() + ")";
                log.warn(msg);

                String uri = request.getResolvedUri().toString();
                uri = uri.replaceAll(workspace, getEntriesWorkspaceName());

                throw new MovedPermanentlyException(msg, uri);
            }

            protected EntryTarget getEntryTarget(RequestContext request) {
                return getURIHandler().getEntryTarget(request, false)
                        .cloneWithNewWorkspace(getEntriesWorkspaceName());
            }

            protected boolean mustAlreadyExist() {
                return true;
            }

            protected boolean setDeletedFlag() {
                return false;
            }

            protected void addCategoriesToEntry(Entry entry, EntryMetaData entryMetaData, Abdera abdera) {
                // do nothing
            }

            protected Entry newEntry(Abdera abdera, EntryMetaData entryMetaData, EntryType entryType)
                    throws AtomServerException {

                log.debug("&&&&&&&&&&&&&&&&&&&&&&&&&& " + parentAtomWorkspace.getName());
                entryMetaData.setWorkspace(parentAtomWorkspace.getName());

                return super.newEntry(abdera, entryMetaData, entryType);
            }

        };
    }
}
