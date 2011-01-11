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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryCategoryLogEvent;
import org.atomserver.testutils.conf.TestConfUtil;

import java.io.StringWriter;
import java.util.Date;
import java.util.List;

public class TagsLogEventDBSTest extends CRUDDBSTestCase {

    public static Test suite() { return new TestSuite(TagsLogEventDBSTest.class); }

    public void setUp() throws Exception {
        TestConfUtil.preSetup("logevents");
        super.setUp();
        categoriesDAO.deleteAllRowsFromEntryCategories();
        categoryLogEventsDAO.deleteAllRowsFromEntryCategoryLogEvent();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    protected String getURLPath() { return "widgets/acme/4.en.xml"; }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------

    public void testEntryWithCategories() throws Exception {
        /*  PUT::
       <entry xmlns="http://www.w3.org/2005/Atom">
         <id>tags:widgets/acme/4.en.xml</id>
         <content type="xhtml">
           <div xmlns="http://www.w3.org/1999/xhtml">
             <categories xmlns="http://www.w3.org/2007/app" xmlns:atom="http://www.w3.org/2005/Atom">
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:0" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:1" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:2" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:3" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:4" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:5" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:6" />
               <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="testutils:7" />
             </categories>
           </div>
         </content>
       </entry>

          THE CORRESPONDING ENTRY NOW RETURNS ::
       <?xml version='1.0' encoding='UTF-8'?>
         <entry xmlns="http://www.w3.org/2005/Atom">
         <id>/atomserver/v1/widgets/acme/4.en.xml</id>
         <title type="text"> Entry: acme 4.en</title>
         <author><name>atomserver Atom Service</name></author>
         <link href="/atomserver/v1/widgets/acme/4.en.xml" rel="self" />
         <link href="/atomserver/v1/widgets/acme/4.en.xml/1" rel="edit" />
         <updated>2008-01-04T19:03:21.000Z</updated>
         <published>2007-12-16T03:33:33.000Z</published>
         <category scheme="urn:widgets/foo" term="testutils:0" />
         <category scheme="urn:widgets/foo" term="testutils:1" />
         <category scheme="urn:widgets/foo" term="testutils:2" />
         <category scheme="urn:widgets/foo" term="testutils:3" />
         <category scheme="urn:widgets/foo" term="testutils:4" />
         <category scheme="urn:widgets/foo" term="testutils:5" />
         <category scheme="urn:widgets/foo" term="testutils:6" />
         <category scheme="urn:widgets/foo" term="testutils:7" />
         <content type="application/xml">
           <property xmlns="http://wvrgroup.com/propertyom" systemId="acme" id="4" homeAwayNetwork="false">[\n]">
              .....
        */

        // COUNT
        int startCount = categoryLogEventsDAO.getTotalCount("widgets");
        log.debug("startCount = " + startCount);

        // First let's add a bunch of Categories for the Entry
        // Create a standard APP Categories doc
        //  which is the Content for this "tags:widgets" Entry
        String urlPath = "tags:widgets/acme/4.en.xml/*";
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        Categories categories = getFactory().newCategories();

        int numCats = 8;
        for (int ii = 0; ii < numCats; ii++) {
            Category category = getFactory().newCategory();
            category.setScheme("urn:widgets/foo");
            category.setTerm("testutils:" + ii);
            categories.addCategory(category);
        }
        StringWriter stringWriter = new StringWriter();
        categories.writeTo(stringWriter);
        String categoriesXML = stringWriter.toString();
        log.debug("Categories= " + categoriesXML);

        //INSERT to tags:widgets
        update(id, fullURL, categoriesXML);

        int count = categoriesDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);
        count = categoryLogEventsDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);

        EntryCategory entryIn = new EntryCategory();
        entryIn.setWorkspace("widgets");
        entryIn.setCollection("acme");
        entryIn.setEntryId("4");
        entryIn.setLanguage("en");
        List<EntryCategoryLogEvent> logEvents = categoryLogEventsDAO.selectEntryCategoryLogEvent(entryIn);
        log.debug("====> logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == numCats);

        //Date lastDate = new Date();
        Date lastDate = categoriesDAO.selectSysDate();

        for( EntryCategoryLogEvent event : logEvents ) {
            assertTrue( (event.getCreateDate().compareTo(lastDate) <= 0) );
            lastDate= event.getCreateDate();
        }

        entryIn.setScheme( "urn:widgets/foo" );
        logEvents = categoryLogEventsDAO.selectEntryCategoryLogEventByScheme(entryIn);
        log.debug("====> logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == numCats);

        //lastDate = new Date();
        lastDate = categoriesDAO.selectSysDate();

        for( EntryCategoryLogEvent event : logEvents ) {
            assertTrue( (event.getCreateDate().compareTo(lastDate) <= 0) );
            lastDate= event.getCreateDate();
        }
        
        entryIn.setTerm( "testutils:1" );
        logEvents = categoryLogEventsDAO.selectEntryCategoryLogEventBySchemeAndTerm(entryIn);
        log.debug("====> logEvents = " + logEvents);
        assertNotNull(logEvents);
        assertTrue(logEvents.size() == 1);

        //INSERT to tags:widgets
        update(id, fullURL, categoriesXML);

        count = categoriesDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);
        count = categoryLogEventsDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats*2), count);

        //INSERT to tags:widgets
        update(id, fullURL, categoriesXML);

        count = categoriesDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);
        count = categoryLogEventsDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats*3), count);

        categoriesDAO.deleteAllRowsFromEntryCategories();
        categoryLogEventsDAO.deleteAllRowsFromEntryCategoryLogEvent();
    }
}
