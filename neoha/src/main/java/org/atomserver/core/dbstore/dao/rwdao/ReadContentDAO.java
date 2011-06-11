/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.core.EntryMetaData;

/**
 * The read-only DAO for accessing Content
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface ReadContentDAO {
    String selectContent(EntryMetaData entry);

    boolean contentExists(EntryMetaData entry);
}
