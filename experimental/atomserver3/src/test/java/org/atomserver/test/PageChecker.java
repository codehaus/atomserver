package org.atomserver.test;

import org.apache.abdera.model.Feed;

public interface PageChecker {
    void check(Feed page) throws Exception;
}
