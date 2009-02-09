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

import org.apache.abdera.model.Entry;

/**
 * UpdateCreateOrDeleteEntry - A simple wrapper class for Entries, which allows the updateEntry() method
 * of the AtomCollection to send back the information to AtomServer about whether the Entry was either created of updated,
 * so that a 200 or 201 can retrned. This class is also used by the "batch methods" to indicate whether a delete
 * occured as well, and to return Exceptions which may have occurred, which allows AtomServer to create the apropriate
 * batch response XML.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface UpdateCreateOrDeleteEntry {
    Entry getEntry();
    boolean isNewlyCreated();
    boolean isDeleted();
    Exception getException();

    static abstract class BaseUpdateOrDeleteEntry implements UpdateCreateOrDeleteEntry {
        private Entry entry = null;
        private Exception exception = null;
        public BaseUpdateOrDeleteEntry( Entry entry) {
            this.entry = entry;
        }
        public Entry getEntry()
        { return entry; }
        public boolean isNewlyCreated() {
            return false;
        }
        public boolean isDeleted() {
            return false;
        }
        public Exception getException() {
            return exception;
        }
        public void setException(Exception exception) {
            this.exception = exception;
        }
    }

    public static class CreateOrUpdateEntry extends BaseUpdateOrDeleteEntry {
        private boolean isNewlyCreated = false;

        public CreateOrUpdateEntry( Entry entry, boolean isNewlyCreated ) {
            super(entry);
            this.isNewlyCreated = isNewlyCreated;
        }
        public boolean isNewlyCreated()
        { return isNewlyCreated; }
    }

    public static class DeleteEntry extends BaseUpdateOrDeleteEntry {
        public DeleteEntry(Entry entry) {
            super(entry);
        }
        public boolean isDeleted() {
            return true;
        }
    }
}
