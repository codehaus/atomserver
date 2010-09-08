/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao.rwdao;

import org.atomserver.core.EntryMetaData;

/**
 * The read-write DAO for accessing Content
 * This DAO must extend the ReadContentDAO because the POST,PUT,DELETE sequence
 * will need to perform read queries as well as write queries.
 * <b>And ALL queries in a given transaction MUST take place within the same DataSource.</b>
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface WriteReadContentDAO
        extends ReadContentDAO {
    void putContent(EntryMetaData entry, String content);

    void deleteContent(EntryMetaData entry);

    void deleteAllContent(String workspace);

    void deleteAllContent(String workspace, String collection);

    void deleteAllRowsFromContent();
}
