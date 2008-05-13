/* Copyright (c) 2007 HomeAway, Inc.
 *  All rights reserved.  http://www.atomserver.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atomserver.utils;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import static java.lang.Character.MAX_RADIX;
import static java.lang.Character.MIN_RADIX;

/**
 * ShardedPathGenerator - a hashing algorithm to generate a path of partition directories from a String seed.
 * <p/>
 * this class constructs a sharded path based on a filename, giving the caller control over how
 * many levels of depth they want in their paths, and how many file nodes, at most, can be created
 * in the intermediate shard directories.  This algorithm is repeatable, so lookup can be done
 * later by simply constructing a new ShardedPathGenerator with the same inputs as when the file was
 * created.
 * <p/>
 * if you assume that you will have N files, and you want to have no more than M file nodes per
 * directory, you can accomplish this by using M as your nodesPerLevel in the constructor, and
 * using (N/M)*2 as the depth.  This does not "guarantee" that you will never have more than M
 * nodes in a terminal node, but it guarantees that no intermediate nodes will ever have more
 * than M, and makes it statistically improbable that terminal nodes will accumulate more than M
 * files in any one directory.  Using (N/M)*2 for the depth makes the likelihood of there being a
 * directory with more than M file nodes around 2% (if my memory of standard deviation serves me),
 * and in any case you can achieve an arbitrarily small likelihood of collisions by increasing the
 * depth.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class ShardedPathGenerator implements PartitionPathGenerator {
    // the radix that we use for the shards is the maximum radix allowed by Integer.toString
    public static final int DEFAULT_RADIX = MAX_RADIX;
    // by default, paths are one sharded one level deep
    public static final int DEFAULT_DEPTH = 1;
    // we set the default nodes per level to what can be represented by a two-ngit base-36
    // number (36^2 = 1,296)
    public static final int DEFAULT_NODES_PER_LEVEL = DEFAULT_RADIX * DEFAULT_RADIX;

    private final int radix;
    private final int depth;
    private final int nodesPerLevel;

    /**
     * construct a new ShardedPathGenerator with default values.
     */
    public ShardedPathGenerator() {
        this(DEFAULT_RADIX, DEFAULT_DEPTH, DEFAULT_NODES_PER_LEVEL);
    }

    /**
     * construct a new ShardedPathGenerator with the given radix, depth, and nodesPerLevel.
     *
     * @param radix         the base to use for converting numbers to Strings
     * @param depth         the depth of the tree of directories
     * @param nodesPerLevel the maximum number of subnodes in all intermediate levels
     */
    public ShardedPathGenerator(int radix,
                                int depth,
                                int nodesPerLevel) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            throw new IllegalArgumentException("radix " + radix + " is out of bounds.  " +
                                               "use a value in the range [" + MIN_RADIX + "," +
                                               MAX_RADIX + "].");
        }
        this.radix = radix;

        if (depth < 1) {
            throw new IllegalArgumentException("non positive depth not allowed.");
        }
        this.depth = depth;

        if (nodesPerLevel < radix) {
            throw new IllegalArgumentException("smaller nodes per level than radix makes no sense -" +
                                               "generally nodes per level would be on the order of " +
                                               "radix^2 or higher.  make sure you understand how to " +
                                               "use the ShardedPathGenerator.");
        }
        this.nodesPerLevel = nodesPerLevel;
    }

    /**
     * {@inheritDoc}
     */
    public File generatePath(File parent, String seed) {
        String[] shards = new String[depth];
        int hash = hash(seed);
        for (int i = 0; i < depth; i++) {
            shards[i] = Integer.toString(
                    Math.abs(hash * prime(i)) % nodesPerLevel, radix);
        }
        return new File(parent, StringUtils.join(shards, "/"));
    }

    private static final int[] PRIMES = {8675309, 16661, 17};

    /**
     * return the ith "approximately" prime number.
     *
     * @param i which number in the approximately prime sequence to return
     * @return a number that is "approximately" prime
     */
    private static int prime(int i) {
        return prime(i % PRIMES.length, i / PRIMES.length);
    }

    /**
     * recursive function to compute the ith "approximately" prime integer.
     * <p/>
     * this function assumes that i is in the range [0, PRIMES.length).  if page is 0, then
     * we simply return a lookup (PRIMES[i]) - otherwise, we return (2n - 1) where n is
     * prime(i, page-1).
     *
     * @param i    the index into the PRIMES array for ultimately doing the lookup.
     * @param page which page to return (i.e. how many levels to recurse)
     * @return the "approximately" prime number
     */
    private static int prime(int i, int page) {
        return page == 0 ? PRIMES[i] : 2 * prime(i, page - 1) - 1;
    }

    /**
     * generate a stable, nicely distributed hash code from String.
     * <p/>
     * this is identical to the algoritm used by String.hashCode - but since that may change,
     * we have re-implmented it here so that this is stable across java versions
     *
     * @param s the String to hash
     * @return the hash
     */
    private static int hash(String s) {
        int hash = 0;
        for (int i = 0; i < s.length(); i++) {
            hash = hash * 31 + s.charAt(i);
        }
        return hash;
    }
}
