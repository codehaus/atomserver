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


public class DBBasedVirtualAtomWorkspace extends DBBasedAtomWorkspace {

    private static final Log log = LogFactory.getLog(DBBasedJoinWorkspace.class);

    public DBBasedVirtualAtomWorkspace(AtomService parentAtomService, String name) {
        super(parentAtomService, name);
    }

    public AtomCollection newAtomCollection(AtomWorkspace parentWorkspace, String collectionName) {
        return new DBBasedAtomCollection(this, collectionName) {
            /**
             * A convenience method to determine the acual is a Workspace affliated with a Cateories Workspace,
             * which is a special internal Workspace created to handle Category manipulation.
             * @return The affliated Workspace name
             */
            /*
            protected String getCategoriesAffilliatedWorkspaceName() {
                return ((parentAtomWorkspace.getOptions().getAffiliatedAtomWorkspace() != null )
                        ? parentAtomWorkspace.getOptions().getAffiliatedAtomWorkspace().getName()
                        : null );
            }
            */

            protected String getAffilliatedWorkspaceName() {
                String workspaceName = parentAtomWorkspace.getName();
                log.debug( "+++++++++++++++++++++++++++++++++ " + workspaceName );
                String[] parts = workspaceName.split( ":" );
                if ( parts.length != 2 ) {
                    return null;
                }
                return parts[1];
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
                //uri = uri.replaceAll(workspace, getCategoriesAffilliatedWorkspaceName());
                uri = uri.replaceAll(workspace, getAffilliatedWorkspaceName());

                throw new MovedPermanentlyException(msg, uri);
            }

            protected EntryTarget getEntryTarget(RequestContext request) {
                EntryTarget entryTarget = getURIHandler().getEntryTarget(request, true);
                //entryTarget = entryTarget.cloneWithNewWorkspace(getCategoriesAffilliatedWorkspaceName());
                entryTarget = entryTarget.cloneWithNewWorkspace(getAffilliatedWorkspaceName());
                return entryTarget;
            }

            protected boolean mustAlreadyExist() {
                return true;
            }

            protected boolean setDeletedFlag() {
                return false;
            }

            protected void addCategoriesToEntry(Entry entry, EntryMetaData entryMetaData, Abdera abdera) {                
            }

            //~~~~~~~~~~~~~~~~~~~~~~
            protected Entry newEntry(Abdera abdera, EntryMetaData entryMetaData, EntryType entryType)
                throws AtomServerException {

                log.debug("&&&&&&&&&&&&&&&&&&&&&&&&&& " + parentAtomWorkspace.getName() );
                entryMetaData.setWorkspace( parentAtomWorkspace.getName() );

                return super.newEntry(abdera, entryMetaData, entryType);
            }

        };
    }
}
