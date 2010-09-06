/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.AtomServerDAO;
import org.atomserver.utils.logic.BooleanExpression;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Read Only DAO for Entries
 */
public interface ReadEntriesDAO extends AtomServerDAO {

    EntryMetaData selectEntry(EntryDescriptor entry);

    Object selectEntryInternalId(EntryDescriptor entryDescriptor);

    EntryMetaData selectEntryByInternalId(Object internalId);

    //======================================
    //          BATCH OPERATIONS
    //======================================

    List<EntryMetaData> selectEntryBatch(Collection<? extends EntryDescriptor> entries);

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

    List<EntryMetaData> selectEntriesByLastModified(String workspace,
                                                    String collection,
                                                    Date updatedMin);

    //======================================
    //               MISC
    //======================================

    int getTotalCount(ServiceDescriptor service);

    int getTotalCount(FeedDescriptor feed);

    int getCountByLastModified(ServiceDescriptor service, Date updatedMin);

    int getCountByLastModified(FeedDescriptor feed, Date updatedMax);

    void ensureWorkspaceExists(String name);

    void ensureCollectionExists(String workspace, String collection);

    List<String> listWorkspaces();

    List<String> listCollections(String workspace);

    long selectMaxIndex(Date updatedMax);
}
