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
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Categories;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.protocol.client.ClientResponse;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;
import org.atomserver.utils.locale.LocaleUtils;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 */
public class TagsCRUDDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( TagsCRUDDBSTest.class ); }

    public void setUp() throws Exception
    { 
        super.setUp(); 
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
    }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected String getURLPath() { return "tags:widgets/acme/642.en.xml"; }

    protected String getPropfileBase() {
        return (userdir + "/var/widgets/acme/64/642/en/642.xml");
     }

    protected File getPropfile() {
        File propFile = new File( getPropfileBase() + "r0" );
        return propFile;
    }

    // --------------------
    //       tests
    //---------------------
    public void NOtestNothing() {}

    public void testCRUD() throws Exception {
        String urlPath = getURLPath();
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;

        // Create a standard APP Categories doc
        Categories categories = getFactory().newCategories();

        Category category = getFactory().newCategory();
        category.setScheme( "urn:widgets/foo" );
        category.setTerm( "scooby" );
        categories.addCategory( category );

        category = getFactory().newCategory();
        category.setScheme( "urn:widgets/foo" );
        category.setTerm( "doo" );
        categories.addCategory( category );

        StringWriter stringWriter = new StringWriter();
        categories.writeTo( stringWriter ); 
        String categoriesXML = stringWriter.toString();
        log.debug( "Categories= " + categoriesXML );

        //INSERT
        /* 
           PUT ::
           <entry xmlns="http://www.w3.org/2005/Atom">
             <id>tags:widgets/acme/642.en.xml</id>
             <content type="xhtml">
               <div xmlns="http://www.w3.org/1999/xhtml">
                 <categories xmlns="http://www.w3.org/2007/app" xmlns:atom="http://www.w3.org/2005/Atom">
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="scooby" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="doo" />
                 </categories>
               </div>
             </content>
           </entry>
           
           RETURNS::
           <?xml version='1.0' encoding='UTF-8'?>
           <entry xmlns="http://www.w3.org/2005/Atom">
             <id>/atomserver/v1/tags:widgets/acme/642.en.xml</id>
             <title type="text"> Entry: acme 642.en</title>
             <author><name>AtomServer APP Service</name></author>
             <link href="/atomserver/v1/tags:widgets/acme/642.en.xml" rel="self" />
             <link href="/atomserver/v1/tags:widgets/acme/642.en.xml/0" rel="edit" /
             <updated>2007-12-25T17:23:41.000Z</updated>
             <published>2007-12-25T17:23:41.000Z</published>
             <content type="application/xml">
               <categories xmlns="http://www.w3.org/2007/app" xmlns:atom="http://www.w3.org/2005/Atom">
                 <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="doo" />
                 <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="scooby" />
               </categories>
             </content>
           </entry>
        */

        log.debug( "************************************************************* INSERT ************************************" );

        // The first insert should FAIL, cuz we must have previously INSERTED the actual widget
        String editURI = insert(id, fullURL, categoriesXML, false, 400);

        // Now let's actually create property 642
        String realEntryURL = getServerURL() + "widgets/acme/642.en.xml";
        String realId = urlPath;
        String realEditURI = insert( realId, realEntryURL );
        
        // Now we should be able to actually add Categories
        editURI = insert(id, fullURL, categoriesXML, false );

        // SELECT
        log.debug( "************************************************************* SELECT ************************************" );
        editURI = select(fullURL, "urn:widgets/foo" );

        // UPDATE
        log.debug( "************************************************************* UPDATE ************************************" );
        editURI = update(id, editURI, categoriesXML );

        // SELECT
        log.debug( "************************************************************* SELECT ************************************" );
        editURI = select(fullURL, "urn:widgets/foo" );

        // DELETE -- this delete only marked the EntryStore row as deleted 
        //            BUT -- it also causes ALL Categories associated with this row to be deleted
        log.debug( "************************************************************* DELETE ************************************" );
        editURI = delete(editURI);

        // DELETE
        // Now delete the actual row for 642
        IRI entryIRI = IRI.create("http://localhost:8080/"
                                  + widgetURIHelper.constructURIString( "widgets", "acme", "642", LocaleUtils.toLocale("en") ));
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    protected String select( String fullURL, String xmlTestString ) throws Exception {
        ClientResponse response = clientGetWithFullURL(fullURL, 200);
        assertEquals(200, response.getStatus());

        Entry entryOut = verifyProperty642( response ); 

        IRI editLink = entryOut.getEditLinkResolvedHref();

        log.debug( "CONTENT:: " + entryOut.getContent() );
        log.debug( "xmlTestString:: " + xmlTestString );
        assertTrue(entryOut.getContent().indexOf(xmlTestString) != -1);

        assertNotNull("link rel='edit' must not be null", editLink);
        response.release();
        return editLink.toString();
    }

    private Entry verifyProperty642( ClientResponse response ) {
        Document<Entry> doc = response.getDocument();
        Entry entry = doc.getRoot();

        String contentXML = entry.getContent();
        log.debug( "++++++++++++++++++++ contentXML = " + contentXML );

        Parser parser = abdera.getParser();
        Document<Categories> contentDoc = parser.parse( new StringReader( contentXML ) );
        assertNotNull( contentDoc );

        Categories categories = contentDoc.getRoot(); 
        assertNotNull( categories );
        List<Category> categoryList = categories.getCategories();
        assertEquals( categoryList.size(), 2 );

        assertTrue( contentXML.indexOf( "scooby" ) != -1 );
        assertTrue( contentXML.indexOf( "doo" ) != -1 );

        assertTrue( entry.getUpdated().getTime() > 0L );

        assertTrue( entry.getId().toString().indexOf( "tags:widgets/acme/642" ) != -1  );

        // alternate link should be null when there is content element
        assertNull( entry.getAlternateLink() );

        assertNotNull( entry.getEditLink() );
        assertNotNull( entry.getSelfLink() );

        // Categories Entry docs don't have "external" Category entries
        assertEquals( 0, entry.getCategories().size() ); 

        return entry;
    }


}
