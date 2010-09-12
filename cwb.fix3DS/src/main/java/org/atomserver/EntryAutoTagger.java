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

package org.atomserver;

import org.atomserver.core.EntryMetaData;
import org.w3c.dom.Document;

/**
 * EntryAutoTagger - API for automatically applying categories to entries as they are written to
 * the ContnetStorage. Implementations of this interface are wired to an AtomCollection.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface EntryAutoTagger {
    /**
     * Implementations should implement this method to write categories for each entry as it is
     * written.
     * @param entry   The metadata about the entry being written
     * @param content The content of the entry
     * @return a boolean flag set to true if the set of categories has changed and false otherwise.
     */
    boolean tag(EntryMetaData entry, String content);

    // TODO: javadoc
    boolean tag(EntryMetaData entry, Document doc);
}
