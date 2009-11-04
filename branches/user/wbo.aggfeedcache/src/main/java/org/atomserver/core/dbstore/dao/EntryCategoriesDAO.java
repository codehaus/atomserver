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

import org.atomserver.EntryDescriptor;
import org.atomserver.cache.AggregateFeedCacheManager;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The DAO for accessing an Entry's Categories
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 *         TODO: ideally, we would like to change all of the methods that refer
 *               to workspace or collection as strings into ones that take a descriptor of some sort.
 */
public interface EntryCategoriesDAO
        extends AtomServerDAO {

    //======================================
    //          CRUD OPERATIONS
    //======================================
    int insertEntryCategory(EntryCategory entry);

    EntryCategory selectEntryCategory(EntryCategory entryQuery);

    void deleteEntryCategory(EntryCategory entryQuery);

    //======================================
    //          BATCH OPERATIONS
    //======================================
    void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList);

    void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList);

    //======================================
    //          LIST QUERIES
    //======================================
    List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds);

    List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery);

    List<EntryCategory> selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme);

    void deleteEntryCategories(EntryDescriptor entryQuery);

    void deleteEntryCategoriesInScheme(EntryMetaData entryQuery, String scheme);

    List<String> selectDistictCollections(String workspace);

    List<Map<String, String>> selectDistictCategoriesPerCollection(String workspace, String collection);

    //======================================
    //          LIST OPERATIONS
    //======================================
    void deleteAllEntryCategories(String workspace);

    void deleteAllEntryCategories(String workspace, String collection);

    void deleteAllRowsFromEntryCategories();

    //======================================
    //          COUNT QUERIES
    //======================================
    int getTotalCount(String workspace);

    int getTotalCount(String workspace, String collection);

    //======================================
    //          OTHER
    //======================================
    void setCacheManager(AggregateFeedCacheManager cacheManager);
}
