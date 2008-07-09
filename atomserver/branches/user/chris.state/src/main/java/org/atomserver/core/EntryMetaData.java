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

package org.atomserver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.utils.locale.LocaleUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Extends the EntryDescriptor with all the metadata for an Entry's state.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryMetaData extends BaseEntryDescriptor {
    private static final Log log = LogFactory.getLog(EntryMetaData.class);

    private Long entryStoreId = null;

    private Date lastModifiedDate = null;
    private Date publishedDate = null;

    private long lastModifiedSeqNum = 0L;

    private boolean deleted = false;

    private boolean isNewlyCreated = true;

    private String language = "**";
    private String country = "**";
    private List<EntryCategory> categories = null;

    public EntryMetaData() {}

    /**
     * Constructs a new EntryMetaData.
     * @param workspace      the workspace for the entry
     * @param collection   the collection for the entry.
     * @param locale       the locale for the entry.
     * @param entryId      the id for the entry.
     * @param lastModified the last modified time for the entry.
     */
    public EntryMetaData(String workspace,
                         String collection,
                         Locale locale,
                         String entryId,
                         long lastModified) {
        this(workspace, collection, locale, entryId, lastModified, true);
    }

    /**
     * Constructs a new EntryMetaData.
     * @param workspace      the workspace for the entry
     * @param collection     the collection for the entry.
     * @param locale         the locale for the entry.
     * @param entryId        the id for the entry.
     * @param lastModified   the last modified time for the entry (as a long).
     * @param isNewlyCreated true iff the entry is new.
     */
    public EntryMetaData(String workspace,
                         String collection,
                         Locale locale,
                         String entryId,
                         long lastModified,
                         boolean isNewlyCreated) {
        this( workspace, collection, locale, entryId, (new Date(lastModified)), isNewlyCreated);
    }

    /**
     * Constructs a new EntryMetaData.
     * @param workspace      the workspace for the entry
     * @param collection     the collection for the entry.
     * @param locale         the locale for the entry.
     * @param entryId        the id for the entry.
     * @param lastModified   the last modified time for the entry (as a Date).
     * @param isNewlyCreated true iff the entry is new.
     */
    public EntryMetaData(String workspace,
                         String collection,
                         Locale locale,
                         String entryId,
                         Date lastModified,
                         boolean isNewlyCreated) {
        setWorkspace(workspace);
        setCollection(collection);
        setLocale(locale);
        setEntryId(entryId);

        this.lastModifiedDate = lastModified;
        this.isNewlyCreated = isNewlyCreated;

        decodeLocale(locale);
    }


    public List<EntryCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<EntryCategory> categories) {
        this.categories = categories;
    }

    public Long getEntryStoreId() {
        return entryStoreId;
    }

    public void setEntryStoreId(Long entryStoreId) {
        this.entryStoreId = entryStoreId;
    }

    /**
     * {@inheritDoc}
     */
    public Locale getLocale() {
        if (super.getLocale() == null) {
            createLocale();
        }
        return super.getLocale();
    }

    /**
     * {@inheritDoc}
     */
    public void setLocale(Locale locale) {
        super.setLocale(locale);
        decodeLocale(locale);
    }

    private void createLocale() {
        if (language == null || "**".equals(language)) {
            setLocale(null);
        } else {
            String localeStr = language;
            if (country != null && !("**".equals(country))) {
                localeStr += "_" + country;
            }
            setLocale(LocaleUtils.toLocale(localeStr));
        }
    }

    private void decodeLocale(Locale localeObj) {
        if (localeObj == null) {
            language = country = "**";
        } else {
            language = localeObj.getLanguage();
            if (language.equals("")) {
                String msg = "Unknown Locale:: " + localeObj;
                log.error(msg);
                throw new AtomServerException(msg);
            }
            country = (localeObj.getCountry().equals("")) ? "**" : localeObj.getCountry();
        }
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Date getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(Date lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    public Date getPublishedDate() { return publishedDate; }
    public void setPublishedDate(Date publishedDate) { this.publishedDate = publishedDate; }

    public long getLastModifiedSeqNum() { return lastModifiedSeqNum; }
    public void setLastModifiedSeqNum(long lastModifiedSeqNum) { this.lastModifiedSeqNum = lastModifiedSeqNum; }

    public boolean getDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public boolean isNewlyCreated() { return isNewlyCreated; }
    public void setNewlyCreated(boolean isNewlyCreated) { this.isNewlyCreated = isNewlyCreated; }

    public Object clone() {
        EntryMetaData clone = new EntryMetaData();
        clone.setEntryStoreId( getEntryStoreId() );
        clone.setWorkspace( getWorkspace() );
        clone.setCollection( getCollection() );
        clone.setEntryId( getEntryId() );
        clone.setLocale( getLocale() );
        clone.setRevision( getRevision() );

        clone.setLastModifiedDate( getLastModifiedDate() );
        clone.setPublishedDate( getPublishedDate() );
        clone.setNewlyCreated( isNewlyCreated() );
        clone.setDeleted( getDeleted() );
        clone.setCategories( getCategories() );

        clone.setLastModifiedSeqNum( getLastModifiedSeqNum() );

        return clone;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return new StringBuffer()
            .append("Entry:: [")
            .append(getWorkspace())
            .append(" ").append(getCollection())
            .append(" ").append(getEntryId())
            .append(" ").append(getLocale())
            .append(" ").append(language)
            .append(" ").append(country)
            .append(" ").append(getRevision())
            .append(" ").append(deleted)
            .append(" <").append(lastModifiedDate).append(">")
            .append(" <").append(publishedDate).append(">")
            .append(" ").append(isNewlyCreated)
            .append(" ").append(lastModifiedSeqNum)
            .append("]").toString();
    }
}
