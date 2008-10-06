package org.atomserver.core.dbstore;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.atomserver.core.BaseServiceDescriptor;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.core.filestore.FileBasedContentStorage;
import org.atomserver.testutils.conf.TestConfUtil;
import org.atomserver.testutils.latency.LatencyUtil;
import org.atomserver.utils.PartitionPathGenerator;
import org.atomserver.utils.PrefixPartitionPathGenerator;
import org.atomserver.utils.ShardedPathGenerator;
import static org.atomserver.utils.ShardedPathGenerator.DEFAULT_RADIX;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GzipAndShardingMigrationTest extends DBSTestCase {
    private static final ShardedPathGenerator SHARDED_PATH_GENERATOR =
            new ShardedPathGenerator(DEFAULT_RADIX,
                                     2,
                                     DEFAULT_RADIX * DEFAULT_RADIX * DEFAULT_RADIX);

    public void setUp() throws Exception {
        TestConfUtil.preSetup("migration");
        super.setUp();

        entriesDao.deleteAllEntries(new BaseServiceDescriptor("widgets"));
        entriesDao.deleteAllEntries(new BaseServiceDescriptor("dummy"));

        for (int i = 6000; i < 6006; i++) {
            String entryId = "" + i;
            String locale = i % 2 == 0 ? Locale.ENGLISH.toString() : Locale.US.toString();
            createWidget("widgets", "acme", entryId, locale, createWidgetXMLFileString(entryId));
            createWidget("dummy", "dumbo", entryId, null, createWidgetXMLFileString(entryId));
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestConfUtil.postTearDown();
    }

    public void testGzipAndShardingMigration()
            throws Exception {
        // spot-check that some files exist
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6002/en/6002.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6003/en/US/6003.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6004/6004.xml.r0").exists());

        // update widgets/acme/6002, widgets/acme/6003, and dummy/dumbo/6004 once each
        updateWidget("widgets", "acme", "6002",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6002"), "*");
        updateWidget("widgets", "acme", "6003",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6003"), "*");
        updateWidget("dummy", "dumbo", "6004",
                     null,
                     createWidgetXMLFileString("6004"), "*");

        // update widgets/acme/6000, widgets/acme/6001, and dummy/dumbo/6002 twice each
        updateWidget("widgets", "acme", "6000",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6000"), "*");
        updateWidget("widgets", "acme", "6001",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6001"), "*");
        updateWidget("dummy", "dumbo", "6002",
                     null,
                     createWidgetXMLFileString("6002"), "*");
        Thread.sleep(2000); // wait 2 seconds, so we will sweep to the trash
        updateWidget("widgets", "acme", "6000",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6000"), "*");
        updateWidget("widgets", "acme", "6001",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6001"), "*");
        updateWidget("dummy", "dumbo", "6002",
                     null,
                     createWidgetXMLFileString("6002"), "*");

        // for the ones that were modified once, r0 and r1 should still exist
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6002/en/6002.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6003/en/US/6003.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6002/en/6002.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6003/en/US/6003.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6004/6004.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6004/6004.xml.r1").exists());

        // for the ones that were modified twice, r1 and r2 should still exist, and r0 should be trashed
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6000/en/6000.xml.r0").exists());
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6001/en/US/6001.xml.r0").exists());
        assertFalse(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6002/6002.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6000/en/6000.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6001/en/US/6001.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6002/6002.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6000/en/6000.xml.r2").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6001/en/US/6001.xml.r2").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6002/6002.xml.r2").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6000/en/_trash/6000.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6001/en/US/_trash/6001.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6002/_trash/6002.xml.r0").exists());

        // change the gzipping and sharding settings
        ((FileBasedContentStorage) contentStorage).setGzipEnabled(true);

        // failing in cobertura
        //((FileBasedContentStorage) contentStorage).setPartitionPathGenerators(
        //       Arrays.asList(SHARDED_PATH_GENERATOR, new PrefixPartitionPathGenerator()));
        List<PartitionPathGenerator> pathGenerators =
                Arrays.asList(SHARDED_PATH_GENERATOR, new PrefixPartitionPathGenerator());
        ((FileBasedContentStorage) contentStorage).setPartitionPathGenerators(pathGenerators);                

        Thread.sleep(2000); // wait 2 seconds, so we will sweep to the trash

        // insert some new entries
        createWidget("widgets", "acme", "7000", Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("7000"));
        createWidget("widgets", "acme", "7001", Locale.US.toString(),
                     createWidgetXMLFileString("7001"));
        createWidget("dummy", "dumbo", "8000", null,
                     createWidgetXMLFileString("8000"));

        // update some entries that were at r0 (widgets/acme/6004, widgets/acme/6005, dummy/dumbo/6005)
        updateWidget("widgets", "acme", "6004",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6004"), "*");
        updateWidget("widgets", "acme", "6005",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6005"), "*");
        updateWidget("dummy", "dumbo", "6005",
                     null,
                     createWidgetXMLFileString("6005"), "*");

        // update some entries that were at r1 (widgets/acme/6002, widgets/acme/6003, dummy/dumbo/6004)
        updateWidget("widgets", "acme", "6002",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6002"), "*");
        updateWidget("widgets", "acme", "6003",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6003"), "*");
        updateWidget("dummy", "dumbo", "6004",
                     null,
                     createWidgetXMLFileString("6004"), "*");

        // update some entries that were at r2 (widgets/acme/6000, widgets/acme/6001, dummy/dumbo/6002)
        updateWidget("widgets", "acme", "6000",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6000"), "*");
        updateWidget("widgets", "acme", "6001",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6001"), "*");
        updateWidget("dummy", "dumbo", "6002",
                     null,
                     createWidgetXMLFileString("6002"), "*");

        generateShardedDir("widgets/acme", "6000", "en/6000.xml.gz");

        // check the new entries
        assertTrue(generateShardedDir("widgets/acme", "7000", "7000/en/7000.xml.r0.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "7001", "7001/en/US/7001.xml.r0.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "8000", "8000/8000.xml.r0.gz").exists());

        // check the ones that migrated from r0
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6004/en/6004.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6005/en/US/6005.xml.r0").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6005/6005.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6004", "6004/en/6004.xml.r1.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6005", "6005/en/US/6005.xml.r1.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6005", "6005/6005.xml.r1.gz").exists());

        // check the ones that migrated from r1
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6002/en/6002.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6003/en/US/6003.xml.r1").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6004/6004.xml.r1").exists());
        assertTrue(generateShardedDir("widgets/acme", "6002", "6002/en/6002.xml.r2.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6003", "6003/en/US/6003.xml.r2.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6004", "6004/6004.xml.r2.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6002", "6002/en/_trash/6002.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6003", "6003/en/US/_trash/6003.xml.r0").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6004", "6004/_trash/6004.xml.r0").exists());

        // check the ones that migrated from r2
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6000/en/6000.xml.r2").exists());
        assertTrue(new File(TEST_DATA_DIR, "/widgets/acme/60/6001/en/US/6001.xml.r2").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6002/6002.xml.r2").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/6000.xml.r3.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/6001.xml.r3.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/6002.xml.r3.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/_trash/6000.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/_trash/6001.xml.r0").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/_trash/6002.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/_trash/6000.xml.r1").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/_trash/6001.xml.r1").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/_trash/6002.xml.r1").exists());

        Thread.sleep(2000); // wait 2 seconds, so we will sweep to the trash

        // update all of the "migrated" entries one more time -- this should "clean up" the old directories
        updateWidget("widgets", "acme", "6004",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6004"), "*");
        updateWidget("widgets", "acme", "6005",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6005"), "*");
        updateWidget("dummy", "dumbo", "6005",
                     null,
                     createWidgetXMLFileString("6005"), "*");
        updateWidget("widgets", "acme", "6002",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6002"), "*");
        updateWidget("widgets", "acme", "6003",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6003"), "*");
        updateWidget("dummy", "dumbo", "6004",
                     null,
                     createWidgetXMLFileString("6004"), "*");
        updateWidget("widgets", "acme", "6000",
                     Locale.ENGLISH.toString(),
                     createWidgetXMLFileString("6000"), "*");
        updateWidget("widgets", "acme", "6001",
                     Locale.US.toString(),
                     createWidgetXMLFileString("6001"), "*");
        updateWidget("dummy", "dumbo", "6002",
                     null,
                     createWidgetXMLFileString("6002"), "*");

        LatencyUtil.updateLastWrote();

        // check the ones that migrated from r0
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6004").exists());
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6005").exists());
        assertFalse(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6005").exists());
        assertTrue(generateShardedDir("widgets/acme", "6004", "6004/en/6004.xml.r1.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6005", "6005/en/US/6005.xml.r1.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6005", "6005/6005.xml.r1.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6004", "6004/en/6004.xml.r2.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6005", "6005/en/US/6005.xml.r2.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6005", "6005/6005.xml.r2.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6004", "6004/en/_trash/6004.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6005", "6005/en/US/_trash/6005.xml.r0").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6005", "6005/_trash/6005.xml.r0").exists());

        // check the ones that migrated from r1
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6002").exists());
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6003").exists());
        assertFalse(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6004").exists());
        assertTrue(generateShardedDir("widgets/acme", "6002", "6002/en/6002.xml.r2.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6003", "6003/en/US/6003.xml.r2.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6004", "6004/6004.xml.r2.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6002", "6002/en/6002.xml.r3.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6003", "6003/en/US/6003.xml.r3.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6004", "6004/6004.xml.r3.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6002", "6002/en/_trash/6002.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6003", "6003/en/US/_trash/6003.xml.r0").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6004", "6004/_trash/6004.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6002", "6002/en/_trash/6002.xml.r1").exists());
        assertTrue(generateShardedDir("widgets/acme", "6003", "6003/en/US/_trash/6003.xml.r1").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6004", "6004/_trash/6004.xml.r1").exists());

        // check the ones that migrated from r2
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6000").exists());
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60/6001").exists());
        assertFalse(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6002").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/6000.xml.r3.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/6001.xml.r3.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/6002.xml.r3.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/6000.xml.r4.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/6001.xml.r4.gz").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/6002.xml.r4.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/_trash/6000.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/_trash/6001.xml.r0").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/_trash/6002.xml.r0").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/_trash/6000.xml.r1").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/_trash/6001.xml.r1").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/_trash/6002.xml.r1").exists());
        assertTrue(generateShardedDir("widgets/acme", "6000", "6000/en/_trash/6000.xml.r2").exists());
        assertTrue(generateShardedDir("widgets/acme", "6001", "6001/en/US/_trash/6001.xml.r2").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6002", "6002/_trash/6002.xml.r2").exists());

        // at this point, we have migrated ALL of the widgets under 60..., but not all the dummies
        assertFalse(new File(TEST_DATA_DIR, "/widgets/acme/60").exists());
        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60").exists());

        LatencyUtil.accountForLatency();

        // finally, just sanity check that the feeds still contain the exact data that we've
        // published to them...
        Feed feed;
        feed = getPage("widgets/acme?locale=en&entry-type=full");
        assertEquals(4, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryId = entry.getSimpleExtension(AtomServerConstants.ENTRY_ID);
            assertEquals(createWidgetXMLFileString(entryId), entry.getContent());
        }

        feed = getPage("widgets/acme?locale=en_US&entry-type=full");
        assertEquals(4, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryId = entry.getSimpleExtension(AtomServerConstants.ENTRY_ID);
            assertEquals(createWidgetXMLFileString(entryId), entry.getContent());
        }

        feed = getPage("dummy/dumbo?entry-type=full");
        assertEquals(7, feed.getEntries().size());
        for (Entry entry : feed.getEntries()) {
            String entryId = entry.getSimpleExtension(AtomServerConstants.ENTRY_ID);
            assertEquals(createWidgetXMLFileString(entryId), entry.getContent());
        }

        deleteEntry2("http://localhost:60080" +
                     widgetURIHelper.constructURIString("dummy", "dumbo", "6001", null) + "/*");
        deleteEntry2("http://localhost:60080" +
                     widgetURIHelper.constructURIString("widgets", "acme", "7000", Locale.ENGLISH) + "/*");

        assertTrue(new File(TEST_DATA_DIR, "/dummy/dumbo/60/6001/6001.xml.r0").exists());
        assertTrue(generateShardedDir("dummy/dumbo", "6001", "6001/6001.xml.r1.gz").exists());

        assertTrue(generateShardedDir("widgets/acme", "7000", "7000/en/7000.xml.r0.gz").exists());
        assertTrue(generateShardedDir("widgets/acme", "7000", "7000/en/7000.xml.r1.gz").exists());        
    }

    private File generateShardedDir(String collectionDir, String entryId, String subPath) {
        return new File(
                SHARDED_PATH_GENERATOR.generatePath(
                        new File(TEST_DATA_DIR, collectionDir),
                        entryId),
                subPath);
    }
}
