/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.core.EntryMetaData;

/**
 *
 */
public interface WriteReadContentDAO
        extends ReadContentDAO {
    void putContent(EntryMetaData entry, String content);

    void deleteContent(EntryMetaData entry);

    void deleteAllContent(String workspace);

    void deleteAllContent(String workspace, String collection);

    void deleteAllRowsFromContent();
}
