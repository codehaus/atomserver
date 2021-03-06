/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.dbstore.dao.AtomServerDAO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Read-only DAO for accessing an Entry's Categories
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface ReadCategoriesDAO
        extends AtomServerDAO {

    EntryCategory selectEntryCategory(EntryCategory entryQuery);

    List<EntryCategory> selectEntriesCategories(String workspace, String collection, Set<String> entryIds);

    List<EntryCategory> selectEntryCategories(EntryDescriptor entryQuery);

    List<EntryCategory> selectEntryCategoriesInScheme(EntryDescriptor entryQuery, String scheme);

    List<String> selectDistictCollections(String workspace);

    List<Map<String, String>> selectDistictCategoriesPerCollection(String workspace, String collection);

    int getTotalCount(String workspace);

    int getTotalCount(String workspace, String collection);
}
