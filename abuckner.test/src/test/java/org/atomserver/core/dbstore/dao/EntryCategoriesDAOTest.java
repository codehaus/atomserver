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

package org.atomserver.core.dbstore.dao;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.lang.LocaleUtils;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.EntryMetaData;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;

import java.util.ArrayList;
import java.util.List;


public class EntryCategoriesDAOTest extends DAOTestCase {

    // -------------------------------------------------------
    public static Test suite() { return new TestSuite(EntryCategoriesDAOTest.class); }

    // -------------------------------------------------------
    protected void setUp() throws Exception {
        super.setUp();
        categoriesDAO.deleteAllRowsFromEntryCategories();
    }

    // -------------------------------------------------------
    protected void tearDown() throws Exception { super.tearDown(); }

    //----------------------------
    //          Tests 
    //----------------------------
    public void testSelectDistictCategoriesPerCollection() throws Exception {
        int startCount = categoriesDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        String[] collections = {  "fred",  "fred",  "fred", "wilma", "betty", "wilma", "wilma", "betty" };
        String[] schemes =     { "urn:a", "urn:a", "urn:b", "urn:a", "urn:a", "urn:a", "urn:a", "urn:a" };
        String[] terms =       { "term1", "term1", "term1", "term1", "term1", "term1", "term1", "term2" };

        String propId = "41200";

        List<EntryMetaData> entries = new ArrayList<EntryMetaData>();
        int numRows = collections.length;
        for ( int ii = 0; ii < numRows; ii++ ) {
            EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, collections[ii], propId + ii, null,0);
            entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
            entriesDAO.insertEntry(descriptor);
            entries.add(entriesDAO.selectEntry(descriptor));
        }

        // INSERT
        for ( int ii = 0; ii < numRows; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setEntryStoreId(entries.get(ii).getEntryStoreId());
            entryIn.setScheme( schemes[ii] );
            entryIn.setTerm( terms[ii] );

            int inserts = categoriesDAO.insertEntryCategory(entryIn);
            assertTrue(inserts > 0);
        }

        int count = categoriesDAO.getTotalCount(workspace);
        assertEquals((startCount + numRows), count);
       
        List colls = categoriesDAO.selectDistictCategoriesPerCollection( workspace, "fred" );
        log.debug("====> colls for FRED= " + colls);
        assertNotNull( colls );
        assertEquals( 2, colls.size() );

        colls = categoriesDAO.selectDistictCategoriesPerCollection( workspace, "wilma" );
        log.debug("====> colls for WILMA= " + colls);
        assertNotNull( colls );
        assertEquals( 1, colls.size() );

        colls = categoriesDAO.selectDistictCategoriesPerCollection( workspace, "betty" );
        log.debug("====> colls for BETTY= " + colls);
        assertNotNull( colls );
        assertEquals( 2, colls.size() );

        for ( int ii = 0; ii < numRows; ii++ ) {
            entriesDAO.obliterateEntry(entries.get(ii));
        }
        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testSelectDistinctCollections() throws Exception {
        int startCount = categoriesDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        String[] collections = { "fred", "fred", "barney", "wilma", "betty", "wilma" } ;
        String propId = "81300";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        // INSERT
        List<EntryMetaData> entries = new ArrayList<EntryMetaData>();
        int numRows = collections.length;
        for ( int ii = 0; ii < numRows; ii++ ) {
            EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, collections[ii], propId + ii, null,0);
            entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
            entriesDAO.insertEntry(descriptor);
            entries.add(entriesDAO.selectEntry(descriptor));
        }

        for ( int ii = 0; ii < numRows; ii++ ) { 
            EntryCategory entryIn = new EntryCategory();
            entryIn.setEntryStoreId(entries.get(ii).getEntryStoreId());
            entryIn.setScheme( scheme );
            entryIn.setTerm( term );
            
            int inserts = categoriesDAO.insertEntryCategory(entryIn);
            assertTrue(inserts > 0);
        }

        int count = categoriesDAO.getTotalCount(workspace);
        assertEquals((startCount + numRows), count);

        List colls = categoriesDAO.selectDistictCollections( workspace );
        log.debug("====> colls = " + colls);
        assertNotNull( colls );
        assertEquals( colls.size(), 4 );
        assertTrue( colls.contains( "fred" ));
        assertTrue( colls.contains( "barney" ));
        assertTrue( colls.contains( "wilma" ));
        assertTrue( colls.contains( "betty" ));

