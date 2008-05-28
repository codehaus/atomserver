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


package org.atomserver.core.dbstore;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.history.FeedPagingHelper;
import org.apache.abdera.ext.opensearch.OpenSearchConstants;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.*;
import org.atomserver.core.*;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.dbstore.dao.EntryCategoriesDAO;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.exceptions.EntryNotFoundException;
import org.atomserver.exceptions.OptimisticConcurrencyException;
import org.atomserver.uri.*;
import org.atomserver.utils.collections.MultiHashMap;
import org.atomserver.utils.collections.MultiMap;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

/**
 * A Store implementation that uses the DB to store entry meta data.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBBasedAtomCollection extends AbstractAtomCollection {

    private static final Log log = LogFactory.getLog(DBBasedAtomCollection.class);

    public DBBasedAtomCollection( AtomWorkspace parentAtomWorkspace, String name ) {
        super( parentAtomWorkspace, name );
    }

    public Collection<Category> listCategories(RequestContext request) throws AtomServerException {
        return null;
    }

    public EntriesDAO getEntriesDAO() {
         return ((DBBasedAtomService)parentAtomWorkspace.getParentAtomService()).getEntriesDAO();
    }

     public EntryCategoriesDAO getEntryCategoriesDAO() {
         return ((DBBasedAtomService)parentAtomWorkspace.getParentAtomService()).getEntryCategoriesDAO();
     }

     public TransactionTemplate getTransactionTemplate() {
         return ((DBBasedAtomService)parentAtomWorkspace.getParentAtomService()).getTransactionTemplate();
     }

    //--------------------------------
    //      public methods
    //--------------------------------

    public void obliterateEntry(final EntryMetaData entryMetaData) {
        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    getEntriesDAO().obliterateEntry(entryMetaData);
                    getEntryCategoriesDAO().deleteEntryCategories(entryMetaData);
                    getContentStorage().obliterateContent(entryMetaData);
                }
            });
        } finally {
            if ( getPerformanceLog() != null ) {
                getPerformanceLog().log( "obliterate", getPerformanceLog().getPerfLogEntryString( entryMetaData ), stopWatch );
            }
        }
    }

    protected <T> T executeTransactionally(final TransactionalTask<T> task) {
        return (T) getTransactionTemplate().execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus transactionStatus) {
                getEntriesDAO().acquireLock();
                return task.execute();
            }
        });
    }

    protected Object getInternalId(EntryTarget entryTarget) {
        return getEntriesDAO().selectEntryInternalId(entryTarget);
    }

    //--------------------------------
    //      protected methods
    //--------------------------------
    /**
     */
    protected long getEntries(RequestContext request,
                              FeedTarget feedTarget,
                              long ifModifiedSinceLong,
                              Feed feed)
            throws AtomServerException {

        Abdera abdera = request.getServiceContext().getAbdera();
        IRI iri = request.getUri();

        Date ifModifiedSince = new Date(ifModifiedSinceLong);
        String collection = feedTarget.getCollection();
        String workspace = feedTarget.getWorkspace();

        int totalEntries = 0;
        if ( isProducingTotalResultsFeedElement() ) {
            // SELECT COUNT BY LastModified
            totalEntries = getEntriesDAO().getCountByLastModified(feedTarget, ifModifiedSince);

            if (totalEntries <= 0) {
                return 0L;
            }
        }

        int startingPageDelim = feedTarget.getPageDelimParam();
        Locale locale = feedTarget.getLocaleParam();
        EntryType entryType = (feedTarget.getEntryTypeParam() != null) ? feedTarget.getEntryTypeParam() : EntryType.link;

        int pageSize = calculatePageSize( feedTarget, entryType );
        if (log.isDebugEnabled()) {
            log.debug("getEntries:: startingPageDelim= " + startingPageDelim + " " + pageSize + " " + pageSize);
        }

        Collection<BooleanExpression<AtomCategory>> categoryQuery = feedTarget.getCategoriesQuery();

        // Ask for one more than the pageSize !!!
        //   This enables us to know if we're on the last page when the last page equals pageSize
        int pageSizePlus1 = pageSize + 1;

        // SELECT Entries BY Page and Locale
        List<EntryMetaData> sortedList = null;
        if (locale == null) {
            if ( categoryQuery != null ) {
                sortedList = getEntriesDAO().selectEntriesByPagePerCategory(feedTarget,
                                                                        ifModifiedSince, startingPageDelim, pageSizePlus1,
                                                                         categoryQuery );
            } else {
                sortedList = getEntriesDAO().selectEntriesByPage(feedTarget,
                                                                        ifModifiedSince, startingPageDelim, pageSizePlus1);
            }
        } else {
            if ( categoryQuery != null ) {
                sortedList = getEntriesDAO().selectEntriesByPageAndLocalePerCategory(feedTarget,
                                                                        ifModifiedSince, startingPageDelim, pageSizePlus1,
                                                                                 locale.toString(),
                                                                                 categoryQuery);
             } else {
                sortedList = getEntriesDAO().selectEntriesByPageAndLocale(feedTarget,
                                                                        ifModifiedSince, startingPageDelim,
                                                                     pageSizePlus1, locale.toString());
            }
        }

        int numEntries = sortedList.size();
        if (numEntries <= 0) {
            return 0L;
        }

        // Load the Categories to the EntryMetaData in the Feed 
        //   NOTE: this method calls the database!!!
        if ( isProducingEntryCategoriesFeedElement() ) {
            loadCategoriesToEntryMetaData( sortedList, workspace, collection );
        }

        // Add elements to the Feed document
        return createFeedElements(feed, abdera, iri, feedTarget, entryType,
                                  sortedList, workspace, collection, locale,
                                  numEntries, pageSizePlus1, pageSize,
                                  startingPageDelim, totalEntries);
    }

    /**
     * NOTE: "deleted" entries ARE returned (since they are never really deleted from the DB)
     * And Feed clients will want to know that an entry has been deleted
     */
    protected EntryMetaData getEntry(RequestContext request,
                                     EntryTarget entryTarget)
            throws AtomServerException {

        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();
        int revision = entryTarget.getRevision();
        if (log.isDebugEnabled()) {
            log.debug("DBBasedEntriestore.(SELECT) [" + collection + ", "
                      + locale + ", " + entryId + ", " + revision + "]");
        }

        // SELECT
        EntryMetaData entry = innerGetEntry(entryTarget);

        if (log.isDebugEnabled()) {
            log.debug("DBBasedEntriestore.(SELECT) entry= [" + entry + "]");
        }

        if (entry == null) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale + "] NOT FOUND";
            log.warn(msg);
            throw new EntryNotFoundException(msg);
        }

        // Does NOT require revision in the URL, but if you give it to me it must match !!!
        if ((revision != 0) && (revision != URIHandler.REVISION_OVERRIDE) && (entry.getRevision() != revision)) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                         + "] does NOT match the revision requested (requested= "
                         + revision + " actual= " + entry.getRevision() + ")";
            log.error(msg);

            String editURI = getURIHandler().constructURIString(workspace, entry.getCollection(), entry.getEntryId(),
                                                               entry.getLocale(), entry.getRevision());
            throw new OptimisticConcurrencyException(msg, editURI);
        }
        return entry;
    }

    protected EntryMetaData innerGetEntry(EntryTarget entryTarget) {
        return getEntriesDAO().selectEntry(entryTarget);
    }

    /**
     */
    protected Collection<BatchEntryResult> deleteEntries(RequestContext request,
                                                         Collection<EntryTarget> allEntriesUriData) throws AtomServerException {

        MultiMap<Locale, EntryTarget> dataByLocale = new MultiHashMap<Locale, EntryTarget>();
        for (EntryTarget uriData : allEntriesUriData) {
            dataByLocale.put(uriData.getLocale(), uriData);
        }

        List<BatchEntryResult> returnValue = new ArrayList<BatchEntryResult>();

        for (Set<EntryTarget> entriesURIData : dataByLocale.values()) {

            Set<EntryDescriptor> descriptors = new HashSet<EntryDescriptor>();
            for (EntryTarget entryTarget : entriesURIData) {
                descriptors.add(entryTarget);
            }

            getEntriesDAO().deleteEntryBatch(entriesURIData.iterator().next().getWorkspace(), descriptors);

            EntryMap<EntryMetaData> metaDataAfterDeleted = new EntryMap<EntryMetaData>();
            List<EntryMetaData> entryMetaData = getEntriesDAO().selectEntryBatch(descriptors);
            for (EntryMetaData metaDatum : entryMetaData) {
                metaDataAfterDeleted.put(metaDatum, metaDatum);
            }

            for (EntryTarget entryTarget : entriesURIData) {
                int revision = entryTarget.getRevision();
                EntryMetaData metaData = metaDataAfterDeleted.get(entryTarget);
                if (metaData == null) {
                    String msg = "Entry [" + entryTarget.getWorkspace() + ", " + entryTarget.getCollection() + ", " +
                                 entryTarget.getEntryId() + ", " + entryTarget.getLocale() + "] NOT FOUND";
                    log.warn(msg);
                    returnValue.add(new BatchEntryResult(entryTarget, new EntryNotFoundException(msg)));
                }
                else if (URIHandler.REVISION_OVERRIDE != revision && (metaData.getRevision() != (revision + 1))) {
                    String msg = "Entry [" + entryTarget.getWorkspace() + ", " + entryTarget.getCollection() + ", " +
                                 entryTarget.getEntryId() + ", " + entryTarget.getLocale()
                                 + "] Someone beat you to it (requested= " + revision
                        + " but it should be " + metaData.getRevision() + ")";
                    log.error(msg);
                    String editURI = getURIHandler().constructURIString(metaData.getWorkspace(),
                                                                       metaData.getCollection(),
                                                                       metaData.getEntryId(),
                                                                       metaData.getLocale(),
                                                                       metaData.getRevision());
                    returnValue.add(new BatchEntryResult(entryTarget, new OptimisticConcurrencyException(msg, editURI)));
                } else {
                    returnValue.add(new BatchEntryResult(entryTarget, metaData));
                }
            }
        }

        return returnValue;
    }

    /**
     */
    protected Collection<BatchEntryResult> modifyEntries(RequestContext request,
                                                         Collection<EntryTarget> allEntriesUriData) throws AtomServerException {
        MultiMap<Locale, EntryTarget> dataByLocale = new MultiHashMap<Locale, EntryTarget>();
        for (EntryTarget uriData : allEntriesUriData) {
            dataByLocale.put(uriData.getLocale(), uriData);
        }

        List<BatchEntryResult> returnValue = new ArrayList<BatchEntryResult>();

        for (Set<EntryTarget> entriesURIData : dataByLocale.values()) {

            Set<EntryDescriptor> descriptors = new HashSet<EntryDescriptor>();
            for (EntryTarget entryTarget : entriesURIData) {
                log.debug("about to update " + entryTarget.getEntryId());
                descriptors.add(entryTarget);
            }

            EntryMap<EntryMetaData> metaData = new EntryMap<EntryMetaData>();

            List<EntryMetaData> list = getEntriesDAO().selectEntryBatch(descriptors);
            for (EntryMetaData metaDatum : list) {
                metaData.put(metaDatum, metaDatum);
            }

            List<EntryDescriptor> toInsert = new ArrayList<EntryDescriptor>();
            List<EntryDescriptor> toUpdate = new ArrayList<EntryDescriptor>();
            EntryMap<AtomServerException> failed = new EntryMap<AtomServerException>();


            for (EntryTarget entryURIData : entriesURIData) {
                String workspace = entryURIData.getWorkspace();
                String collection = entryURIData.getCollection();
                Locale locale = entryURIData.getLocale();
                String entryId = entryURIData.getEntryId();
                int revision = entryURIData.getRevision();
                EntryMetaData entryMetaData = metaData.get(entryURIData);
                if (revision != URIHandler.REVISION_OVERRIDE &&
                    ((entryMetaData == null && revision > 0) ||
                     (entryMetaData != null && (revision != entryMetaData.getRevision())))) {
                    String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                                 + "] does NOT match the revision requested (requested= "
                                 + revision + " actual= " +
                                 (entryMetaData == null ? 0 : entryMetaData.getRevision()) + ")";
                    log.error(msg);
                    String editURI =
                            entryMetaData == null ? null :
                            getURIHandler().constructURIString(workspace, entryMetaData.getCollection(), entryMetaData.getEntryId(),
                                                              entryMetaData.getLocale(), entryMetaData.getRevision());
                    failed.put(entryURIData, new OptimisticConcurrencyException(msg, editURI));
                    continue;
                }

                (entryMetaData == null ? toInsert : toUpdate).add(entryURIData);
            }

            EntryMap<EntryMetaData> metaDataAfterModified = new EntryMap<EntryMetaData>();
            if (!toInsert.isEmpty()) {
                getEntriesDAO().insertEntryBatch(toInsert.get(0).getWorkspace(), toInsert);
                List<EntryMetaData> afterModified = getEntriesDAO().selectEntryBatch(toInsert);
                for (EntryMetaData metaDatum : afterModified) {
                    metaDatum.setNewlyCreated(true);
                    metaDataAfterModified.put(metaDatum, metaDatum);
                }
            }
            if (!toUpdate.isEmpty()) {
                getEntriesDAO().updateEntryBatch(toUpdate.get(0).getWorkspace(), toUpdate);
                List<EntryMetaData> afterModified = getEntriesDAO().selectEntryBatch(toUpdate);
                for (EntryMetaData metaDatum : afterModified) {
                    metaDatum.setNewlyCreated(false);
                    metaDataAfterModified.put(metaDatum, metaDatum);
                }
            }

            for (EntryTarget entry : entriesURIData) {
                if (failed.get(entry) != null) {
                    returnValue.add(new BatchEntryResult(entry, metaDataAfterModified.get(entry), failed.get(entry)));
                } else {
                    returnValue.add(new BatchEntryResult(entry, metaDataAfterModified.get(entry)));
                }
            }
        }
        return returnValue;
    }


    /**
     */
    protected EntryMetaData modifyEntry(Object internalId,
                                        RequestContext request,
                                        EntryTarget entryTarget,
                                        boolean mustAlreadyExist)
            throws AtomServerException {

        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();
        int revision = entryTarget.getRevision();
        if (log.isDebugEnabled()) {
            log.debug("DBBasedEntriestore.(MODIFY) [" + workspace + ", " + collection + ", "
                      + locale + ", " + entryId + ", " + revision + "]");
        }

        boolean isNewEntry = (internalId == null);

        if (!isNewEntry) {
            int numRowsModified = getEntriesDAO().updateEntry( entryTarget, false );
            if (log.isDebugEnabled())
                log.debug( "AFTER UPDATE:: [" + entryTarget.getEntryId() + "] numRowsModified= " + numRowsModified );

        } else {
            if (mustAlreadyExist) {
                String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                             + "] does NOT already exist, and MUST in this case (most likely for a Categories PUT)";
                log.error(msg);
                throw new BadRequestException(msg);
            }
            try {
                internalId = getEntriesDAO().insertEntry(entryTarget );
                if (log.isDebugEnabled())
                   log.debug( "AFTER INSERT :: [" + entryTarget.getEntryId() + "] internalId= " + internalId);

            } catch( DataIntegrityViolationException ee )  {

                // SELECT -- we do this select to know what revision we actually had,
                // so we can create the proper editURI
                EntryMetaData entryMetaData = getEntriesDAO().selectEntryByInternalId(internalId);

                String msg = null;
                if (revision == URIHandler.REVISION_OVERRIDE) {
                    msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                          + "] threw a DataIntegrityViolationException during an INSERT."
                          + "\nThis is because someone else was inserting this record at exactly the same time"
                          + "\nThus, you lost the race, and must attempt your INSERT again"
                          + "\nException = " + ee.getMessage();
                } else {
                    msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                          + "] does NOT match the revision requested (requested= "
                          + revision + " actual= " +
                          (entryMetaData == null ? 0 : entryMetaData.getRevision()) + ")";
                }
                log.error(msg, ee);
                String editURI =
                        entryMetaData == null ? null :
                        getURIHandler().constructURIString(workspace, entryMetaData.getCollection(), entryMetaData.getEntryId(),
                                                           entryMetaData.getLocale(), entryMetaData.getRevision());
                throw new OptimisticConcurrencyException(msg, ee, editURI);
            }
        }

        // SELECT -- We must select again
        //  because we need data that was set during the INSERT or UPDATE (e.g. published & lastModified)
        EntryMetaData bean = getEntriesDAO().selectEntryByInternalId(internalId);
        if (bean == null) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                         + "] returned an empty row (Null ResultSet) AFTER an INSERT or UPDATE!";
            log.error(msg);
            throw new AtomServerException(msg);
        }
        bean.setNewlyCreated(isNewEntry);

        if (revision != URIHandler.REVISION_OVERRIDE && (!isNewEntry && (bean.getRevision() != (revision + 1)))) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                + "] Someone beat you to it (requested= " + revision
                + " but it should be " + bean.getRevision() + ")";
            log.error(msg);
            String editURI = getURIHandler().constructURIString(workspace, bean.getCollection(), bean.getEntryId(),
                                                                bean.getLocale(), bean.getRevision());
            throw new OptimisticConcurrencyException(msg, editURI);
        }

        return bean;
    }


    /**
     * When we deleted entry XML files, we write a new "deleted" file
     * <p/>
     * NOTE: we do NOT actually delete the row from the DB, we simply mark it as "deleted"
     */
    protected EntryMetaData deleteEntry(RequestContext request,
                                        final EntryTarget entryTarget,
                                        final boolean setDeletedFlag)
            throws AtomServerException {

        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();
        int revision = entryTarget.getRevision();
        if (log.isDebugEnabled()) {
            log.debug("DBBasedAtomCollection.(DELETE) [" + collection + ", "
                      + locale + ", " + entryId + ", " + revision + "]");
        }

        // DELETE
        int numRowsModified = getEntriesDAO().deleteEntry(entryTarget, setDeletedFlag);

        // SELECT -- We must select again
        //  because we need data that was set during the DELETE (e.g. published & lastModified)
        EntryMetaData bean = null;
        if (URIHandler.REVISION_OVERRIDE == revision && numRowsModified == 0 ||
            (bean = getEntriesDAO().selectEntry(entryTarget)) == null) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale + "] NOT FOUND";
            log.error(msg);
            throw new EntryNotFoundException(msg);
        }

        if (URIHandler.REVISION_OVERRIDE != revision &&
            (bean.getRevision() != (revision + 1)) || (numRowsModified == 0)) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                         + "] Someone beat you to it (requested= " + revision
                         + " but it should be " + bean.getRevision() + ")";
            log.error(msg);
            String editURI = getURIHandler().constructURIString(workspace, bean.getCollection(), bean.getEntryId(),
                                                                bean.getLocale(), bean.getRevision());
            throw new OptimisticConcurrencyException(msg, editURI);
        }

        return bean;
    }

    //--------------------------------
    //      private methods
    //--------------------------------
    private void addOpenSearchElements(Feed feed, int startingPageDelim, 
                                       int pageSize, int totalEntries ) {
        if ( totalEntries > 0 ) 
            feed.addSimpleExtension(OpenSearchConstants.TOTAL_RESULTS, Integer.toString(totalEntries));

        feed.addSimpleExtension(OpenSearchConstants.START_INDEX, Integer.toString(startingPageDelim));
        feed.addSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE, Integer.toString(pageSize));
    }

    private void addAtomServerFeedElements(Feed feed, int endingPageDelim ) {
        feed.addSimpleExtension(AtomServerConstants.END_INDEX, Integer.toString(endingPageDelim));
    }

    // We do NOT write "previous" link, because we do not have any way to know the starting index
    // for the previous page.
    private void addPagingLinks(Feed feed, IRI iri, int endingPageDelim,
                                int pageSize, URITarget uriTarget ) {
        String nextURI = iri.getPath() + "?" +
                         QueryParam.startIndex.getParamName() + "=" + endingPageDelim
            + "&" + QueryParam.maxResults.getParamName() + "=" + pageSize;
        
        Locale locale = uriTarget.getLocaleParam();
        if ( locale != null ) 
            nextURI += "&" + QueryParam.locale.getParamName() + "=" + locale.toString();
        
        EntryType entryType = uriTarget.getEntryTypeParam();
        if ( entryType != null ) 
            nextURI += "&" + QueryParam.entryType.getParamName() + "=" + entryType.toString();
        
        FeedPagingHelper.setNext(feed, nextURI);
    }

    private void addSelfLink(Abdera abdera, Feed feed, IRI iri, int startingPageDelim, int pageSize) {
        String selfURI = iri.getPath();
        selfURI += "?" + QueryParam.maxResults.getParamName() + "=" + pageSize;
        if ( startingPageDelim != 0 ) {
            selfURI += "&" + QueryParam.startIndex.getParamName() + "=" + startingPageDelim;
        }

        addLinkToEntry(AtomServer.getFactory( abdera ), feed, selfURI, "self");
    }

    private void addFeedEntries(Abdera abdera, Feed feed, List list, int pageSize, EntryType entryType) {
        // Note: pageSize is actually one larger than it really is,
        //   (So that we can figure out when the final page is the same as the real pageSize)
        int knt = 0;
        for (Object obj : list) {
            if ( knt < pageSize ) {
                EntryMetaData entryMetaData = (EntryMetaData) obj;
                if (log.isDebugEnabled()) {
                    log.debug("addFeedEntries ADD:: " + entryMetaData );
                }

                Entry entry = newEntry( abdera, entryMetaData, entryType );
                feed.addEntry(entry);
            }
            knt++;
        }
    }

    protected int calculatePageSize( URITarget feedURIData, EntryType entryType ) {
        int pageSize = feedURIData.getPageSizeParam();

        // we must throttle the max results per page to avoid monster requests
        int maxEntriesPerPage = ( entryType == EntryType.link ) ? getMaxLinkEntriesPerPage() : getMaxFullEntriesPerPage();
        if (log.isTraceEnabled()) {
            log.trace("getEntries:: entryType= " + entryType + " " + maxEntriesPerPage );
        }

        if ( pageSize > maxEntriesPerPage ) 
            log.info("Resetting pagesize(" + pageSize + ") to  MAX_RESULTS_PER_PAGE (" + maxEntriesPerPage + ")");        
        
        if ( pageSize == 0 || pageSize > maxEntriesPerPage ) {
            pageSize = maxEntriesPerPage;
        }
        return pageSize;
    }


    private void loadCategoriesToEntryMetaData( List<EntryMetaData> sortedList, String workspace, String collection ) {
        Map<String, EntryMetaData> entriesByEntryId = new HashMap<String, EntryMetaData>(sortedList.size());
        for (EntryMetaData entryMetaData : sortedList) {
            entriesByEntryId.put(entryMetaData.getEntryId(), entryMetaData);
        }

        List<EntryCategory> entryCategories =
            getEntryCategoriesDAO().selectEntriesCategories( workspace, collection, entriesByEntryId.keySet());

        for (EntryCategory entryCategory : entryCategories) {
            EntryMetaData metaData = entriesByEntryId.get(entryCategory.getEntryId());
            if (metaData.getCategories() == null) {
                metaData.setCategories(new ArrayList<EntryCategory>());
            }
            metaData.getCategories().add(entryCategory);
        }
    }

    protected long createFeedElements( Feed feed, Abdera abdera, IRI iri,
                                     FeedTarget feedTarget, EntryType entryType,
                                     List<? extends EntryMetaData> sortedList,
                                     String workspace, String collection, Locale locale,
                                     int numEntries, int pageSizePlus1, int pageSize,  
                                     int startingPageDelim, int totalEntries ) {

        boolean resultsFitOnOnePage = numEntries != pageSizePlus1;
        
        // Pick out the last item in the list and pull lastModified from it
        //  Note: we asked for one more than we really needed so subtract 2...
        int subtract = ( resultsFitOnOnePage ) ? 1 : 2;  

        int lastIndex = ( (sortedList.size() - subtract) >= 0) ? (sortedList.size() - subtract) : 0;
          
        EntryMetaData entry = sortedList.get(lastIndex);
        long lastModified = (entry.getLastModifiedDate() != null) ? entry.getLastModifiedDate().getTime() : 0L;
        int endingPageDelim = (int) (entry.getLastModifiedSeqNum());
        if (log.isDebugEnabled()) {
            log.debug("DBBasedEntriestore.loadFeedEntries:: lastModifiedSeqNum= "
                      + endingPageDelim + " lastModified= " + lastModified
                      + " numEntries= " + numEntries + " totalEntries= " + totalEntries);
        }

        boolean isLastPage = ((startingPageDelim != 0) && (numEntries > 0) && (numEntries <= pageSize));

        StopWatch stopWatch = new AutomaticStopWatch();
        try { 
            addAtomServerFeedElements(feed, endingPageDelim );
            if ( ! resultsFitOnOnePage || startingPageDelim != 0 ) {
                addOpenSearchElements(feed, startingPageDelim, pageSize, totalEntries);
                
                if ( ! isLastPage ) 
                    addPagingLinks(feed, iri, endingPageDelim, pageSize, feedTarget );
            }
            addSelfLink(abdera, feed, iri, startingPageDelim, pageSize );
            addFeedEntries( abdera, feed, sortedList, pageSize, entryType );
        } finally {
            if ( getPerformanceLog() != null ) {
                getPerformanceLog().log( "XML.feed", getPerformanceLog().getPerfLogFeedString( locale, workspace, collection ), stopWatch );
            }
        }
        return lastModified; 
    }

    private final Set<String> seenCollections = new HashSet<String>();

    public void ensureCollectionExists(final String collection) {
        // if the collection has ever existed since this server started, then it will continue to
        // do so, so we can safely cache the collections we've seen.  I see no reason to bound
        // this, but of course if there were 10 million collections then we should probably just
        // remove this and go to the DB every time.
        if (!seenCollections.contains(collection)) {
            // executing this transactionally just makes sure that we don't race on checking the
            // db for existence and then inserting.  This txn inside the outer "if" technically
            // is double-check, but it's okay in this case -- it's just to prevent data integrity
            // errors on the updates - the only practical upshot is that two threads both calling
            // ensureCollectionExists on the same NEW collection will occasionally still both get
            // in here, and the txn ensures that only ONE of them will actually get so far as an
            // INSERT statement.
            executeTransactionally(new TransactionalTask<Object>() {
                public Object execute() {
                    getEntriesDAO().ensureCollectionExists(
                            getParentAtomWorkspace().getName(), collection);
                    seenCollections.add(collection);
                    return null;
                }
            });
        }
    }
}
