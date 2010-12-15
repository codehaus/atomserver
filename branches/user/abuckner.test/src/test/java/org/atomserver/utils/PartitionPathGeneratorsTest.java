package org.atomserver.utils;

import junit.framework.TestCase;

import java.io.File;

public class PartitionPathGeneratorsTest extends TestCase {
    private static final String SLASH = "/";

    public void testPrefixPartitionPathGenerator() throws Exception {
        doGeneratorTest(new PrefixPartitionPathGenerator(),
                        "myseed", "my", "notmyseed", "en/US/myseed.xml.r0");
    }

    public void testShardedPartitionPathGenerator() throws Exception {
        doGeneratorTest(new ShardedPathGenerator(),
                        "myseed", "95", "notmyseed", "en/US/myseed.xml.r0");
        doGeneratorTest(new ShardedPathGenerator(16, 3, 65536),
                        "myseed", "bd9/7d61/7813", "notmyseed", "en/US/myseed.xml.r0");
    }


    private void doGeneratorTest(PartitionPathGenerator generator, String seed, String partition, String notseed, String rest) {
        File root = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(generator.generatePath(root, seed), seed);

        assertEquals(file, new File(root, partition +
                                          SLASH +
                                          seed));

        PartitionPathGenerator.ReverseMatch match = generator.reverseMatch(root, file);
        assertEquals(partition, match.getPartition());
        assertEquals(seed, match.getSeed());
        assertEquals("", match.getRest());

        match = generator.reverseMatch(root, new File(file, rest));
        assertEquals(partition, match.getPartition());
        assertEquals(seed, match.getSeed());
        assertEquals(SLASH + rest, match.getRest());

        match = generator.reverseMatch(root, new File(root, partition + SLASH + notseed));
        assertNull(match);

        match = generator.reverseMatch(root, new File(root, partition + SLASH + notseed + SLASH + rest));
        assertNull(match);
    }
}
