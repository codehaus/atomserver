package org.atomserver.core.dbstore;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.AbstractAtomCollection;
import org.atomserver.core.BaseEntryDescriptor;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


public class TransactionTimeoutTest extends CRUDDBSTestCase {
    public void testTransactionBehavior() throws Exception {
        DBBasedAtomService service = (DBBasedAtomService) getSpringFactory().getBean("org.atomserver-atomServiceV2");

        int timeout = service.getTransactionTemplate().getTimeout();
        try {
            service.getTransactionTemplate().setTimeout(2);

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

            long start = System.currentTimeMillis();

            TimeoutTransactionalTask task = new TimeoutTransactionalTask(collection, entryDescriptor);
            id = null;
            try {
                id = collection.executeTransactionally(task);

            } catch (Exception e) {
                log.error("e = " + e, e);
                log.error("cause = " + e.getCause(), e.getCause());
                log.debug("%%%%%%%%%%%%%%% Time of Exception = " + (System.currentTimeMillis() - start));
                assertNull(id);
                Thread.sleep(2000);
                assertNull(collection.getEntriesDAO().selectEntry(entryDescriptor));
                assertTrue( task.isInterrupted() );
                assertTrue( task.isStopped() );
            }
        } finally {
            service.getTransactionTemplate().setTimeout(timeout);
        }
    }

    class TimeoutTransactionalTask implements AbstractAtomCollection.TransactionalTask<Object> {
        DBBasedAtomCollection collection;
        EntryDescriptor entryDescriptor;
        boolean isStopped = false;
        boolean isInterrupted = false;

        TimeoutTransactionalTask(DBBasedAtomCollection collection, EntryDescriptor entryDescriptor) {
            this.collection = collection;
            this.entryDescriptor = entryDescriptor;
        }

        public Object execute() {
            long tstart = System.currentTimeMillis();
            try {
                Object id = collection.getEntriesDAO().insertEntry(entryDescriptor);
                try {
                    Thread.sleep(3000);
                } catch( InterruptedException ee ) {
                    log.debug("TASK WAS INTERRUPTED");
                    isInterrupted = true;
                    throw new RuntimeException(ee);
                }
                fail("expected an exception");
                return id;
            } finally {
                log.debug("%%%%%%%%%%%%%%% Time in Thread = " + (System.currentTimeMillis() - tstart));
                isStopped = true;
            }
        }

        public boolean isStopped() {
            return isStopped;
        }

        public boolean isInterrupted() {
            return isInterrupted;
        }
    }
}
