package org.atomserver;

import java.util.Locale;

public class ContentStorageWrapper implements ContentStorage {
    private ContentStorage storage;

    public void setStorage(ContentStorage storage) {
        this.storage = storage;
    }

    public ContentStorage getStorage() {
        return storage;
    }

    public String getContent(EntryDescriptor descriptor) {
        return storage.getContent(descriptor);
    }

    public void deleteContent(String deletedContentXml, EntryDescriptor descriptor) {
        storage.deleteContent(deletedContentXml, descriptor);
    }

    public void obliterateContent(EntryDescriptor descriptor) {
        storage.obliterateContent(descriptor);
    }

    public void putContent(String contentXml, EntryDescriptor descriptor) {
        storage.putContent(contentXml, descriptor);
    }

    public void initializeWorkspace(String workspace) {
        storage.initializeWorkspace(workspace);
    }

    public void testAvailability() {
        storage.testAvailability();
    }

    public boolean canRead() {
        return storage.canRead();
    }

    public boolean contentExists(EntryDescriptor descriptor) {
        return storage.contentExists(descriptor);
    }

    public void revisionChangedWithoutContentChanging(EntryDescriptor descriptor) {
        storage.revisionChangedWithoutContentChanging(descriptor);
    }

    public Object getPhysicalRepresentation(String workspace,
                                            String collection,
                                            String entryId,
                                            Locale locale,
                                            int revision) {
        return storage.getPhysicalRepresentation(workspace, collection,  entryId, locale, revision);
    }
}
