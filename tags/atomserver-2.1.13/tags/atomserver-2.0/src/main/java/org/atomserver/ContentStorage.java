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
 * ContentStorage - API for where to actually put the contents of Entries.
 * Implementations of this interface are wired to an AtomCollection.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface ContentStorage {

    /**
     * Get the contents of the Entry described by the descriptor.
     * @param descriptor The descriptor of the entry to retrieve
     * @return The contents of the entry
     */
    String getContent( EntryDescriptor descriptor );

    /**
     * Delete the Entry described by the descriptor.
     * @param deletedContentXml The possible contentXml to write on delete. May be null.
     * @param descriptor the descriptor of the entry to delete
     */
    void deleteContent( String deletedContentXml, EntryDescriptor descriptor  );

    /**
     * Obliterate the entry decribe by the descriptor. This physically removes the file from disk!
     * This method is really only used by JUnits.
     * @param descriptor The descriptor of the entry to obliterate
     */
    void obliterateContent( EntryDescriptor descriptor );
    
    /**
     * Put the given contents into the storage at the position identified by the given descriptor.
     * @param contentXml The contents to put into storage
     * @param descriptor The descriptor of the entry to put
     */
    void putContent( String contentXml, EntryDescriptor descriptor );

    /**
     * Initialize the workspace identified by the supplied workspace name.
     * @param workspace The name of the workspace to initialize
     */
    void initializeWorkspace(String workspace);

    /**
     * Test the availabilty of the ContentStorage. This method is used by the IsAliveHandler
     * to determine if the ContentStorage is up and running. For a FileBasedContentStorage this
     * would test that the NFS mount is accessible, and for DbBasedContentStorage this would check
     * the DB Connection. Note that the method does not bother returning a boolean. Instead, it is assumed
     * that a RuntimeException will be thrown
     */
    void testAvailability();

    /**
     * Verify the the ContentStorage is readable.
     * @return Whether the the ContentStorage is readable.
     */
    boolean canRead();

    /**
     * Does the content identified by the given descriptor exist?
     * @param descriptor The descriptor of the entry to check.
     * @return Whether the content identified by the given descriptor exists
     */
    boolean contentExists(EntryDescriptor descriptor);

    /**
     * Indicates that the revision has changed without actually changing the content.  For a FileBasedContentStorage this
     * might allow a simple copy to a new revision file, and for DbBasedContentStorage this would allow the application
     * to do nothing. This situation occurs, for example, when only the Categories change.
     * @param descriptor The descriptor of the entry to check.
     */
    void revisionChangedWithoutContentChanging(EntryDescriptor descriptor);
}
