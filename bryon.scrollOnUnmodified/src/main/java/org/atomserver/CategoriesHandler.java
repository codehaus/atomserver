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

package org.atomserver;

import org.apache.abdera.model.Category;
import org.atomserver.core.EntryCategory;

import java.util.List;
import java.util.Set;

/**
 * CategoriesHandler - The API for handling AtomPub Categories.
 * Implementations of this interface are wired to an AtomCollection.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface CategoriesHandler extends VirtualWorkspaceHandler {

    /**
     * Returns the list of Abdera Categories associated with this workspace and collection
     * @param workspace The name of this workspace.
     * @param collection The name of this collection
     * @return The list of Abdera Categories
     */
    List<Category> listCategories( String workspace, String collection );

    /**
     * Delete all EntryCategories associated with this EntryDescriptor
     * @param entryQuery  The EntryDescriptor to operate on
     */
    void deleteEntryCategories(EntryDescriptor entryQuery);

    /**
     * Select all EntryCategories associated with this EntryDescriptor
     * @param entryQuery The EntryDescriptor to operate on
     * @return  The List of EntryCategories associated with this EntryDescriptor
     */
    List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery);

    /**
     * Select the EntryCategories associated with this set of entryIds
     * @param workspace    The workspace to operate on
     * @param collection   The collection to operate on
     * @param entryIds     The Set of Entry Ids to operate on
     * @return  The List of EntryCategories associated with these parameters
     */
    List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds);

    /**
     * Insert this List of EntryCategories as a Batch.
     * @param entryCategoryList the List of EntryCategories to operate on.
     */
    void insertEntryCategoryBatch(List<EntryCategory> entryCategoryList);

    /**
     * Delete this List of EntryCategories as a Batch.
     * @param entryCategoryList the List of EntryCategories to operate on.
     */
    void deleteEntryCategoryBatch(List<EntryCategory> entryCategoryList);
}
