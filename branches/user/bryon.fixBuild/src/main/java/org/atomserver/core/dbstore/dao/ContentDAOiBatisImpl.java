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

import java.util.Map;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ContentDAOiBatisImpl
        extends AbstractDAOiBatisImpl
        implements ContentDAO {
    public void putContent(EntryMetaData entry, String content) {
        Map paramMap = paramMap()
                .param("entryStoreId", entry.getEntryStoreId())
                .param("content", content);
        if (contentExists(entry)) {
            getSqlMapClientTemplate().update("updateContent", paramMap);
        } else {
            getSqlMapClientTemplate().insert("insertContent", paramMap);
        }
    }

    public String selectContent(EntryMetaData entry) {
        return (String)
                getSqlMapClientTemplate().queryForObject("selectContent",
                                                         paramMap().param("entryStoreId",
                                                                          entry.getEntryStoreId()));
    }

    public void deleteContent(EntryMetaData entry) {
        getSqlMapClientTemplate().delete("deleteContent",
                                         paramMap().param("entryStoreId", entry.getEntryStoreId()));
    }

    public boolean contentExists(EntryMetaData entry) {
        Integer count = (Integer)
                getSqlMapClientTemplate().queryForObject("selectContentExists",
                                                         paramMap().param("entryStoreId",
                                                                          entry.getEntryStoreId()));
        return count > 0;
    }

    public void deleteAllContent() {
        getSqlMapClientTemplate().delete("deleteAllContent");
    }
}
