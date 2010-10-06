package org.atomserver.core.dbstore;


import org.atomserver.EntryDescriptor;
import org.atomserver.FeedDescriptor;
import org.atomserver.core.AbstractAtomCollection;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.BaseFeedDescriptor;

import java.io.PrintWriter;
import java.util.concurrent.TimeoutException;

public class TransactionTimeoutTest extends CRUDDBSTestCase {
    public static final boolean JDBC_DEBUG_VERBOSE = false;

    public void testTransactionBehavior() throws Exception {

        if ( JDBC_DEBUG_VERBOSE ) {
            PrintWriter p = new PrintWriter(System.out);
            net.sourceforge.jtds.util.Logger.setLogWriter(p);
            net.sourceforge.jtds.util.Logger.setActive(true);
        }

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

            // This one passes
            Object id = collection.executeTransactionally(new AbstractAtomCollection.TransactionalTask<Object>() {
                public Object execute() {
                    return collection.getEntriesDAO().insertEntry(entryDescriptor);
                }
            });

            // clean it up
            assertNotNull(id);
            assertNotNull(collection.getEntriesDAO().selectEntry(entryDescriptor));
            collection.getEntriesDAO().obliterateEntry(entryDescriptor);
            assertNull(collection.getEntriesDAO().selectEntry(entryDescriptor));

            // This one fails and rolls back
            long start = System.currentTimeMillis();
            TimeoutTransactionalTask task = new TimeoutTransactionalTask(collection, entryDescriptor);
            id = null;
            try {
                id = collection.executeTransactionally(task);

            } catch (Exception e) {
                // make sure it didn't get committed
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

    // Please retain
    public void XXXtestTransactionBehavior2() throws Exception {
        DBBasedAtomService service = (DBBasedAtomService) getSpringFactory().getBean("org.atomserver-atomServiceV2");

        int timeout = service.getTransactionTemplate().getTimeout();
        try {
            service.getTransactionTemplate().setTimeout(1);
            
            String[] path = getURLPath().split("/");
            final String workspaceName = path[0];
            final String collectionName = path[1];
            final DBBasedAtomCollection collection =
                    (DBBasedAtomCollection) service.getAtomWorkspace(workspaceName)
                            .getAtomCollection(collectionName);

            final FeedDescriptor feedDescriptor = new BaseFeedDescriptor("listings", "trips");
           
            long startTime = System.currentTimeMillis();
            Object entries = null;
            try {
                entries = collection.executeTransactionally(new AbstractAtomCollection.TransactionalTask<Object>() {
                    public Object execute() {
                        return collection.getEntriesDAO().selectFeedPage(null,null,0,-1,1000,null,feedDescriptor,null);
                    }
                });
                fail("should throw a TimeoutException");
            } catch (Exception e){
                log.error("e = " + e, e);
                log.error("cause = " + e.getCause(), e.getCause());
                assertTrue(e.getCause() instanceof TimeoutException);
            }
            log.debug("time= " + (System.currentTimeMillis() - startTime));
            assertNull(entries);

        } finally {
            service.getTransactionTemplate().setTimeout(timeout);
        }
    }

}
