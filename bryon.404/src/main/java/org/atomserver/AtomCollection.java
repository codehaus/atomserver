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

import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.atomserver.core.CollectionOptions;
import org.atomserver.exceptions.AtomServerException;

/**
 * AtomCollection - A container within which Atom Entries are stored.
 * In AtomPub, a collection of Entries is called a Feed. An AtomCollection is responsible for returning
 * either Feeds or individual Entries, depending on the Request URL.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public interface AtomCollection {

    /**
     * Return the AtomWorkspace to which this AtomCollection belongs.
     * @return The AtomWorkspace to which this AtomCollection belongs
     */
    AtomWorkspace getParentAtomWorkspace();

    /**
     * Return the name associated with this AtomCollection
     * @return The name associated with this AtomCollection
     */
    String getName();

    /**
     * Return the list of Abdera Categories for this Request.
     * @param request The Abdera RequestContext, which contains the URL and all HTTP Headers.
     * @return The list of Abdera Categories for this AtomCollection
     * @throws AtomServerException
     */
    java.util.Collection<Category> listCategories(RequestContext request)
            throws AtomServerException;

    /**
     * Return the Abdera Feed object (i.e. a collection of Entries) associated with this GET Request.
     * This is a list of all entries that have been modified since IfModifiedSince.
     * Returns null when no entries have been found.  It is likely that this method will return only a "page"
     * of results. Query parameters such as start-index, max-results, and updated-min will have a significant
     * effect on which Entries are returned, if any, within the Feed.
     * <p/>
     * Note that the AtomServer (actually the URIHandler) has already decoded the URL enough to know
     * that the URL refers to a Feed request, but we must pass along the RequestContext because
     * other parts of the RequestContext may be required for processing (Headers, etc.)
     * @param request The Abdera RequestContext, which contains the URL and all HTTP Headers.
     * @return The Abdera Feed object
     */
    Feed getEntries(RequestContext request);

    /**
     * Return the Abdera Entry object associated with this GET Request.
     * Returns null when the entry is not modified since ifModifiedSince.
     * <p/>
     * Note that the AtomServer (actually the URIHandler) has already decoded the URL enough to know
     * that the URL refers to an Entry request, but we must pass along the RequestContext because
     * other parts of the RequestContext may be required for processing (Headers, etc.)
     * <p/>
     * Note also that if you submit a GET to a URL which contains a revision number (i.e "/widgets/acme/123.xml/0"),
     * then you <b>must</b> request the correct, current revision, <i>or you will receive a 404 NOT FOUND</i>.
     * This URL would correspond to the "self" link available in the Entry. Because of this restriction, it is
     * generally advisable to submit GETs to a URL without a revision number (i.e "/widgets/acme/123.xml").
     * @param request The Abdera RequestContext, which contains the URL and all HTTP Headers.
     * @return The Abdera Entry object.
     * @throws AtomServerException
     */
    Entry getEntry(RequestContext request)
            throws AtomServerException;

    /**
     * This method is used to both create and update an Entry.
     * <ol>
     * <li>For PUT requests (to a Entry URL), it determines whether the Entry already exists and if not creates it,
     * otherwise it updates the existing Entry.</li>
     * <li>For POST requests (to a Feed URL), it creates the Entry and in doing so, creates an Entry Id.</li>
     * </ol>
     * This method returns a CreateOrUpdateEntry object, which is
     * simply a wrapper around the Abdera Entry, with the additional information of whether an update or create occurred.
     * (This information is required to determine whether a 200 or a 201 should be returned)
     * <p/>
     * Note that the AtomServer (actually the URIHandler) has already decoded the URL enough to know
     * that the URL refers to an update Entry request, but we must pass along the RequestContext because
     * other parts of the RequestContext may be required for processing (Headers, etc.)
     * <p/>
     * Note also that the when using Optimistic Concurency, you <b>must</b> submit <i>the revision you intend
     * to write</i>. This would correspond to the "edit" link available in the Entry. E.g. if the current Entry
     * was at, say, "/widgets/acme/123.xml/4", then you must submit the PUT to "/widgets/acme/123.xml/5".
     * <p/>
     * When doing the "initial PUT" (i.e. the initial Entry creation), and you are using Optimistic Concurency,
     * you should PUT to either "/0" or leave off the revision number altogether.  E.g. either, say,
     * "/widgets/acme/123.xml/0" or "/widgets/acme/123.xml".
     * @param request The Abdera RequestContext, which contains the URL and all HTTP Headers.
     * @return CreateOrUpdateEntry which
     * @throws AtomServerException
     */
    UpdateCreateOrDeleteEntry.CreateOrUpdateEntry updateEntry(RequestContext request)
            throws AtomServerException;

    /**
     * Return the Abdera Entry object associated with this DELETE Request, which indicates the deletion of an Entry.
     * <p/>
     * Note that the AtomServer (actually the URIHandler) has already decoded the URL enough to know
     * that the URL refers to a delete Entry request, but we must pass along the RequestContext because
     * other parts of the RequestContext may be required for processing (Headers, etc.)
     * @param request The Abdera RequestContext, which contains the URL and all HTTP Headers.
     * <p/>
     * Note that the when using Optimistic Concurency, you <b>must</b> submit <i>the revision you intend
     * to write</i>. This would correspond to the "edit" link available in the Entry. E.g. if the current Entry
     * was at, say, "/widgets/acme/123.xml/4", then you must submit the DELETE to "/widgets/acme/123.xml/5".

     * @return An Abdera Entry object
     * @throws AtomServerException
     */
    Entry deleteEntry(RequestContext request)
            throws AtomServerException;

    /**
     * Handles a "batched operations" request.
     * Returns a list of UpdateCreateOrDeleteEntry objects, which are simply wrappers
     * around Abdera Entries with the additional information on whether that Entry was updated,
     * created, or deleted. This additional information is used to determine the appropriate HTTP status code
     * and in the case of deletes, the form of actual XML response. 
     * <p/>
     * Note that the AtomServer (actually the URIHandler) has already decoded the URL enough to know
     * that the URL refers to an update Entry request, but we must pass along the RequestContext because
     * other parts of the RequestContext may be required for processing (Headers, etc.)
     * @param request The Abdera RequestContext, which contains the URL and all HTTP Headers.
     * @return A list of UpdateCreateOrDeleteEntry objects
     * @throws AtomServerException
     */
    java.util.Collection<UpdateCreateOrDeleteEntry> updateEntries(RequestContext request)
            throws AtomServerException;

    /**
     * Returns the ContentStorage associated with this AtomCollection
     * @return The ContentStorage associated with this AtomCollection
     */
    ContentStorage getContentStorage();

    /**
     * Returns the ContentValidator associated with this AtomCollection
     * @return The ContentValidator associated with this AtomCollection
     */
    ContentValidator getContentValidator();

    /**
     * Returns the CategoriesHandler associated with this AtomCollection
     * @return The CategoriesHandler associated with this AtomCollection
     */
    CategoriesHandler getCategoriesHandler();

    /**
     * Returns the EntryAutoTagger associated with this AtomCollection
     * @return The EntryAutoTagger associated with this AtomCollection
     */
    EntryAutoTagger getAutoTagger();

    /**
     * Returns the CollectionOptions associated with this AtomCollection
     * @return The CollectionOptions associated with this AtomCollection
     */
    CollectionOptions getOptions();

    /**
     * Sets the CollectionOptions associated with this AtomCollection. This is how a AtomCollection is provisioned.
     * Most likely, this method is called by the IOC container (e.g. Spring)
     * @param options The CollectionOptions associated with this AtomCollection
     */
    void setOptions(CollectionOptions options);

    /**
     * Ensure that the collection associated with collectionName exists
     * @param collectionName The collection name to ensure exists.
     */
    void ensureCollectionExists(String collectionName);
}
