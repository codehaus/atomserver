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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.*;
import org.atomserver.core.*;
import org.atomserver.core.dbstore.dao.EntriesDAO;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.exceptions.EntryNotFoundException;
import org.atomserver.exceptions.OptimisticConcurrencyException;
import org.atomserver.server.servlet.AtomServerUserInfo;
import org.atomserver.uri.*;
import org.atomserver.utils.AtomDate;
import org.atomserver.utils.collections.MultiHashMap;
import org.atomserver.utils.collections.MultiMap;
import org.atomserver.utils.logic.BooleanExpression;
import org.atomserver.utils.perf.AtomServerPerfLogTagFormatter;
import org.atomserver.utils.perf.AtomServerStopWatch;
import org.perf4j.StopWatch;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * A Store implementation that uses the DB to store entry meta data.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
public class DBBasedAtomCollection extends AbstractAtomCollection {

    private static final Log log = LogFactory.getLog(DBBasedAtomCollection.class);
    private static final int DEFAULT_TXN_TIMEOUT = 300;

    public DBBasedAtomCollection( AtomWorkspace parentAtomWorkspace, String name ) {
        super( parentAtomWorkspace, name );
    }

    public Collection<Category> listCategories(RequestContext request) throws AtomServerException {
        return null;
    }

    public EntriesDAO getEntriesDAO() {
         return ((DBBasedAtomService)parentAtomWorkspace.getParentAtomService()).getEntriesDAO();
    }

     public TransactionTemplate getTransactionTemplate() {
         return ((DBBasedAtomService)parentAtomWorkspace.getParentAtomService()).getTransactionTemplate();
     }

