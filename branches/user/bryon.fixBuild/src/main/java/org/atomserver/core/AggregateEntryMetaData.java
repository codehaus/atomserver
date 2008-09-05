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

import java.util.*;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class AggregateEntryMetaData extends EntryMetaData {
    private final Set<EntryMetaData> members = new TreeSet<EntryMetaData>(
            new Comparator<EntryMetaData>() {
                public int compare(EntryMetaData a, EntryMetaData b) {
                    int[] comparisons = new int[] {
                            a.getWorkspace().compareTo(b.getWorkspace()),
                            a.getCollection().compareTo(b.getCollection()),
                            a.getEntryId().compareTo(b.getEntryId())
                    };
                    for (int comparison : comparisons) {
                        if (comparison != 0) return comparison;
                    }
                    return 0;
                }
            }
    );
    private final Set<EntryCategory> categories = new HashSet<EntryCategory>();

    public AggregateEntryMetaData(String workspace, String collection, Locale locale, String entryId) {
        setWorkspace(workspace);
        setCollection(collection);
        setEntryId(entryId);
        setLocale(locale);
    }

    public void add(EntryMetaData... members) {
        for (EntryMetaData entry : members) {
            this.members.add(entry);
            if (getLastModifiedDate() == null ||
                entry.getLastModifiedDate().getTime() > getLastModifiedDate().getTime()) {
                setLastModifiedDate(entry.getLastModifiedDate());
            }
            if (getPublishedDate() == null ||
                entry.getPublishedDate().getTime() < getPublishedDate().getTime()) {
                setPublishedDate(entry.getPublishedDate());
            }
            if (entry.getLastModifiedSeqNum() > getLastModifiedSeqNum()) {
                setLastModifiedSeqNum(entry.getLastModifiedSeqNum());
            }
            for (EntryCategory entryCategory : entry.getCategories()) {
                EntryCategory category = new EntryCategory();
                category.setWorkspace(getWorkspace());
                category.setCollection(getCollection());
                category.setEntryId(getEntryId());
                category.setScheme(entryCategory.getScheme());
                category.setTerm(entryCategory.getTerm());
                category.setLabel(entryCategory.getLabel());
                categories.add(category);
            }
        }
    }

    public List<EntryCategory> getCategories() {
        return new ArrayList<EntryCategory>(this.categories);
    }

    public Set<EntryMetaData> getMembers() {
        return members;
    }

    public static Map<String, AggregateEntryMetaData> aggregate(String workspace,
                                                                String collectionName,
                                                                Locale locale,
                                                                Collection<EntryMetaData> metaData) {
        HashMap<String, AggregateEntryMetaData> map = new LinkedHashMap<String, AggregateEntryMetaData>();
        for (EntryMetaData entry : metaData) {
            for (EntryCategory category : entry.getCategories()) {
                if (category.getScheme().equals(collectionName)) {
                    AggregateEntryMetaData agg = map.get(category.getTerm());
                    if (agg == null) {
                        map.put(category.getTerm(), agg =
                                new AggregateEntryMetaData(workspace, collectionName, locale, category.getTerm()));
                    }
                    agg.add(entry);
                }
            }
        }
        return map;
    }
}
