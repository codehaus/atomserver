/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;

import java.util.List;

/**
 *
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
