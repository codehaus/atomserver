package org.atomserver.filter;

import org.apache.abdera.model.Entry;

public interface EntryFilter {
    void filter(Entry entry, EntryFilterChain chain);
}
