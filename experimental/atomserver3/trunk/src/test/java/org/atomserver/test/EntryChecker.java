package org.atomserver.test;

import org.apache.abdera.model.Entry;

public interface EntryChecker {
    void check(Entry entry) throws Exception;
}
