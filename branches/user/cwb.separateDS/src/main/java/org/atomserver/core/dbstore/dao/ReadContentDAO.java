/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.atomserver.core.dbstore.dao;

import org.atomserver.core.EntryMetaData;

/**
 *
 */
public interface ReadContentDAO {
    String selectContent(EntryMetaData entry);

    boolean contentExists(EntryMetaData entry);
}
