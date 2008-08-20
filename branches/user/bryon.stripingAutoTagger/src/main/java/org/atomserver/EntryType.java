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

/**
 * EntryType - An enumeration which defines whether Entries in a Feed should be "full" or "link".
 * <p/>
 * A "link" Entry is a an <entry> which contains a <link> which the Client may subsequently use to access the
 * actual Entry (which would contain the Entry's <content>)
 * <p/>
 * A "full" Entry is a an <entry> which contains the entire Entry, including it's <content>.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public enum EntryType {
    full, link
}
