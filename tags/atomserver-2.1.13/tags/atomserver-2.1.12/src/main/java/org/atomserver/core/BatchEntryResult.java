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

package org.atomserver.core;

import org.atomserver.uri.EntryTarget;

/**
 * BatchEntryResult - a simple wrapper for information necessary for creating an Entry result
 * in a batch response document.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class BatchEntryResult {

    private EntryTarget entryTarget;
    private EntryMetaData metaData;
    private Exception exception;

    public BatchEntryResult(EntryTarget entryTarget,
                            EntryMetaData metaData) {
        this(entryTarget, metaData, null);
    }

    public BatchEntryResult(EntryTarget entryTarget,
                            Exception exception) {
        this(entryTarget, null, exception);
    }

    public BatchEntryResult(EntryTarget entryTarget,
                            EntryMetaData metaData,
                            Exception exception) {
        this.entryTarget = entryTarget;
        this.metaData = metaData;
        this.exception = exception;
    }

    public EntryTarget getEntryTarget() {
        return entryTarget;
    }

    public EntryMetaData getMetaData() {
        return metaData;
    }

    public Exception getException() {
        return exception;
    }
}
