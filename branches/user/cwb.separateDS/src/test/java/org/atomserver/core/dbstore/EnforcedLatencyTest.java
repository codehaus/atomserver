package org.atomserver.core.dbstore;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.atomserver.EntryDescriptor;
import org.atomserver.core.BaseEntryDescriptor;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.dbstore.dao.EntriesDAOiBatisImpl;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.testutils.latency.TimeLine;
import static org.atomserver.uri.URIHandler.REVISION_OVERRIDE;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import static java.util.Locale.US;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class EnforcedLatencyTest extends DBSTestCase {

    private static final int LATENCY_SECONDS = 9;
    private static final String WIDGETS = "widgets";
    private static final String ACME = "acme";

    private TransactionTemplate transactionTemplate;
    private static final int GAP = 3100;

    public void setUp() throws Exception {
        super.setUp();
        ((EntriesDAOiBatisImpl) entriesDao).setLatencySeconds(LATENCY_SECONDS);

        entryCategoriesDAO.deleteAllRowsFromEntryCategories();
        entriesDao.deleteAllRowsFromEntries();

        DBBasedAtomService service =
                (DBBasedAtomService) getSpringFactory().getBean("org.atomserver-atomService");
        transactionTemplate = service.getTransactionTemplate();
    }

    public void testRaceCondition() throws Exception {

        assertEquals( LATENCY_SECONDS, ((EntriesDAOiBatisImpl) entriesDao).getLatencySeconds() );

        for (String entryId : Arrays.asList("1", "2", "3", "4")) {
            createWidget(WIDGETS, ACME, entryId, US.toString(),
                         createWidgetXMLFileString(entryId));
        }
        Thread.sleep(1000 * LATENCY_SECONDS);

        Entry entry = getEntry(WIDGETS, ACME, "2", US.toString());
        final String updateIndex = entry.getSimpleExtension(AtomServerConstants.UPDATE_INDEX);

        // This test is designed to "force" the three-way race condition that we have seen, and
        // to prove that the "latency solution" solves it.  We accomplish this by constructing
        // two writers in such a way that we can "hold their transactions open" for an artificially
        // long time to cause them to race.
        //
        // Initially, the AtomServer contains widgets 1, 2, 3, and 4 - which were inserted in that
        // order.  We then have one writer updating widget 2, another updating widget 1, and a
        // series of readers that try to read the feed starting right after the point where
        // widget 2 was inserted.
        //
        // The sequence of events looks like:
        //
        // |       writer1       |       writer2       |     reader      |
        // |                     |                     |                 |
        // |- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --| *NOTE* the dashed lines
        // |                     |                     | READ # 1        |  are spaced apart such
        // | BEGIN TRANSACTION   |                     |                 |  that just over three
        // |                     |                     |                 |  seconds elapse between
        // |                     | BEGIN TRANSACTION   |                 |  each one - since the
        // |                     |                     |                 |  latency is set to
        // |   UPDATE WIDGET 2   |                     |                 |  nine seconds, this
        // |- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --|  allows for us to make
        // |                     |                     | READ # 2        |  assertions about the
        // |                     |                     |                 |  contents of the DB and
        // |                     |   UPDATE WIDGET 1   |                 |  about what should be
        // |                     | COMMIT TRANSACTION  |                 |  visible from a feed at
        // |- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --|  each of these points
        // |                     |                     | READ # 3        |
        // |                     |                     |                 |
        // | COMMIT TRANSACTION  |                     |                 |
        // |- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --|
        // |                     |                     | READ # 4        |
        // |- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --|
        // |                     |                     | READ # 5        |
        // |- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --|
        // |                     |                     | READ # 6        |
        //
        // At each of the numbered reads, we make two checks -- first, we query the DB directly to
        // get the set of entries that have changed since the point in time where widget 2 was
        // inserted, and we pull a feed - which uses the real "latency" query and filters out any
        // entries that are "unsafe".  The results at each read are:
        //
        // (1) DB returns 3, 4 and so does the feed -- nothing has changed yet
        // (2) DB returns 3, 4 and so does the feed -- no transactions have committed yet
        // (3) DB returns 3, 4, 1 ; feed returns 3, 4 -- writer2 has committed his update, so
        //     widget 1 is in the DB, but the latency gap prevents us from seeing it in the feed
        // (4) DB returns 3, 4, 2, 1 ; feed returns 3, 4 -- writer 1 has now committed, so now
        //     widget 2 is in the DB, but both are still "dirty", so the feed doesn't see them
        // (5) DB returns 3, 4, 2, 1 ; feed returns 3, 4, 2 -- the update date on widget 2 is now
        //     old enough that the entry is "clean" -- note that this has nothing to do with the
        //     time at which it was COMMITTED - only with when the UPDATE itself occurred
        // (6) DB returns 3, 4, 2, 1, and so does the feed -- now, everything is past the latency
        //     period, and so the feed contains all four entries, in the order that the UPDATEs
        //     occurred (again, NOT in the order that the transactions completed - which is
        //     irrelevant to feed order)



        final List<CheckPoint> checkpoints = new ArrayList<CheckPoint>();
        final String feedUrl = WIDGETS + "/" + ACME + "?start-index=" + updateIndex;

        final TimeLine timeLine = new TimeLine(
                GAP, 2,
                new Runnable() {
                    public void run() {
                        checkpoints.add(new CheckPoint(updateIndex, feedUrl));
                    }
                });

        Callable<Object> writer1 = new Callable<Object>() {
            public Object call() throws Exception {
                timeLine.tick();
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        try {
                            EntryDescriptor entry = new BaseEntryDescriptor(WIDGETS, ACME, "2",
                                                                            US, REVISION_OVERRIDE);
                            entriesDao.updateEntry(entry, false);
                            EntryMetaData emd = entriesDao.getWriteEntriesDAO().selectEntry(entry);
                            contentStorage.revisionChangedWithoutContentChanging(emd);
                            log.debug("::trace-race-condition:: updated 2 to " +
                                               emd.getUpdateTimestamp() + ", " +
                                               emd.getUpdatedDate());
                            timeLine.tick();
                            timeLine.tick();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                log.debug("::trace-race-condition:: committed 2");
                timeLine.tick();
                timeLine.tick();
                return null;
            }
        };

        Callable<Object> writer2 = new Callable<Object>() {
            public Object call() throws Exception {
                timeLine.tick();
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        try {
                            timeLine.tick();
                            EntryDescriptor entry = new BaseEntryDescriptor(WIDGETS, ACME, "1",
                                                                            US, REVISION_OVERRIDE);
                            entriesDao.updateEntry(entry, false);
                            EntryMetaData emd = entriesDao.getWriteEntriesDAO().selectEntry(entry);
                            contentStorage.revisionChangedWithoutContentChanging(emd);
                            log.debug("::trace-race-condition:: updated 1 to " +
                                               emd.getUpdateTimestamp() + ", " +
                                               emd.getUpdatedDate());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                log.debug("::trace-race-condition:: committed 1");
                timeLine.tick();
                timeLine.tick();
                timeLine.tick();
                return null;
            }
        };

        Executors.newFixedThreadPool(2).invokeAll(Arrays.asList(writer1, writer2));

        // this test needed more tiem to complete, when run in the entire test suite.
        Thread.sleep(GAP * 3);

        checkpoints.add(new CheckPoint(updateIndex, feedUrl));

        for (CheckPoint checkPoint : checkpoints) {
            log.debug("::trace-race-condition::FEED");
            log.debug("::trace-race-condition::checkPoint.visible = " + checkPoint.visibleFromDatabase);
            for (Entry e : checkPoint.feedPage.getEntries()) {
                log.debug("::trace-race-condition::ENTRY - " + e.getId() + "; ts=" +
                                   e.getSimpleExtension(AtomServerConstants.UPDATE_INDEX));
            }
        }
        
        assertEquals( 6, checkpoints.size() );

        checkpoints.get(0).doAssert(Arrays.asList("3", "4"),
                                    Arrays.asList("3", "4"));
        checkpoints.get(1).doAssert(Arrays.asList("3", "4"),
                                    Arrays.asList("3", "4"));
        checkpoints.get(2).doAssert(Arrays.asList("3", "4", "1"),
                                    Arrays.asList("3", "4"));
        checkpoints.get(3).doAssert(Arrays.asList("3", "4", "1", "2"),
                                    Arrays.asList("3", "4"));
        checkpoints.get(4).doAssert(Arrays.asList("3", "4", "1", "2"),
                                    Arrays.asList("3", "4", "2"));
        checkpoints.get(5).doAssert(Arrays.asList("3", "4", "1", "2"),
                                    Arrays.asList("3", "4", "2", "1"));
    }


    class CheckPoint {
        private Set<String> visibleFromDatabase;
        private Feed feedPage;

        CheckPoint(String updateIndex, String feedUrl) {
            visibleFromDatabase = new HashSet<String>();
            Connection conn = null;
            try {
                conn = entriesDao.getWriteEntriesDAO().getDataSource().getConnection();
                ResultSet resultSet = conn.createStatement().executeQuery(
                        "SELECT EntryId FROM EntryStore WHERE UpdateTimestamp > " + updateIndex);
                while (resultSet.next()) {
                    visibleFromDatabase.add(resultSet.getString(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            feedPage = getPage(feedUrl);
        }

        void doAssert(List<String> expectedFromDatabase,
                      List<String> expectedFromFeed) {
            if (!"hsql".equals(entriesDao.getDatabaseType())) {
                assertEquals(new HashSet<String>(expectedFromDatabase),
                             visibleFromDatabase);
            }
            assertEquals(expectedFromFeed.size(), feedPage.getEntries().size());
            List<String> fromFeed = new ArrayList<String>();
            for (Entry entry : feedPage.getEntries()) {
                fromFeed.add(entry.getSimpleExtension(AtomServerConstants.ENTRY_ID));
            }
            assertEquals(expectedFromFeed, fromFeed);
        }
    }
}
