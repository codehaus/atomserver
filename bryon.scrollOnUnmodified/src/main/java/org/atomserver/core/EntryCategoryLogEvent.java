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

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.Date;

/**
 * Describes the fundamental things that identify an entry Category Log Event in an AtomServer - the
 * (workspace, collection, entryId, scheme, term) tuple.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryCategoryLogEvent extends EntryCategory {
    private Date createDate = null;

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append( super.toString() );
        buff.append(" - ").append(createDate);
        return buff.toString();
    }

    public int hashCode() {
        return (entryStoreId == null) ?
               new HashCodeBuilder(17, 8675309)
                       .append(workspace).append(collection).append(entryId)
                       .append(language).append(country)
                       .append(scheme).append(term).append(label)
                       .append(createDate)
                       .toHashCode() :
                                     new HashCodeBuilder(17, 8675309)
                                             .append(entryStoreId)
                                             .append(scheme).append(term).append(label)
                                             .append(createDate)
                                             .toHashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        EntryCategoryLogEvent other = (EntryCategoryLogEvent) obj;
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
                       .append(createDate, other.createDate)
                       .isEquals() :
                                   new EqualsBuilder()
                                           .append(entryStoreId, other.entryStoreId)
                                           .append(scheme, other.scheme)
                                           .append(term, other.term)
                                           .append(label, other.label)
                                           .append(createDate, other.createDate)
                                           .isEquals();
    }
}
