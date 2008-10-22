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
package org.atomserver.core.idgenerators;

import org.atomserver.EntryIdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.util.UUID;

/**
 * UUIDEntryIdGenerator - generates a UUID for the EntryId
 */
public class UUIDEntryIdGenerator implements EntryIdGenerator {
    static private Log log = LogFactory.getLog(EntryIdGenerator.class);

    public String generateId() {
        String uuid = UUID.randomUUID().toString();
        // the limit is 32 chars, and uuid has "-" chars in it
        uuid = StringUtils.remove(uuid, "-");
        log.debug( "Created new EntryId = " + uuid );
        return uuid;
    }
}
