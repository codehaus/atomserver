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

/**
 * PartitionPathGenerator - an interface representing an algorithm for partitioning a directory.
 * <p/>
 * This interface defines a set of algorithms that can be used to "partition" a root directory into
 * a nested tree of subdirectories, generally to prevent there from being too many file nodes at
 * any level of the hierarchy.
 *
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface PartitionPathGenerator {

    /**
     * return a File object representing the partitioned path for the given seed, rooted at the given root.
     *
     * implementations of this class should return a File object with the given parent as the
     * root, and with a path of subdirectories beneath it that is stable across multiple calls
     * - that is, if the generatePath method is called multiple times with the same parent (or a
     * parent that represents the same abstract path) and the same seed, it should return a path
     * that represents the same abstract path. 
     *
     * @param parent the root from which to generate a path
     * @param seed   the seed to use for generating the path
     * @return a File representing the partitioned path
     */
    public File generatePath(File parent, String seed);

    public ReverseMatch reverseMatch(File root, File file);

    public interface ReverseMatch {
        String getPartition();
        String getSeed();
        String getRest();
    }
}
