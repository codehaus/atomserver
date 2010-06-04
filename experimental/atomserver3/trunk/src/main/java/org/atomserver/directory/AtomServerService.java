package org.atomserver.directory;

import org.apache.abdera.model.Service;
import org.atomserver.filter.EntryFilterChain;

public interface AtomServerService {
    Service getService();
    EntryFilterChain getEntryFilterChain(String workspaceId, String collectionId);
}
