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

package org.atomserver.core.dbstore.dao;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.cache.AggregateFeedCacheManager;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.logic.BooleanExpression;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The DAO for accessing Entries
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface EntriesDAO
        extends AtomServerDAO {

    //======================================
    //          CRUD OPERATIONS
    //======================================
    Object insertEntry(EntryDescriptor entry);

    Object insertEntry(EntryDescriptor entry, boolean isSeedingDB);

    Object insertEntry(EntryDescriptor entry,
                       boolean isSeedingDB,
                       Date published,
                       Date lastModified);

    EntryMetaData selectEntry(EntryDescriptor entry);

    Object selectEntryInternalId(EntryDescriptor entryDescriptor);

    EntryMetaData selectEntryByInternalId(Object internalId);

    int updateEntry(EntryDescriptor entry, boolean deleted);

    // Note: rows are NOT really deleted, instead the row is marked "deleted=true"
    int deleteEntry(EntryDescriptor entryQuery);

    int deleteEntry(EntryDescriptor entryQuery, boolean setDeletedFlag);

    //======================================
    //          BATCH OPERATIONS
    //======================================
    List<EntryMetaData> selectEntryBatch(Collection<? extends EntryDescriptor> entries);

    int insertEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries);

    int updateEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries);

    int deleteEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries);

    //======================================
    //          LIST OPERATIONS
    //======================================
    List<EntryMetaData> selectEntries(EntryDescriptor entry);

    List<EntryMetaData> selectFeedPage(Date updatedMin,
                                       Date updatedMax,
                                       int startIndex,
                                       int endIndex, int pageSize,
                                       String locale,
                                       FeedDescriptor feed,
                                       Collection<BooleanExpression<AtomCategory>> categoryQuery);

    List<EntryMetaData> selectEntriesByLastModifiedSeqNum(FeedDescriptor feed,
                                                          Date updatedMin);

    List<AggregateEntryMetaData> selectAggregateEntriesByPage(FeedDescriptor feed,
                                                              Date updatedMin,
                                                              Date updatedMax,
                                                              Locale locale,
                                                              int startIndex,
                                                              int endIndex,
                                                              int pageSize,
                                                              Collection<BooleanExpression<AtomCategory>> categoriesQuery,
                                                              List<String> joinWorkspaces);

    AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor,
                                                List<String> joinWorkspaces);

    List<EntryMetaData> selectEntriesByLastModified(String workspace,
                                                    String collection,
                                                    Date updatedMin);

    List<EntryMetaData> updateLastModifiedSeqNumForAllEntries(ServiceDescriptor service);

    void deleteAllEntries(ServiceDescriptor service);

    void deleteAllEntries(FeedDescriptor feed);

    void deleteAllRowsFromEntries();

    //======================================
    //               MISC
    //======================================
    int getTotalCount(ServiceDescriptor service);

    int getTotalCount(FeedDescriptor feed);

    int getCountByLastModified(ServiceDescriptor service, Date updatedMin);

    int getCountByLastModified(FeedDescriptor feed, Date updatedMax);

    void obliterateEntry(EntryDescriptor entryQuery);

    void ensureWorkspaceExists(String name);

    void ensureCollectionExists(String workspace, String collection);

    List<String> listWorkspaces();

    List<String> listCollections(String workspace);

    void acquireLock() throws AtomServerException;

    long selectMaxIndex(Date updatedMax);

    void setCacheManager(AggregateFeedCacheManager manager);

}
