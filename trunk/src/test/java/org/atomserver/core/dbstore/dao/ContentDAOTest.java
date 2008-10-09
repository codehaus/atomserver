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

import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryMetaData;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

public class ContentDAOTest extends DAOTestCase {

    public void testContentDAO() throws Exception {
        EntryDescriptor entry = new BaseEntryDescriptor("widgets", "acme", "16661", Locale.US);

        entriesDAO.ensureCollectionExists(entry.getWorkspace(), entry.getCollection());
        entriesDAO.insertEntry(entry);
        EntryMetaData entryMetaData = entriesDAO.selectEntry(entry);

        assertFalse(contentDAO.contentExists(entryMetaData));
        assertEquals(null, contentDAO.selectContent(entryMetaData));

        contentDAO.putContent(entryMetaData, "<content/>");

        assertTrue(contentDAO.contentExists(entryMetaData));
        assertEquals("<content/>", contentDAO.selectContent(entryMetaData));

        contentDAO.putContent(entryMetaData, "<newcontent/>");

        assertTrue(contentDAO.contentExists(entryMetaData));
        assertEquals("<newcontent/>", contentDAO.selectContent(entryMetaData));

        contentDAO.deleteContent(entryMetaData);

        assertFalse(contentDAO.contentExists(entryMetaData));
        assertEquals(null, contentDAO.selectContent(entryMetaData));

        entriesDAO.obliterateEntry(entry);

        try {
            contentDAO.putContent(entryMetaData, "<content/>");
            assertTrue("expected an exception to be thrown!", false);
        } catch (DataIntegrityViolationException e) {
            // do nothing - we expect this!
        }
    }

    public void testDeleteAll1() throws Exception {
        EntryDescriptor entry = new BaseEntryDescriptor("widgets", "acme", "16661", Locale.US);
        EntryMetaData entryMetaData = insertContent( entry );
        contentDAO.deleteAllContent("widgets", "acme");

        assertFalse(contentDAO.contentExists(entryMetaData));
        assertEquals(null, contentDAO.selectContent(entryMetaData));

        entriesDAO.obliterateEntry(entry);
    }

    public void testDeleteAll2() throws Exception {
        EntryDescriptor entry = new BaseEntryDescriptor("widgets", "acme", "16661", Locale.US);
        EntryMetaData entryMetaData = insertContent( entry );
        contentDAO.deleteAllContent("widgets");

        assertFalse(contentDAO.contentExists(entryMetaData));
        assertEquals(null, contentDAO.selectContent(entryMetaData));

        entriesDAO.obliterateEntry(entry);
    }

    public void testDeleteAll3() throws Exception {
        EntryDescriptor entry = new BaseEntryDescriptor("widgets", "acme", "16661", Locale.US);
        EntryMetaData entryMetaData = insertContent( entry );
        contentDAO.deleteAllRowsFromContent();

        assertFalse(contentDAO.contentExists(entryMetaData));
        assertEquals(null, contentDAO.selectContent(entryMetaData));

        entriesDAO.obliterateEntry(entry);
    }

    private EntryMetaData insertContent( EntryDescriptor entry ) {
        entriesDAO.ensureCollectionExists(entry.getWorkspace(), entry.getCollection());
        entriesDAO.insertEntry(entry);
        EntryMetaData entryMetaData = entriesDAO.selectEntry(entry);
        contentDAO.putContent(entryMetaData, "<content/>");
        return entryMetaData;
    }
}
