/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.ServiceDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.exceptions.AtomServerException;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Read/Write DAO for Entries.
 * This DAO must extend the ReadEntriesDAO because the POST,PUT,DELETE sequence
 * will need to perform read queries as well as write queries.
 * <b>And ALL queries in a given transaction MUST take place within the same DataSource.</b>
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface WriteReadEntriesDAO
        extends ReadEntriesDAO {

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
