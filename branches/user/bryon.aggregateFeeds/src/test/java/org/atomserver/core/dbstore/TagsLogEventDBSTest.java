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
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Document;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.lang.LocaleUtils;
import org.atomserver.uri.EntryTarget;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.BaseServiceDescriptor;

import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

public class TagsLogEventDBSTest extends CRUDDBSTestCase {

    public static Test suite() { return new TestSuite(TagsLogEventDBSTest.class); }

    public void setUp() throws Exception {
        TestConfUtil.preSetup("logevents");
        super.setUp();
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        entryCategoryLogEventDAO.deleteAllRowsFromEntryCategoryLogEvent();
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
        int startCount = entryCategoryLogEventDAO.getTotalCount("widgets");
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
        String editURI = update(id, fullURL, categoriesXML);

        int count = entryCategoriesDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);
        count = entryCategoryLogEventDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);

        //INSERT to tags:widgets
        editURI = update(id, fullURL, categoriesXML);

        count = entryCategoriesDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);
        count = entryCategoryLogEventDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats*2), count);

        //INSERT to tags:widgets
        editURI = update(id, fullURL, categoriesXML);

        count = entryCategoriesDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats), count);
        count = entryCategoryLogEventDAO.getTotalCount("widgets");
        assertEquals((startCount + numCats*3), count);

        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        entryCategoryLogEventDAO.deleteAllRowsFromEntryCategoryLogEvent();       
    }
}
