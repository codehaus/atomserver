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

package org.atomserver.core.dbstore;

import org.atomserver.ContentStorage;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.ContentDAO;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Locale;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
@ManagedResource(description = "Content Storage")
public class DBBasedContentStorage implements ContentStorage {

    private EntriesDAO entriesDAO;
    private ContentDAO contentDAO;

    public void setEntriesDAO(EntriesDAO entriesDAO) {
        this.entriesDAO = entriesDAO;
    }

    public EntriesDAO getEntriesDAO() {
        return entriesDAO;
    }

    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    public ContentDAO getContentDAO() {
        return contentDAO;
    }

    public String getContent(EntryDescriptor descriptor) {
        return contentDAO.selectContent(safeCastToEntryMetaData(descriptor));
    }

    public void deleteContent(String deletedContentXml, EntryDescriptor descriptor) {
        if (deletedContentXml == null) {
            obliterateContent(descriptor);
        } else {
            putContent(deletedContentXml, descriptor);
        }
    }

    public void obliterateContent(EntryDescriptor descriptor) {
        contentDAO.deleteContent(safeCastToEntryMetaData(descriptor));
    }

    public void putContent(String contentXml, EntryDescriptor descriptor) {
        contentDAO.putContent(safeCastToEntryMetaData(descriptor), contentXml);
    }

    public void initializeWorkspace(String workspace) {
        entriesDAO.ensureWorkspaceExists(workspace);
    }

    public void createCollection(String workspace, String collection) {
        entriesDAO.ensureCollectionExists(workspace, collection);
    }

    public void testAvailability() {
        // if we can select the system date from the DB, then our data connection is hot - this
        // will throw an exception if we can't.
        entriesDAO.selectSysDate();
    }

    public boolean canRead() {
        try {
            // try the same test as above for availability -- but if an exception is
            // thrown, translate that into a boolean return.
            testAvailability();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean contentExists(EntryDescriptor descriptor) {
        return contentDAO.contentExists(entriesDAO.selectEntry(descriptor));
    }

    public void revisionChangedWithoutContentChanging(EntryDescriptor descriptor) {
        //Nothing to do in this case
    }

    /**
     * returns the corresponding EntryMetaData for the given EntryDescriptor.
     * <p/>
     * this method returns the object it is passed if it happens to be an EntryMetaData, or it
     * retrieves the corresponding EntryMetaData from the database if not.
     *
     * @param entryDescriptor the EntryDescriptor of the EntryMetaData to retrieve
     * @return the EntryMetaData corresponding to the given EntryDescriptor
     */
    private EntryMetaData safeCastToEntryMetaData(EntryDescriptor entryDescriptor) {
        return entryDescriptor instanceof EntryMetaData ?
               (EntryMetaData) entryDescriptor :
               entriesDAO.selectEntry(entryDescriptor);
    }

    public Object getPhysicalRepresentation(String workspace,
                                            String collection,
                                            String entryId,
                                            Locale locale,
                                            int revision) {
        throw new UnsupportedOperationException("not implemented");        
    }
}
