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
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.atomserver.*;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.FeedTarget;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBBasedJoinWorkspace extends DBBasedAtomWorkspace {

    public DBBasedJoinWorkspace(AtomService parentAtomService) {
        super(parentAtomService, "$join");
        setOptions(new WorkspaceOptions());
    }

    public AtomCollection newAtomCollection(AtomWorkspace parentWorkspace, String collectionName) {
        return new DBBasedAtomCollection(this, collectionName) {

            protected EntryMetaData innerGetEntry(EntryTarget entryTarget) {
                return getEntriesDAO().selectAggregateEntry(entryTarget);
            }

            protected long getEntries(RequestContext request,
                                      FeedTarget feedTarget,
                                      long ifModifiedSinceLong,
                                      Feed feed) throws AtomServerException {

                Abdera abdera = request.getServiceContext().getAbdera();
                IRI iri = request.getUri();

                EntryType entryType = (feedTarget.getEntryTypeParam() != null) ? feedTarget.getEntryTypeParam() : EntryType.link;
                int pageSize = calculatePageSize(feedTarget, entryType);
                List<AggregateEntryMetaData> list =
                        getEntriesDAO().selectAggregateEntriesByPage(feedTarget,
                                                                     new Date(ifModifiedSinceLong),
                                                                     feedTarget.getPageDelimParam(),
                                                                     pageSize + 1,
                                                                     feedTarget.getCategoriesQuery());

                Collections.sort(list, new Comparator<AggregateEntryMetaData>() {
                    public int compare(AggregateEntryMetaData a, AggregateEntryMetaData b) {
                        return a.getLastModifiedSeqNum() < b.getLastModifiedSeqNum() ? -1 :
                               a.getLastModifiedSeqNum() > b.getLastModifiedSeqNum() ? 1 :
                               0;
                    }
                });

                if (list.size() == 0) {
                    return 0L;
                }

                return createFeedElements(feed, abdera, iri, feedTarget, entryType,
                                          list, feedTarget.getWorkspace(), feedTarget.getCollection(),
                                          feedTarget.getLocaleParam(),
                                          list.size(), pageSize + 1, pageSize,
                                          feedTarget.getPageDelimParam(), 0 /*total entries*/);
            }

            //~~~~~~~~~~~~~~~~~~~~~~
            protected void addFullEntryContent(Abdera abdera, EntryDescriptor entryMetaData, Entry entry) {
                StringBuilder builder = new StringBuilder("<aggregate xmlns='" +
                                                           AtomServerConstants.SCHEMAS_NAMESPACE + "'>");

                AggregateEntryMetaData agg = (AggregateEntryMetaData) entryMetaData;
                for (EntryMetaData emd : agg.getMembers()) {
                    ContentStorage contentStorage =
                            getParentAtomService().getAtomWorkspace(emd.getWorkspace())
                                    .getAtomCollection(emd.getCollection()).getContentStorage();

                    String xml = contentStorage.getContent(emd);
                    if (xml == null) {
                        throw new AtomServerException("Could not read entry (" + emd + ")");
                    }
                    xml = xml.replaceFirst("<[?].*[?]>", "");


                    builder.append(xml);
                }

                builder.append("</aggregate>");

                entry.setContent(builder.toString());
            }

        };
    }
}
