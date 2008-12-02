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


package org.atomserver.core.filestore;

import org.atomserver.core.AbstractAtomService;
import org.atomserver.AtomWorkspace;
import org.atomserver.AtomService;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class FileBasedAtomService extends AbstractAtomService {

    FileBasedContentStorage contentStorage;

    public long getMaxIndex() {
        return -1;
    }

    public void setContentStorage(FileBasedContentStorage contentStorage) {
        this.contentStorage = contentStorage;
    }

    public FileBasedContentStorage getContentStorage() {
        return contentStorage;
    }

    public AtomWorkspace newAtomWorkspace(AtomService parentService, String name) {
        return new FileBasedAtomWorkspace(parentService, name);
    }

}
