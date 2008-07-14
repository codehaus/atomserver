package org.atomserver.core.dbstore;

import org.atomserver.core.AtomServerTestCase;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Feed;

import java.util.Arrays;
import java.util.Locale;

public class WriteCategoriesWithEntryTest extends DBSTestCase {
    public void setUp() throws Exception {
        super.setUp();
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        entriesDao.deleteAllRowsFromEntries();
    }

    public void testWritingCategoriesWithSingleEntry() throws Exception {
        createWidget("widgets", "acme", "8675309", Locale.US.toString(),
                     createWidgetXMLFileString("8675309"),
                     Arrays.asList(
                             makeCategory("urn:test.scheme.1", "FOO", null),
                             makeCategory("urn:test.scheme.2", "BAR", null)
                     ));

        // check that the two categories we sent are added when INSERTED
        assertEquals(1, getPage("widgets/acme/-/(urn:test.scheme.1)FOO", 200).getEntries().size());
        assertEquals(1, getPage("widgets/acme/-/(urn:test.scheme.2)BAR", 200).getEntries().size());

        modifyEntry("widgets", "acme", "8675309", Locale.US.toString(),
                    createWidgetXMLFileString("8675309"), false, "*");

        // check that the categories are not overwritten when we don't send any
        assertEquals(1, getPage("widgets/acme/-/(urn:test.scheme.1)FOO", 200).getEntries().size());
        assertEquals(1, getPage("widgets/acme/-/(urn:test.scheme.2)BAR", 200).getEntries().size());

        modifyEntry("widgets", "acme", "8675309", Locale.US.toString(),
                    createWidgetXMLFileString("8675309"),
                    Arrays.asList(
                             makeCategory("urn:test.scheme.1", "FOO", null),
                             makeCategory("urn:test.scheme.2", "BAZ", null)
                    ),
                    false, "*", true, true);

        // check that the categories ARE overwritten when we send them along
        assertEquals(1, getPage("widgets/acme/-/(urn:test.scheme.1)FOO", 200).getEntries().size());
        assertEquals(1, getPage("widgets/acme/-/(urn:test.scheme.2)BAZ", 200).getEntries().size());
        getPage("widgets/acme/-/(urn:test.scheme.2)BAR", 304);

    }

    Category makeCategory(String scheme, String term, String label) {
        Category category = AtomServerTestCase.getFactory().newCategory();
        category.setScheme(scheme);
        category.setTerm(term);
        category.setLabel(label);
        return category;
    }
}
