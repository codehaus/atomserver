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

import org.atomserver.core.AbstractAtomWorkspace;
import org.atomserver.AtomService;
import org.atomserver.AtomCollection;
import org.atomserver.AtomWorkspace;

import java.util.List;

/**
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class FileBasedAtomWorkspace extends AbstractAtomWorkspace {

    public AtomCollection newAtomCollection(AtomWorkspace parentWorkspace, String collectionName) {
        return new FileBasedAtomCollection(parentWorkspace, collectionName);
    }

    public FileBasedAtomWorkspace( AtomService parentAtomService, String name ) {
        super( parentAtomService, name );
    }

    public void bootstrap() {
        // do nothing;
    }

    public List<String> listCollections() {
        return ((FileBasedAtomService)getParentAtomService()).getContentStorage().listCollections(getName());
    }

    public void createCollection(String collection) {
        ((FileBasedAtomService)getParentAtomService()).getContentStorage().createCollection(getName(), collection);
    }
}
