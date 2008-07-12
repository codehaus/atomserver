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

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.util.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atomserver.*;
import org.atomserver.core.etc.AtomServerConstants;
import org.atomserver.core.etc.AtomServerPerformanceLog;
import org.atomserver.exceptions.AtomServerException;
import org.atomserver.exceptions.BadContentException;
import org.atomserver.exceptions.BadRequestException;
import org.atomserver.ext.batch.Operation;
import org.atomserver.uri.*;
import org.atomserver.utils.perf.AutomaticStopWatch;
import org.atomserver.utils.perf.StopWatch;
import org.atomserver.utils.xml.XML;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.Collection;

/**
 * The abstract, base AtomCollection implementation. Subclasses must implement several specific
 * methods for manipulating Entries; getEntries, getEntry, modifyEntry, and deleteEntry.
 * @author Chris Berry  (chriswberry at gmail.com)
 * @author Bryon Jacob (bryon at jacob.net)
 */
abstract public class AbstractAtomCollection implements AtomCollection {

    //--------------------------------
    //      abstract methods
    //--------------------------------
    /**
     * The getEntries() method on the AtomCollection API delegates to this method within the subclass
     * to do the real work. This method will, most likely, return only a "page" of results.
     * @param abdera
     * @param iri
     * @param feedTarget  The FeedTarget, which was decoded from the URI
     * @param ifModifiedSince  The ifModifiedSince Date (as a long) determined using either the Header or a Query param.
     * @param feed The Feed to which to add Entries
     * @return  The latest lastModified Date (as a long) for an Entry in this Feed. Used to set the <updated> element in the Feed
     * @throws AtomServerException
     */
    abstract protected long getEntries(Abdera abdera,
                                       IRI iri,
                                       FeedTarget feedTarget,
                                       long ifModifiedSince,
                                       Feed feed) throws AtomServerException;

    /**
     * The getEntry() method on the AtomCollection API delegates to this method within the subclass
     * to do the real work.
     * @param entryTarget The EntryTarget, which was decoded from the URI
     * @return The EntryMetaData associated with the requested Entry.
     * @throws AtomServerException
     */
    abstract protected EntryMetaData getEntry(EntryTarget entryTarget) throws AtomServerException;

    /**
     * The updateEntry() method on the AtomCollection API delegates to this method within the subclass
     * to do the real work.
     * @param internalId  The internalId of this Entry. In general, this is pre-selected from the database,
     * although this is not required. The method getInternalId() is used to obtain thisi value.
     * It is used to determine whether this is an insert or an update within modifyEntry()
     * @param entryTarget The EntryTarget, which was decoded from the URI
     * @param mustAlreadyExist Should this Entry already exist?
     * @return The EntryMetaData associated with the requested Entry.
     * @throws AtomServerException
     */
    abstract protected EntryMetaData modifyEntry(Object internalId,
                                                 EntryTarget entryTarget,
                                                 boolean mustAlreadyExist) throws AtomServerException;

    /**
     * The deleteEntry() method on the AtomCollection API delegates to this method within the subclass
     * to do the real work.
     * @param entryTarget The EntryTarget, which was decoded from the URI
     * @param setDeletedFlag  Should the deleted flag be set for this Entry?
     * @return The EntryMetaData associated with the requested Entry.
     * @throws AtomServerException
     */
    abstract protected EntryMetaData deleteEntry(EntryTarget entryTarget,
                                                 boolean setDeletedFlag) throws AtomServerException;

    // ----------
    //   statics
    // ----------
    static private final Log log = LogFactory.getLog(AbstractAtomCollection.class);

    // ------------
    //   instance
    // ------------
    protected CollectionOptions options = null;
    protected AtomWorkspace parentAtomWorkspace = null;
    protected String name = null;

