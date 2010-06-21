package org.atomserver.core.dbstore;

import org.atomserver.EntryDescriptor;
import org.atomserver.core.AbstractAtomCollection;
import org.atomserver.core.BaseEntryDescriptor;


public class TransactionTimeoutTest extends CRUDDBSTestCase {
    public void testTransactionBehavior() throws Exception {
        DBBasedAtomService service = (DBBasedAtomService) getSpringFactory().getBean("org.atomserver-atomServiceV2");

        int timeout = service.getTransactionTemplate().getTimeout();
        try {
            service.getTransactionTemplate().setTimeout(1);

            String[] path = getURLPath().split("/");
            final String workspaceName = path[0];
            final String collectionName = path[1];
            final String entryId = "8675309";
            final DBBasedAtomCollection collection =
                    (DBBasedAtomCollection) service.getAtomWorkspace(workspaceName)
                            .getAtomCollection(collectionName);

            final EntryDescriptor entryDescriptor = new BaseEntryDescriptor(workspaceName, collectionName, entryId, null);

            collection.getEntriesDAO().obliterateEntry(entryDescriptor);

            assertNull(collection.getEntriesDAO().selectEntry(entryDescriptor));

            Object id;

            id = collection.executeTransactionally(new AbstractAtomCollection.TransactionalTask<Object>() {
                public Object execute() {
                    return collection.getEntriesDAO().insertEntry(entryDescriptor);
                }
            });

            assertNotNull(id);
            assertNotNull(collection.getEntriesDAO().selectEntry(entryDescriptor));
            collection.getEntriesDAO().obliterateEntry(entryDescriptor);
            assertNull(collection.getEntriesDAO().selectEntry(entryDescriptor));

            try {
                id = null;
                id = collection.executeTransactionally(new AbstractAtomCollection.TransactionalTask<Object>() {
                    public Object execute() {
                        Object id = collection.getEntriesDAO().insertEntry(entryDescriptor);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                        return id;
                    }
                });
                fail("expected an exception");
            } catch (Exception e) {
                System.out.println("e = " + e);
                e.printStackTrace();
                assertNull(id);
                Thread.sleep(2000);
                assertNull(collection.getEntriesDAO().selectEntry(entryDescriptor));
            }
        } finally {
            service.getTransactionTemplate().setTimeout(timeout);
        }

    }
}