        for ( int ii = 0; ii < numRows; ii++ ) {
            entriesDAO.obliterateEntry(entries.get(ii));
        }
        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testCRUD() throws Exception {
        // COUNT
        int startCount = categoriesDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        //String workspace = "widgets";
        String sysId = "acme";
        String propId = "2182";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        // INSERT
        EntryCategory entryIn = new EntryCategory();
        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null, 0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);
        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);
        entryIn.setEntryStoreId(metaData.getEntryStoreId());
        entryIn.setScheme( scheme );
        entryIn.setTerm( term );

        int numRows = categoriesDAO.insertEntryCategory(entryIn);
        assertTrue(numRows > 0);

        int count = categoriesDAO.getTotalCount(workspace);
        assertEquals((startCount + 1), count);

        // SELECT 
        EntryCategory entryOut = categoriesDAO.selectEntryCategory(entryIn);
        log.debug("====> entryOut = " + entryOut);
        assertNotNull(entryOut);

        // DELETE
        categoriesDAO.deleteEntryCategory(entryIn);
        entriesDAO.obliterateEntry(descriptor);

        // SELECT again
        EntryCategory entryOut2 = categoriesDAO.selectEntryCategory(entryIn);
        log.debug("====> entryOut2 = " + entryOut2);
        assertNull(entryOut2);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testBatch() throws Exception {
        // COUNT
        int startCount = categoriesDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        //String workspace = "widgets";
        String sysId = "acme";
        String propId = "31300";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null, 0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);
        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);


        // INSERT
        int numTags = 5; 
        List<EntryCategory> ecList = new ArrayList();
 
        for ( int ii=0; ii< numTags; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setEntryStoreId(metaData.getEntryStoreId());
            entryIn.setScheme( scheme );
            entryIn.setTerm( term + ii );

            ecList.add( entryIn );
        }

        categoriesDAO.insertEntryCategoryBatch( ecList );

        int count = categoriesDAO.getTotalCount(workspace);
        assertEquals((startCount + numTags), count);

        // SELECT 
        IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, LocaleUtils.toLocale("en") ) );
        EntryTarget entryTarget =
                entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        List tags = categoriesDAO.selectEntryCategories(entryTarget);
        log.debug("====> tags = " + tags);
        assertNotNull( tags );
                                 
        int jj = 0; 
        for ( Object obj : tags ) {
            EntryCategory tag = (EntryCategory)obj;
            String ttt = term + jj; 

            assertEquals( tag.getWorkspace(), workspace); 
            assertEquals( tag.getCollection(), sysId ); 
            assertEquals( tag.getEntryId(), propId); 
            assertEquals( tag.getScheme(), scheme); 
            assertEquals( tag.getTerm(), ttt ); 
            jj++; 
        }

        // DELETE
        ecList = new ArrayList();
        for ( int ii=0; ii< numTags; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setWorkspace(workspace);
            entryIn.setCollection(sysId);
            entryIn.setEntryId(propId);
            // TODO: locale "en"?
            entryIn.setScheme( scheme );
            entryIn.setTerm( term + ii );

            ecList.add( entryIn );
        }

        categoriesDAO.deleteEntryCategoryBatch( ecList );
        entriesDAO.obliterateEntry(descriptor);

        // COUNT
        Thread.sleep( DB_CATCHUP_SLEEP); // give the DB a chance to catch up
        int finalCount = categoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

    public void testSelectEntryCategories() throws Exception {
        // COUNT
        int startCount = categoriesDAO.getTotalCount(workspace);
        log.debug("startCount = " + startCount);

        //String workspace = "widgets";
        String sysId = "acme";
        String propId = "31300";
        String scheme = "urn:ha/widgets";
        String term = "foobar";

        EntryDescriptor descriptor  = new BaseEntryDescriptor(workspace, sysId, propId, null,0);
        entriesDAO.ensureCollectionExists(descriptor.getWorkspace(), descriptor.getCollection());
        entriesDAO.insertEntry(descriptor);
        EntryMetaData metaData = entriesDAO.selectEntry(descriptor);

        // INSERT
        int numTags = 5; 
        for ( int ii=0; ii< numTags; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setEntryStoreId(metaData.getEntryStoreId());
            entryIn.setScheme( scheme );
            entryIn.setTerm( term + ii );

            int numRows = categoriesDAO.insertEntryCategory(entryIn);
            assertTrue(numRows > 0);
        }

        int count = categoriesDAO.getTotalCount(workspace);
        assertEquals((startCount + numTags), count);

        // SELECT 
        IRI iri = IRI.create("http://localhost:8080/"
                             + entryURIHelper.constructURIString(workspace, sysId, propId, LocaleUtils.toLocale("en") ) );
        EntryTarget entryTarget =
                entryURIHelper.getEntryTarget(new MockRequestContext(serviceContext, "GET", iri.toString()), true);

        List tags = categoriesDAO.selectEntryCategories(entryTarget);
        log.debug("====> tags = " + tags);
        assertNotNull( tags );
                                 
        int jj = 0; 
        for ( Object obj : tags ) {
            EntryCategory tag = (EntryCategory)obj;
            String ttt = term + jj; 

            assertEquals( tag.getWorkspace(), workspace); 
            assertEquals( tag.getCollection(), sysId ); 
            assertEquals( tag.getEntryId(), propId); 
            assertEquals( tag.getScheme(), scheme); 
            assertEquals( tag.getTerm(), ttt ); 
            jj++; 
        }

        // DELETE
        for ( int ii=0; ii< numTags; ii++ ) {
            EntryCategory entryIn = new EntryCategory();
            entryIn.setWorkspace(workspace);
            entryIn.setCollection(sysId);
            entryIn.setEntryId(propId);
            // TODO: locale "en"?
            entryIn.setScheme( scheme );
            entryIn.setTerm( term + ii );
            categoriesDAO.deleteEntryCategory(entryIn);
        }
        entriesDAO.deleteEntry(descriptor);

        // COUNT
        Thread.sleep(500); // give the DB a chance to catch up
        int finalCount = categoriesDAO.getTotalCount(workspace);
        log.debug("finalCount = " + finalCount);
        assertEquals(startCount, finalCount);
    }

}
