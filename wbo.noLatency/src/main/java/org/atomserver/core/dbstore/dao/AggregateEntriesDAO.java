/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.AtomCategory;
import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.core.AggregateEntryMetaData;
import org.atomserver.utils.logic.BooleanExpression;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The DAO API for Aggregate Feeds and Entries
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface AggregateEntriesDAO extends AtomServerDAO {
    List<AggregateEntryMetaData> selectAggregateEntriesByPage(FeedDescriptor feed,
                                                              Date updatedMin,
                                                              Date updatedMax,
                                                              Locale locale,
                                                              int startIndex,
                                                              int endIndex,
                                                              int pageSize,
                                                              boolean noLatency,
                                                              Collection<BooleanExpression<AtomCategory>> categoriesQuery,
                                                              List<String> joinWorkspaces);

    AggregateEntryMetaData selectAggregateEntry(EntryDescriptor entryDescriptor,
                                                List<String> joinWorkspaces);
}
