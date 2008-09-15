package org.atomserver.core.dbstore.dao;

import org.apache.commons.lang.mutable.MutableLong;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.DBBasedAtomService;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

/**
 * there was some confusion about whether our transactions were behaving as expected.  This test
 * serves to prove beyond a shadow of a doubt that Transactions work as we expect.
 */
public class TransactionTest extends DAOTestCase {

    public void testTransactionalBehavior() throws Exception {

        if ("hsql".equals(entriesDAO.getDatabaseType())) {
            log.warn("HSQLDB is non-transactional.");
            return;
        }

        final TransactionTemplate template =
                ((DBBasedAtomService) springFactory.getBean("org.atomserver-atomService"))
                        .getTransactionTemplate();

        final EntryMetaData entryIn = new EntryMetaData();
        entryIn.setWorkspace("widgets");
        entryIn.setCollection("acme");
        entryIn.setLocale(Locale.US);
        entryIn.setEntryId("8675309");

        entriesDAO.ensureCollectionExists(entryIn.getWorkspace(), entryIn.getCollection());
        entriesDAO.obliterateEntry(entryIn);

        // test whether things work when we commit the txn
        runTest(template, entryIn, false);

        // test whether things work when we roll the txn back
        runTest(template, entryIn, true);
    }

    private void runTest(final TransactionTemplate template,
                         final EntryMetaData entryIn,
                         final boolean shouldRollback)
            throws Exception {
        // we use these three CyclicBarriers to sync up at t0, t1, and t2
        final CyclicBarrier[] t = new CyclicBarrier[]{
                new CyclicBarrier(2), new CyclicBarrier(2), new CyclicBarrier(2)
        };

        final MutableLong idWithinTransaction = new MutableLong(0);

        Executors.newSingleThreadExecutor().submit(
                new Runnable() {
                    public void run() {
                        template.execute(new TransactionCallbackWithoutResult() {
                            public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                                // first, we insert the entry and select it back
                                entriesDAO.insertEntry(entryIn);
                                EntryMetaData metaData = entriesDAO.selectEntry(entryIn);
                                idWithinTransaction.setValue(metaData.getEntryStoreId());

                                // sync up at t0 - the entry has been inserted IN txn, but not committed
                                try {
                                    t[0].await();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // sync up at t1 - we've verified that the entry is not visible
                                // outside the txn, but we have not yet committed
                                try {
                                    t[1].await();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (shouldRollback) {
                                    transactionStatus.setRollbackOnly();
                                }
                            }
                        });
                        // sync up at t2 - txn committed
                        try {
                            t[2].await();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // sync up at t0 - the entry has been inserted IN txn, but not committed
        t[0].await();

        EntryMetaData metaData = entriesDAO.selectEntry(entryIn);

        assertNull(metaData);

        // sync up at t1 - we've verified that the entry is not visible outside the txn, but we
        // have not yet committed
        t[1].await();

        // sync up at t2 - txn committed
        t[2].await();

        metaData = entriesDAO.selectEntry(entryIn);
        if (shouldRollback) {
            // verify that we still can't see it
            assertNull(metaData);
        } else {
            // verify that we CAN see it now
            assertNotNull(metaData);
            // verify that we see the SAME thing we saw inside the txn
            assertEquals(idWithinTransaction.getValue(), metaData.getEntryStoreId());
            // clean up
            entriesDAO.obliterateEntry(entryIn);
        }
    }
}