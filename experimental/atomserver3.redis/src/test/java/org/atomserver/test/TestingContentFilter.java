package org.atomserver.test;

import org.atomserver.filter.EntryFilter;
import org.atomserver.filter.EntryFilterChain;
import org.atomserver.app.BadRequestException;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.junit.Ignore;

@Ignore
public class TestingContentFilter implements EntryFilter {
    public TestingContentFilter(ExtensibleElement config) {
    }

    public void filter(Entry entry, EntryFilterChain chain) {
        if (entry.getContent().contains("INVALID")) {
            throw new BadRequestException("Invalid content!");
        }

        chain.doChain(entry);
    }
}
