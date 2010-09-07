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

import org.atomserver.EntryDescriptor;

import java.util.Locale;

/**
 * Describes the fundamental things that identify an Entry in an AtomServer - the
 * (workspace, collection, entryId, locale) tuple.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BaseEntryDescriptor implements EntryDescriptor {
    private String workspace = null;
    private String collection = null;
    private String entryId = null;
    private Locale locale = null;
    private String contentHashCode = null;

    private int revision = UNDEFINED_REVISION;

    public BaseEntryDescriptor() {}

    public BaseEntryDescriptor(String workspace, String collection, String entryId, Locale locale) {
         this(workspace, collection, entryId, locale, UNDEFINED_REVISION );
    }

    public BaseEntryDescriptor(String workspace, String collection, String entryId, Locale locale, int revision) {
        this.workspace = workspace;
        this.collection = collection;
        setEntryId(entryId);
        this.locale = locale;
        this.revision = revision;
    }

    public Object clone() {
        BaseEntryDescriptor clone = new BaseEntryDescriptor();
        clone.setWorkspace(getWorkspace());
        clone.setCollection(getCollection());
        clone.setEntryId(getEntryId());
        clone.setLocale(getLocale());
        clone.setRevision(getRevision());
        return clone;
    }

    public String getWorkspace() { return workspace; }

    public void setWorkspace(String workspace) { this.workspace = workspace; }

    public String getCollection() { return collection; }

    public void setCollection(String collection) { this.collection = collection; }

    public String getEntryId() { return entryId; }

    public void setEntryId(String entryId) { this.entryId = (entryId != null) ? entryId.trim() : entryId; }

    public Locale getLocale() { return locale; }

    public void setLocale(Locale locale) { this.locale = locale; }

    public int getRevision() { return revision; }

    public void setRevision(int revision) { this.revision = revision; }

    public String getContentHashCode() { return contentHashCode; }

    public void setContentHashCode(String contentHashCode) { this.contentHashCode = contentHashCode; }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append("[ " + workspace);
        buff.append(", " + collection);
        buff.append(", " + entryId);
        buff.append(", " + locale);
        buff.append(", " + revision);
        buff.append(", " + contentHashCode +"]");
        return buff.toString();
    }
}