    public void obliterateEntry(final EntryMetaData entryMetaData) {
        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

                    getContentStorage().obliterateContent(entryMetaData);
                    getEntriesDAO().obliterateEntry(entryMetaData);
                }
            });
        } finally {
            stopWatch.stop("obliterate", AtomServerPerfLogTagFormatter.getPerfLogEntryString( entryMetaData ));
        }
    }

    protected <T> T executeTransactionally(final TransactionalTask<T> task)  {
        FutureTask<T> timeoutTask = null;
        try {
            // create new timeout task
            timeoutTask = new FutureTask<T>(new Callable() {
                public T call() throws Exception {
                    return (T) getTransactionTemplate().execute(new TransactionCallback() {
                        public Object doInTransaction(TransactionStatus transactionStatus) {
                            StopWatch stopWatch = new AtomServerStopWatch();
                            try {
                                // NOTE: we will actually wait for all of these to possibly finish,
                                //       unless the methods below honor InterruptedExceptions
                                //       BUT the transaction will still be rolled back eventually by the catch below.
                                getEntriesDAO().acquireLock();
                                return task.execute();

                            } catch( Exception ee ) {
                                if (ee instanceof EntryNotFoundException &&
                                    (((EntryNotFoundException)ee).getType() == EntryNotFoundException.EntryNotFoundType.DELETE)) {
                                    log.warn("Exception in DB transaction", ee );
                                } else {
                                    log.error("Exception in DB transaction", ee );
                                }

                                // the following is not really required, but ensures that this will rollback, without question
                                transactionStatus.setRollbackOnly();
                                
                                if ( ee instanceof InterruptedException ) {
                                    // InterruptedException - if the current thread was interrupted while waiting
                                    // Re-assert the thread's interrupted status
                                    Thread.currentThread().interrupt();                                    
                                }
                                // NOTE: per the Spring manual, a transaction is ONLY rolled back
                                //       when a RuntimeException is thrown!!!
                                //       And the timeout causes an InterruptedException (via task.cancel()),
                                //       which is NOT Runtime!!
                                //       (AtomServerException extends RuntimeException)
                                throw (ee instanceof AtomServerException)
                                      ? (AtomServerException)ee
                                      : new AtomServerException("A " + ee.getCause().getClass().getSimpleName() +
                                                                " caught in Transaction", ee.getCause());
                                
                            } finally {
                                stopWatch.stop("DB.txn", "DB.txn");
                            }
                        }
                    });
                }
            });
            // start timeout task in a new thread
            new Thread(timeoutTask).start();

            // wait for the execution to finish, timeout after X secs
            int timeout = (getTransactionTemplate().getTimeout() > 0)
                          ? getTransactionTemplate().getTimeout() : DEFAULT_TXN_TIMEOUT;

            return timeoutTask.get(timeout, TimeUnit.SECONDS);

        } catch ( AtomServerException ee ) {
            throw ee;
        } catch ( ExecutionException ee ) {
            throw ee.getCause() instanceof AtomServerException ?
                    (AtomServerException)ee.getCause() :
                    new AtomServerException("A " + ee.getCause().getClass().getSimpleName() +
                            " caught in Transaction", ee.getCause());
        } catch( Exception ee ) {
            throw new AtomServerException("A " + ee.getClass().getSimpleName() + " caught in Transaction", ee);
        } finally {
            // NOTE: We MUST call timeoutTask.cancel() here.
            //       This is the ONLY way that we see an InterruptedException in the transaction task,
            //       and thus, the ONLY way that we can make the transaction rollback.
            // NOTE: Calling cancel() on a completed task is a noop.
            log.debug("@@@@@@@@@@@@@@ Calling task.cancel");
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }

    protected Object getInternalId(EntryDescriptor descriptor) {
        Object id = getEntriesDAO().selectEntryInternalId(descriptor);
        log.debug("getInternalId= " + id);
        return id;
    }

    protected long getEntries(Abdera abdera,
                              IRI iri,
                              FeedTarget feedTarget,
                              Date updatedMin,
                              Date updatedMax,
                              Feed feed )
            throws AtomServerException {

        if(getEntriesMonitor() != null) {
            getEntriesMonitor().updateNumberOfGetEntriesRequests(1);
        }
        String collection = feedTarget.getCollection();
        String workspace = feedTarget.getWorkspace();

        int totalEntries = 0;
        if ( isProducingTotalResultsFeedElement() ) {
            // SELECT COUNT BY LastModified
            totalEntries = getEntriesDAO().getCountByLastModified(feedTarget, updatedMin);

            if (totalEntries <= 0) {
                if(getEntriesMonitor() != null) {
                    getEntriesMonitor().updateNumberOfGetEntriesRequestsReturningNone(1);
                }
                return 0L;
            }
        }

        int startIndex = feedTarget.getStartIndexParam();
        int endIndex = feedTarget.getEndIndexParam();

        if ( endIndex != -1 && endIndex < startIndex ) {
            String msg = "endIndex parameter (" + endIndex + ") is less than the startIndex (" + startIndex +")";
            log.error(msg);
            throw new BadRequestException(msg);
        }

        Locale locale = feedTarget.getLocaleParam();
        EntryType entryType = (feedTarget.getEntryTypeParam() != null) ? feedTarget.getEntryTypeParam() : EntryType.link;

        int pageSize = calculatePageSize( feedTarget, entryType );
        if (log.isDebugEnabled()) {
            log.debug("getEntries:: startIndex= " + startIndex + " endIndex= " + endIndex + " pageSize " + pageSize );
        }

        Collection<BooleanExpression<AtomCategory>> categoryQuery = feedTarget.getCategoriesQuery();

        // SELECT Entries BY Page and Locale
        List<EntryMetaData> sortedList =
                getEntriesDAO().selectFeedPage( updatedMin,
                                                updatedMax,
                                                startIndex,
                                                endIndex,
                                                pageSize + 1 /* ask for 1 more than pageSize, to detect the end of the feed */,
                                                locale == null ? null : locale.toString(),
                                                feedTarget,
                                                categoryQuery);

        int numEntries = sortedList.size();
        if (numEntries <= 0) {
            if(getEntriesMonitor() != null) {
                getEntriesMonitor().updateNumberOfGetEntriesRequestsReturningNone(1);
            }
            return 0L;
        }

        for (EntryMetaData entryMetaData : sortedList) {
            // we are looking for the unthinkable here -- if these if blocks ever trigger, that
            // means that SQL Server returned us a row with an UpdateTimestamp less than or equal
            // to the page delimiter that we requested!

            if (entryMetaData.getUpdateTimestamp() <= startIndex) {
                String message = MessageFormat.format("SQL-SERVER-ERROR!  (TIMESTAMP)  We requested the page " +
                                                      "starting at {0}, " +
                                                      "and the response to the query contained an entry at {1}!\n" +
                                                      "** the full offending entry was: {2}\n" +
                                                      "** the list of all entries was: \n *{3}",
                                                      startIndex,
                                                      entryMetaData.getUpdateTimestamp(),
                                                      entryMetaData,
                                                      StringUtils.join(sortedList, "\n *"));
                log.error(message);
                throw new AtomServerException(message);
            }

        }

        // Load the Categories to the EntryMetaData in the Feed 
        //   NOTE: this method calls the database!!!
        //   TODO: we could load these in the same query as above, except for HSQL limitations.
        if ( isProducingEntryCategoriesFeedElement() ) {
            loadCategoriesToEntryMetaData( sortedList, workspace, collection );
        }

        // Add elements to the Feed document
        return createFeedElements(feed, abdera, iri, feedTarget, entryType,
                                  sortedList, workspace, collection, locale,
                                  numEntries, (numEntries <= pageSize), pageSize,
                                  startIndex, totalEntries);
    }

    /**
     * NOTE: "deleted" entries ARE returned (since they are never really deleted from the DB)
     * And Feed clients will want to know that an entry has been deleted
     */
    protected EntryMetaData getEntry(EntryTarget entryTarget)
            throws AtomServerException {

        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();
        int revision = entryTarget.getRevision();
        if (log.isDebugEnabled()) {
            log.debug("DBBasedAtomCollection.(SELECT) [" + collection + ", "
                      + locale + ", " + entryId + ", " + revision + "]");
        }

        // SELECT
        EntryMetaData entry = innerGetEntry(entryTarget);

        if (log.isDebugEnabled()) {
            log.debug("DBBasedAtomCollection.(SELECT) entry= [" + entry + "]");
        }

        if (entry == null) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale + "] NOT FOUND";
            log.warn(msg);
            throw new EntryNotFoundException(EntryNotFoundException.EntryNotFoundType.GET, msg);
        }

        return entry;
    }

    protected EntryMetaData innerGetEntry(EntryTarget entryTarget) {
        return getEntriesDAO().selectEntry(entryTarget);
    }

    protected Collection<BatchEntryResult> deleteEntries(RequestContext request,
                                                         Collection<EntryTarget> allEntriesUriData)
            throws AtomServerException {

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
                    returnValue.add(new BatchEntryResult(entryTarget,
                                                         new EntryNotFoundException(EntryNotFoundException.EntryNotFoundType.DELETE, msg)));
                }
                else if (URIHandler.REVISION_OVERRIDE != revision && (metaData.getRevision() != revision)) {
                    String msg = "Entry [" + entryTarget.getWorkspace() + ", " + entryTarget.getCollection() + ", " +
                                 entryTarget.getEntryId() + ", " + entryTarget.getLocale()
                                 + "] Someone beat you to it (requested= " + revision
                        + " but it should be " + (metaData.getRevision()+1) + ")";
                    log.error(msg);
                    String editURI = getURIHandler().constructURIString(metaData.getWorkspace(),
                                                                        metaData.getCollection(),
                                                                        metaData.getEntryId(),
                                                                        metaData.getLocale(),
                                                                        (metaData.getRevision() + 1) );
                    returnValue.add(new BatchEntryResult(entryTarget, new OptimisticConcurrencyException(msg, editURI)));
                } else {
                    returnValue.add(new BatchEntryResult(entryTarget, metaData, true));
                }
            }
        }

        return returnValue;
    }

    protected Collection<BatchEntryResult> modifyEntries(RequestContext request,
                                                         Collection<EntryTarget> allEntriesUriData)
            throws AtomServerException {

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
            EntryMap<Boolean> unchangedEntries = new EntryMap<Boolean>();

            for (EntryTarget entryURIData : entriesURIData) {
                String workspace = entryURIData.getWorkspace();
                String collection = entryURIData.getCollection();
                Locale locale = entryURIData.getLocale();
                String entryId = entryURIData.getEntryId();
                int revision = entryURIData.getRevision();
                EntryMetaData entryMetaData = metaData.get(entryURIData);

                if (revision != URIHandler.REVISION_OVERRIDE &&
                    ((entryMetaData == null && revision != 0) ||
                     (entryMetaData != null && (revision != (entryMetaData.getRevision() + 1) )))) {

                    String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                                 + "] edit revision does NOT match the revision requested (requested= "
                                 + revision + " actual= " +
                                 (entryMetaData == null ? 0 : (entryMetaData.getRevision() + 1)) + ")";
                    log.error(msg);
                    String editURI =
                            entryMetaData == null ? null :
                            getURIHandler().constructURIString(workspace, entryMetaData.getCollection(),
                                                               entryMetaData.getEntryId(), entryMetaData.getLocale(),
                                                               (entryMetaData.getRevision() + 1) );
                    failed.put(entryURIData, new OptimisticConcurrencyException(msg, editURI));
                    continue;
                }

                if(this.alwaysUpdateEntry()) {
                    (entryMetaData == null ? toInsert : toUpdate).add(entryURIData);
                } else {
                    // compare old and new contents
                    boolean changed = isContentChanged(null, entryMetaData);
                    if(changed || entryMetaData != null && entryMetaData.getDeleted()) {
                        (entryMetaData == null ? toInsert : toUpdate).add(entryURIData);
                    } else {
                        unchangedEntries.put(entryURIData, changed);
                    }
                }
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
                    returnValue.add(new BatchEntryResult(entry, metaDataAfterModified.get(entry), false, failed.get(entry)));
                } else if( unchangedEntries.get(entry) != null ) {
                    returnValue.add(new BatchEntryResult(entry, metaData.get(entry), false));
                } else {
                    returnValue.add(new BatchEntryResult(entry, metaDataAfterModified.get(entry), true));
                }
            }
        }
        return returnValue;
    }


    /**
     * This method handles BOTH inserts and updates
     * <p/>
     * NOTE: A PUT will receive an EntryTarget which points at the NEXT revision
     */
    protected EntryMetaDataStatus modifyEntry(Object internalId,
                                        EntryTarget entryTarget,
                                        boolean mustAlreadyExist)
            throws AtomServerException {

        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();
        int revision = entryTarget.getRevision();
        if (log.isDebugEnabled()) {
            log.debug("DBBasedAtomCollection.(MODIFY) [" + workspace + ", " + collection + ", "
                      + locale + ", " + entryId + ", " + revision + "]" + "Id= " + internalId );
        }

        boolean isNewEntry = (internalId == null);
        boolean writeFailed = true;

        if (!isNewEntry) {

            if (revision == 0) {
                // SELECT -- we do this select to know what revision we actually had,
                // so we can create the proper editURI
                EntryMetaData metaData = getEntriesDAO().selectEntryByInternalId(internalId);
                int rev = metaData.getRevision();
                String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                             + "] You requested a write at revision 0, but this has already been written"
                             + " It should be " + (rev + 1) + ")";

                throwOptimisticConcurrencyException(msg, workspace, collection, entryId, locale, (rev + 1));
            }

            if(this.alwaysUpdateEntry()) { // update entry regardless of its content.

                int numRowsModified = getEntriesDAO().updateEntry( entryTarget, false );
                if (numRowsModified > 0) {
                    writeFailed = false;
                }
                if (log.isDebugEnabled())
                    log.debug( "AFTER UPDATE:: [" + entryTarget.getEntryId() + "] numRowsModified= " + numRowsModified );

            } else {
                // Don't update entry if the content are the same.

                // SELECT -- we do this select to know what revision we actually had and its content hash code.
                EntryMetaData metaData = getEntriesDAO().selectEntryByInternalId(internalId);

                // check revision compatibility
                boolean revisionError = (revision >= 0) && (metaData.getRevision() >= revision);

                // if the entry is deleted or the content has changed, proceed with update.
                if(!revisionError && !metaData.getDeleted() && !isContentChanged(entryTarget, metaData)) {
                   if( log.isDebugEnabled())
                        log.debug(" CONTENT Hash is the same: [" + entryTarget.getEntryId() + "]");
                    metaData.setNewlyCreated(false);
                    // If content has not changed, do not update the Entry (unless the categories are changed).
                    return new EntryMetaDataStatus(metaData,false);
                }

                if(!revisionError) {
                    int numRowsModified = getEntriesDAO().updateEntry( entryTarget, false );
                    if (numRowsModified > 0) {
                        writeFailed = false;
                    }

                    if (log.isDebugEnabled())
                    log.debug( "AFTER UPDATE:: [" + entryTarget.getEntryId() + "] numRowsModified= " + numRowsModified );
                 } else {
                    writeFailed = true;
                }
            }

        } else {
            if (mustAlreadyExist) {
                String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                             + "] does NOT already exist, and MUST in this case (most likely for a Categories PUT)";
                log.error(msg);
                throw new BadRequestException(msg);
            }

            if ( revision != 0 && revision != URIHandler.REVISION_OVERRIDE ) {
                String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                             + "] does NOT exist, but you requested it to be created at revision= " + revision +
                             "\nNOTE: only /0, /*, or nothing is acceptable for initial creation";
                throwOptimisticConcurrencyException( msg, workspace, collection, entryId, locale, 0);
            }

            try {
                internalId = getEntriesDAO().insertEntry(entryTarget );
                if (log.isDebugEnabled())
                   log.debug( "AFTER INSERT :: [" + entryTarget.getEntryId() + "] internalId= " + internalId);
                if (internalId != null) {
                    writeFailed = false;
                }
            } catch( DataIntegrityViolationException ee )  {

                // SELECT -- we do this select to know what revision we actually had,
                // so we can create the proper editURI
                EntryMetaData entryMetaData = getEntriesDAO().selectEntryByInternalId(internalId);

                String msg;
                if (revision == URIHandler.REVISION_OVERRIDE) {
                    msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                          + "] threw a DataIntegrityViolationException during an INSERT."
                          + "\nThis is because someone else was inserting this record at exactly the same time"
                          + "\nThus, you lost the race, and must attempt your INSERT again"
                          + "\nException = " + ee.getMessage();
                } else {
                    msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                          + "] edit revision does NOT match the revision requested (requested= "
                          + revision + " actual= " +
                          (entryMetaData == null ? 0 : (entryMetaData.getRevision() + 1)) + ")";
                }
                if (entryMetaData == null) {
                    throwOptimisticConcurrencyException(msg, null, null, null, null, 0, ee);
                } else {
                    throwOptimisticConcurrencyException(msg, workspace, entryMetaData.getCollection(),
                                                        entryMetaData.getEntryId(), entryMetaData.getLocale(),
                                                        (entryMetaData.getRevision() + 1), ee);
                }
            }
        }

        return postModifyEntry(internalId, entryTarget, isNewEntry, writeFailed);
    }

    /**
     * Update the entry. This is called when the entry content has not changed but the categories have on update.
     * @param internalId
     * @param entryTarget
     * @return <code>EntryMetaDataStatus</code> object
     * @throws AtomServerException
     */
    protected EntryMetaDataStatus reModifyEntry(Object internalId, EntryTarget entryTarget)
            throws AtomServerException {
        boolean writeFailed = true;

        int numRowsModified = getEntriesDAO().updateEntry( entryTarget, false );
        if (numRowsModified > 0) {
            writeFailed = false;
        }
        return postModifyEntry(internalId, entryTarget, false, writeFailed);

    }

    /**
     * Post processing after update or insert of an entry.
     * @param internalId
     * @param entryTarget
     * @param writeFailed
     * @param isNewEntry
     * @return
     */
    protected EntryMetaDataStatus postModifyEntry(Object internalId,
                                                  EntryTarget entryTarget,
                                                  boolean isNewEntry,
                                                  boolean writeFailed) {

         // SELECT -- We must select again
        //  because we need data that was set during the INSERT or UPDATE (e.g. published & lastModified)
        EntryMetaData bean = getEntriesDAO().selectEntryByInternalId(internalId);

        String workspace = entryTarget.getWorkspace();
        String collection = entryTarget.getCollection();
        Locale locale = entryTarget.getLocale();
        String entryId = entryTarget.getEntryId();
        int revision = entryTarget.getRevision();

        if (bean == null) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                         + "] returned an empty row (Null ResultSet) AFTER an INSERT or UPDATE!";
            log.error(msg);
            throw new AtomServerException(msg);
        }
        bean.setNewlyCreated(isNewEntry);

        if ( writeFailed
            || (revision != URIHandler.REVISION_OVERRIDE
                && (!isNewEntry && (bean.getRevision() != revision)))) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                + "] Someone beat you to it (requested= " + revision
                + " but it should be " + (bean.getRevision() + 1) + ")";
            throwOptimisticConcurrencyException(msg,
                                                workspace, bean.getCollection(), bean.getEntryId(), bean.getLocale(), 
                                                (bean.getRevision() + 1));
        }

        return new EntryMetaDataStatus(bean,true);
    }

    /**
     * When we delete entry XML files, we write a new "deleted" file
     * <p/>
     * NOTE: we do NOT actually delete the row from the DB, we simply mark it as "deleted"
     * NOTE: A DELETE will receive an EntryTarget which points at the NEXT revision
     */
    protected EntryMetaData deleteEntry(final EntryTarget entryTarget,
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
            log.warn(msg);
            throw new EntryNotFoundException(EntryNotFoundException.EntryNotFoundType.DELETE, msg);
        }

        if (URIHandler.REVISION_OVERRIDE != revision && bean.getRevision() != revision || numRowsModified == 0) {
            String msg = "Entry [" + workspace + ", " + collection + ", " + entryId + ", " + locale
                         + "] Someone beat you to it (requested= " + revision
                         + " but it should be " + bean.getRevision() + ")";
            throwOptimisticConcurrencyException(msg,
                                                workspace, bean.getCollection(), bean.getEntryId(), bean.getLocale(),
                                                (bean.getRevision()+1));
        }

        return bean;
    }

    private void addOpenSearchElements(Feed feed, int startIndex,
                                       int pageSize, int totalEntries ) {
        if ( totalEntries > 0 )
            feed.addSimpleExtension(OpenSearchConstants.TOTAL_RESULTS, Integer.toString(totalEntries));

        feed.addSimpleExtension(OpenSearchConstants.START_INDEX, Integer.toString(startIndex));
        feed.addSimpleExtension(OpenSearchConstants.ITEMS_PER_PAGE, Integer.toString(pageSize));
    }

    private void addAtomServerFeedElements(Feed feed, int endIndex ) {
        feed.addSimpleExtension(AtomServerConstants.END_INDEX, Integer.toString(endIndex));
    }

    // We do NOT write "previous" link, because we do not have any way to know the starting index
    // for the previous page.
    private void addPagingLinks(Feed feed, IRI iri, int endIndex,
                                int pageSize, URITarget uriTarget ) {
        String nextURI = iri.getPath() + "?" +
                         QueryParam.startIndex.getParamName() + "=" + endIndex +
                         "&" + QueryParam.maxResults.getParamName() + "=" + pageSize;

        Locale locale = uriTarget.getLocaleParam();
        if ( locale != null ) {
            nextURI += "&" + QueryParam.locale.getParamName() + "=" + locale.toString();
        }
        EntryType entryType = uriTarget.getEntryTypeParam();
        if ( entryType != null ) {
            nextURI += "&" + QueryParam.entryType.getParamName() + "=" + entryType.toString();
        }

        // NOTE: we do NOT add the updated-min param because, by definition, we have already satisfied that
        //       condition on the first page of this page set. (i.e. we're past that point)
        //       And passing along start-index ensures this.
        Date updatedMax = uriTarget.getUpdatedMaxParam();
        if ( updatedMax != null ) {
            nextURI += "&" + QueryParam.updatedMax.getParamName() + "=" + AtomDate.format( updatedMax );
        }
        int endIndexMax = uriTarget.getEndIndexParam();
        if ( endIndexMax != -1 ) {
            nextURI += "&" + QueryParam.endIndex.getParamName() + "=" + endIndexMax;
        }

        FeedPagingHelper.setNext(feed, nextURI);
    }

    private void addFeedSelfLink(Abdera abdera, Feed feed, IRI iri, int startIndex, int pageSize) {
        String selfURI = iri.getPath();
        selfURI += "?" + QueryParam.maxResults.getParamName() + "=" + pageSize;
        if ( startIndex != 0 ) {
            selfURI += "&" + QueryParam.startIndex.getParamName() + "=" + startIndex;
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
        int pageSize = feedURIData.getMaxResultsParam();

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
                getCategoriesHandler().selectEntriesCategories( workspace, collection, entriesByEntryId.keySet());

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
                                     int numEntries, boolean resultsFitOnOnePage, int pageSize,
                                     int startIndex, int totalEntries ) {

        // Pick out the last item in the list and pull lastModified from it
        //  Note: we asked for one more than we really needed so subtract 2...
        int subtract = ( resultsFitOnOnePage ) ? 1 : 2;

        int lastIndex = ( (sortedList.size() - subtract) >= 0) ? (sortedList.size() - subtract) : 0;

        EntryMetaData entry = sortedList.get(lastIndex);
        long lastUpdatedDate = (entry.getUpdatedDate() != null) ? entry.getUpdatedDate().getTime() : 0L;
        int lastTimestamp = (int) (entry.getUpdateTimestamp());
        if (log.isDebugEnabled()) {
            log.debug("DBBasedEntriestore.loadFeedEntries:: lastTimestamp= "
                      + lastTimestamp + " lastUpdatedDate= " + lastUpdatedDate
                      + " numEntries= " + numEntries + " totalEntries= " + totalEntries);
        }

        boolean isLastPage = ((startIndex != 0) && resultsFitOnOnePage);

        StopWatch stopWatch = new AtomServerStopWatch();
        try {
            addAtomServerFeedElements(feed, lastTimestamp );
            if ( ! resultsFitOnOnePage || startIndex != 0 ) {
                addOpenSearchElements(feed, startIndex, pageSize, totalEntries);

                if ( ! isLastPage )
                    addPagingLinks(feed, iri, lastTimestamp, pageSize, feedTarget );
            }
            addFeedSelfLink(abdera, feed, iri, startIndex, pageSize );
            addFeedEntries( abdera, feed, sortedList, pageSize, entryType );
        } finally {
            stopWatch.stop("XML.feed", AtomServerPerfLogTagFormatter.getPerfLogFeedString( locale, workspace, collection ));
        }
        return lastUpdatedDate;
    }

    private final Set<String> seenCollections = new HashSet<String>();

    public void ensureCollectionExists(final String collection) {
        // if the collection has ever existed since this server started, then it will continue to
        // do so, so we can safely cache the collections we've seen.  I see no reason to bound
        // this, but of course if there were 10 million collections then we should probably just
        // remove this and go to the DB every time.
        //
        // first, we check OUTSIDE of a synchronized block, so in the exceedingly likely case that
        // the collection exists, we don't ever block.
        if (seenCollections.contains(collection)) {
            return;
        }
        // then, we syncrhonize the update to the collection, in case we do encounter a race
        // condition between two actors on this server
        synchronized (seenCollections) {
            if (!seenCollections.contains(collection)) {
                // executing this transactionally just makes sure that we don't race BETWEEN
                // multiple servers on checking the db for existence and then inserting.  This
                // txn inside the outer "if" technically is double-check, but it's okay in this
                // case -- it's just to prevent data integrity errors on the updates - the only
                // practical upshot is that two processes both calling ensureCollectionExists on
                // the same NEW collection will occasionally still both get in here, and the txn
                // ensures that only ONE of them will actually get so far as an INSERT statement.
                try {
                    executeTransactionally(new TransactionalTask<Object>() {
                        public Object execute() {
                            getEntriesDAO().ensureCollectionExists( getParentAtomWorkspace().getName(), collection );
                            seenCollections.add(collection);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    log.warn("exception occurred while ensuring the existence of " +
                             getParentAtomWorkspace().getName() + "/" + collection +
                             " - this is probably okay.  This exception should be rare, but" +
                             "could occasionally occur when two writers race to insert entries" +
                             "in the same NEW collection.  This stack trace could provide useful" +
                             "debug info if this WARNing is followed by ERRORs", e);
                    // otherwise, do nothing - just means we lost the race!
                }
            }
        }
    }

    private void throwOptimisticConcurrencyException(String msg,
                                                     String workspace, String collection,
                                                     String entryId, Locale locale,
                                                     int revision
                                                     )
    {
        throwOptimisticConcurrencyException(msg, workspace, collection, entryId, locale, revision, null);
    }

    private void throwOptimisticConcurrencyException(String msg,
                                                     String workspace, String collection,
                                                     String entryId, Locale locale,
                                                     int revision,
                                                     Exception ee)
    {
        log.error(msg, ee);
        String editURI = (collection == null) ? null :
                         getURIHandler().constructURIString(workspace, collection, entryId, locale, revision);
        throw new OptimisticConcurrencyException(msg, ee, editURI);
    }
}
