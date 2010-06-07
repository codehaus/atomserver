package org.atomserver.filter;

import org.apache.abdera.model.Entry;

public interface EntryFilterChain {
    void doChain(Entry entry);
}
