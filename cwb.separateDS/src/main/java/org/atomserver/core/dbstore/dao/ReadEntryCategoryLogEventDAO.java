/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;

import java.util.List;

/**
 *
 */
public interface ReadEntryCategoryLogEventDAO
        extends AtomServerDAO {

    List<EntryCategoryLogEvent> selectEntryCategoryLogEvent(EntryCategory entryQuery);
    List<EntryCategoryLogEvent> selectEntryCategoryLogEventByScheme(EntryCategory entryQuery);
    List<EntryCategoryLogEvent> selectEntryCategoryLogEventBySchemeAndTerm(EntryCategory entryQuery);

    public int getTotalCount(String workspace);
    public int getTotalCount(String workspace, String collection);
}
