/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;

import java.util.List;

/**
 * The read-write DAO for accessing an Entry's Category Log Events
 * This DAO must extend the ReadCategoryLogEventsDAO because the POST,PUT,DELETE sequence
 * will need to perform read queries as well as write queries.
 * <b>And ALL queries in a given transaction MUST take place within the same DataSource.</b>
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface WriteReadCategoryLogEventsDAO
        extends ReadCategoryLogEventsDAO {

    int insertEntryCategoryLogEvent(EntryCategory entry);

    void deleteEntryCategoryLogEvent(EntryDescriptor entryQuery);

    void deleteEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery);

    public void insertEntryCategoryLogEventBatch(List<EntryCategory> entryCategoryList);

    public void deleteAllEntryCategoryLogEvents(String workspace);

    public void deleteAllEntryCategoryLogEvents(String workspace, String collection);

    public void deleteAllRowsFromEntryCategoryLogEvent();
}
