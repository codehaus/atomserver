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

import org.atomserver.EntryDescriptor;
import org.atomserver.*;
import org.atomserver.uri.EntryTarget;
import org.atomserver.uri.FeedTarget;
import org.atomserver.core.EntryMetaData;
import org.atomserver.core.AbstractAtomCollection;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.EntryNotFoundException;
import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Category;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Collection;

/**
 * A Store implementation that provides a Feed from the file-system.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class FileBasedAtomCollection
        extends AbstractAtomCollection {
    private static final Log log = LogFactory.getLog(FileBasedAtomCollection.class);

    public FileBasedAtomCollection( AtomWorkspace parentAtomWorkspace, String name ) {
        super( parentAtomWorkspace, name );
    }

    // FIXME
    public Collection<Category> listCategories(RequestContext request) throws AtomServerException {
        return null;
    }

    //--------------------------------
    //      public methods
    //--------------------------------
    protected long getEntries(Abdera abdera,
                              IRI iri,
                              FeedTarget feedTarget,
                              long ifModifiedSince,
                              Feed feed) throws AtomServerException {
        log.debug("FileBasedAtomCollection::getEntries");

        String workspace = feedTarget.getWorkspace();
        File workspaceDir = new File(((FileBasedContentStorage) getContentStorage()).getRootDir(),
                                     workspace);
        String collection = feedTarget.getCollection();

        feed.addLink(iri.getPath(), "self");

        FileBasedAtomCollection.LastModified lastModified = new FileBasedAtomCollection.LastModified();
        boolean foundModifiedFile = loadFeedEntries(abdera, iri, workspace, collection, workspaceDir, collection,
                                                    feed, lastModified, ifModifiedSince);
        return (foundModifiedFile) ? lastModified.getTime() : 0L;
    }

    /**
     */
    protected EntryMetaData getEntry(
            EntryTarget entryTarget)
            throws AtomServerException {
        log.debug("FileBasedAtomCollection::getEntry");
        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();

        if ( !contentExists(workspace, entryTarget) ) {
            throw new EntryNotFoundException("Property [" + collection + ", " + entryId
                                             + ", " + locale + "] NOT FOUND");
        }
        EntryMetaData bean = new EntryMetaData(workspace, collection, locale, entryId, getLastModified(workspace,entryTarget));

        bean.setWorkspace(workspace);
        return bean;
    }

    /**
     */
    protected EntryMetaData modifyEntry(Object internalId,
                                        EntryTarget entryTarget,
                                        Collection<Category> categories, boolean mustAlreadyExist)
            throws AtomServerException {
        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();

        boolean isNewlyCreated = !contentExists(workspace,entryTarget);

        EntryMetaData bean = new EntryMetaData(workspace, collection, locale, entryId,
                                               getLastModified(workspace,entryTarget), isNewlyCreated);

        bean.setWorkspace(entryTarget.getWorkspace());
        if (isNewlyCreated) {
            bean.setNewlyCreated(true);
        }

        return bean;
    }

    /**
     */
    protected EntryMetaData deleteEntry(
            EntryTarget entryTarget,
            boolean setDeletedFlag)
            throws AtomServerException {
        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();

        if ( !contentExists(workspace, entryTarget) ) {
            throw new EntryNotFoundException("Property [" + collection + ", " + entryId
                                             + ", " + locale + "] NOT FOUND");
        }

        return null;
    }


    //--------------------------------
    //      private methods
    //--------------------------------
    /**
     * Recursively load files that have been modified since IfModifiedSince
     *
     * @return boolean indicating whether any files have been modified
     */
    private boolean loadFeedEntries(Abdera abdera, IRI iri, String workspace, String collection, 
                                    File baseDir, String dir, Feed feed,
                                    FileBasedAtomCollection.LastModified lastModified, long ifModifiedSince) {

        boolean foundModifiedFile = false;
        File[] files = new File(baseDir, dir).listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                try {
                    if (files[i].isDirectory()) {
                        String pathInfo[] = files[i].getPath().split("/");
                        String thisDir = pathInfo[pathInfo.length - 1];

                        foundModifiedFile = (loadFeedEntries(abdera, iri, workspace, collection, files[i].getParentFile(), thisDir,
                                                             feed, lastModified, ifModifiedSince))
                                            ? true : foundModifiedFile;

                    } else if (files[i].isFile()
                               && !files[i].isHidden()
                               && (FilenameUtils.getExtension(files[i].getName())).equals("xml")) {

                        long thisFileLastModified = files[i].lastModified();
                        log.debug( "+++++++++ file= " + files[i].getName() + " thisFileLastModified= " + thisFileLastModified );
                        
                        if (thisFileLastModified > ifModifiedSince) {
                            foundModifiedFile = true;

                            Entry entry = newLinkEntry(abdera, workspace, files[i], new Date(thisFileLastModified), iri);
                            feed.addEntry(entry);

                            if (thisFileLastModified > lastModified.getTime()) {
                                lastModified.setTime(thisFileLastModified);
                            }
                        }
                    }
                } catch (ParseException e) {
                    throw new AtomServerException("Can't parse " + files[i]);
                } catch (IRISyntaxException e) {
                    throw new AtomServerException(e);
                }
            }
        }
        return foundModifiedFile;
    }

    private Entry newLinkEntry(Abdera abdera, String workspace, File actualFile, Date updated, IRI iri) {
        EntryDescriptor pid = ((FileBasedContentStorage)getContentStorage()).getEntryMetaData(actualFile.getAbsolutePath());
        if (pid == null) {
            return null;
        }

        String sysId = pid.getCollection();
        String propId = pid.getEntryId();
        Locale locale = pid.getLocale();
        if (log.isDebugEnabled()) {
            log.debug("actualFile= " + actualFile + " pid = " + pid);
        }

        EntryMetaData entryMetaData = new EntryMetaData( workspace, sysId, locale, propId, updated, false );
        return newEntry( abdera, entryMetaData, EntryType.link );
    }

    public long getLastModified( String workspace, EntryTarget entryTarget ) {
        return ((FileBasedContentStorage)getContentStorage()).lastModified(entryTarget);
    }

    public boolean contentExists( String workspace, EntryTarget entryTarget) {
        return getContentStorage().contentExists(entryTarget);
    }


    //--------------------------------
    //      private classes
    //--------------------------------
    /**
     * A utility class so that we can pass lastModified by reference.
     */
    private class LastModified {
        long time = 0L;

        public String toString() { return "" + time; }

        long getTime() { return time; }

        void setTime(long time) { this.time = time; }
    }
}
