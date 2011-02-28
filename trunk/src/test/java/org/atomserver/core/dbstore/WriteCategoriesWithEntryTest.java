package org.atomserver.core.dbstore;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;

public class WriteCategoriesWithEntryTest extends CRUDDBSTestCase {
    Abdera abdera;

    public void setUp() throws Exception {
        super.setUp();
        categoriesDAO.deleteAllRowsFromEntryCategories();
        entriesDAO.deleteAllRowsFromEntries();
        entriesDAO.ensureWorkspaceExists("widgets");
        entriesDAO.ensureCollectionExists("widgets", "acme");

        abdera = new Abdera();
    }


    public void testWritingCategoriesAlongWithEntry() throws Exception {
        // URL for our test widget
        String id = "widgets/acme/42.en_US.xml";

        // override optimistic concurrency, since we're not testing that
        String fullURL = getServerURL() + id + "/*";

        ClientResponse response;
        Entry entry;

        try {
            // create a new entry, setting two categories on it
            response = put(fullURL, entry(id, getFileXMLInsert(),
                    category("urn:color", "red", null),
                    category("urn:size", "large", null)));
            assertEquals(201, response.getStatus());

            entry = clientGet(id).<Entry>getDocument().getRoot();
            assertEquals(2, entry.getCategories().size());
            for (Category category : entry.getCategories()) {
                assertTrue(
                        ("urn:color".equals(category.getScheme().toString()) && "red".equals(category.getTerm())) ||
                                ("urn:size".equals(category.getScheme().toString()) && "large".equals(category.getTerm()))
                );
            }

            // update that entry, replacing one of the categories with a new one
            response = put(fullURL, entry(id, getFileXMLUpdate(1),
                    category("urn:color", "blue", null),
                    category("urn:size", "large", null)));
            assertEquals(200, response.getStatus());

            entry = clientGet(id).<Entry>getDocument().getRoot();
            assertEquals(2, entry.getCategories().size());
            for (Category category : entry.getCategories()) {
                assertTrue(
                        ("urn:color".equals(category.getScheme().toString()) && "blue".equals(category.getTerm())) ||
                                ("urn:size".equals(category.getScheme().toString()) && "large".equals(category.getTerm()))
                );
            }

            // now add another category
            response = put(fullURL, entry(id, getFileXMLUpdate(2),
                    category("urn:color", "blue", null),
                    category("urn:shape", "square", null),
                    category("urn:size", "large", null)));
            assertEquals(200, response.getStatus());

            entry = clientGet(id).<Entry>getDocument().getRoot();
            assertEquals(3, entry.getCategories().size());
            for (Category category : entry.getCategories()) {
                assertTrue(
                        ("urn:color".equals(category.getScheme().toString()) && "blue".equals(category.getTerm())) ||
                                ("urn:shape".equals(category.getScheme().toString()) && "square".equals(category.getTerm())) ||
                                ("urn:size".equals(category.getScheme().toString()) && "large".equals(category.getTerm()))
                );
            }

            // finally, do an update that doesn't include any categories - this should have NO effect - in case any
            // clients are expecting the behavior of passing no categories to be a no-op, we leave that alone.
            //
            // yes, that means that you can't use this mechanism to delete all the categories from an entry - you have
            // to use the tags:workspace operation for that
            response = put(fullURL, entry(id, getFileXMLUpdate(3)));
            assertEquals(200, response.getStatus());

            entry = clientGet(id).<Entry>getDocument().getRoot();
            assertEquals(3, entry.getCategories().size());
            for (Category category : entry.getCategories()) {
                assertTrue(
                        ("urn:color".equals(category.getScheme().toString()) && "blue".equals(category.getTerm())) ||
                                ("urn:shape".equals(category.getScheme().toString()) && "square".equals(category.getTerm())) ||
                                ("urn:size".equals(category.getScheme().toString()) && "large".equals(category.getTerm()))
                );
            }
        } finally {
            delete(fullURL);
        }
    }

    public void testNoninterferenceWithAutotagging() throws Exception {
        // URL for our test widget (the dummy workspace has autotagging rules set up on it)
        String id = "dummy/acme/42.en_US.xml";

        // override optimistic concurrency, since we're not testing that
        String fullURL = getServerURL() + id + "/*";

        ClientResponse response;
        Entry entry;

        try {
            // create a new entry, setting two categories on it
            response = put(fullURL, entry(id, getFileXMLInsert(),
                    category("urn:foo.colors", "red", null),
                    category("urn:size", "large", null)));
            assertEquals(201, response.getStatus());

            // the categories on the PUT get applied FIRST, then autotagging rules take over from there - so
            // we should expect the foo.colors category to be overwritten, but the size one to stick around.
            entry = clientGet(id).<Entry>getDocument().getRoot();
            assertEquals(7, entry.getCategories().size());
            boolean foundColor = false, foundSize = false;
            for (Category category : entry.getCategories()) {
                if ("urn:foo.colors".equals(category.getScheme().toString()) && !category.getTerm().startsWith("DEFAULT:")) {
                    // we set a category of "red", but the content sets this to "teal"
                    assertEquals("teal", category.getTerm());
                    foundColor = true;
                } else if ("urn:size".equals(category.getScheme().toString())) {
                    assertEquals("large", category.getTerm());
                    foundSize = true;
                }
            }
            assertTrue(foundColor && foundSize);
        } finally {
            delete(fullURL);
        }
    }

    private ClientResponse put(String fullURL, Entry entry) {
        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");
        return client.put(fullURL, entry, options);
    }

    Entry entry(String id, String content, Category... categories) {
        Entry entry = getFactory().newEntry();
        entry.setId(id);
        entry.setContentAsXhtml(content);
        for (Category category : categories) {
            entry.addCategory(category);
        }
        return entry;
    }

    Category category(String scheme, String term, String label) {
        Category category = abdera.getFactory().newCategory();
        category.setScheme(scheme);
        category.setTerm(term);
        category.setLabel(label);
        return category;
    }
}
