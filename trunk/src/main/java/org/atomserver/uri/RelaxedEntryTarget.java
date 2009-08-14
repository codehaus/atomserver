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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.atomserver.EntryDescriptor;

/**
 * RelaxedEntryTarget - An EntryTarget that simply relaxes the equals() method, such that it considers all
 * revisions of an EntryTarget to be the same. We need this for Batching, to determine if
 * we've seen an Entry more than once, which is illegal within a Batch.
 */
public class RelaxedEntryTarget extends EntryTarget {

    public RelaxedEntryTarget( EntryTarget target ) {
        super( target.getRequestContext(),
               target.getWorkspace(),
               target.getCollection(),
               target.getEntryId(),
               target.getRevision(),
               target.getLocale() );
    }

    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(getClass()) ) {
            return false;
        }
        EntryDescriptor other = (EntryDescriptor) o;
        return new EqualsBuilder()
                .append(getWorkspace(), other.getWorkspace())
                .append(getCollection(), other.getCollection())
                .append(getEntryId(), other.getEntryId())
                .append(getLocale(), other.getLocale()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder( 7675707, 17771 )
                .append(getWorkspace())
                .append(getCollection())
                .append(getEntryId())
                .append(getLocale())
                .toHashCode();
    }

}
