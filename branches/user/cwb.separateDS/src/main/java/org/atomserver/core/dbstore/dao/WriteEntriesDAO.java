/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.exceptions.AtomServerException;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 *
 */
public interface WriteEntriesDAO
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

    int updateEntry(EntryDescriptor entry, boolean deleted);

    // Note: rows are NOT really deleted, instead the row is marked "deleted=true"

    int deleteEntry(EntryDescriptor entryQuery);

    int deleteEntry(EntryDescriptor entryQuery, boolean setDeletedFlag);

    //======================================
    //          BATCH OPERATIONS
    //======================================

    int insertEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries);

    int updateEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries);

    int deleteEntryBatch(String workspace, Collection<? extends EntryDescriptor> entries);

    //======================================
    //          LIST OPERATIONS
    //======================================

    List<EntryMetaData> updateLastModifiedSeqNumForAllEntries(ServiceDescriptor service);

    void deleteAllEntries(ServiceDescriptor service);

    void deleteAllEntries(FeedDescriptor feed);

    void deleteAllRowsFromEntries();

    //======================================
    //               MISC
    //======================================

    void obliterateEntry(EntryDescriptor entryQuery);

    void acquireLock() throws AtomServerException;
}
