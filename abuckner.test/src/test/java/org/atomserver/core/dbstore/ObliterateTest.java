
package org.atomserver.core.dbstore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.core.BaseFeedDescriptor;
import org.atomserver.core.EntryCategory;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.EntryDescriptor;

public class ObliterateTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( ObliterateTest.class ); }

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    {  super.tearDown();  }

    protected boolean requiresDBSeeding() { return true; }

    // --------------------
    //       tests
    //---------------------
     public void testObliterate() throws Exception {
        int startCount = entriesDAO.getTotalCount( new BaseFeedDescriptor("widgets", "acme"));
        log.debug( "startCount = " + startCount );

        assertTrue( contentStorage.contentExists(getEntryTarget("widgets", "acme", "9999", "en")) );
        assertTrue( contentStorage.contentExists(getEntryTarget("widgets", "acme", "2797", "en")) );
        assertTrue( contentStorage.contentExists(getEntryTarget("widgets", "acme", "9993", "en")) ); 

        DBBasedAtomService service = (DBBasedAtomService) getSpringFactory().getBean("org.atomserver-atomService");

        service.obliterateEntries("widgets/acme/9999?locale=en");

        int count = entriesDAO.getTotalCount( new BaseFeedDescriptor("widgets", "acme"));
        assertEquals( startCount-1, count );

        assertFalse( contentStorage.contentExists(getEntryTarget("widgets", "acme", "9999", "en")) );

        service.obliterateEntries("widgets/acme/2797?locale=en,widgets/acme/9993?locale=en");

        count = entriesDAO.getTotalCount( new BaseFeedDescriptor("widgets", "acme"));
        assertEquals( startCount-3, count );

        assertFalse( contentStorage.contentExists(getEntryTarget("widgets", "acme", "2797", "en")) );
        assertFalse( contentStorage.contentExists(getEntryTarget("widgets", "acme", "9993", "en")) );
    }

    // delete entry that have a category
    public void testObliterate2() throws Exception {

        String workspace = "widgets";
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

        // DELETE
        int startCount = entriesDAO.getTotalCount( new BaseFeedDescriptor("widgets", "acme"));
        DBBasedAtomService service = (DBBasedAtomService) getSpringFactory().getBean("org.atomserver-atomService");
        service.obliterateEntries("widgets/acme/9999?locale=en");
        int endCount = entriesDAO.getTotalCount( new BaseFeedDescriptor("widgets", "acme"));

        assertEquals(startCount - 1, endCount);

    }

}