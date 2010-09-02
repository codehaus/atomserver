/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;

import java.util.List;

/**
 *
 */
public interface WriteReadEntryCategoriesDAO
        extends ReadEntryCategoriesDAO {

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
