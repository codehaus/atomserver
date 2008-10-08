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

package org.atomserver.core.dbstore.dao;

import org.atomserver.core.EntryMetaData;

/**
 * The DAO for accessing Content
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface ContentDAO {
    void putContent(EntryMetaData entry, String content);

    String selectContent(EntryMetaData entry);

    void deleteContent(EntryMetaData entry);

    boolean contentExists(EntryMetaData entry);

    //void deleteAllContent();

    void deleteAllContent(String workspace);

    void deleteAllContent(String workspace, String collection);

    void deleteAllRowsFromContent();
}
