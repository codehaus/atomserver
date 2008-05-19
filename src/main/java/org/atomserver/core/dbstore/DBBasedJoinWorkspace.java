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
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import static org.apache.abdera.model.Content.Type.XML;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.*;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.WorkspaceOptions;
import org.atomserver.core.etc.AtomServerConstants;
import static org.atomserver.core.etc.AtomServerConstants.WORKSPACE;
import static org.atomserver.core.etc.AtomServerConstants.COLLECTION;
import static org.atomserver.core.etc.AtomServerConstants.LOCALE;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.FeedTarget;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.io.StringWriter;
import java.io.IOException;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBBasedJoinWorkspace extends DBBasedAtomWorkspace {

    private static final Log log = LogFactory.getLog(DBBasedJoinWorkspace.class);

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
                                                                     feedTarget.getLocaleParam(),
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
            protected void addFullEntryContent(Abdera abdera,
                                               EntryDescriptor entryMetaData,
                                               Entry entry) {
                // this method is called at multiple levels while the aggregate is being built - we
                // need to handle the call differently at the TOP level, where we have Aggregate
                // entries, and at the lower levels where we do not.
                if (entryMetaData instanceof AggregateEntryMetaData) {
                    addAggregateEntryContent(abdera,
                                             (AggregateEntryMetaData) entryMetaData,
                                             entry);
                } else {
                    addMemberEntryContent(entryMetaData,
                                          entry);
                }
            }

            private void addAggregateEntryContent(Abdera abdera,
                                                  AggregateEntryMetaData agg,
                                                  Entry entry) {
                StringBuilder builder = new StringBuilder("<aggregate xmlns='" +
                                                           AtomServerConstants.SCHEMAS_NAMESPACE +
                                                           "'>");
                try {
                    StringWriter entries = new StringWriter();
                    for (EntryMetaData emd : agg.getMembers()) {
                        newEntry(abdera, emd, EntryType.full).writeTo(entries);
                    }
                    builder.append(entries.toString());
                } catch (IOException e) {
                    throw new AtomServerException(e);
                }

                builder.append("</aggregate>");

                entry.setContent(builder.toString(), XML);
            }

            private void addMemberEntryContent(EntryDescriptor entryMetaData,
                                               Entry entry) {
                ContentStorage contentStorage =
                        getParentAtomService().getAtomWorkspace(entryMetaData.getWorkspace())
                                .getAtomCollection(entryMetaData.getCollection()).getContentStorage();

                String xml = contentStorage.getContent( entryMetaData );
                if (xml == null) {
                    throw new AtomServerException("Could not read entry (" + entryMetaData + ")");
                }
                xml = xml.replaceFirst("<[?].*[?]>", "" );

                entry.setContent( xml, XML );
            }

            protected void addEditLink(int revision, Factory factory, Entry entry, String fileURI) {
                if (revision != EntryTarget.UNDEFINED_REVISION) {
                    super.addEditLink(revision, factory, entry, fileURI);
                }
            }

            //~~~~~~~~~~~~~~~~~~~~~~
            protected Entry newEntryWithCommonContentOnly(Abdera abdera,
                                                          EntryDescriptor entryMetaData)
                    throws AtomServerException {
                Entry entry = super.newEntryWithCommonContentOnly(abdera, entryMetaData);

                if (!(entryMetaData instanceof AggregateEntryMetaData)) {
                    entry.addSimpleExtension(WORKSPACE, entryMetaData.getWorkspace());
                    entry.addSimpleExtension(COLLECTION, entryMetaData.getCollection());
                    if (entryMetaData.getLocale() != null) {
                        entry.addSimpleExtension(LOCALE, entryMetaData.getLocale().toString());
                    }
                }

                return entry;
            }
        };
    }

}
