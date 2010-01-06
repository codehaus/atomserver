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

import java.util.Locale;

/**
 * EntryDescriptor - An API that describes the fundamental things that identify an Entry in an AtomServer - the
 * (workspace, collection, entryId, locale, revision) tuple. Locales may, or may not,
 * be required, depending on whether the isLocalized property is set for the AtomCollection.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface EntryDescriptor {

    static public final int UNDEFINED_REVISION = -111;

    String getWorkspace();

    String getCollection();

    String getEntryId();

    Locale getLocale();

    int getRevision();

    String getContentHashCode();
}
