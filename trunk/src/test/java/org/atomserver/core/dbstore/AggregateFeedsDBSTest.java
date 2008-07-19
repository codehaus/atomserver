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
package org.atomserver.core.dbstore;

import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.atomserver.core.etc.AtomServerConstants;

import java.io.StringWriter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AggregateFeedsDBSTest extends DBSTestCase {
    private static final int BASE_WIDGET_ID = 88000;
    private static final int BASE_DUMMY_ID = 99000;
    private static final int NUM_ENTRIES = 10;

    public void setUp() throws Exception {
        super.setUp();
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        entriesDao.deleteAllRowsFromEntries();
    }

    public void tearDown() throws Exception { super.tearDown(); }

    public void testNothing() throws Exception {

    }

    public void XXXtestAggregateFeeds() throws Exception {
        for (int i = 0; i < NUM_ENTRIES; i++) {
            String widgetId = "" + (BASE_WIDGET_ID + i);
            Locale locale = i == 5 ? Locale.UK : Locale.US;
            createWidget("widgets", "mywidgets", widgetId,
                         locale.toString(), createWidgetXMLFileString(widgetId));

            String dummyId = "" + (BASE_DUMMY_ID + i);
            createWidget("dummy", "mydummies", dummyId,
                         null, createWidgetXMLFileString(dummyId));

            Categories categories = getFactory().newCategories();
            Category category = getFactory().newCategory();
            category.setScheme("urn:myjoin");
            category.setTerm(widgetId);
            categories.addCategory(category);

            category = getFactory().newCategory();
            category.setScheme("urn:color");
            category.setTerm((BASE_WIDGET_ID + i) % 3 == 0 ? "blue" : "red");
            categories.addCategory(category);

            StringWriter stringWriter = new StringWriter();
            categories.writeTo(stringWriter);
            String categoriesXML = stringWriter.toString();

            modifyEntry("tags:widgets", "mywidgets", widgetId, locale.toString(),
                        categoriesXML, false, "*", false);

            categories = getFactory().newCategories();
            category = getFactory().newCategory();
            category.setScheme("urn:myjoin");
            category.setTerm(widgetId);
            categories.addCategory(category);

            category = getFactory().newCategory();
            category.setScheme("urn:parity");
            category.setTerm(i % 2 == 0 ? "even" : "odd");
            categories.addCategory(category);

            stringWriter = new StringWriter();
            categories.writeTo(stringWriter);
            categoriesXML = stringWriter.toString();


            modifyEntry("tags:dummy", "mydummies", dummyId, null,
                        categoriesXML, false, "*", false);
        }

        //-------------
        Feed feed = getPage("$join/urn:myjoin", 200);
        assertEquals(NUM_ENTRIES, feed.getEntries().size());

        Feed catFeed = getPage("$join/urn:myjoin/-/(urn:parity)even?entry-type=full", 200);
        assertEquals(NUM_ENTRIES / 2, catFeed.getEntries().size());
        for (Entry entry : catFeed.getEntries()) {
            Matcher matcher = Pattern.compile(".*\\/(\\d+)\\.xml$").matcher(entry.getId().toString());
            matcher.matches();
            int entryId = Integer.parseInt(matcher.group(1));
            assertEquals(0, entryId % 2);
        }

        catFeed = getPage("$join/urn:myjoin/-/(urn:parity)even/(urn:color)blue?entry-type=full", 200);
        int numBlueEntries = catFeed.getEntries().size();
        assertTrue(numBlueEntries > 0);
        assertTrue(numBlueEntries < (NUM_ENTRIES / 2));
        for (Entry entry : catFeed.getEntries()) {
            Matcher matcher = Pattern.compile(".*\\/(\\d+)\\.xml$").matcher(entry.getId().toString());
            matcher.matches();
            int entryId = Integer.parseInt(matcher.group(1));
            assertEquals(0, entryId % 2);
            assertEquals(0, entryId % 3);
        }

        catFeed = getPage("$join/urn:myjoin/-/(urn:parity)even/(urn:color)red?entry-type=full", 200);
        assertEquals(NUM_ENTRIES / 2, catFeed.getEntries().size() + numBlueEntries);
        for (Entry entry : catFeed.getEntries()) {
            Matcher matcher = Pattern.compile(".*\\/(\\d+)\\.xml$").matcher(entry.getId().toString());
            matcher.matches();
            int entryId = Integer.parseInt(matcher.group(1));
            assertEquals(0, entryId % 2);
            assertFalse(0 == (entryId % 3));
        }

        String endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        modifyEntry("widgets", "mywidgets", "" + BASE_WIDGET_ID, Locale.US.toString(),
                    createWidgetXMLFileString("" + BASE_WIDGET_ID), false, "*", false);

        //-------------
        feed = getPage("$join/urn:myjoin?start-index=" + endIndex, 200);
        assertEquals(1, feed.getEntries().size());
        assertNull(feed.getEntries().get(0).getContent());
        assertEquals("/" + getBaseURI() + "/$join/urn:myjoin/" + BASE_WIDGET_ID + ".xml",
                     feed.getEntries().get(0).getLink("self").getHref().toString());

        feed = getPage("$join/urn:myjoin?entry-type=full&start-index=" + endIndex, 200);
        assertEquals(1, feed.getEntries().size());
        assertTrue(feed.getEntries().get(0).getContent().startsWith(
                "<aggregate xmlns=\"http://schemas.atomserver.org/atomserver/v1/rev0\">"));
        assertTrue(feed.getEntries().get(0).getContent().contains(
                getEntry("widgets", "mywidgets", "" + BASE_WIDGET_ID, Locale.US.toString()).getContent()));
        assertTrue(feed.getEntries().get(0).getContent().contains(
                getEntry("dummy", "mydummies", "" + BASE_DUMMY_ID, null).getContent()));
        assertTrue(feed.getEntries().get(0).getContent().endsWith("</aggregate>"));

        String content = getEntry("$join", "urn:myjoin", "" + BASE_WIDGET_ID, null).getContent();
        assertEquals(feed.getEntries().get(0).getContent(),
                     content);

        endIndex = feed.getSimpleExtension(AtomServerConstants.END_INDEX);

        modifyEntry("dummy", "mydummies", "" + BASE_DUMMY_ID, null,
                    createWidgetXMLFileString("" + BASE_DUMMY_ID), false, "*", false);

        //-------------
        feed = getPage("$join/urn:myjoin?start-index=" + endIndex, 200);
        assertEquals(1, feed.getEntries().size());

        feed = getPage("$join/urn:myjoin?entry-type=full&locale=en_US", 200);

        assertEquals(NUM_ENTRIES - 1, feed.getEntries().size());

        assertEquals(feed.getEntries().get(0).getContent(),
                     getEntry("$join", "urn:myjoin", "" + (BASE_WIDGET_ID + 1), Locale.US.toString()).getContent());


        feed = getPage("$join/urn:myjoin?locale=en_GB&entry-type=full", 200);
        assertEquals(1, feed.getEntries().size());

        Entry entry = getEntry("$join", "urn:myjoin", "" + (BASE_WIDGET_ID + 5), Locale.UK.toString());

        assertEquals(feed.getEntries().get(0).getContent(),
                     entry.getContent());

        String url = getServerURL() +
                     getURLPath("$join", "urn:myjoin",
                                "" + (BASE_WIDGET_ID + 5), Locale.US.toString(), null);

        clientGet(url, null, 404, true);
    }
}
