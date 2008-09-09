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
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.lang.LocaleUtils;
import org.atomserver.core.EntryMetaData;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;

import java.io.StringWriter;
import java.util.List;

/**
 */
public class TagsBasicsDBSTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( TagsBasicsDBSTest.class ); }

    public void setUp() throws Exception { 
        super.setUp(); 
        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
    }

    public void tearDown() throws Exception
    { super.tearDown(); }

    protected String getURLPath() { return "widgets/acme/4.en.xml"; }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------
    public void testFeedWithCategories() throws Exception {

        String urlPath = "tags:widgets/acme" ;
        String fullURL = getServerURL() + urlPath;
       
        // Note this is a 301 Moved Permanently -- so the Client will redirect
        //  based on the Location Header....
        ClientResponse response = clientGetWithFullURL( fullURL, 200 );               
    }

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

        // First let's add a bunch of Categories for the Entry
        // Create a standard APP Categories doc
        //  which is the Content for this "tags:widgets" Entry
        String urlPath = "tags:widgets/acme/4.en.xml/*" ;
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;
        
        Categories categories = getFactory().newCategories();

        int numCats = 8;
        for ( int ii=0; ii < numCats; ii++ ) { 
            Category category = getFactory().newCategory();
            category.setScheme( "urn:widgets/foo" );
            category.setTerm( "testutils:" + ii );
            categories.addCategory( category );
        }
        StringWriter stringWriter = new StringWriter();
        categories.writeTo( stringWriter ); 
        String categoriesXML = stringWriter.toString();
        log.debug( "Categories= " + categoriesXML );

        //INSERT
        String editURI = update(id, fullURL, categoriesXML );

        // Now let's GET the corresponding "widgets" Entry
        urlPath = "widgets/acme/4.en.xml" ;
        fullURL = getServerURL() + urlPath;
       
        ClientResponse response = clientGetWithFullURL( fullURL, 200 );
        assertEquals(200, response.getStatus());

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();

        String xmlContent = entryOut.getContent();
        assertTrue( xmlContent.indexOf( "id=\"4\"" ) != -1 );

        // verify that the Categories appear in the Entry
        List<Category> entryCategories = entryOut.getCategories();
        assertNotNull( entryCategories ); 
        assertEquals( numCats, entryCategories.size() );

        response.release();

        // Let's delete the Categories -- these are actually deleted -- not just "marked deleted"
        editURI = delete(editURI);

        // Let's verify that the Categories are gone and that the Entry content has not been deleted
        response = clientGetWithFullURL( fullURL, 200 );
        assertEquals(200, response.getStatus());

        doc = response.getDocument();
        entryOut = doc.getRoot();

        xmlContent = entryOut.getContent();
        assertTrue( xmlContent.indexOf( "id=\"4\"" ) != -1 );

        // verify that the Categories appear in the Entry
        entryCategories = entryOut.getCategories();
        assertNotNull( entryCategories ); 
        assertEquals( 0, entryCategories.size() );

        response.release();

        // Let's verify that the actual Entry is NOT marked "Deleted"
        IRI entryIRI = IRI.create("http://localhost:8080/"
                                  + widgetURIHelper.constructURIString( "widgets", "acme", "4",  LocaleUtils.toLocale("en") ) );
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI.toString()), true);
        EntryMetaData metaData = entriesDAO.selectEntry(entryTarget);
        assertFalse( metaData.getDeleted() );
    }

    //========================================
    public void testGetServiceDocument() throws Exception {

        // First let's add some Categories for some Entries
        // Create a standard APP Categories doc
        //  which is the Content for this "tags:widgets" Entry
        String urlPath = "tags:widgets/acme/4.en.xml/*" ;
        String fullURL = getServerURL() + urlPath;
        String id = urlPath;
        
        Categories categories = getFactory().newCategories();
        int numCats4 = 3;
        String[] schemes4 = { "urn:widgets/foo", "urn:widgets/foo", "urn:widgets/bar" };
        String[] terms4 = { "term1", "term2", "term1" };

        for ( int ii=0; ii < numCats4; ii++ ) { 
            Category category = getFactory().newCategory();
            category.setScheme( schemes4[ii] );
            category.setTerm( terms4[ii] );
            categories.addCategory( category );
        }
        StringWriter stringWriter = new StringWriter();
        categories.writeTo( stringWriter ); 
        String categoriesXML4 = stringWriter.toString();
        log.debug( "Categories= " + categoriesXML4 );

        //INSERT
        String editURI4 = insert(id, fullURL, categoriesXML4, false );

        //-------------
        urlPath = "tags:widgets/acme/2797.en.xml/*" ;
        fullURL = getServerURL() + urlPath;
        id = urlPath;
        
        categories = getFactory().newCategories();
        int numCats2797 = 3;
        String[] schemes2797 = { "urn:widgets/foo", "urn:widgets/foo", "urn:widgets/bar" };
        String[] terms2797 = { "term3", "term1", "term1" };

        for ( int ii=0; ii < numCats2797; ii++ ) { 
            Category category = getFactory().newCategory();
            category.setScheme( schemes2797[ii] );
            category.setTerm( terms2797[ii] );
            categories.addCategory( category );
        }
        stringWriter = new StringWriter();
        categories.writeTo( stringWriter ); 
        String categoriesXML2797 = stringWriter.toString();
        log.debug( "Categories= " + categoriesXML2797 );

        //INSERT
        String editURI2797 = insert(id, fullURL, categoriesXML2797, false );

        //=====================
        // Now get the Service Document
        /*
          <?xml version='1.0' encoding='UTF-8'?>
          <service xmlns="http://www.w3.org/2007/app" xmlns:atom="http://www.w3.org/2005/Atom">
            <workspace>
              <atom:title type="text">widgets</atom:title>
              <collection href="widgets/dummy/">
                 <atom:title type="text">dummy</atom:title>
                 <accept>application/atom+xml;type=entry</accept>
                 <categories />
              </collection>
              <collection href="widgets/acme/">
                <atom:title type="text">acme</atom:title>
                <accept>application/atom+xml;type=entry</accept>
                <categories>
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/bar" term="term1" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="term1" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="term2" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="term3" />
                </categories>
              </collection>
            </workspace>
            ......
         </service>
        */

        AbderaClient client = new AbderaClient();
        RequestOptions options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // do the introspection step
        ClientResponse response = client.get( getServerURL(), options );
        assertEquals(200, response.getStatus());

        Document<Service> service_doc = response.getDocument();
        assertNotNull(service_doc);

        // there are several workspaces configured for the base URL
        //int numWorkspaces = store.getNumberOfWorkspaces();
        int numWorkspaces = store.getNumberOfVisibleWorkspaces();

        assertEquals(numWorkspaces, service_doc.getRoot().getWorkspaces().size());

        Workspace workspace1 = service_doc.getRoot().getWorkspace( "tags:widgets" );
        assertNull(workspace1);
        Workspace workspace = service_doc.getRoot().getWorkspace( "widgets" );
        assertNotNull(workspace);

        for (Collection c: workspace.getCollections()) {
            assertNotNull(c.getTitle());
            assertNotNull(c.getHref());
            assertTrue(c.getHref().toString().startsWith(workspace.getTitle() + '/'));
        }
        response.release();

        //===============
        // Now get the Service Document
        /*
          <?xml version='1.0' encoding='UTF-8'?>
          <service xmlns="http://www.w3.org/2007/app" xmlns:atom="http://www.w3.org/2005/Atom">
            <workspace>
              <atom:title type="text">widgets</atom:title>
              <collection href="widgets/dummy/">
                <atom:title type="text">dummy</atom:title>
                <accept>application/atom+xml;type=entry</accept>
                <categories />
              </collection>
              <collection href="widgets/acme/">
                <atom:title type="text">acme</atom:title>
                <accept>application/atom+xml;type=entry</accept>
                <categories>
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/bar" term="term1" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="term1" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="term2" />
                   <category xmlns="http://www.w3.org/2005/Atom" scheme="urn:widgets/foo" term="term3" />
                </categories>
              </collection>
            </workspace>
          </service>
        */
        client = new AbderaClient();
        options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // do the introspection step
        response = client.get( getServerURL() + "widgets" , options );
        assertEquals(200, response.getStatus());

        service_doc = response.getDocument();
        assertNotNull(service_doc);

        workspace = service_doc.getRoot().getWorkspace( "widgets" );
        assertNotNull(workspace);

        for (Collection c: workspace.getCollections()) {
            assertNotNull(c.getTitle());
            assertNotNull(c.getHref());
            assertTrue(c.getHref().toString().startsWith(workspace.getTitle() + '/'));
        }
        response.release();

        //===============
        // Now get the Service Document
        // NOTE: should return the same service docs a "widgets"
        client = new AbderaClient();
        options = client.getDefaultRequestOptions();
        options.setHeader("Connection", "close");

        // do the introspection step
        response = client.get( getServerURL() + "tags:widgets" , options );
        assertEquals(200, response.getStatus());

        service_doc = response.getDocument();
        assertNotNull(service_doc);

        workspace = service_doc.getRoot().getWorkspace( "widgets" );
        assertNotNull(workspace);

        for (Collection c: workspace.getCollections()) {
            assertNotNull(c.getTitle());
            assertNotNull(c.getHref());
            assertTrue(c.getHref().toString().startsWith(workspace.getTitle() + '/'));
        }
        response.release();

        //===============
        // Let's delete the Categories -- these are actually deleted -- not just "marked deleted"
        String editURI = delete(editURI4);
        editURI = delete(editURI2797);

        // Delete the Categories we created (permanently)
        IRI entryIRI4 = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString( "tags:widgets", "acme", "4",  LocaleUtils.toLocale("en") ) );
        EntryTarget entryTarget4 =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI4.toString()), false);
        entriesDAO.obliterateEntry(entryTarget4);

        // Let's verify that the Categories are gone and that the Entry content has not been deleted
        urlPath = "widgets/acme/4.en.xml" ;
        fullURL = getServerURL() + urlPath;

        response = clientGetWithFullURL( fullURL, 200 );
        assertEquals(200, response.getStatus());

        Document<Entry> doc = response.getDocument();
        Entry entryOut = doc.getRoot();

        IRI entryIRI2797 = IRI.create("http://localhost:8080/"
                              + widgetURIHelper.constructURIString( "tags:widgets", "acme", "2797",  LocaleUtils.toLocale("en") ) );
        EntryTarget entryTarget2797 =
                widgetURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", entryIRI2797.toString()), false);
        entriesDAO.obliterateEntry(entryTarget2797);

        String xmlContent = entryOut.getContent();
        assertTrue( xmlContent.indexOf( "id=\"4\"" ) != -1 );
        
        List<Category> entryCategories = entryOut.getCategories();
        assertNotNull( entryCategories ); 
        assertEquals( 0, entryCategories.size() );

        response.release();

        //---------
        urlPath = "widgets/acme/2797.en.xml" ;
        fullURL = getServerURL() + urlPath;
        response = clientGetWithFullURL( fullURL, 200 );
        assertEquals(200, response.getStatus());

        doc = response.getDocument();
        entryOut = doc.getRoot();

        xmlContent = entryOut.getContent();
        assertTrue( xmlContent.indexOf( "id=\"2797\"" ) != -1 );

        entryCategories = entryOut.getCategories();
        assertNotNull( entryCategories ); 
        assertEquals( 0, entryCategories.size() );

        response.release();
    }


}
