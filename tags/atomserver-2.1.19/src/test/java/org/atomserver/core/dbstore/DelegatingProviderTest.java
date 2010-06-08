
package org.atomserver.core.dbstore;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.atomserver.DelegatingProvider;
import org.atomserver.testutils.client.MockRequestContext;
import org.atomserver.uri.EntryTarget;

public class DelegatingProviderTest extends CRUDDBSTestCase {

    public static Test suite()
    { return new TestSuite( DelegatingProviderTest.class ); }

    public void setUp() throws Exception
    { super.setUp(); }

    public void tearDown() throws Exception
    {
        super.tearDown();
        // NOTE: the super class tearDown() will call cleanUp(), so we should NOT call it here...
    }

    protected void cleanUp() throws Exception{
        super.cleanUp();
        EntryTarget entryTarget =
                widgetURIHelper.getEntryTarget(
                        new MockRequestContext(serviceContext, "GET", getEntryIRI().toString()), true);
        entriesDAO.obliterateEntry(entryTarget);
    }

    // --------------------
    //       tests
    //---------------------

    public void testCRUD() throws Exception {

        // Let's use the current provider
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        runCRUDTest();
        cleanUp();

        // reset the Provider
        String currentName = provider.getCurrentProviderName();
        DelegatingProvider.AtomServerType currentType = DelegatingProvider.AtomServerType.valueOf(currentName);
        DelegatingProvider.AtomServerType otherType = null;
        if ( currentType.equals( DelegatingProvider.AtomServerType.normal )) {
            otherType = DelegatingProvider.AtomServerType.throttled;
        } else {
            otherType = DelegatingProvider.AtomServerType.normal;
        }

        provider.setCurrentProviderName( otherType.toString() );

        // INSERT/SELECT/UPDATE/SELECT/DELETE
        runCRUDTest();
        cleanUp();

        // reset the Provider back
        // INSERT/SELECT/UPDATE/SELECT/DELETE
        provider.setCurrentProviderName( currentType.toString() );
        runCRUDTest();
        cleanUp();
     }
}
