package org.atomserver.utils;

import junit.framework.TestCase;

import java.io.File;

public class ShardedPathTest extends TestCase {

    static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));


    public void testShardedPath() throws Exception {

        check("test.txt", 2, ShardedPathGenerator.DEFAULT_NODES_PER_LEVEL,
              new File(TMP_DIR, "s/hg").getAbsolutePath());
        check("test.txt", 4, ShardedPathGenerator.DEFAULT_NODES_PER_LEVEL,
              new File(TMP_DIR, "s/hg/78/38").getAbsolutePath());
        check("test.txt", 3, 1000,
              new File(TMP_DIR, "7o/a4/i4").getAbsolutePath());
        check("test.txt", 1, 1000000,
              new File(TMP_DIR, "4nng").getAbsolutePath());
        check("test.xml", 2, ShardedPathGenerator.DEFAULT_NODES_PER_LEVEL,
              new File(TMP_DIR, "z5/6v").getAbsolutePath());
        check("test.xml", 4, ShardedPathGenerator.DEFAULT_NODES_PER_LEVEL,
              new File(TMP_DIR, "z5/6v/1n/hp").getAbsolutePath());
        check("test.xml", 3, 1000,
              new File(TMP_DIR, "ax/fj/1v").getAbsolutePath());
        check("test.xml", 1, 1000000,
              new File(TMP_DIR, "gsgx").getAbsolutePath());
    }

    private void check(String name, int depth, int nodesPerLevel, String expected) {
        PartitionPathGenerator pathGenerator =
                new ShardedPathGenerator(ShardedPathGenerator.DEFAULT_RADIX, depth, nodesPerLevel);
        assertEquals(expected,
                     pathGenerator.generatePath(TMP_DIR, name).getAbsolutePath());
    }
}
