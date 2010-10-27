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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PrefixPartitionPathGenerator - implemenation of PartitionPathGenerator that uses the leftmost N characters to partition.
 * <p/>
 * This is a very simple PartitionPathGenerator that simply creates one-level-deep partitioning
 * based on the leftmost N characters of the seed.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class PrefixPartitionPathGenerator implements PartitionPathGenerator {

    private static final int DEFAULT_NUM_CHARS = 2;
    private int numChars;
    private Pattern pattern;

    /**
     * construct a PrefixPartitionPathGenerator with the default N.
     */
    public PrefixPartitionPathGenerator() {
        this(DEFAULT_NUM_CHARS);
    }

    /**
     * construct a PrefixPartitionPathGenerator to use the given number of characters.
     *
     * @param numChars the number of characters to use for generating the partition directories.
     */
    public PrefixPartitionPathGenerator(int numChars) {
        if (numChars < 1) {
            throw new IllegalArgumentException("you must use a positive number of characters to " +
                                               "partition directories.");
        }
        this.numChars = numChars;
        this.pattern = Pattern.compile("/?([^/]{" + numChars + "})/(\\1[^/]*)(.*)");
    }

    /**
     * {@inheritDoc}
     */
    public File generatePath(File parent, String seed) {
        return new File(parent, seed.substring(0, Math.min(numChars, seed.length())));
    }

    public ReverseMatch reverseMatch(File root, File file) {
        String filePath = file.getAbsolutePath();
        String rootPath = root.getAbsolutePath();
        String relativePath = filePath.replace(rootPath, "");
        final Matcher matcher = pattern.matcher(relativePath);
        return matcher.matches() ?
               new ReverseMatch() {
                   public String getPartition() {
                       return matcher.group(1);
                   }

                   public String getSeed() {
                       return matcher.group(2);
                   }

                   public String getRest() {
                       return matcher.group(3);
                   }
               } :
                 null;
    }
}
