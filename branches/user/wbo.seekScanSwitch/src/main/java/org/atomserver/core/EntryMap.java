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

import java.util.*;

/**
 * EntryMap - A Map of EntryDescriptors
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryMap<V> extends HashMap<EntryDescriptor, V> {

    public V put(EntryDescriptor entry, V v) {
        return super.put(new EntryKey(entry), v);
    }

    public V get(Object entry) {
        return super.get(new EntryKey((EntryDescriptor) entry));
    }

    public V remove(Object key) {
        return super.remove(new EntryKey((EntryDescriptor) key));
    }

    public void putAll(Map<? extends EntryDescriptor, ? extends V> t) {
        for (Map.Entry<? extends EntryDescriptor, ? extends V> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    private class EntryKey implements EntryDescriptor {
        EntryDescriptor descriptor;

        EntryKey(EntryDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public String getWorkspace() {
            return descriptor.getWorkspace();
        }

        public String getCollection() {
            return  descriptor.getCollection();
        }

        public String getEntryId() {
            return descriptor.getCollection();
        }

        public Locale getLocale() {
            return descriptor.getLocale();
        }

        public int getRevision() {
            return descriptor.getRevision();
        }

        public String getContentHashCode() {
            return descriptor.getContentHashCode();
        }

        EntryKey reuse(EntryDescriptor descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        // Note: contentHashCode is ignored in the hash
        public int hashCode() {
            return
                    descriptor.getWorkspace().hashCode() +
                    17 * descriptor.getCollection().hashCode() +
                    16661 * descriptor.getEntryId().hashCode() +
                    8675309 * (descriptor.getLocale() == null ? 0 : descriptor.getLocale().hashCode());
        }

        public boolean equals(Object obj) {
            if (obj == null || !EntryKey.class.equals(obj.getClass())) {
                return false;
            }

            EntryKey o = (EntryKey) obj;
            return
                    descriptor.getWorkspace().equals(o.descriptor.getWorkspace()) &&
                    descriptor.getCollection().equals(o.descriptor.getCollection()) &&
                    descriptor.getEntryId().equals(o.descriptor.getEntryId()) &&
                    (descriptor.getLocale() == null ?
                     o.descriptor.getLocale() == null :
                     descriptor.getLocale().equals(o.descriptor.getLocale()));
        }
    }
}
