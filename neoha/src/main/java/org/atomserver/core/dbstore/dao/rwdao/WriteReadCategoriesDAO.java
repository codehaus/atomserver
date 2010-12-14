/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;

import java.util.List;

/**
 * The Read-write DAO for accessing an Entry's Categories
 * This DAO must extend the ReadCategoriesDAO because the POST,PUT,DELETE sequence
 * will need to perform read queries as well as write queries.
 * <b>And ALL queries in a given transaction MUST take place within the same DataSource.</b>
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface WriteReadCategoriesDAO
        extends ReadCategoriesDAO {

    //======================================
    //          CRUD OPERATIONS
    //======================================

    int insertEntryCategory(EntryCategory entry);

    void deleteEntryCategory(EntryCategory entryQuery);

    int updateEntryCategory(EntryCategory categoryToUpdate, String oldTerm);

    //======================================
    //          BATCH OPERATIONS
    //======================================

    void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList);

    void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList);

    //======================================
    //          LIST QUERIES
    //======================================

    void deleteEntryCategories(EntryDescriptor entryQuery);

    void deleteEntryCategoriesWithoutCacheUpdate(EntryDescriptor entryQuery);

    void deleteEntryCategoriesInScheme(EntryMetaData entryQuery, String scheme);

    //======================================
    //          LIST OPERATIONS
    //======================================

    void deleteAllEntryCategories(String workspace);

    void deleteAllEntryCategories(String workspace, String collection);

    void deleteAllRowsFromEntryCategories();
}
