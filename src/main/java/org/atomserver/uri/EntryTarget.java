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

package org.atomserver.uri;

import org.atomserver.EntryDescriptor;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Locale;

/**
 * A URITarget that specifically represents an Entry request
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryTarget extends URITarget implements EntryDescriptor {

    private final RequestContext requestContext;
    private final String workspace;
    private final String collection;
    private final Integer revision;
    private final Locale locale;

    private String entryId;

    // NOTE: purposely this contains invalid HTML chars
    public static final String UNASSIGNED_ID = "{{UNASSIGNED ID}}";

    public EntryTarget(RequestContext requestContext,
                       final String workspace,
                       final String collection,
                       final String entryId,
                       final Integer revision,
                       final Locale locale) {
        super(TargetType.TYPE_ENTRY, requestContext);
        this.requestContext = requestContext;
        this.workspace = workspace;
        this.collection = collection;
        this.entryId = entryId;
        this.revision = revision;
        this.locale = locale;
    }

    public EntryTarget(RequestContext requestContext,
                       final String workspace,
                       final String collection,
                       final Integer revision,
                       final Locale locale) {
        super(TargetType.TYPE_COLLECTION, requestContext);
        this.requestContext = requestContext;
        this.workspace = workspace;
        this.collection = collection;
        this.entryId = UNASSIGNED_ID;
        this.revision = revision;
        this.locale = locale;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getCollection() {
        return collection;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId( String entryId ) {
        this.entryId = entryId;
    }

    public int getRevision() {
        return revision == null ? 0 : revision;
    }

    public Locale getLocale() {
        return locale;
    }

    public EntryTarget cloneWithNewWorkspace(String workspace) {
        return new EntryTarget(this.requestContext,
                               workspace,
                               getCollection(),
                               getEntryId(),
                               getRevision(),
                               getLocale());
    }

    public EntryTarget cloneWithNewRevision(Integer revision) {
        return new EntryTarget(this.requestContext,
                               getWorkspace(),
                               getCollection(),
                               getEntryId(),
                               revision,
                               getLocale());
    }

    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        }
        EntryDescriptor other = (EntryDescriptor) o;
        return new EqualsBuilder()
                .append(workspace, other.getWorkspace())
                .append(collection, other.getCollection())
                .append(entryId, other.getEntryId())
                .append((int) revision, other.getRevision())
                .append(locale, other.getLocale()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(8675309, 16661)
                .append(getWorkspace())
                .append(getCollection())
                .append(getEntryId())
                .append(getRevision())
                .append(getLocale())
                .toHashCode();
    }
}