    // -----------
    //  methods
    // -----------
    public AbstractAtomCollection( AtomWorkspace parentAtomWorkspace, String name ) {
        this.parentAtomWorkspace = parentAtomWorkspace;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void setParentAtomWorkspace(AtomWorkspace parentAtomWorkspace) {
        this.parentAtomWorkspace = parentAtomWorkspace;
    }

    /**
     * Return the AtomWorkspace to which this AtomCollection belongs
     * @return
     */
    public AtomWorkspace getParentAtomWorkspace() {
        return parentAtomWorkspace;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public ContentStorage getContentStorage() {
        if ( options != null && options.getContentStorage() != null ) {
            return options.getContentStorage();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultContentStorage();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentValidator getContentValidator()  {
        if ( options != null && options.getContentValidator() != null ) {
            return options.getContentValidator();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultContentValidator();
        }
    }





    /**
     * {@inheritDoc}
     */
    public CategoriesHandler getCategoriesHandler() {
        /*
        if ( options != null && options.getCategoriesHandler() != null ) {
            return options.getCategoriesHandler();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultCategoriesHandler();
        }
        */
        return parentAtomWorkspace.getParentAtomService().getCategoriesHandler();
    }



    /**
     * A convenience method to determine whether this is a Categories Workspace, which is a special
     * internal Workspace created to handle Category manipulation.
     * @return true or false
     */
    /*
    protected boolean isCategoriesWorkspace() {
        if ( log.isTraceEnabled() )
            log.trace("For workspace(" + parentAtomWorkspace.getName() +
                      ") isCategoriesWorkspace= " + parentAtomWorkspace.getOptions().isCategoriesWorkspace());
        return parentAtomWorkspace.getOptions().isCategoriesWorkspace();
    }
    */

    /**
     * A convenience method to determine the acual is a Workspace affliated with a Cateories Workspace,
     * which is a special internal Workspace created to handle Category manipulation.
     * @return The affliated Workspace name
     */
    /*
    protected String getCategoriesAffilliatedWorkspaceName() {
        return ((parentAtomWorkspace.getOptions().getAffiliatedAtomWorkspace() != null )
                ? parentAtomWorkspace.getOptions().getAffiliatedAtomWorkspace().getName()
                : null );
    }
    */




    //>>>>>>>>>>>>>>
    protected EntryTarget getEntryTarget(RequestContext request) {
        return getURIHandler().getEntryTarget(request, true);
    }

    protected boolean mustAlreadyExist() {
        return false;
    }

    protected boolean setDeletedFlag() {
        return true;
    }


    

    /**
     * {@inheritDoc}
     */
    public EntryAutoTagger getAutoTagger() {
        if ( options != null && options.getAutoTagger() != null ) {
            return options.getAutoTagger();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultAutoTagger();
        }
    }

    /**
     * Return the maximum number of "link" Entries to be returned in a page of results.
     * A "link" Entry is a an <entry> which contains a <link> which the Client may subsequently use to access the
     * actual Entry (which would contain the Entry's <content>)
     * @return The maximum number of "link" Entries to be returned in a page of results
     */
    protected int getMaxLinkEntriesPerPage() {
        if ( options != null && options.getMaxFullEntriesPerPage() != -1 ) {
            return options.getMaxLinkEntriesPerPage();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultMaxLinkEntriesPerPage();
        }
    }

    /**
     * Return the maximum number of "full" Entries to be returned in a page of results.
     * A "full" Entry is a an <entry> which contains the entire Entry, including it's <content>.
     * @return
     */
    protected int getMaxFullEntriesPerPage() {
        if ( options != null && options.getMaxFullEntriesPerPage() != -1 ) {
            return options.getMaxFullEntriesPerPage();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultMaxFullEntriesPerPage();
        }
    }

    /**
     * A convenience method to obtain the return the EntryIdGenerator wired into this AtomCollection
     * @return  The EntryIdGenerator
     */
    protected EntryIdGenerator getEntryIdGenerator() {
        if ( options != null && options.getEntryIdGenerator() != null ) {
            return options.getEntryIdGenerator();
        } else {
            return parentAtomWorkspace.getOptions().getDefaultEntryIdGenerator();
        }
    }

    /**
     * A convenience method to obtain the isLocalized property from the Options
     * @return Whether this Collection is Locale specific, which is reflected in it's URL structure, etc.
     */
    protected boolean isLocalized()  {
        // TODO: optionally use CollectionOptions
        return parentAtomWorkspace.getOptions().getDefaultLocalized();
    }

    /**
     * A convenience method to obtain the isVerboseDeletions property from the Options.
     * Verbose deletions return the <content> of the Entry, wrapped in a <delete> element
     * @return Whether this Collection
     */
    protected boolean isVerboseDeletions()  {
        // TODO: optionally use CollectionOptions
        return parentAtomWorkspace.getOptions().getDefaultVerboseDeletions();
    }

    /**
     * A convenience method to obtain the isProducingEntryCategoriesFeedElement property from the Options
     * @return Whether this Collection produces the <category> elements in the Feed
     */
    protected boolean isProducingEntryCategoriesFeedElement() {
        // TODO: optionally use CollectionOptions
        return parentAtomWorkspace.getOptions().getDefaultProducingEntryCategoriesFeedElement();
    }

    /**
     * A convenience method to obtain the isProducingTotalResultsFeedElement property from the Options
     * @return Whether this Collection produces teh <totalResults> element in the Feed
     */
    protected boolean isProducingTotalResultsFeedElement()  {
        // TODO: optionally use CollectionOptions
        return parentAtomWorkspace.getOptions().getDefaultProducingTotalResultsFeedElement();
    }

    /**
     * A convenience method to obtain the PerformanceLog from the AtomService which owns this AtomCollection
     * @return The PerformanceLog
     */
    public AtomServerPerformanceLog getPerformanceLog() {
        return ((AbstractAtomService)(parentAtomWorkspace.getParentAtomService())).getPerformanceLog();
    }

    /**
     * {@inheritDoc}
     */
    public CollectionOptions getOptions() {
        return options;
    }

    /**
     * {@inheritDoc}
     */
    public void setOptions(CollectionOptions options) {
        this.options = options;
    }

    /**
     * A convenience method to obtain the URIHandler from the AtomService which owns this AtomCollection
     * @return The URIHandler
     */
    protected URIHandler getURIHandler() {
        return parentAtomWorkspace.getParentAtomService().getURIHandler();
    }

    /**
     * A convenience method to pull the Service Base URI from the affliated AtomService.
     * @return The Service Base URI
     */
    protected String getServiceBaseUri() {
        return parentAtomWorkspace.getParentAtomService().getServiceBaseUri();
    }

    /**
     * A "batch method" which calls modifyEntry()
     * This method should be overriden whenever the concrete implementation can take advantage of batching to
     * do a better job, but this simple implementation will suffice for functional correctness, by simply iterating
     * over the batch and calling the one-at-a-time methods above.
     * @param request The Abdera RequestContext
     * @param entriesURIData The list of EntryURIData for this batch
     * @return A list of BatchEntryResults
     * @throws AtomServerException
     */
    protected java.util.Collection<BatchEntryResult> modifyEntries(final RequestContext request,
                                                                   final java.util.Collection<EntryTarget> entriesURIData)
            throws AtomServerException {
        return executeTransactionally(new TransactionalTask<Collection<BatchEntryResult>>() {
            public Collection<BatchEntryResult> execute() {
                java.util.Collection<BatchEntryResult> beans = new ArrayList<BatchEntryResult>();
                for (EntryTarget entryTarget : entriesURIData) {
                    try {
                        EntryMetaData entryMetaData = modifyEntry(null, entryTarget, false );
                        beans.add(new BatchEntryResult(entryTarget, entryMetaData));
                    } catch (Exception e) {
                        beans.add(new BatchEntryResult(entryTarget, e));
                    }
                }
                return beans;
            }
        });
    }

    /**
     * A "batch method" which calls deleteEntry()
     * This method should be overriden whenever the concrete implementation can take advantage of batching to
     * do a better job, but this simple implementation will suffice for functional correctness, by simply iterating
     * over the batch and calling the one-at-a-time methods above.
     * @param request The Abdera RequestContext
     * @param entriesURIData The list of EntryURIData for this batch
     * @return A list of BatchEntryResults
     * @throws AtomServerException
     */
   protected java.util.Collection<BatchEntryResult> deleteEntries(final RequestContext request,
                                                                  final java.util.Collection<EntryTarget> entriesURIData)
            throws AtomServerException {
       return executeTransactionally(new TransactionalTask<Collection<BatchEntryResult>>() {
           public Collection<BatchEntryResult> execute() {
               java.util.Collection<BatchEntryResult> beans = new ArrayList<BatchEntryResult>();
               for (EntryTarget entryTarget : entriesURIData) {
                   try {
                       EntryMetaData entryMetaData = deleteEntry(entryTarget, true);
                       beans.add(new BatchEntryResult(entryTarget, entryMetaData));
                   } catch (Exception e) {
                       beans.add(new BatchEntryResult(entryTarget, e));
                   }
               }
               return beans;

           }
       });
    }

    /**
     * {@inheritDoc}
     */

     // FIXME :: make it delegate to the associated VirtualWorkspace
     public java.util.Collection<org.apache.abdera.model.Category> listCategories( RequestContext request,
                                                                                  String workspace,
                                                                                  String collection ) {
         CategoriesHandler categoriesHandler= getCategoriesHandler();
         if ( categoriesHandler != null ) {
             return categoriesHandler.listCategories( workspace, collection  );
         }
         return null;
     }

     /**
      * {@inheritDoc}
      */
     public Feed getEntries(RequestContext request) throws AtomServerException {
         Abdera abdera = request.getServiceContext().getAbdera();
         FeedTarget feedTarget = getURIHandler().getFeedTarget(request);

         /*
         String workspace = feedTarget.getWorkspace();

         if ( isCategoriesWorkspace() ) {
             String msg = "Cannot ask for a Feed to a tags: workspace (" + request.getResolvedUri() + ")" ;
             log.warn( msg );

             String uri = request.getResolvedUri().toString();
             uri = uri.replaceAll( workspace, getCategoriesAffilliatedWorkspaceName() );

             throw new MovedPermanentlyException( msg, uri );
         }
         */

         long ifModifiedSince = getIfModifiedSince(feedTarget, request);

         Feed feed = AtomServer.getFactory( abdera ).newFeed();

         long lastModified = getEntries(request.getServiceContext().getAbdera(),
                                        request.getUri(),
                                        feedTarget,
                                        ifModifiedSince,
                                        feed);

         if (lastModified != 0L) {
             try {
                 String collection = feedTarget.getCollection();
                 feed.addAuthor("AtomServer APP Service");
                 feed.setTitle(collection + " entries");
                 feed.setUpdated(new java.util.Date(lastModified));
                 feed.setId("tag:atomserver.org,2008:v1:" + collection );

             } catch (IRISyntaxException e) {
                 throw new BadRequestException(e);
             }
             return feed;
         } else {
             // AtomServer will interpret null as "NOT MODIFIED"
             return null;
         }
     }

     /**
      * {@inheritDoc}
      */
     public Entry getEntry(RequestContext request) throws AtomServerException {
         Abdera abdera = request.getServiceContext().getAbdera();

         /*
         IRI iri = request.getUri();

         EntryTarget entryTarget = getURIHandler().getEntryTarget(request, true);
         String origWorkspace = entryTarget.getWorkspace();
         if ( isCategoriesWorkspace() ) {
             entryTarget = entryTarget.cloneWithNewWorkspace( getCategoriesAffilliatedWorkspaceName() );
         }
         */
         EntryTarget entryTarget = getEntryTarget( request );



         EntryMetaData entryMetaData = getEntry(entryTarget);

         long thisLastModified =
             (entryMetaData.getLastModifiedDate() != null) ? entryMetaData.getLastModifiedDate().getTime() : 0L;

         long ifModifiedSince = getIfModifiedSince(entryTarget, request);
         Entry entry = null;
         if (thisLastModified > ifModifiedSince) {

             /*
             if ( isCategoriesWorkspace() ) {
                 entryMetaData.setWorkspace( origWorkspace );
             }
             */

             EntryType entryType =
                 (entryTarget.getEntryTypeParam() != null) ? entryTarget.getEntryTypeParam() : EntryType.full;
             entry =  newEntry( abdera, entryMetaData, entryType );
         }
         return entry;
     }

    /**
     * {@inheritDoc}
     */
     public UpdateCreateOrDeleteEntry.CreateOrUpdateEntry updateEntry(final RequestContext request) throws AtomServerException {
         Abdera abdera = request.getServiceContext().getAbdera();

         //IRI iri = request.getUri();

         final EntryTarget entryTarget = getURIHandler().getEntryTarget(request, false);

         //final String workspace = entryTarget.getWorkspace();
         String collection = entryTarget.getCollection();

         ensureCollectionExists(collection);

         Entry entry = parseEntry(entryTarget, request );
         final String entryXml = validateAndPreprocessEntryContents(entry, entryTarget);

         EntryMetaData entryMetaData = executeTransactionally(
                 new TransactionalTask<EntryMetaData>() {
                     public EntryMetaData execute() {

                         /*
                         EntryTarget target = entryTarget;
                         boolean mustAlreadyExist = false;
                         if (isCategoriesWorkspace()) {
                             target = target.cloneWithNewWorkspace(
                                     getCategoriesAffilliatedWorkspaceName());
                             mustAlreadyExist = true;
                         }
                         */
                         final EntryTarget target = getEntryTarget( request );

                         
                         // determine if we are creating the entryId -- i.e. if this was a POST
                         if ( EntryTarget.UNASSIGNED_ID.equals(target.getEntryId()) ) {
                             if ( getEntryIdGenerator() == null ) {
                                 throw new AtomServerException( "No EntryIdGenerator was wired into the Collection (" +
                                                                 target.toString() + ")");
                             } else {
                                 target.setEntryId( getEntryIdGenerator().generateId() );
                             }
                         }

                         final Object internalId = getInternalId(target);
                         EntryMetaData metaData = modifyEntry(internalId,
                                                              target,
                                                              mustAlreadyExist() );
                         /*
                         EntryMetaData metaData = modifyEntry(internalId,
                                                              target,
                                                              mustAlreadyExist);
                                                              */

                         // Copy the new file contents into the File
                         //  do this as late as possible -- when we're completely sure that it has all passed
                         if (log.isTraceEnabled()) {
                             log.trace("ContentStorage = " + getContentStorage());
                         }
                         getContentStorage().putContent(entryXml, metaData);
                         postProcessEntryContents(entryXml, metaData);

                         return metaData;
                     }
                 }
         );

         // For Create and Update, we always, by definition, return "full" Entries
         entryMetaData.setWorkspace(entryTarget.getWorkspace());
         entry = newEntry( abdera, entryMetaData, EntryType.full );

         return new UpdateCreateOrDeleteEntry.CreateOrUpdateEntry(entry, entryMetaData.isNewlyCreated());
     }

    /**
     * {@inheritDoc}
     * Subclasses should override this method. This implementation does nothing.
     */
     public void ensureCollectionExists(String collection) {
        // do nothing.
     }

    /**
     * Return the internal Id for the Entry identified by this EntryTarget
     * In general, subclasses don't have to support internal ids. This implementation simply returns -1.
     * @param entryTarget The EntryTarget, decoded from the Request URI
     * @return The internal Id
     */
     protected Object getInternalId(EntryTarget entryTarget) {
        return -1;
     }

    /**
     * {@inheritDoc}
     */
     public java.util.Collection<UpdateCreateOrDeleteEntry> updateEntries(final RequestContext request)
             throws AtomServerException {

         Document<Feed> document;
         try {
             document = request.getDocument();
         } catch (IOException e) {
             throw new AtomServerException(e);
         }

         if (document.getRoot().getEntries().size() > getMaxFullEntriesPerPage()) {
             throw new BadRequestException(
                     MessageFormat.format("too many entries ({0}) in batch - max is {1}",
                                          document.getRoot().getEntries().size(),
                                          getMaxFullEntriesPerPage()));
         }

         final List<EntryTarget> entriesToUpdate = new ArrayList<EntryTarget>();
         final List<EntryTarget> entriesToDelete = new ArrayList<EntryTarget>();
         final EntryMap<String> entryXmlMap = new EntryMap<String>();
         final HashMap<EntryTarget, Integer> orderMap = new HashMap<EntryTarget, Integer>();

         Operation defaultOperationExtension = document.getRoot().getExtension(AtomServerConstants.OPERATION);
         String defaultOperation = defaultOperationExtension == null ? "update" : defaultOperationExtension.getType();

         List<Entry> entries = document.getRoot().getEntries();

         UpdateCreateOrDeleteEntry[] updateEntries = new UpdateCreateOrDeleteEntry[entries.size()];
         Set<RelaxedEntryTarget> relaxedEntryTargetSet = new HashSet<RelaxedEntryTarget>();

         int order = 0;
         for (Entry entry : entries) {
             try {
                 IRI baseIri = new IRI(getServiceBaseUri());
                 IRI iri = baseIri.relativize(entry.getLink("edit").getHref());
                 EntryTarget entryTarget = null;
                 try {
                     // The request is always as PUT, so we will get back a FeedTarget when we want an insert
                     URITarget uriTarget = getURIHandler().parseIRI(request, iri);
                     if (uriTarget instanceof FeedTarget) {
                         entryTarget = new EntryTarget((FeedTarget) uriTarget);

                         // determine if we are creating the entryId -- i.e. if this was a POST
                         if (getEntryIdGenerator() == null) {
                             throw new AtomServerException("No EntryIdGenerator was wired into the Collection (" +
                                                           entryTarget.toString() + ")");
                         } else {
                             entryTarget.setEntryId(getEntryIdGenerator().generateId());
                         }

                     } else {
                         entryTarget = (EntryTarget) uriTarget;
                     }
                 } catch (Exception e) {
                     throw new BadRequestException("Bad request URI: " + iri, e);
                 }
                 if (entryTarget == null) {
                     throw new BadRequestException("Bad request URI: " + iri);
                 }

                 String collection = entryTarget.getCollection();
                 ensureCollectionExists(collection);

                 // Verify that we do not have multiple <operation> elements
                 List<Operation> operationExtensions = entry.getExtensions(AtomServerConstants.OPERATION);

                 if (operationExtensions != null && operationExtensions.size() > 1) {
                     throw new BadRequestException("Multiple operations applied to one entry");
                 }

                 // Set to the default operation if none is set.
                 String operation = operationExtensions == null || operationExtensions.isEmpty() ?
                                    defaultOperation :
                                    operationExtensions.get(0).getType();
                 if (log.isDebugEnabled()) {
                     log.debug("operation : " + operation);
                 }

                 // We do not allow an Entry to occur twice in the batch.
                 //   NOTE: the first one wins !!
                 RelaxedEntryTarget relaxedEntryTarget = new RelaxedEntryTarget(entryTarget);
                 if ( relaxedEntryTargetSet.contains( relaxedEntryTarget ) ) {
                     throw new BadRequestException("You may not include the same Entry twice ("
                                                   + entryTarget + ").");
                 } else {
                     relaxedEntryTargetSet.add( relaxedEntryTarget );
                 }

                 // Add to the processing lists.
                 if ("delete".equalsIgnoreCase(operation)) {
                     entriesToDelete.add(entryTarget);
                     orderMap.put( entryTarget, order);
                 } else if ("update".equalsIgnoreCase(operation) || "insert".equalsIgnoreCase(operation)) {
                     String entryXml = validateAndPreprocessEntryContents(entry, entryTarget);
                     entriesToUpdate.add(entryTarget);
                     entryXmlMap.put(entryTarget, entryXml);
                     orderMap.put( entryTarget, order);
                 }
             } catch (AtomServerException e) {
                 UpdateCreateOrDeleteEntry.CreateOrUpdateEntry updateEntry =
                         new UpdateCreateOrDeleteEntry.CreateOrUpdateEntry(entry, false);
                 updateEntry.setException(e);
                 updateEntries[order] = updateEntry;
             }
             order++;
         }

         Abdera abdera = request.getServiceContext().getAbdera();

         // ---------------- process updates ------------------
         if (!entriesToUpdate.isEmpty()) {
             java.util.Collection<BatchEntryResult> results =
                     executeTransactionally(
                             new TransactionalTask<java.util.Collection<BatchEntryResult>>() {
                                 public Collection<BatchEntryResult> execute() {
                                     java.util.Collection<BatchEntryResult> results =
                                             modifyEntries(request, entriesToUpdate);
                                     for (BatchEntryResult result : results) {
                                         if (result.getException() == null) {
                                             String entryXml = entryXmlMap.get(result.getEntryTarget());
                                             getContentStorage().putContent(entryXml,
                                                                            result.getMetaData());
                                         }
                                         if (result.getMetaData() != null) {
                                             postProcessEntryContents(entryXmlMap.get(result.getMetaData()),
                                                                      result.getMetaData());
                                         }
                                     }
                                     return results;
                                 }
                             });

             for (BatchEntryResult result : results) {
                 EntryMetaData metaData = result.getMetaData();
                 if (metaData == null) {
                     EntryTarget target = result.getEntryTarget().cloneWithNewRevision(URIHandler.REVISION_OVERRIDE);
                     try {
                         metaData = getEntry(target);
                     } catch (AtomServerException e) {
                         metaData = null;
                     }
                 }
                 Entry entry = metaData == null ?
                     newEntryWithCommonContentOnly(abdera, result.getEntryTarget()) :
                     newEntry(abdera, metaData, EntryType.full);

                 UpdateCreateOrDeleteEntry.CreateOrUpdateEntry updateEntry =
                         new UpdateCreateOrDeleteEntry.CreateOrUpdateEntry( entry,
                                                                            metaData != null && metaData.isNewlyCreated());
                 if (result.getException() != null) {
                     updateEntry.setException(result.getException());
                 }

                 Integer listOrder = orderMap.get( result.getEntryTarget() );
                 if ( listOrder == null ) {
                     // This should never happen....
                     String msg = "Could not map (" + result.getEntryTarget() + ") in Batch Order Map";
                     log.error( msg );
                     throw new AtomServerException( msg );
                 }
                 updateEntries[listOrder] = updateEntry;
             }
         }

         // ---------------- process deletes ------------------
         if (!entriesToDelete.isEmpty()) {
             java.util.Collection<BatchEntryResult> results =
                     executeTransactionally(new TransactionalTask<Collection<BatchEntryResult>>() {
                         public Collection<BatchEntryResult> execute() {
                             java.util.Collection<BatchEntryResult> results =
                                     deleteEntries(request, entriesToDelete);
                             for (BatchEntryResult result : results) {
                                 if (result.getException() == null) {
                                     EntryMetaData entryMetaDataClone = (EntryMetaData)(result.getMetaData().clone());
                                     int currentRevision = result.getMetaData().getRevision();
                                     entryMetaDataClone.setRevision( (currentRevision - 1) );
                                     String deletedEntryXml = createDeletedEntryXML(entryMetaDataClone);

                                     getContentStorage().deleteContent(deletedEntryXml,
                                                                       result.getMetaData());
                                 }
                             }
                             return results;
                         }
                     });

             for (BatchEntryResult result : results) {
                 // TODO: WRONG!
                 EntryMetaData metaData = result.getMetaData();
                 UpdateCreateOrDeleteEntry.DeleteEntry deleteEntry = null;
                 if (metaData == null) {
                     Factory factory = AtomServer.getFactory(abdera);

                     Entry entry = factory.newEntry();
                     String workspace = result.getEntryTarget().getWorkspace();
                     String collection = result.getEntryTarget().getCollection();
                     String entryId = result.getEntryTarget().getEntryId();
                     Locale locale = result.getEntryTarget().getLocale();
                     String fileURI = getURIHandler().constructURIString(workspace, collection, entryId, locale);
                     setEntryId(factory, entry, fileURI);

                     setEntryTitle(factory, entry,
                                   isLocalized() ?
                                     (" Entry: " + collection + " " + entryId + "." + locale) :
                                     (" Entry: " + collection + " " + entryId));

                     addAuthorToEntry(factory, entry, "AtomServer APP Service");

                     addLinkToEntry(factory, entry, fileURI, "self");

                     String editURL = fileURI + "/" + result.getEntryTarget().getRevision();
                     addLinkToEntry(factory, entry, editURL, "edit");

                     deleteEntry = new UpdateCreateOrDeleteEntry.DeleteEntry(entry);
                 } else {
                     deleteEntry = new UpdateCreateOrDeleteEntry.DeleteEntry( newEntry(abdera, metaData, EntryType.full) );
                 }
                 if (result.getException() != null) {
                     deleteEntry.setException(result.getException());
                 }

                 Integer listOrder = orderMap.get( result.getEntryTarget() );
                 if ( listOrder == null ) {
                     // This should never happen....
                     String msg = "Could not map (" + result.getEntryTarget() + ") in Batch Order Map";
                     log.error( msg );
                     throw new AtomServerException( msg );
                 }
                 updateEntries[listOrder] = deleteEntry;
             }
         }

         // Clear the maps to help out the Garbage Collector
         entryXmlMap.clear();
         entriesToUpdate.clear();
         entriesToDelete.clear();
         orderMap.clear();
         relaxedEntryTargetSet.clear();

         return Arrays.asList( updateEntries );
     }

    /**
     * {@inheritDoc}
     */
    public Entry deleteEntry(final RequestContext request) throws AtomServerException {
        Abdera abdera = request.getServiceContext().getAbdera();

        /*
        final EntryTarget originalEntryTarget = getURIHandler().getEntryTarget(request, true);
        final EntryTarget entryTarget = isCategoriesWorkspace() ?
                                        originalEntryTarget.cloneWithNewWorkspace(
                                                getCategoriesAffilliatedWorkspaceName()) :
                                        originalEntryTarget;
                                        */
        final EntryTarget entryTarget = getEntryTarget( request );


        EntryMetaData entryMetaData = executeTransactionally(
                new TransactionalTask<EntryMetaData>() {
                    public EntryMetaData execute() {

                        /*
                        EntryMetaData entryMetaData =
                                deleteEntry(entryTarget, !isCategoriesWorkspace() );
                                */
                        EntryMetaData entryMetaData =
                                deleteEntry(entryTarget, setDeletedFlag() );

                        // Replace the XML file with a "deleted file"
                        //  we wait to do this now that we know that the delete was successfull

                        EntryMetaData entryMetaDataClone = (EntryMetaData)(entryMetaData.clone());
                        int currentRevision = entryMetaData.getRevision();
                        entryMetaDataClone.setRevision( (currentRevision - 1) );

                        getContentStorage().deleteContent( createDeletedEntryXML(entryMetaDataClone),
                                                           entryMetaData );

                        return entryMetaData;
                    }
                }
        );

        return (entryMetaData == null) ?
               null :
               newEntry( abdera, entryMetaData, EntryType.link );
     }

    //~~~~~~~~~~~~~~~~~~~~~~
     private void postProcessEntryContents(String entryXml, EntryMetaData entryMetaData) {
         EntryAutoTagger autoTagger = getAutoTagger();
         if (autoTagger != null) {
             StopWatch stopWatch = new AutomaticStopWatch();
             try {
                 autoTagger.tag(entryMetaData, entryXml);
             } finally {
                 if ( getPerformanceLog() != null ) {
                     getPerformanceLog().log( "XML.autoTagger", getPerformanceLog().getPerfLogEntryString(entryMetaData),
                                              stopWatch );
                 }
             }
         }
     }

     //~~~~~~~~~~~~~~~~~~~~~~
     protected String validateAndPreprocessEntryContents(Entry entry, EntryTarget entryTarget)
             throws BadContentException {
         // Let's validate upfront so we can fail-fast, so grab the entryXml
         String workspace = entryTarget.getWorkspace();
         String collection = entryTarget.getCollection();
         Locale locale = entryTarget.getLocale();
         String entryId = entryTarget.getEntryId();
         int revision = entryTarget.getRevision();
         String entryXml = null;
         try {
             entryXml = entry.getContent();
         } catch ( Exception ee ) {
             String msg = "Could not process PUT for [" + workspace + ", " + collection + ", "
                 + locale + ", " + entryId + ", " + revision + "]\n Reason:: " + ee.getMessage()
                 + "\n 1) MAKE CERTAIN THAT YOU HAVE A NAMESPACE ON THE <entry> ELEMENT!"
                 + "\n (i.e. <entry xmlns=\"http://www.w3.org/2005/Atom\">)"
                 + "\n 2) MAKE CERTAIN THAT YOU ARE INDEED SENDING UTF-8 CHARACTERS" ;
             log.error( msg, ee );
             throw new BadContentException( msg, ee ) ;
         }
         if ( entryXml == null ) {
             String msg = "Could not process PUT for [" + workspace + ", " + collection + ", "
                 + locale + ", " + entryId + ", " + revision + "]\n Reason:: Content is NULL";
             log.error( msg );
             throw new BadContentException( msg );
         }

         // now validate the <content> with whatever Validator was registered (if any)
         ContentValidator validator = getContentValidator();
         if ( validator != null ) {
             StopWatch stopWatch = new AutomaticStopWatch();
             try {
                 validator.validate( entryXml );
             } finally {
                 if ( getPerformanceLog() != null ) {
                     getPerformanceLog().log( "XML.validator", getPerformanceLog().getPerfLogEntryString(entryTarget), stopWatch );
                 }
             }
         }

         return entryXml;
     }

     //~~~~~~~~~~~~~~~~~~~~~~
     private Entry parseEntry( EntryTarget entryTarget, RequestContext request ) {
         String errMsgPrefix = "Could not process PUT for [" + entryTarget.getWorkspace()
             + ", " + entryTarget.getCollection() + ", " + entryTarget.getLocale() +
             ", " + entryTarget.getEntryId() + ", " + entryTarget.getRevision();
         String errMsgPostfix =  "\n MAKE CERTAIN THAT YOU ARE INDEED SENDING VALID XML";

         Entry entry = null;
         try {
             Document<Entry> document = request.getDocument();
             entry = document.getRoot();
         } catch ( java.lang.ClassCastException ee ) {
             String msg = errMsgPrefix +
                 "]\n Reason:: Could not parse a valid <entry> from the Request provided. " + ee.getMessage()
                 + "\n 1) MAKE CERTAIN THAT YOU HAVE A NAMESPACE ON THE <entry> ELEMENT!"
                 + "\n (i.e. <entry xmlns=\"http://www.w3.org/2005/Atom\">)" +  errMsgPostfix ;
             log.error( msg, ee );
             throw new BadContentException( msg, ee ) ;
         } catch ( java.lang.ArrayIndexOutOfBoundsException ee ) {
             String msg = errMsgPrefix +
                 "]\n Reason:: MOST LIKELY THE <content> IS EMPTY. " + ee.getMessage() + errMsgPostfix ;
             log.error( msg, ee );
             throw new BadContentException( msg, ee ) ;
         } catch ( org.apache.abdera.parser.ParseException ee ) {
             String msg = errMsgPrefix +
                 "]\n Reason:: The <content> XML could not be parsed. " + ee.getMessage() +
                 "\n If this was caused by an ArrayIndexOutOfBoundsException.  MOST LIKELY THE <content> IS EMPTY " +
                 errMsgPostfix ;
             log.error( msg, ee );
             throw new BadContentException( msg, ee ) ;
         } catch ( Exception ee ) {
             String msg = errMsgPrefix +
                 "]\n Reason:: UNKNOWN EXCEPTION THROWN while parsing the <entry>" + ee.getMessage() + errMsgPostfix ;
             log.error( msg, ee );
             throw new BadContentException( msg, ee ) ;
         }
         if ( entry == null ) {
             String msg = errMsgPrefix +
                 "]\n Reason:: Content is NULL. Is the <content> element missing? ";
             log.error( msg );
             throw new BadContentException( msg );
         }
         return entry;
     }

     //~~~~~~~~~~~~~~~~~~~~~~
     protected long getIfModifiedSince(URITarget uriTarget,
                                       RequestContext request)
             throws BadRequestException {

         java.util.Date ifModifiedSinceDate = uriTarget.getUpdatedMinParam();

         // The URL Query param takes precedence, if it is provided
         if (ifModifiedSinceDate == null) {
             ifModifiedSinceDate = request.getIfModifiedSince();
         }
         return (ifModifiedSinceDate == null) ? 0L : ifModifiedSinceDate.getTime();
     }

    
    //~~~~~~~~~~~~~~~~~~~~~~
    protected Entry newEntry(Abdera abdera, EntryMetaData entryMetaData, EntryType entryType)
        throws AtomServerException {

        StopWatch stopWatch = new AutomaticStopWatch();
        try {
            Entry entry = newEntryWithCommonContentOnly(abdera, entryMetaData);

            java.util.Date updated = entryMetaData.getLastModifiedDate();
            java.util.Date published = entryMetaData.getPublishedDate();

            entry.setUpdated( updated );
            if ( published != null )
                entry.setPublished( published );

            String workspace = entryMetaData.getWorkspace();
            String collection = entryMetaData.getCollection();
            String entryId = entryMetaData.getEntryId();
            java.util.Locale locale = entryMetaData.getLocale();

            String fileURI = getURIHandler().constructURIString(workspace, collection, entryId, locale);

            addCategoriesToEntry( entry, entryMetaData, abdera);

            if ( entryType == EntryType.full ) {
                addFullEntryContent(abdera, entryMetaData, entry );
            } else if ( entryType == EntryType.link ) {
                addLinkToEntry(AtomServer.getFactory(abdera), entry, fileURI, "alternate");
            } else {
                throw new AtomServerException( "Must define the EntryType -- full or link" );
            }

            return entry;
        } finally {
            if ( getPerformanceLog() != null ) {
                getPerformanceLog().log( "XML.fine.entry", getPerformanceLog().getPerfLogEntryString(entryMetaData), stopWatch );
            }
        }
    }

    /*
     //~~~~~~~~~~~~~~~~~~~~~~
     protected Entry newEntry(Abdera abdera, EntryMetaData entryMetaData, EntryType entryType)
         throws AtomServerException {

         StopWatch stopWatch = new AutomaticStopWatch();
         try {

             //>>>>>>>>>>>>>>>>>>>>>>>
             // we reset this here for the virtual workspaces, which is much cleaner than reproducing newEntry()
             log.debug("&&&&&&&&&&&&&&&&&&&&&&&&&& " + parentAtomWorkspace.getName() );
             entryMetaData.setWorkspace( parentAtomWorkspace.getName() );



             Entry entry = newEntryWithCommonContentOnly(abdera, entryMetaData);

             java.util.Date updated = entryMetaData.getLastModifiedDate();
             java.util.Date published = entryMetaData.getPublishedDate();

             entry.setUpdated( updated );
             if ( published != null )
                 entry.setPublished( published );

             String workspace = entryMetaData.getWorkspace();
             String collection = entryMetaData.getCollection();
             String entryId = entryMetaData.getEntryId();
             java.util.Locale locale = entryMetaData.getLocale();

             String fileURI = getURIHandler().constructURIString(workspace, collection, entryId, locale);


             //>>>>>>>>>>>>>>>>>>>
             //if ( ! isCategoriesWorkspace() ) {
             //    addCategoriesToEntry( entry, entryMetaData, abdera);
             //}
             addCategoriesToEntry( entry, entryMetaData, abdera);



             if ( entryType == EntryType.full ) {
                 addFullEntryContent(abdera, entryMetaData, entry );
             } else if ( entryType == EntryType.link ) {
                 addLinkToEntry(AtomServer.getFactory(abdera), entry, fileURI, "alternate");
             } else {
                 throw new AtomServerException( "Must define the EntryType -- full or link" );
             }

             return entry;
         } finally {
             if ( getPerformanceLog() != null ) {
                 getPerformanceLog().log( "XML.fine.entry", getPerformanceLog().getPerfLogEntryString(entryMetaData), stopWatch );
             }
         }
     }
     */

     //~~~~~~~~~~~~~~~~~~~~~~
     protected Entry newEntryWithCommonContentOnly(Abdera abdera, EntryDescriptor entryMetaData)
         throws AtomServerException {

         if (log.isTraceEnabled()) {
             log.trace("RETURNING ENTRY:: " + entryMetaData);
         }

         String workspace = entryMetaData.getWorkspace();
         String collection = entryMetaData.getCollection();
         String entryId = entryMetaData.getEntryId();
         java.util.Locale locale = entryMetaData.getLocale();

         int revision = entryMetaData.getRevision();

         Factory factory = AtomServer.getFactory( abdera );
         Entry entry = factory.newEntry();

         String fileURI = getURIHandler().constructURIString(workspace, collection, entryId, locale);
         entry.setId( fileURI );

         entry.setTitle( isLocalized() ?
                         (" Entry: " + collection + " " + entryId + "." + locale) :
                         (" Entry: " + collection + " " + entryId));
         entry.addAuthor("AtomServer APP Service");
         addLinkToEntry(factory, entry, fileURI, "self");

         addEditLink(revision, factory, entry, fileURI);

         entry.addSimpleExtension(AtomServerConstants.ENTRY_ID, entryId);

         return entry;
     }

    protected void addEditLink(int revision, Factory factory, Entry entry, String fileURI) {
        String editURL = (revision != URIHandler.REVISION_OVERRIDE) ? (fileURI + "/" + revision) : fileURI;
        addLinkToEntry(factory, entry, editURL, "edit");
    }

    //~~~~~~~~~~~~~~~~~~~~~~
     protected void addFullEntryContent(Abdera abdera, EntryDescriptor entryMetaData, Entry entry) {
         ContentStorage contentStorage = getContentStorage();

         String xml = contentStorage.getContent( entryMetaData );
         if (xml == null) {
             throw new AtomServerException("Could not read entry (" + entryMetaData + ")");
         }
         xml = xml.replaceFirst("<[?].*[?]>", "" );

         //Note that we cannot just send a FileInputStream because we must strip out the XML declaration
         entry.setContent( xml, org.apache.abdera.model.Content.Type.XML );
     }

     //~~~~~~~~~~~~~~~~~~~~~~
     // Add Categories to the Entry, if a CategoriesHandler has been registered
     protected void addCategoriesToEntry(Entry entry, EntryMetaData entryMetaData, Abdera abdera) {
         if ( (entryMetaData.getCategories() != null) && (entryMetaData.getCategories().size() > 0) ) {
             Collections.sort(entryMetaData.getCategories(),
                              new Comparator<EntryCategory>() {
                                  public int compare(EntryCategory a, EntryCategory b) {
                                      // compare first by scheme, then by term
                                      int schemeCompare = a.getScheme().compareTo(b.getScheme());
                                      return schemeCompare == 0 ?
                                             a.getTerm().compareTo(b.getTerm()) :
                                             schemeCompare;
                                  }
                              });
             for (EntryCategory entryCategory : entryMetaData.getCategories()) {

                 if ( (entryCategory.getScheme() == null) && (entryCategory.getTerm() == null) ) {
                     if ( log.isTraceEnabled() ) {
                         log.trace( "WARNING:: empty Category encountered" );
                     }
                     continue;
                 }

//  Ideally, we would call the following method on our entry here:
//
//                 entry.addCategory(entryCategory.getScheme(),
//                                   entryCategory.getTerm(),
//                                   entryCategory.getLabel());
//
//  but, there is a memory leak in Abdera related to the way it caches elements...  I believe that
//  in 0.4.0, they have applied our patch to rectify the problem, so once we upgrade to the newest
//  version, we should revert to the simpler code above (which is functionally equivalent).
//      -Bryon
//
                 org.apache.abdera.model.Category category =
                         AtomServer.getFactory(abdera).newCategory();
                 if (entryCategory.getScheme() != null) {
                     category.setAttributeValue(Constants.SCHEME,
                                                entryCategory.getScheme());
                 } else {
                     category.removeAttribute(Constants.SCHEME);
                 }
                 category.setTerm(entryCategory.getTerm());
                 category.setLabel(entryCategory.getLabel());
                 entry.addCategory(category);
             }
         }
     }

    /**
     *
     * <pre>
     * <deletion xmlns="http://schemas.atomserver.org/atomserver/v1/rev0"
     *         collection="acme" id="12345" locale="en" workspace="widgets">
     *    [.... content ....]
     * </deletion>
     * </pre>
     */
    private String createDeletedEntryXML(EntryDescriptor descriptor) {

        XML xml = XML.element( "deletion", AtomServerConstants.SCHEMAS_NAMESPACE )
                  .attr( "collection", descriptor.getCollection() )
                  .attr( "id", descriptor.getEntryId() );

        if (descriptor.getLocale() != null) {
            xml.attr( "locale", descriptor.getLocale().toString() );
        }

        xml.attr( "workspace", descriptor.getWorkspace() );

        if ( isVerboseDeletions()) {
            String content = getContentStorage().getContent(descriptor);
            if (log.isTraceEnabled())
                log.trace( "content= " + content );

            if ( content != null )
                content = content.replaceFirst("<[?].*[?]>", "" );

            xml.add( content );
        }

        return xml.toString();
    }



    protected interface TransactionalTask<T> {
        T execute();
    }

    protected <T> T executeTransactionally(TransactionalTask<T> task) {
        // by default, this method doesn't do a thing differently - it simply executes the task
        // as given.  subclasses should override this method to ensure transactionality
        return task.execute();
    }

// The following methods are a workaround to a memory leak in abdera - we should remove these as
// soon as we can update to the trunk of abdera and get their fix.
//
// <workaround>
    protected void setEntryId(Factory factory, Entry entry, String id) {
        if (entry.getIdElement() == null) {
            factory.newID(entry).setValue(id);
        } else {
            entry.getIdElement().setValue(id);
        }
    }

    protected void setEntryTitle(Factory factory, Entry entry, String title) {
        factory.newTitle(entry).setText(title);
    }

    protected void addAuthorToEntry(Factory factory, Entry entry, String name) {
        Person author = factory.newAuthor(entry);
        factory.newName(author).setText(name);
    }

    protected void addLinkToEntry(Factory factory, Element entry, String href, String rel) {
        Link link = factory.newLink(entry);
        link.setAttributeValue(Constants.HREF, href);
        link.setRel(rel);
    }
// </workaround>
}
