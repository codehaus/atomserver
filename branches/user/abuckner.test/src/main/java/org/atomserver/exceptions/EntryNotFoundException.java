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

package org.atomserver.exceptions;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class EntryNotFoundException extends AtomServerException {

    public enum EntryNotFoundType { DELETE, GET; }

    private EntryNotFoundType type;

    public EntryNotFoundException(EntryNotFoundType type) {
        super();
        this.type = type;
    }

    public EntryNotFoundException(EntryNotFoundType type, String message) {
        super(message);
        this.type = type;
    }

    public EntryNotFoundException(EntryNotFoundType type, Throwable cause) {
        super(cause);
        this.type = type;
    }

    public EntryNotFoundException(EntryNotFoundType type, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
    }

    public EntryNotFoundType getType() {
        return type;
    }
}
