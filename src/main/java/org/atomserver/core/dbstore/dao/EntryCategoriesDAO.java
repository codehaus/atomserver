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

import java.util.List;
import java.util.Set;

import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.EntryDescriptor;

/**
 * The DAO for accessing an Entry's Categories
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 * TODO: ideally, we would like to change all of the methods that refer to workspace or collection as strings into ones that take a descriptor of some sort.
 */
public interface EntryCategoriesDAO
    extends AtomServerDAO {

    //======================================
    //          CRUD OPERATIONS
    //======================================
    public int insertEntryCategory(EntryCategory entry);

    public EntryCategory selectEntryCategory(EntryCategory entryQuery);

    public void deleteEntryCategory(EntryCategory entryQuery);

    //======================================
    //          BATCH OPERATIONS
    //======================================
    public void insertEntryCategoryBatch( List<EntryCategory> entryCategoryList );
    public void deleteEntryCategoryBatch( List<EntryCategory> entryCategoryList );

    //======================================
    //          LIST QUERIES
    //======================================
    public List selectEntriesCategories( String workspace, String collection, Set<String> entryIds);
    public List selectEntryCategories( EntryDescriptor entryQuery );
    public List selectEntryCategoriesInScheme( EntryDescriptor entryQuery, String scheme );

    public void deleteEntryCategories(EntryDescriptor entryQuery);
    public void deleteEntryCategoriesInScheme(EntryMetaData entryQuery, String scheme);

    public List selectDistictCollections( String workspace );

    public List selectDistictCategoriesPerCollection( String workspace, String collection );

    //======================================
    //          LIST OPERATIONS
    //======================================
    public void deleteAllEntryCategories(String workspace);
    public void deleteAllEntryCategories(String workspace, String collection);
    public void deleteAllRowsFromEntryCategories();

    //======================================
    //          COUNT QUERIES
    //======================================
    public int getTotalCount(String workspace);
    public int getTotalCount(String workspace, String collection);
}
