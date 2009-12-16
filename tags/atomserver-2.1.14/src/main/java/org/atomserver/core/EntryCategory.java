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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;

/**
 * Describes the fundamental things that identify an entry Category in an AtomServer - the
 * (workspace, collection, entryId, scheme, term) tuple.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategory {
    protected Long entryStoreId = null;
    protected String workspace = null;
    protected String collection = null;
    protected String entryId = null;
    protected String language = "**";
    protected String country = "**";

    protected String scheme = null;
    protected String term = null;
    protected String label = null;

    public EntryCategory() {}

    public Long getEntryStoreId() { return entryStoreId; }

    public void setEntryStoreId(Long entryStoreId) { this.entryStoreId = entryStoreId; }

    public String getWorkspace() { return workspace; }

    public void setWorkspace(String workspace) { this.workspace = workspace; }

    public String getCollection() { return collection; }

    public void setCollection(String collection) { this.collection = collection; }

    public void setLocale(Locale locale) {
        setLanguage(locale == null ? null : locale.getLanguage());
        setCountry(locale == null ? null : locale.getCountry());
    }

    public String getLanguage() { return language; }

    public void setLanguage(String language) { this.language = StringUtils.isEmpty(language) ? "**" : language; }

    public String getCountry() { return country; }

    public void setCountry(String country) { this.country = StringUtils.isEmpty(country) ? "**" : country; }

    public String getEntryId() { return entryId; }

    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getScheme() { return scheme; }

    public void setScheme(String scheme) { this.scheme = scheme; }

    public String getTerm() { return term; }

    public void setTerm(String term) { this.term = term; }

    public String getLabel() { return label; }

    public void setLabel(String label) { this.label = label; }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("[ ").append(entryStoreId)
            .append(", ").append(workspace)
            .append(", ").append(collection)
            .append(", ").append(entryId)
            .append(", ").append(language)
            .append(", ").append(country)
            .append(", ").append(scheme)
            .append(", ").append(term)
            .append(", ").append(label).append("]");
        return buff.toString();
    }

    public int hashCode() {
        return (entryStoreId == null) ?
               new HashCodeBuilder(17, 8675309)
                       .append(workspace).append(collection).append(entryId)
                       .append(language).append(country)
                       .append(scheme).append(term).append(label)
                       .toHashCode() :
                                     new HashCodeBuilder(17, 8675309)
                                             .append(entryStoreId)
                                             .append(scheme).append(term).append(label)
                                             .toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        EntryCategory other = (EntryCategory) obj;
        return (entryStoreId == null || other.entryStoreId == null) ?
               new EqualsBuilder()
                       .append(workspace, other.workspace)
                       .append(collection, other.collection)
                       .append(entryId, other.entryId)
                       .append(language, other.language)
                       .append(country, other.country)
                       .append(scheme, other.scheme)
                       .append(term, other.term)
                       .append(label, other.label)
                       .isEquals() :
                                   new EqualsBuilder()
                                           .append(entryStoreId, other.entryStoreId)
                                           .append(scheme, other.scheme)
                                           .append(term, other.term)
                                           .append(label, other.label)
                                           .isEquals();
    }
}
