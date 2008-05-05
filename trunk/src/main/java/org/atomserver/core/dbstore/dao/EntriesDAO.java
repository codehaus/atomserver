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
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.utils.logic.BooleanExpression;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * The DAO for accessing Entries
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface EntriesDAO
    extends AtomServerDAO {

    //======================================
    //          CRUD OPERATIONS
    //======================================
    public Object insertEntry( EntryDescriptor entry);
    public Object insertEntry( EntryDescriptor entry, boolean isSeedingDB );
    public Object insertEntry( EntryDescriptor entry, boolean isSeedingDB, Date published, Date lastModified );

    public EntryMetaData selectEntry(EntryDescriptor entry);
    public List<EntryMetaData> selectEntries(EntryDescriptor entry);

    public int updateEntry( EntryDescriptor entry, boolean deleted );

    /**
     * Note: rows are NOT really deleted, instead the row is marked "deleted=true"
     */
    public int deleteEntry( EntryDescriptor entryQuery );
    public int deleteEntry( EntryDescriptor entryQuery, boolean setDeletedFlag );

    //======================================
    //          BATCH OPERATIONS
    //======================================
    public List<EntryMetaData> selectEntryBatch( Collection<? extends EntryDescriptor> entries );
    public int insertEntryBatch( String workspace, Collection<? extends EntryDescriptor> entries );
    public int updateEntryBatch( String workspace, Collection<? extends EntryDescriptor> entries );
    public int deleteEntryBatch( String workspace, Collection<? extends EntryDescriptor> entries );

    //======================================
    //          LIST OPERATIONS
    //======================================
    public List selectEntriesByPagePerCategory( FeedDescriptor feed,
                                                Date lastModifiedDate,
                                                int pageDelim,
                                                int pageSize,
                                                Collection<BooleanExpression<AtomCategory>> categoryQuery ) ;

    public List selectEntriesByPageAndLocalePerCategory( FeedDescriptor feed,
                                                         Date lastModifiedDate,
                                                         int pageDelim,
                                                         int pageSize,
                                                         String locale,
                                                         Collection<BooleanExpression<AtomCategory>> categoryQuery );

    public List selectEntriesByPageAndLocale(FeedDescriptor feed,
                                             Date lastModifiedDate,
                                             int pageDelim,
                                             int pageSize,
                                             String locale);

    public List selectEntriesByPage(FeedDescriptor feed,
                                    Date lastModifiedDate,
                                    int pageDelim,
                                    int pageSize);

    public List selectEntriesByLastModifiedSeqNum(FeedDescriptor feed,
                                                  Date lastModifiedDate);

    public List updateLastModifiedSeqNumForAllEntries(ServiceDescriptor service);

    public void deleteAllEntries(ServiceDescriptor service);
    public void deleteAllEntries(FeedDescriptor feed);
    public void deleteAllRowsFromEntries();

    //======================================
    //          COUNT QUERIES
    //======================================
    public int getTotalCount(ServiceDescriptor service);
    public int getTotalCount(FeedDescriptor feed);

    public int getCountByLastModified(ServiceDescriptor service, Date lastModifiedDate);
    public int getCountByLastModified(FeedDescriptor feed, Date lastModifiedDate);

    void obliterateEntry(EntryDescriptor entryQuery);

    void ensureWorkspaceExists(String name);
    void ensureCollectionExists(String workspace, String collection);

    List<String> listWorkspaces();
    List<String> listCollections(String workspace);

    Object selectEntryInternalId(EntryDescriptor entryDescriptor);
    EntryMetaData selectEntryByInternalId(Object internalId);

    void acquireLock();

    List selectAggregateEntriesByPage(FeedDescriptor feed, Date lastModifiedDate, int pageDelim, int pageSize, Collection<BooleanExpression<AtomCategory>> categoriesQuery);
    AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor);

    List selectEntriesByLastModified(String workspace, String collection, Date lastModifiedDate);
}
